package com.hwt.teacher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.EntryRowView
import com.hwt.teacher.data.Grade
import com.hwt.teacher.data.Marks
import com.hwt.teacher.util.DateUtil
import com.hwt.teacher.util.ShareUtil
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.Avatar
import com.hwt.teacher.ui.components.ClassSelector
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogOption
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtDivider
import com.hwt.teacher.ui.components.HwtFab
import com.hwt.teacher.ui.components.LayoutSpacing
import com.hwt.teacher.ui.components.HwtTextButton
import com.hwt.teacher.ui.components.MarkChip
import com.hwt.teacher.ui.components.MarkGroupPanel
import com.hwt.teacher.ui.components.SectionTitle
import com.hwt.teacher.ui.components.StatCard
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.AppViewModel
import com.hwt.teacher.vm.EntryViewModel
import kotlinx.coroutines.launch

@Composable
fun EntryScreen(nav: NavHostController, vm: AppViewModel = hiltViewModel(), entryVm: EntryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val classes by vm.classes.collectAsStateCompat()
    val currentClass by vm.currentClass.collectAsStateCompat()
    val assignment by entryVm.assignment.collectAsStateCompat()
    val rows by entryVm.rows.collectAsStateCompat()
    val stats by entryVm.stats.collectAsStateCompat()
    val bulkPick by entryVm.bulkPick.collectAsStateCompat()
    val bulkSummary by entryVm.bulkSummary.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val bytes = entryVm.pendingExportBytes
        entryVm.pendingExportBytes = null
        if (uri != null && bytes != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
            }.getOrDefault(false)
            ToastBus.show(if (ok) "已导出成功" else "导出失败，请重试")
        }
    }

    fun exportReport() {
        scope.launch {
            val result = entryVm.buildAssignmentReport()
            if (result == null) {
                ToastBus.show("暂无可导出的记录")
                return@launch
            }
            entryVm.pendingExportBytes = result.second
            exportLauncher.launch(result.first)
        }
    }

    fun sendReport() {
        scope.launch {
            val result = entryVm.buildAssignmentReport()
            if (result == null) {
                ToastBus.show("暂无可导出的记录")
                return@launch
            }
            val err = ShareUtil.shareXlsx(context, result.first, result.second)
            if (err != null) ToastBus.show(err)
        }
    }

    val a = assignment
    if (a == null) {
        Box(Modifier.fillMaxSize()) {}
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = a.title,
            onBack = { nav.popBackStack() },
            actions = {
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
            }
        )
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LayoutSpacing.FabListBottom)
            ) {
                item { SectionTitle("批量设置 · 可多维度同时选") }
                item {
                    BulkPanel(
                        pick = bulkPick,
                        summary = bulkSummary,
                        total = stats.total,
                        onToggle = { g, v -> entryVm.toggleBulkPick(g, v) },
                        onClear = { entryVm.clearBulkPick() },
                        onApply = { entryVm.applyBulk(a.classId) }
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.ButtonGap)
                    ) {
                        StatCard("${stats.rate}%", "完成率")
                        StatCard("${stats.pending}", "待订正")
                        StatCard("${stats.miss}", "未完成")
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(LayoutSpacing.Screen), horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.ButtonGap)) {
                        HwtButton("发送到…", onClick = { sendReport() }, filled = false, modifier = Modifier.weight(1f))
                        HwtButton("导出 Excel", onClick = { exportReport() }, filled = true, modifier = Modifier.weight(1f))
                    }
                }
                item { SectionTitle("学生记录 · ${DateUtil.monthDay(a.assignedDate)}") }
                items(rows, key = { it.student.id }) { row ->
                    Column {
                        EntryRow(
                            row = row,
                            onCycle = { g -> entryVm.cycleMark(row.student.id, g) },
                            onLongPick = { g ->
                                val options = when (g) {
                                    Marks.GROUP_COMPLETION -> Completion.ALL.map { DialogOption(it, Completion.label(it)) }
                                    Marks.GROUP_CORRECTION -> Correction.ALL.map { DialogOption(it, Correction.label(it)) }
                                    else -> Grade.ALL.map { DialogOption(it, Grade.label(it)) }
                                }
                                dialog = DialogConfig(
                                    title = "${row.student.name} · ${Marks.groupLabel(g)}",
                                    body = DialogBody.Options(options, entryVm.currentMark(row.student.id, g)) { v ->
                                        entryVm.setMark(row.student.id, g, v)
                                    },
                                    cancel = "关闭"
                                )
                            }
                        )
                        HwtDivider()
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(LayoutSpacing.FabMargin)) {
                HwtFab(AppIcons.Qr, onClick = { nav.navigate("scan/${a.id}") })
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}

@Composable
private fun BulkPanel(
    pick: Map<String, String?>,
    summary: String,
    total: Int,
    onToggle: (String, String) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        MarkGroupPanel(
            "完成情况",
            Completion.ALL.map { it to Completion.label(it) },
            pick[Marks.GROUP_COMPLETION],
            { onToggle(Marks.GROUP_COMPLETION, it) }
        )
        MarkGroupPanel(
            "订正情况",
            Correction.ALL.map { it to Correction.label(it) },
            pick[Marks.GROUP_CORRECTION],
            { onToggle(Marks.GROUP_CORRECTION, it) }
        )
        MarkGroupPanel(
            "评级",
            Grade.ALL.map { it to Grade.label(it) },
            pick[Marks.GROUP_GRADE],
            { onToggle(Marks.GROUP_GRADE, it) }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (summary.isEmpty()) "未选择任何项" else "将批量设为：$summary",
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HwtTextButton("清空选择", onClick = onClear)
            Spacer(Modifier.width(4.dp))
            HwtButton("应用到全班 $total 人", onClick = onApply, enabled = summary.isNotEmpty())
        }
    }
}

@Composable
private fun EntryRow(
    row: EntryRowView,
    onCycle: (String) -> Unit,
    onLongPick: (String) -> Unit
) {
    val comp = Completion.label(row.completion)
    val corr = Correction.label(row.correction)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(row.code)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.student.name, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                if (row.saved) "$comp · $corr" else "尚未记录",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MarkChip(
                kind = Marks.GROUP_COMPLETION,
                value = row.completion,
                onClick = { onCycle(Marks.GROUP_COMPLETION) },
                onLongClick = { onLongPick(Marks.GROUP_COMPLETION) }
            )
            MarkChip(
                kind = Marks.GROUP_CORRECTION,
                value = row.correction,
                onClick = { onCycle(Marks.GROUP_CORRECTION) },
                onLongClick = { onLongPick(Marks.GROUP_CORRECTION) }
            )
            MarkChip(
                kind = Marks.GROUP_GRADE,
                value = row.grade,
                onClick = { onCycle(Marks.GROUP_GRADE) },
                onLongClick = { onLongPick(Marks.GROUP_GRADE) }
            )
        }
    }
}
