package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.data.WeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform-agnostic lightweight ViewModel managing Weight list and add/delete logic.
 */
class WeightListViewModel(private val repo: WeightRepository) {
    private val _items = MutableStateFlow<List<WeightLog>>(emptyList())
    val items: StateFlow<List<WeightLog>> = _items.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun openAddDialog(open: Boolean = true) {
        _showAddDialog.value = open
        if (open) {
            _error.value = null
            _input.value = ""
        }
    }

    fun onInputChanged(new: String) {
        // allow digits and a single dot/comma
        val filtered = new.filter { it.isDigit() || it == '.' || it == ',' }
        if (filtered.count { it == '.' || it == ',' } <= 1) {
            _input.value = filtered
        }
    }

    fun saveToday() {
        val normalized = _input.value.replace(',', '.')
        val v = normalized.toDoubleOrNull()
        if (v == null || v <= 0.0) {
            _error.value = "Enter a valid weight in kg"
            return
        }
        repo.setForToday(v)
        _showAddDialog.value = false
        _input.value = ""
        _error.value = null
        refresh()
    }

    fun deleteById(id: Long) {
        repo.deleteById(id)
        refresh()
    }

    fun refresh() {
        _items.value = repo.getAll()
    }
}
