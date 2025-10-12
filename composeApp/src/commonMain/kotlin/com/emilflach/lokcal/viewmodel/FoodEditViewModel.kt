package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.data.AlbertHeijnScraper
import com.emilflach.lokcal.data.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FoodEditViewModel(
    private val repo: FoodRepository,
) {
    // Manage/list state
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

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
    )

    private val _edit = MutableStateFlow(EditState())
    val edit: StateFlow<EditState> = _edit.asStateFlow()

    private val scraper by lazy { AlbertHeijnScraper() }

    init {
        // initial load
        reloadFoods()
    }

    fun setSearch(value: String) {
        _search.value = value
        reloadFoods()
    }

    fun reloadFoods() {
        val q = _search.value.trim()
        _foods.value = if (q.isBlank()) repo.getAll().sortedBy { it.name.lowercase() } else repo.search(q)
    }

    fun startEditing(foodId: Long?) {
        if (foodId == null) {
            _edit.value = EditState(id = null, isEdit = false, energyText = "0", source = "manual")
        } else {
            val f = repo.getById(foodId)
            if (f != null) {
                _edit.value = EditState(
                    id = f.id,
                    isEdit = true,
                    name = f.name,
                    energyText = (f.energy_kcal_per_100g).toInt().toString(),
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
        } catch (e: Exception) {
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

    fun save(): Long? {
        val s = _edit.value
        val name = s.name.trim()
        if (name.isBlank()) return null
        val energy = s.energyText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val resultId: Long = if (s.isEdit && s.id != null) {
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
            s.id
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
        return resultId
    }

    fun delete() {
        val id = _edit.value.id ?: return
        repo.delete(id)
        reloadFoods()
    }
}
