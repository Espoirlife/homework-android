package com.hwt.teacher.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.PersonReport
import com.hwt.teacher.data.SettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repo: HomeworkRepository
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> =
        repo.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    val classes: StateFlow<List<ClassEntity>> =
        repo.classesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentClass: StateFlow<ClassEntity?> =
        combine(classes, settings) { cs, s -> cs.firstOrNull { it.id == s.currentClassId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val assignments = currentClass.flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else repo.assignmentRows(c.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val students = currentClass.flatMapLatest { c ->
        if (c == null) flowOf(emptyList()) else repo.studentsOf(c.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedAssignmentId = MutableStateFlow<String?>(null)
    val selectedAssignment = combine(assignments, _selectedAssignmentId) { list, id ->
        list.firstOrNull { it.assignment.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedStudentId = MutableStateFlow<String?>(null)
    val selectedStudent = combine(students, _selectedStudentId) { list, id ->
        list.firstOrNull { it.student.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val personReport: StateFlow<PersonReport?> =
        combine(currentClass, selectedStudent) { c, s -> (c?.id to s?.student?.id) }
            .flatMapLatest { (cid, sid) ->
                if (cid == null || sid == null) flowOf(null) else repo.personReport(cid, sid)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val assignmentEntries: StateFlow<List<com.hwt.teacher.data.EntryRowView>?> =
        selectedAssignment.flatMapLatest { sel ->
            if (sel == null) flowOf(null) else repo.entryRows(sel.assignment.id)
                .map { it }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectAssignment(id: String) { _selectedAssignmentId.value = id }
    fun selectStudent(id: String) { _selectedStudentId.value = id }

    fun switchClass(id: String) {
        viewModelScope.launch { repo.switchClass(id) }
    }

    var pendingExportBytes: ByteArray? = null
}
