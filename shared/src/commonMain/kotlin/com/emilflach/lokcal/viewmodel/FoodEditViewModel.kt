package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.AllItemFrequencies
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.ItemsMissingImage
import com.emilflach.lokcal.data.AlbertHeijnScraper
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.ui.dialogs.StealImageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FoodEditViewModel(
    private val repo: FoodRepository,
    private val intakeRepo: IntakeRepository,
) {
    enum class Tab {
        ALL, MISSING_IMAGES
    }

    private val _selectedTab = MutableStateFlow(Tab.ALL)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    // Manage/list state
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

    private val _itemFrequencies = MutableStateFlow<Map<Pair<String, Long>, Long>>(emptyMap())
    val itemFrequencies: StateFlow<Map<Pair<String, Long>, Long>> = _itemFrequencies.asStateFlow()

    private val _missingImages = MutableStateFlow<List<ItemsMissingImage>>(emptyList())
    val missingImages: StateFlow<List<ItemsMissingImage>> = _missingImages.asStateFlow()

    // Edit state
    data class EditState(
        val id: Long? = null,
        val isEdit: Boolean = false,
        val name: String = "",
        val energyText: String = "0",
        val servingSize: String = "",
        val brandName: String = "",
        val englishName: String = "",
        val dutchName: String = "",
        val productUrl: String = "",
        val imageUrl: String = "",
        val gtin13: String = "",
        val source: String = "manual",
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

    private val _allListState = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val allListState = _allListState.asStateFlow()

    private val _missingListState = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val missingListState = _missingListState.asStateFlow()

    fun saveListState(tab: Tab, index: Int, offset: Int) {
        if (tab == Tab.ALL) {
            _allListState.value = mapOf(index to offset)
        } else {
            _missingListState.value = mapOf(index to offset)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    private val scraper by lazy { AlbertHeijnScraper() }

    init {
        // initial load
        reloadFoods()
        loadMissingImages()
        loadFrequencies()
    }

    fun refresh() {
        reloadFoods()
        loadMissingImages()
        loadFrequencies()
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

    fun setSelectedTab(tab: Tab) {
        _selectedTab.value = tab
    }

    fun setSearch(value: String) {
        _search.value = value
        reloadFoods()
    }

    fun loadMissingImages() {
        scope.launch {
            _missingImages.value = intakeRepo.getItemsMissingImage().filter { it.source_type == "FOOD" }
        }
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
                    _edit.value = EditState(
                        id = f.id,
                        isEdit = true,
                        name = f.name,
                        energyText = (f.energy_kcal_per_100g).roundToInt().toString(),
                        servingSize = f.serving_size ?: "",
                        brandName = f.brand_name ?: "",
                        englishName = f.english_name ?: "",
                        dutchName = f.dutch_name ?: "",
                        productUrl = f.product_url ?: "",
                        imageUrl = f.image_url ?: "",
                        gtin13 = f.gtin13 ?: "",
                        source = f.source ?: ""
                    )
                } else {
                    _edit.value = EditState()
                }
            }
        }
    }

    // Import dialog controls
    fun openImportDialog() { _edit.value = _edit.value.copy(showUrlDialog = true, importError = null) }
    fun closeImportDialog() { _edit.value = _edit.value.copy(showUrlDialog = false, isImporting = false, importError = null) }
    fun setUrlInput(v: String) { _edit.value = _edit.value.copy(urlInput = v) }

    suspend fun importFromUrl(urlRaw: String) {
        val url = urlRaw.trim()
        if (url.isEmpty()) {
            _edit.value = _edit.value.copy(importError = "Please enter a URL")
            return
        }
        _edit.value = _edit.value.copy(isImporting = true, importError = null)

        try {
            val r = scraper.scrape(url)
            val current = _edit.value
            _edit.value = current.copy(
                name = r.name.toString().ifBlank { current.name },
                energyText = r.kcalPer100g.toString().ifBlank { current.energyText },
                servingSize = r.servingSizeGrams.toString().ifBlank { current.servingSize },
                productUrl = url,
                imageUrl = r.imageUrl ?: current.imageUrl,
                gtin13 = r.gtin13 ?: current.gtin13,
                brandName = r.name.toString().ifBlank { current.name },
                source = "ah",
                showUrlDialog = false,
                isImporting = false,
                importError = null,
            )
        } catch (_: Exception) {
            _edit.value = _edit.value.copy(isImporting = false, importError = "Failed to import from URL")
        }
    }

    fun updateName(v: String) { _edit.value = _edit.value.copy(name = v) }
    fun updateEnergyText(v: String) { _edit.value = _edit.value.copy(energyText = v.filter { it.isDigit() || it == '.' || it == ',' }) }
    fun updateServingSize(v: String) { _edit.value = _edit.value.copy(servingSize = v) }
    fun updateBrandName(v: String) { _edit.value = _edit.value.copy(brandName = v) }
    fun updateEnglishName(v: String) { _edit.value = _edit.value.copy(englishName = v) }
    fun updateDutchName(v: String) { _edit.value = _edit.value.copy(dutchName = v) }
    fun updateProductUrl(v: String) { _edit.value = _edit.value.copy(productUrl = v) }
    fun updateImageUrl(v: String) { _edit.value = _edit.value.copy(imageUrl = v) }
    fun updateGtin13(v: String) { _edit.value = _edit.value.copy(gtin13 = v.filter { it.isDigit() }) }
    fun updateSource(v: String) { _edit.value = _edit.value.copy(source = v) }
    
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
            val meals = intakeRepo.searchMeals(query).map {
                StealImageItem(it.id, it.name, it.image_url, "MEAL")
            }
            _edit.value = _edit.value.copy(stealResults = (foods + meals).sortedBy { it.name.lowercase() })
        }
    }
    fun stealImage(item: StealImageItem) {
        _edit.value = _edit.value.copy(imageUrl = item.imageUrl ?: "", showStealDialog = false)
    }

    fun save(onSaved: () -> Unit) {
        val s = _edit.value
        val name = s.name.trim()
        if (name.isBlank()) return
        val energy = s.energyText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        scope.launch {
            if (s.isEdit && s.id != null) {
                repo.updateDetails(
                    id = s.id,
                    name = name,
                    brandName = s.brandName.trim().ifBlank { null },
                    energyKcalPer100g = energy,
                    productUrl = s.productUrl.trim().ifBlank { null },
                    imageUrl = s.imageUrl.trim().ifBlank { null },
                    gtin13 = s.gtin13.trim().ifBlank { null },
                    servingSize = s.servingSize.trim().ifBlank { null },
                    englishName = s.englishName.trim().ifBlank { null },
                    dutchName = s.dutchName.trim().ifBlank { null },
                    source = s.source.trim().ifBlank { null },
                )
            } else {
                repo.insertManual(
                    name = name,
                    brandName = s.brandName.trim().ifBlank { null },
                    energyKcalPer100g = energy,
                    productUrl = s.productUrl.trim().ifBlank { null },
                    imageUrl = s.imageUrl.trim().ifBlank { null },
                    gtin13 = s.gtin13.trim().ifBlank { null },
                    servingSize = s.servingSize.trim().ifBlank { null },
                    englishName = s.englishName.trim().ifBlank { null },
                    dutchName = s.dutchName.trim().ifBlank { null },
                    source = s.source.trim().ifBlank { "manual" }
                )
            }
            // refresh list after save
            reloadFoods()
            onSaved()
        }
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
