package com.hwt.teacher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hwt.teacher.data.AssignmentRowView
import com.hwt.teacher.util.DateUtil
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.ClassSelector
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogMenuItem
import com.hwt.teacher.ui.components.DialogOption
import com.hwt.teacher.ui.components.EmptyState
import com.hwt.teacher.ui.components.HwtCard
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtFab
import com.hwt.teacher.ui.components.LayoutSpacing
import com.hwt.teacher.ui.components.ListItemRow
import com.hwt.teacher.ui.components.PillCount
import com.hwt.teacher.ui.components.SectionTitle
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.AppViewModel
import com.hwt.teacher.vm.HomeworkViewModel

@Composable
fun HomeworkScreen(
    onOpenEntry: (String) -> Unit,
    onAddAssignment: (String) -> Unit,
    vm: AppViewModel = hiltViewModel(),
    hwVm: HomeworkViewModel = hiltViewModel()
) {
    val classes by vm.classes.collectAsStateCompat()
    val currentClass by vm.currentClass.collectAsStateCompat()
    val rows by hwVm.rows.collectAsStateCompat()
    val ready by hwVm.ready.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }
    var newAssignmentDialog by remember { mutableStateOf(false) }

    fun openAssignmentMenu(row: AssignmentRowView) {
        val a = row.assignment
        dialog = DialogConfig(
            title = "${a.title} · 作业设置",
            body = DialogBody.Menu(
                items = listOf(
                    DialogMenuItem("asg-rename", AppIcons.Edit, "修改作业名称", a.title),
                    DialogMenuItem("asg-date", AppIcons.Restore, "修改布置日期", a.assignedDate),
                    DialogMenuItem("asg-del", AppIcons.Delete, "删除作业", "已录入 ${row.stats.counted} 条记录将一并删除", danger = true)
                ),
                onPick = { act ->
                    when (act) {
                        "asg-rename" -> dialog = DialogConfig(
                            "修改作业名称",
                            DialogBody.Input(a.title, "作业名称", "") {
                                if (it.isBlank()) { ToastBus.show("请输入作业名称"); false }
                                else { hwVm.updateAssignment(a.id, title = it); true }
                            },
                            confirm = "确定"
                        )
                        "asg-date" -> dialog = DialogConfig(
                            "修改布置日期",
                            DialogBody.Input(a.assignedDate, "格式 2026-09-01", "") {
                                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                                fmt.isLenient = false
                                val ok = runCatching { fmt.parse(it) != null }.getOrDefault(false)
                                if (!ok) { ToastBus.show("日期格式应为 yyyy-MM-dd，如 2026-09-01"); false }
                                else { hwVm.updateAssignment(a.id, assignedDate = it); true }
                            },
                            confirm = "确定"
                        )
                        "asg-del" -> dialog = DialogConfig(
                            "删除${a.title}？",
                            DialogBody.Text("该作业已录入 ${row.stats.counted} 条完成记录，删除后不可恢复。"),
                            cancel = "取消", confirm = "删除",
                            onConfirm = { hwVm.deleteAssignment(a.id) }
                        )
                    }
                }
            ),
            cancel = "关闭"
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("作业", actions = {
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
        Box(Modifier.fillMaxSize()) {
            if (currentClass == null) {
                EmptyState(AppIcons.School, "还没有班级", "先到「班级」页新建一个班级")
            } else if (!ready) {
                Box(Modifier.fillMaxSize())
            } else if (rows.isEmpty()) {
                EmptyState(AppIcons.Assignment, "还没有作业", "点右下角布置第一份作业")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = LayoutSpacing.FabListBottom)
                ) {
                    items(rows, key = { it.assignment.id }) { row ->
                        AssignmentRow(row, onClick = { onOpenEntry(row.assignment.id) }, onLongClick = { openAssignmentMenu(row) })
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(LayoutSpacing.FabMargin)) {
                HwtFab(AppIcons.Add, "布置作业") { newAssignmentDialog = true }
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }

    if (newAssignmentDialog) {
        val c = currentClass
        NewAssignmentDialog(
            onDone = { title ->
                if (c != null) {
                    hwVm.createAssignment(c.id, title) { id -> onAddAssignment(id) }
                }
            },
            onDismiss = { newAssignmentDialog = false }
        )
    }
}

@Composable
private fun AssignmentRow(row: AssignmentRowView, onClick: () -> Unit, onLongClick: () -> Unit) {
    HwtCard(modifier = Modifier.padding(horizontal = LayoutSpacing.Screen, vertical = LayoutSpacing.CardGap)) {
        ListItemRow(
            avatar = null,
            title = row.assignment.title,
            sub = "${DateUtil.monthDay(row.assignment.assignedDate)} · 完成率 ${row.stats.rate}% · 待订正 ${row.stats.pending}",
            onClick = onClick,
            onLongClick = onLongClick,
            trailing = {
                PillCount("${row.stats.counted}/${row.stats.total}")
            },
            minHeight = LayoutSpacing.CardRowHeight
        )
    }
}

@Composable
fun NewAssignmentDialog(
    onDone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp)
        ) {
            Text("布置作业", fontSize = 18.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(12.dp))
            com.hwt.teacher.ui.components.HwtTextField(title, { title = it }, "作业名称")
            Spacer(Modifier.height(4.dp))
            Text(
                "日期默认为今天，新作业不会预先生成记录。",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                com.hwt.teacher.ui.components.HwtTextButton("取消") { onDismiss() }
                Spacer(Modifier.width(8.dp))
                com.hwt.teacher.ui.components.HwtButton("布置", onClick = {
                    if (title.isBlank()) {
                        ToastBus.show("请输入作业名称")
                        return@HwtButton
                    }
                    onDone(title)
                    onDismiss()
                })
            }
        }
    }
}
