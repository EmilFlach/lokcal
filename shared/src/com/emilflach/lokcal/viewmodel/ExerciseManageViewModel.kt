package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.ExerciseType
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.ExerciseTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExerciseManageViewModel(
    private val repo: ExerciseTypeRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    private val _types = MutableStateFlow<List<ExerciseType>>(emptyList())
    val types: StateFlow<List<ExerciseType>> = _types.asStateFlow()

    data class EditState(
        val id: Long? = null,
        val isEdit: Boolean = false,
        val name: String = "",
        val kcalText: String = "0",
        val imageUrl: String = "",
    ) {
        val isBuiltIn: Boolean get() = name == ExerciseRepository.AUTOMATIC_STEPS_KEY
    }

    private val _edit = MutableStateFlow(EditState())
    val edit: StateFlow<EditState> = _edit.asStateFlow()
    private val persistMutex = Mutex()

    init {
        reloadTypes()
    }

    fun reloadTypes() {
        scope.launch {
            _types.value = repo.getAll()
        }
    }

    fun startEditing(id: Long?) {
        if (id == null) {
            _edit.value = EditState()
            return
        }
        scope.launch {
            val type = repo.getById(id)
            _edit.value = if (type != null) {
                EditState(
                    id = type.id,
                    isEdit = true,
                    name = type.name,
                    kcalText = type.kcal_per_hour.toInt().toString(),
                    imageUrl = type.image_url ?: "",
                )
            } else {
                EditState()
            }
        }
    }

    fun setName(value: String) { _edit.update { it.copy(name = value) }; persist() }
    fun setKcal(value: String) { _edit.update { it.copy(kcalText = value) }; persist() }
    fun setImageUrl(value: String) { _edit.update { it.copy(imageUrl = value) }; persist() }

    fun persist() {
        scope.launch {
            persistMutex.withLock {
                val state = _edit.value
                val name = state.name.trim()
                val kcal = state.kcalText.toDoubleOrNull() ?: return@withLock
                if (name.isBlank()) return@withLock
                val imageUrl = state.imageUrl.trim().ifEmpty { null }
                if (state.isEdit && state.id != null) {
                    repo.update(state.id, name, kcal, imageUrl = imageUrl)
                } else if (!state.isEdit) {
                    val newId = repo.insertManual(name, kcal, imageUrl = imageUrl)
                    _edit.update { it.copy(id = newId, isEdit = true) }
                }
            }
            reloadTypes()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val state = _edit.value
        if (state.isBuiltIn) return
        val id = state.id ?: return
        scope.launch {
            repo.delete(id)
            reloadTypes()
            onDeleted()
        }
    }
}
