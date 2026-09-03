package com.hwt.teacher.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.AssignmentEntity
import com.hwt.teacher.data.AssignmentRowView
import com.hwt.teacher.data.AssignmentStats
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.EntryRowView
import com.hwt.teacher.data.Grade
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.Marks
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryStats(val rate: Int, val pending: Int, val miss: Int, val total: Int)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repo: HomeworkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val assignmentId: String = checkNotNull(savedStateHandle["assignmentId"])

    val assignment: StateFlow<AssignmentEntity?> = repo.assignmentById(assignmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rows: StateFlow<List<EntryRowView>> = repo.entryRows(assignmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<EntryStats> = combine(rows, assignment) { list, _ ->
        val done = list.count { it.completion == Completion.DONE }
        val partial = list.count { it.completion == Completion.PARTIAL }
        val miss = list.count { it.completion == Completion.MISS }
        val pending = list.count { it.correction == Correction.PENDING }
        val total = list.size
        val rate = if (total == 0) 0 else (done + partial) * 100 / total
        EntryStats(rate, pending, miss, total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EntryStats(0, 0, 0, 0))

    private val _bulkPick = MutableStateFlow<Map<String, String?>>(
        mapOf(Marks.GROUP_COMPLETION to null, Marks.GROUP_CORRECTION to null, Marks.GROUP_GRADE to null)
    )
    val bulkPick: StateFlow<Map<String, String?>> = _bulkPick

    private val _bulkSummary = MutableStateFlow("")
    val bulkSummary: StateFlow<String> = _bulkSummary

    private var cycleCount = 0

    init {
        viewModelScope.launch {
            _bulkPick.collect { pick ->
                val parts = mutableListOf<String>()
                if (pick[Marks.GROUP_COMPLETION] != null) parts.add(Completion.label(pick[Marks.GROUP_COMPLETION]))
                if (pick[Marks.GROUP_CORRECTION] != null) parts.add(Correction.label(pick[Marks.GROUP_CORRECTION]))
                if (pick[Marks.GROUP_GRADE] != null) parts.add(Grade.label(pick[Marks.GROUP_GRADE]))
                _bulkSummary.value = parts.joinToString(" · ")
            }
        }
    }

    fun toggleBulkPick(group: String, value: String) {
        val cur = _bulkPick.value
        _bulkPick.value = cur + (group to if (cur[group] == value) null else value)
    }

    fun clearBulkPick() {
        _bulkPick.value = mapOf(Marks.GROUP_COMPLETION to null, Marks.GROUP_CORRECTION to null, Marks.GROUP_GRADE to null)
    }

    fun applyBulk(classId: String) {
        val patch = buildPatch(_bulkPick.value)
        if (patch.isEmpty()) {
            ToastBus.show("请先选择要批量设置的项")
            return
        }
        val summary = _bulkSummary.value
        viewModelScope.launch {
            val list = rows.value
            repo.bulkApply(assignmentId, classId, patch)
            ToastBus.show("已批量设为：$summary（${list.size} 人）")
        }
    }

    fun cycleMark(studentId: String, group: String) {
        val row = rows.value.firstOrNull { it.student.id == studentId } ?: return
        val next = when (group) {
            Marks.GROUP_COMPLETION -> Completion.next(row.completion)
            Marks.GROUP_CORRECTION -> Correction.next(row.correction)
            else -> Grade.next(row.grade)
        }
        viewModelScope.launch { repo.writeRecord(assignmentId, studentId, mapOf(group to next)) }
        cycleCount++
        if (cycleCount == 3) {
            ToastBus.show("提示：长按状态标可直接选择，不必逐次循环")
        }
    }

    fun setMark(studentId: String, group: String, value: String) {
        viewModelScope.launch { repo.writeRecord(assignmentId, studentId, mapOf(group to value)) }
    }

    fun currentMark(studentId: String, group: String): String {
        val row = rows.value.firstOrNull { it.student.id == studentId } ?: return ""
        return when (group) {
            Marks.GROUP_COMPLETION -> row.completion
            Marks.GROUP_CORRECTION -> row.correction
            else -> row.grade
        }
    }

    var pendingExportBytes: ByteArray? = null

    /** 组装单次作业完成情况表；数据未就绪时返回 null。 */
    suspend fun buildAssignmentReport(): Pair<String, ByteArray>? {
        val a = assignment.value ?: return null
        val list = rows.value
        if (list.isEmpty()) return null
        val cls = repo.snapshotClass(a.classId)
        val st = stats.value
        val row = AssignmentRowView(
            assignment = a,
            stats = AssignmentStats(
                total = st.total,
                done = list.count { it.completion == Completion.DONE },
                miss = st.miss,
                partial = list.count { it.completion == Completion.PARTIAL },
                pending = st.pending,
                counted = list.count { it.saved },
                rate = st.rate
            )
        )
        val bytes = ReportExporter.assignmentReport(cls, row, list)
        return "作业-${a.title}.xlsx" to bytes
    }

    companion object {
        fun buildPatch(pick: Map<String, String?>): Map<String, String> {
            val out = mutableMapOf<String, String>()
            pick.forEach { (k, v) -> if (v != null) out[k] = v }
            return out
        }
    }
}
