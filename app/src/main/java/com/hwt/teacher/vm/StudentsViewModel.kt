package com.hwt.teacher.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.StudentView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 单个班级的学生名单页（班级页的下一级）。 */
@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val repo: HomeworkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classId: String = checkNotNull(savedStateHandle["classId"])

    val classEntity: StateFlow<ClassEntity?> = repo.classById(classId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val studentsOrNull: StateFlow<List<StudentView>?> = repo.studentsOf(classId)
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val students: StateFlow<List<StudentView>> = studentsOrNull
        .map { it ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ready: StateFlow<Boolean> = studentsOrNull
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun addStudent(name: String) = viewModelScope.launch { repo.addStudent(classId, name) }

    fun updateStudentName(studentId: String, name: String) =
        viewModelScope.launch { repo.updateStudentName(studentId, name) }

    fun updateStudentNote(studentId: String, note: String) =
        viewModelScope.launch { repo.updateStudentNote(studentId, note) }

    fun deleteStudent(studentId: String) = viewModelScope.launch { repo.deleteStudent(studentId) }

    suspend fun updateStudentSeq(studentId: String, seq: Int): String? = repo.updateStudentSeq(studentId, seq)
}
