package com.hwt.teacher.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.ClassCardView
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.SettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: HomeworkRepository
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    val classes: StateFlow<List<ClassEntity>> = repo.classesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val classCards: StateFlow<List<ClassCardView>> = repo.classCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentClass: StateFlow<ClassEntity?> =
        combine(classes, settings) { cs, s -> cs.firstOrNull { it.id == s.currentClassId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { repo.ensureSettings() }
    }

    fun switchClass(id: String) = viewModelScope.launch { repo.switchClass(id) }

    fun completeOnboarding(name: String, note: String, prefix: String, digits: Int, recycle: Boolean) {
        viewModelScope.launch { repo.completeOnboarding(name, note, prefix, digits, recycle) }
    }

    fun createClass(name: String, prefix: String, digits: Int, recycle: Boolean) {
        viewModelScope.launch { repo.createClass(name, prefix, digits, recycle) }
    }

    fun deleteClass(id: String) = viewModelScope.launch { repo.deleteClass(id) }

    fun renameClass(id: String, name: String) = viewModelScope.launch { repo.renameClass(id, name) }

    fun updateClassNote(id: String, note: String) = viewModelScope.launch { repo.updateClassNote(id, note) }

    fun updateClassRule(id: String, prefix: String, digits: Int, recycle: Boolean) {
        viewModelScope.launch { repo.updateClassRule(id, prefix, digits, recycle) }
    }

    fun renumberClass(id: String) = viewModelScope.launch { repo.renumberClass(id) }
}
