package com.hwt.teacher.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.AssignmentRowView
import com.hwt.teacher.data.HomeworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeworkViewModel @Inject constructor(
    private val repo: HomeworkRepository
) : ViewModel() {

    private val rowsOrNull: StateFlow<List<AssignmentRowView>?> =
        repo.settingsFlow.flatMapLatest { s ->
            if (s.currentClassId == null) flowOf(emptyList()) else repo.assignmentRows(s.currentClassId)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val rows: StateFlow<List<AssignmentRowView>> =
        rowsOrNull.map { it ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ready: StateFlow<Boolean> =
        rowsOrNull.map { it != null }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun createAssignment(classId: String, title: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repo.createAssignment(classId, title)
            onCreated(id)
        }
    }

    fun updateAssignment(assignmentId: String, title: String? = null, assignedDate: String? = null) {
        viewModelScope.launch { repo.updateAssignment(assignmentId, title, assignedDate) }
    }

    fun deleteAssignment(assignmentId: String) {
        viewModelScope.launch { repo.deleteAssignment(assignmentId) }
    }
}
