package com.hwt.teacher.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.Grade
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.Marks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repo: HomeworkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val assignmentId: String = checkNotNull(savedStateHandle["assignmentId"])

    private val _scanPick = MutableStateFlow<Map<String, String?>>(
        mapOf(
            Marks.GROUP_COMPLETION to Completion.DONE,
            Marks.GROUP_CORRECTION to null,
            Marks.GROUP_GRADE to null
        )
    )
    val scanPick: StateFlow<Map<String, String?>> = _scanPick

    private val _scanSummary = MutableStateFlow("")
    val scanSummary: StateFlow<String> = _scanSummary

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage

    private var lastRaw: String? = null
    private var lastAt = 0L

    init {
        viewModelScope.launch {
            _scanPick.collect { pick ->
                val parts = mutableListOf<String>()
                if (pick[Marks.GROUP_COMPLETION] != null) parts.add(Completion.label(pick[Marks.GROUP_COMPLETION]))
                if (pick[Marks.GROUP_CORRECTION] != null) parts.add(Correction.label(pick[Marks.GROUP_CORRECTION]))
                if (pick[Marks.GROUP_GRADE] != null) parts.add(Grade.label(pick[Marks.GROUP_GRADE]))
                _scanSummary.value = parts.joinToString(" · ")
            }
        }
    }

    fun toggleScanPick(group: String, value: String) {
        val cur = _scanPick.value
        _scanPick.value = cur + (group to if (cur[group] == value) null else value)
    }

    /** 相同内容 1.5s 内去重（FR-7.5）。 */
    fun onScan(raw: String) {
        val now = System.currentTimeMillis()
        if (raw == lastRaw && now - lastAt < 1500) return
        lastRaw = raw
        lastAt = now
        viewModelScope.launch {
            when (val res = repo.resolveScan(assignmentId, raw)) {
                is HomeworkRepository.ScanResolution.Student -> {
                    val patch = EntryViewModel.buildPatch(_scanPick.value)
                    if (patch.isEmpty()) {
                        _lastMessage.value = "请先选择要标记的状态"
                        return@launch
                    }
                    repo.writeRecord(assignmentId, res.studentId, patch)
                    _lastMessage.value = "已记录：${res.name} · ${_scanSummary.value}"
                }
                is HomeworkRepository.ScanResolution.NotOurs -> _lastMessage.value = "不是本应用二维码"
                is HomeworkRepository.ScanResolution.WrongClass -> _lastMessage.value = "非本班二维码"
                is HomeworkRepository.ScanResolution.NotFound -> _lastMessage.value = "未找到该学生"
            }
        }
    }
}
