package com.hwt.teacher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hwt.teacher.data.AssignmentRowView
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.EntryRowView
import com.hwt.teacher.data.PersonReport
import com.hwt.teacher.data.StudentView
import com.hwt.teacher.util.DateUtil
import com.hwt.teacher.util.ReportExporter
import com.hwt.teacher.util.ShareUtil
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.Banner
import com.hwt.teacher.ui.components.ClassSelector
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogOption
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtDivider
import com.hwt.teacher.ui.components.HwtTabRow
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.ui.components.SectionTitle
import com.hwt.teacher.ui.components.SelectPill
import com.hwt.teacher.ui.components.StatCard
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.ui.theme.MdPrimary
import com.hwt.teacher.ui.theme.StDone
import com.hwt.teacher.ui.theme.StMiss
import com.hwt.teacher.ui.theme.StPartial
import com.hwt.teacher.ui.theme.StNone
import com.hwt.teacher.ui.theme.StPending
import com.hwt.teacher.vm.ReportViewModel

@Composable
fun ReportScreen(
    initialPersonId: String?,
    onPersonConsumed: () -> Unit,
    vm: ReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val classes by vm.classes.collectAsStateCompat()
    val currentClass by vm.currentClass.collectAsStateCompat()
    val assignments by vm.assignments.collectAsStateCompat()
    val students by vm.students.collectAsStateCompat()
    val selectedAssignment by vm.selectedAssignment.collectAsStateCompat()
    val selectedStudent by vm.selectedStudent.collectAsStateCompat()
    val personReport by vm.personReport.collectAsStateCompat()
    val assignmentEntries by vm.assignmentEntries.collectAsStateCompat()

    var tab by rememberSaveable { mutableStateOf("assignment") }
    var dialog by remember { mutableStateOf<DialogConfig?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val bytes = vm.pendingExportBytes
        vm.pendingExportBytes = null
        if (uri != null && bytes != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
            }.getOrDefault(false)
            ToastBus.show(if (ok) "已导出成功" else "导出失败，请重试")
        }
    }

    fun launchExport(name: String, bytes: ByteArray) {
        vm.pendingExportBytes = bytes
        exportLauncher.launch(name)
    }

    fun launchSend(name: String, bytes: ByteArray) {
        val err = ShareUtil.shareXlsx(context, name, bytes)
        if (err != null) ToastBus.show(err)
    }

    LaunchedEffect(initialPersonId) {
        if (initialPersonId != null) {
            vm.selectStudent(initialPersonId)
            tab = "person"
            onPersonConsumed()
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("统计报表", actions = {
            currentClass?.let { c ->
                ClassSelector(c.name) {
                    dialog = DialogConfig(
                        "切换班级",
                        DialogBody.Options(
                            options = classes.map { DialogOption(it.id, it.name) },
                            value = currentClass?.id ?: "",
                            onPick = { vm.switchClass(it) }
                        ),
                        cancel = "关闭"
                    )
                }
            }
        })
        HwtTabRow(
            listOf("assignment" to "单次作业", "person" to "个人报表"),
            tab
        ) { tab = it }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (tab) {
                "person" -> PersonReportTab(
                    students = students,
                    selected = selectedStudent,
                    report = personReport,
                    onPickStudent = { vm.selectStudent(it) },
                    onExport = {
                        val p = personReport ?: return@PersonReportTab
                        launchExport(
                            "个人-${p.student.student.name}.xlsx",
                            ReportExporter.personReport(currentClass?.name ?: "", p)
                        )
                    },
                    onSend = {
                        val p = personReport ?: return@PersonReportTab
                        launchSend(
                            "个人-${p.student.student.name}.xlsx",
                            ReportExporter.personReport(currentClass?.name ?: "", p)
                        )
                    }
                )
                else -> AssignmentReportTab(
                    selected = selectedAssignment,
                    entries = assignmentEntries,
                    allAssignments = assignments,
                    onPickAssignment = { vm.selectAssignment(it) },
                    onExport = {
                        val sel = selectedAssignment ?: return@AssignmentReportTab
                        val entries = assignmentEntries ?: emptyList()
                        launchExport(
                            "作业-${sel.assignment.title}.xlsx",
                            ReportExporter.assignmentReport(currentClass, sel, entries)
                        )
                    },
                    onSend = {
                        val sel = selectedAssignment ?: return@AssignmentReportTab
                        val entries = assignmentEntries ?: emptyList()
                        launchSend(
                            "作业-${sel.assignment.title}.xlsx",
                            ReportExporter.assignmentReport(currentClass, sel, entries)
                        )
                    }
                )
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}

// ---------- 单次作业 ----------

@Composable
private fun AssignmentReportTab(
    selected: AssignmentRowView?,
    entries: List<EntryRowView>?,
    allAssignments: List<AssignmentRowView>,
    onPickAssignment: (String) -> Unit,
    onExport: () -> Unit,
    onSend: () -> Unit
) {
    if (selected == null) {
        Banner(AppIcons.Info, "还没有作业记录")
        return
    }
    AnimatedVisibility(visible = entries == null, enter = fadeIn(), exit = fadeOut()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    Box(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow))
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow))
        }
    }
    AnimatedVisibility(visible = entries != null, enter = fadeIn(), exit = fadeOut()) {
        val rows = entries ?: return@AnimatedVisibility
        var dialog by remember { mutableStateOf<DialogConfig?>(null) }
        val st = selected.stats
        Column(Modifier.fillMaxWidth()) {
            RowField("作业") {
                SelectPill("${selected.assignment.title} · ${DateUtil.monthDaySlash(selected.assignment.assignedDate)}") {
                    dialog = DialogConfig(
                        "选择作业",
                        DialogBody.Options(
                            allAssignments.map {
                                DialogOption(it.assignment.id, "${it.assignment.title}（${DateUtil.monthDaySlash(it.assignment.assignedDate)}）")
                            },
                            selected.assignment.id,
                            onPickAssignment
                        ),
                        cancel = "关闭"
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${st.rate}%", "完成率", compact = true)
                StatCard("${st.done}", "已完成", compact = true)
                StatCard("${st.partial}", "部分完成", compact = true)
                StatCard("${st.pending}", "待订正", compact = true)
            }
            SectionTitle("完成情况明细")
            TableHeader(
                listOf(
                    Cell("学号", null, dim = true, width = 40.dp),
                    Cell("姓名", null, dim = true, width = 60.dp),
                    Cell("完成情况", null, dim = true, center = true, width = 60.dp),
                    Cell("订正情况", null, dim = true, center = true, width = 76.dp),
                    Cell("评级", null, dim = true, right = true, width = 28.dp)
                )
            )
            rows.forEach { e ->
                TableRow(
                    cells = listOf(
                        Cell(e.code, null, dim = true, width = 40.dp),
                        Cell(e.student.name, null, dim = false, width = 60.dp),
                        Cell(Completion.label(e.completion), colorOfCompletion(e.completion), dim = false, center = true, width = 60.dp),
                        Cell(Correction.label(e.correction), colorOfCorrection(e.correction), dim = false, center = true, width = 76.dp),
                        Cell(e.grade, if (e.grade.isEmpty()) StNone else MdPrimary, dim = false, right = true, width = 28.dp)
                    )
                )
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HwtButton("发送到…", onClick = onSend, filled = false, modifier = Modifier.weight(1f))
                HwtButton("导出 Excel", onClick = onExport, filled = true, modifier = Modifier.weight(1f))
            }
            HwtDialogHost(dialog) { dialog = null }
        }
    }
}

// ---------- 个人报表 ----------

@Composable
private fun PersonReportTab(
    students: List<StudentView>,
    selected: StudentView?,
    report: PersonReport?,
    onPickStudent: (String) -> Unit,
    onExport: () -> Unit,
    onSend: () -> Unit
) {
    if (students.isEmpty() || selected == null || report == null) {
        Banner(AppIcons.Info, "没有可统计的数据")
        return
    }
    var dialog by remember { mutableStateOf<DialogConfig?>(null) }
    RowField("学生") {
        SelectPill("${selected.code} ${selected.student.name}") {
            dialog = DialogConfig(
                "选择学生",
                DialogBody.Options(
                    students.map { DialogOption(it.student.id, "${it.code} ${it.student.name}") },
                    selected.student.id,
                    onPickStudent
                ),
                cancel = "关闭"
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("${report.rate}%", "个人完成率")
        StatCard("${report.doneCount}", "已完成")
        StatCard("${report.items.size - report.doneCount}", "未完成")
    }
    SectionTitle("逐次明细")
    TableHeader(
        listOf(
            Cell("作业", null, dim = true, width = 100.dp),
            Cell("日期", null, dim = true, width = 44.dp),
            Cell("完成情况", null, dim = true, center = true, width = 60.dp),
            Cell("订正情况", null, dim = true, center = true, width = 76.dp),
            Cell("评级", null, dim = true, right = true, width = 28.dp)
        )
    )
    report.items.forEach { item ->
        TableRow(
            cells = listOf(
                Cell(item.assignment.title, null, dim = false, width = 100.dp),
                Cell(DateUtil.monthDaySlash(item.assignment.assignedDate), null, dim = true, width = 44.dp),
                Cell(Completion.label(item.completion), colorOfCompletion(item.completion), dim = false, center = true, width = 60.dp),
                Cell(Correction.label(item.correction), colorOfCorrection(item.correction), dim = false, center = true, width = 76.dp),
                Cell(item.grade, if (item.grade.isEmpty()) StNone else MdPrimary, dim = false, right = true, width = 28.dp)
            )
        )
    }
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HwtButton("发送到…", onClick = onSend, filled = false, modifier = Modifier.weight(1f))
        HwtButton("导出 Excel", onClick = onExport, filled = true, modifier = Modifier.weight(1f))
    }
    HwtDialogHost(dialog) { dialog = null }
}

// ---------- 表格 ----------

private data class Cell(val text: String, val color: Color?, val dim: Boolean, val center: Boolean = false, val right: Boolean = false, val width: Dp? = null)

private fun cellAlignment(c: Cell): Alignment = when {
    c.right -> Alignment.CenterEnd
    c.center -> Alignment.Center
    else -> Alignment.CenterStart
}

@Composable
private fun TableHeader(columns: List<Cell>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEachIndexed { i, c ->
            if (i > 0) Spacer(Modifier.weight(1f))
            Box(Modifier.width(c.width ?: 0.dp)) {
                Text(
                    c.text,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(cellAlignment(c))
                )
            }
        }
    }
    HwtDivider(MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun TableRow(cells: List<Cell>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { i, c ->
            if (i > 0) Spacer(Modifier.weight(1f))
            Box(Modifier.width(c.width ?: 0.dp)) {
                Text(
                    c.text,
                    fontSize = 14.sp,
                    color = c.color ?: if (c.dim) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(cellAlignment(c))
                )
            }
        }
    }
}

private fun colorOfCompletion(v: String?): Color = when (v) {
    Completion.DONE -> StDone
    Completion.PARTIAL -> StPartial
    else -> StMiss
}

private fun colorOfCorrection(v: String?): Color = if (v == Correction.FIXED) StDone else StPending
