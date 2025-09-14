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
    data class ChartState(
        val sorted: List<WeightLog> = emptyList(),
        val startIndex: Int = 0,
        val endIndex: Int = -1,
    ) {
        val displayedItems: List<WeightLog> =
            if (sorted.isEmpty() || startIndex > endIndex) emptyList()
            else sorted.subList(startIndex.coerceAtLeast(0), (endIndex + 1).coerceAtMost(sorted.size))
        val weights: List<Double> = displayedItems.map { it.weight_kg }
        val min: Double = weights.minOrNull() ?: 0.0
        val max: Double = weights.maxOrNull() ?: 0.0
    }

    private val _chart = MutableStateFlow(ChartState())
    val chart: StateFlow<ChartState> = _chart.asStateFlow()
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

    private fun updateChartFor(items: List<WeightLog>, resetRange: Boolean = false) {
        val sorted = items.sortedBy { it.date }
        val defaultStart = (sorted.size - 12).coerceAtLeast(0)
        val defaultEnd = (sorted.size - 1).coerceAtLeast(-1)
        val current = _chart.value
        val start = if (current.sorted.isEmpty() || resetRange) defaultStart else current.startIndex.coerceIn(0, (sorted.size - 1).coerceAtLeast(0))
        val end = if (current.sorted.isEmpty() || resetRange) defaultEnd else current.endIndex.coerceIn(start, (sorted.size - 1).coerceAtLeast(-1))
        _chart.value = ChartState(sorted = sorted, startIndex = start, endIndex = end)
    }

    fun onChartRangeChanged(start: Int, end: Int) {
        val sorted = _chart.value.sorted
        if (sorted.isEmpty()) return
        _chart.value = ChartState(
            sorted = sorted,
            startIndex = start.coerceIn(0, (sorted.size - 1).coerceAtLeast(0)),
            endIndex = end.coerceIn(start.coerceAtLeast(0), (sorted.size - 1).coerceAtLeast(0))
        )
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
        refresh(resetChartRange = true)
    }

    fun deleteById(id: Long) {
        repo.deleteById(id)
        refresh()
    }

    fun refresh(resetChartRange: Boolean = false) {
        val list = repo.getAll()
        _items.value = list
        updateChartFor(list, resetRange = resetChartRange)
    }
}
