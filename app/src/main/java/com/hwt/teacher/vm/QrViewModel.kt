package com.hwt.teacher.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.SettingsEntity
import com.hwt.teacher.data.StudentView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QrViewModel @Inject constructor(
    private val repo: HomeworkRepository
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> =
        repo.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    val classes: StateFlow<List<ClassEntity>> =
        repo.classesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentClass: StateFlow<ClassEntity?> =
        combine(classes, settings) { cs, s -> cs.firstOrNull { it.id == s.currentClassId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val students: StateFlow<List<StudentView>> = currentClass.flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else repo.studentsOf(c.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateParams(perRow: Int, margin: Int, level: String, withNo: Boolean) {
        viewModelScope.launch { repo.updateQrParams(perRow, margin, level, withNo) }
    }

    fun switchClass(id: String) = viewModelScope.launch { repo.switchClass(id) }
}
