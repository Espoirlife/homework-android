package com.hwt.teacher.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.util.XlsxUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

data class WizardCol(val name: String, val samples: List<String>, val role: String)

data class WizardRow(val name: String, val note: String, val reason: String)

data class WizardState(
    val step: Int = 1,
    val fileName: String? = null,
    val cols: List<WizardCol> = emptyList(),
    val rows: List<Pair<String, String>> = emptyList(),
    val nameIdx: Int = 0,
    val noteIdx: Int = -1,
    val preview: List<WizardRow> = emptyList(),
    val nameColRole: String = "忽略",
    val noteColRole: String = "忽略"
)

/** Excel 导入向导（FR-3.6、附录 B）。 */
@HiltViewModel
class WizardViewModel @Inject constructor(
    private val repo: HomeworkRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state

    private val NAME_KEYS = listOf("学生姓名", "姓名", "名字", "学生", "name", "student", "studentname", "fullname")
    private val NOTE_KEYS = listOf("备注", "说明", "注释", "note", "remark", "comment")

    private fun norm(s: String) = s.lowercase().replace(" ", "")

    /** 读取第一个工作表；自动识别姓名/备注列。 */
    fun loadSheet(fileName: String, input: InputStream) {
        viewModelScope.launch {
            try {
                val lower = fileName.lowercase()
                if (lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
                    ToastBus.show("暂不支持旧版 .xls，请用 WPS/Excel 另存为 .xlsx 再导入")
                    return@launch
                }
                val sheet = XlsxUtil.readFirstSheet(input)
                if (sheet.isEmpty()) {
                    ToastBus.show("未能读取表格内容，请确认是有效的 .xlsx 文件")
                    return@launch
                }
                val header = sheet.first().map { it.trim() }
                val dataRows = sheet.drop(1).filter { row -> row.any { it.isNotBlank() } }
                if (dataRows.isEmpty()) {
                    ToastBus.show("表格中没有数据行（第一行会被当作表头）")
                    return@launch
                }
                var nameIdx = -1
                var noteIdx = -1
                header.forEachIndexed { i, h ->
                    val k = norm(h)
                    if (nameIdx < 0 && NAME_KEYS.any { norm(it) == k }) nameIdx = i
                    if (noteIdx < 0 && NOTE_KEYS.any { norm(it) == k }) noteIdx = i
                }
                if (nameIdx < 0) nameIdx = 0
                val cols = header.mapIndexed { i, h ->
                    WizardCol(
                        h.ifEmpty { "列${i + 1}" },
                        dataRows.take(3).mapNotNull { it.getOrNull(i)?.takeIf { c -> c.isNotBlank() } },
                        roleOf(i, nameIdx, noteIdx)
                    )
                }
                val rows = dataRows.map { row ->
                    val name = row.getOrNull(nameIdx)?.trim() ?: ""
                    val note = if (noteIdx >= 0) row.getOrNull(noteIdx)?.trim() ?: "" else ""
                    name to note
                }
                _state.value = _state.value.copy(
                    step = 1, fileName = fileName, cols = cols, rows = rows,
                    nameIdx = nameIdx, noteIdx = noteIdx,
                    nameColRole = roleOf(nameIdx, nameIdx, noteIdx),
                    noteColRole = if (noteIdx >= 0) roleOf(noteIdx, nameIdx, noteIdx) else "忽略"
                )
            } catch (e: Exception) {
                ToastBus.show("读取失败：${e.message ?: "文件无法解析"}")
            }
        }
    }

    private fun roleOf(i: Int, nameIdx: Int, noteIdx: Int): String = when (i) {
        nameIdx -> "姓名"
        noteIdx -> "备注"
        else -> "忽略"
    }

    fun pickCol(index: Int, role: String) {
        val s = _state.value
        var nameIdx = s.nameIdx
        var noteIdx = s.noteIdx
        when (role) {
            "姓名" -> {
                if (noteIdx == index) noteIdx = -1
                nameIdx = index
            }
            "备注" -> {
                if (nameIdx == index) nameIdx = -1
                noteIdx = index
            }
            else -> {
                if (nameIdx == index) nameIdx = -1
                if (noteIdx == index) noteIdx = -1
            }
        }
        if (nameIdx < 0) nameIdx = 0
        _state.value = s.copy(
            nameIdx = nameIdx, noteIdx = noteIdx,
            nameColRole = if (nameIdx >= 0) "姓名" else "忽略",
            noteColRole = if (noteIdx >= 0) "备注" else "忽略",
            cols = s.cols.mapIndexed { i, c -> c.copy(role = roleOf(i, nameIdx, noteIdx)) }
        )
    }

    fun next() {
        val s = _state.value
        when (s.step) {
            1 -> if (s.fileName == null) ToastBus.show("请先选择文件") else _state.value = s.copy(step = 2)
            2 -> if (s.nameIdx < 0) {
                ToastBus.show("请先指定姓名列")
            } else {
                viewModelScope.launch {
                    val preview = buildPreview(s)
                    _state.value = s.copy(step = 3, preview = preview)
                }
            }
        }
    }

    fun prev() {
        val s = _state.value
        _state.value = if (s.step > 1) s.copy(step = s.step - 1) else s
    }

    private suspend fun buildPreview(s: WizardState): List<WizardRow> {
        val classId = repo.currentClassId() ?: return emptyList()
        val existNames = repo.snapshotStudents(classId).map { it.name }.toSet()
        val seen = mutableSetOf<String>()
        return s.rows.map { (name, note) ->
            val reason = when {
                name.isEmpty() -> "empty"
                seen.contains(name) -> "dup"
                existNames.contains(name) -> "exist"
                else -> ""
            }
            if (reason.isEmpty()) seen.add(name)
            WizardRow(name, note, reason)
        }
    }

    fun commit(classId: String) {
        val s = _state.value
        val ok = s.preview.filter { it.reason.isEmpty() }
        if (ok.isEmpty()) {
            ToastBus.show("没有可导入的学生")
            return
        }
        viewModelScope.launch {
            val n = repo.importStudents(classId, ok.map { it.name to it.note })
            ToastBus.show("已导入 $n 人")
            _state.value = WizardState()
        }
    }
}
