package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.AllItemFrequencies
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.FoodAlias
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.ImageCacheRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.MealRepository
import com.emilflach.lokcal.ui.dialogs.StealImageItem
import com.emilflach.lokcal.ui.util.EntityImageData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class FoodEditViewModel(
    private val repo: FoodRepository,
    private val intakeRepo: IntakeRepository,
    private val mealRepo: MealRepository,
    private val imageCacheRepo: ImageCacheRepository? = null,
) {
    // Manage/list state
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

    private val _itemFrequencies = MutableStateFlow<Map<Pair<String, Long>, Long>>(emptyMap())
    val itemFrequencies: StateFlow<Map<Pair<String, Long>, Long>> = _itemFrequencies.asStateFlow()

    private val _filterMissingImages = MutableStateFlow(false)
    val filterMissingImages: StateFlow<Boolean> = _filterMissingImages.asStateFlow()

    private val _cachedImageFoodIds = MutableStateFlow<Set<Long>>(emptySet())
    val cachedImageFoodIds: StateFlow<Set<Long>> = _cachedImageFoodIds.asStateFlow()

    // Edit state
    data class EditState(
        val id: Long? = null,
        val isEdit: Boolean = false,
        val name: String = "",
        val energyText: String = "0",
        val servingSize: String = "",
        val productUrl: String = "",
        val imageUrl: String = "",
        val gtin13: String = "",
        val source: String = "manual",
        val aliases: List<FoodAlias> = emptyList(),
        // Import dialog state
        val showUrlDialog: Boolean = false,
        val urlInput: String = "",
        val isImporting: Boolean = false,
        val importError: String? = null,
        // Steal image state
        val showStealDialog: Boolean = false,
        val stealSearchQuery: String = "",
        val stealResults: List<StealImageItem> = emptyList(),
    )

    private val _edit = MutableStateFlow(EditState())
    val edit: StateFlow<EditState> = _edit.asStateFlow()

    private val _listState = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val listState = _listState.asStateFlow()

    fun saveListState(index: Int, offset: Int) {
        _listState.value = mapOf(index to offset)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    init {
        // initial load
        reloadFoods()
        loadFrequencies()
        loadCachedImageIds()
    }

    fun refresh() {
        reloadFoods()
        loadFrequencies()
        loadCachedImageIds()
    }

    private fun loadCachedImageIds() {
        if (imageCacheRepo == null) return
        scope.launch {
            _cachedImageFoodIds.value = imageCacheRepo.getCachedIdsByType(EntityImageData.FOOD)
        }
    }

    fun loadFrequencies() {
        scope.launch {
            val freqs = intakeRepo.getAllItemFrequencies()
            _itemFrequencies.value = freqs.associate { item: AllItemFrequencies ->
                val id = when (item.source_type) {
                    "FOOD" -> item.source_food_id
                    "MEAL" -> item.source_meal_id
                    else -> null
                }
                (item.source_type to (id ?: -1L)) to item.frequency
            }
        }
    }

    fun toggleMissingImagesFilter() {
        _filterMissingImages.value = !_filterMissingImages.value
    }

    fun setSearch(value: String) {
        _search.value = value
        reloadFoods()
    }

    fun reloadFoods() {
        searchJob?.cancel()
        searchJob = scope.launch {
            val q = _search.value.trim()
            val result = if (q.isBlank()) {
                repo.getAll().sortedBy { it.name.lowercase() }
            } else {
                repo.search(q)
            }
            _foods.value = result
        }
    }

    fun startEditing(foodId: Long?) {
        scope.launch {
            if (foodId == null) {
                _edit.value = EditState(id = null, isEdit = false, energyText = "0", source = "manual")
            } else {
                val f = repo.getById(foodId)
                if (f != null) {
                    val aliases = repo.getAliases(f.id)
                    _edit.value = EditState(
                        id = f.id,
                        isEdit = true,
                        name = f.name,
                        energyText = (f.energy_kcal_per_100g).roundToInt().toString(),
                        servingSize = f.serving_size?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: "",
                        productUrl = f.product_url ?: "",
                        imageUrl = f.image_url ?: "",
                        gtin13 = f.gtin13 ?: "",
                        source = f.source ?: "",
                        aliases = aliases
                    )
                } else {
                    _edit.value = EditState()
                }
            }
        }
    }

    fun updateName(v: String) { _edit.value = _edit.value.copy(name = v); persist() }
    fun updateEnergyText(v: String) { _edit.value = _edit.value.copy(energyText = v.filter { it.isDigit() || it == '.' || it == ',' }); persist() }
    fun updateServingSize(v: String) { _edit.value = _edit.value.copy(servingSize = v.filter { it.isDigit() || it == '.' }); persist() }
    fun updateProductUrl(v: String) { _edit.value = _edit.value.copy(productUrl = v); persist() }
    fun updateImageUrl(v: String) { _edit.value = _edit.value.copy(imageUrl = v); persist() }
    fun updateGtin13(v: String) { _edit.value = _edit.value.copy(gtin13 = v.filter { it.isDigit() }); persist() }
    fun updateSource(v: String) { _edit.value = _edit.value.copy(source = v); persist() }

    private fun persist() {
        val s = _edit.value
        val name = s.name.trim()
        val energy = s.energyText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        scope.launch {
            if (s.isEdit && s.id != null) {
                repo.updateDetails(
                    id = s.id,
                    name = name.ifBlank { "Unnamed" },
                    energyKcalPer100g = energy,
                    productUrl = s.productUrl.trim().ifBlank { null },
                    imageUrl = s.imageUrl.trim().ifBlank { null },
                    gtin13 = s.gtin13.trim().ifBlank { null },
                    servingSize = s.servingSize.trim().replace(',', '.').ifBlank { null }?.toDoubleOrNull(),
                    source = s.source.trim().ifBlank { null },
                )
            } else if (!s.isEdit && name.isNotBlank()) {
                val id = repo.insertManual(
                    name = name,
                    energyKcalPer100g = energy,
                    servingSize = s.servingSize.trim().replace(',', '.').ifBlank { null }?.toDoubleOrNull(),
                    gtin13 = s.gtin13.trim().ifBlank { null },
                    imageUrl = s.imageUrl.trim().ifBlank { null },
                    productUrl = s.productUrl.trim().ifBlank { null },
                    source = s.source.trim().ifBlank { "manual" }
                )
                _edit.value = _edit.value.copy(id = id, isEdit = true)
                reloadFoods()
            }
        }
    }

    fun addAlias(alias: String, type: String) {
        val current = _edit.value
        val foodId = current.id ?: return
        scope.launch {
            repo.addAlias(foodId, alias, type)
            val updated = repo.getAliases(foodId)
            _edit.value = current.copy(aliases = updated)
        }
    }

    fun deleteAlias(aliasId: Long) {
        val current = _edit.value
        val foodId = current.id ?: return
        scope.launch {
            repo.deleteAlias(aliasId)
            val updated = repo.getAliases(foodId)
            _edit.value = current.copy(aliases = updated)
        }
    }
    
    // Steal image logic
    private var stealSearchJob: Job? = null
    fun openStealDialog() {
        _edit.value = _edit.value.copy(showStealDialog = true, stealSearchQuery = "", stealResults = emptyList())
    }
    fun closeStealDialog() { _edit.value = _edit.value.copy(showStealDialog = false) }
    fun setStealSearchQuery(q: String) {
        _edit.value = _edit.value.copy(stealSearchQuery = q)
        stealSearchJob?.cancel()
        stealSearchJob = scope.launch {
            val query = q.trim()
            if (query.length < 2) {
                _edit.value = _edit.value.copy(stealResults = emptyList())
                return@launch
            }
            val foods = repo.search(query).map {
                StealImageItem(it.id, it.name, it.image_url, "FOOD")
            }
            val meals = mealRepo.searchMeals(query).map {
                StealImageItem(it.id, it.name, it.image_url, "MEAL")
            }
            _edit.value = _edit.value.copy(stealResults = (foods + meals).sortedBy { it.name.lowercase() })
        }
    }
    fun stealImage(item: StealImageItem) {
        _edit.value = _edit.value.copy(imageUrl = item.imageUrl ?: "", showStealDialog = false)
        persist()
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _edit.value.id ?: return
        scope.launch {
            repo.delete(id)
            reloadFoods()
            onDeleted()
        }
    }
}
