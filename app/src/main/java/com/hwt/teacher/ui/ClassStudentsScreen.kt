package com.hwt.teacher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.hwt.teacher.data.ClassCardView
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogMenuItem
import com.hwt.teacher.ui.components.EmptyState
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtCard
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtFab
import com.hwt.teacher.ui.components.LayoutSpacing
import com.hwt.teacher.ui.components.ListItemRow
import com.hwt.teacher.ui.components.HwtSwitch
import com.hwt.teacher.ui.components.HwtTextField
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.AppViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClassStudentsScreen(
    nav: androidx.navigation.NavHostController,
    vm: AppViewModel = hiltViewModel()
) {
    val classCards by vm.classCards.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }
    var newClassDialog by remember { mutableStateOf(false) }
    var ruleDialogClass by remember { mutableStateOf<String?>(null) }

    fun openClassMenu(c: ClassCardView) {
        dialog = DialogConfig(
            title = "${c.classEntity.name} · 班级设置",
            body = DialogBody.Menu(
                items = listOf(
                    DialogMenuItem("cls-rename", AppIcons.Groups, "修改班级名称", c.classEntity.name),
                    DialogMenuItem("cls-note", AppIcons.Info, "修改备注", c.classEntity.note.ifEmpty { "暂无备注" }),
                    DialogMenuItem("cls-rule", AppIcons.Qr, "学号规则", "前缀 ${c.classEntity.prefix.ifEmpty { "无" }} · 补零 ${c.classEntity.digits} 位 · 回收空号 ${if (c.classEntity.recycle) "开" else "关"}"),
                    DialogMenuItem("cls-renumber", AppIcons.Restore, "一键重排学号", "按当前顺序重新连续编号"),
                    DialogMenuItem("cls-del", AppIcons.Delete, "删除班级", "${c.studentCount} 名学生 · ${c.assignmentCount} 份作业将一并删除", danger = true)
                ),
                onPick = { act ->
                    when (act) {
                        "cls-rename" -> dialog = DialogConfig(
                            "修改班级名称",
                            DialogBody.Input(c.classEntity.name, "班级名称", "") {
                                if (it.isEmpty()) { ToastBus.show("请输入班级名称"); false }
                                else { vm.renameClass(c.classEntity.id, it); true }
                            },
                            confirm = "确定"
                        )
                        "cls-note" -> dialog = DialogConfig(
                            "修改备注",
                            DialogBody.Input(c.classEntity.note, "可由：2025 秋季 · 数学", "") {
                                vm.updateClassNote(c.classEntity.id, it); true
                            },
                            confirm = "确定"
                        )
                        "cls-rule" -> {
                            ruleDialogClass = c.classEntity.id
                            dialog = null
                        }
                        "cls-renumber" -> dialog = DialogConfig(
                            "重排${c.classEntity.name}学号？",
                            DialogBody.Text(
                                "将按当前顺序重编为 01-${c.studentCount.toString().padStart(c.classEntity.digits.coerceIn(1, 4), '0')}（${c.studentCount} 人）。\n" +
                                    "已打印贴纸上的学号将与系统不一致，建议重新打印；二维码按学生身份定位，扫码录入仍能对应到正确的人。"
                            ),
                            cancel = "取消", confirm = "重排",
                            onConfirm = { vm.renumberClass(c.classEntity.id) }
                        )
                        "cls-del" -> dialog = DialogConfig(
                            "删除${c.classEntity.name}？",
                            DialogBody.Text("${c.studentCount} 名学生、${c.assignmentCount} 份作业及全部记录将一并删除，不可撤销。"),
                            cancel = "取消", confirm = "删除",
                            onConfirm = { vm.deleteClass(c.classEntity.id) }
                        )
                    }
                }
            ),
            cancel = "关闭"
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("班级")
        Box(Modifier.fillMaxSize()) {
            if (classCards.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                    EmptyState(AppIcons.School, "还没有班级", "点右下角新建一个班级开始使用")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = LayoutSpacing.FabListBottom)
                ) {
                    items(classCards, key = { it.classEntity.id }) { card ->
                        HwtCard(modifier = Modifier.padding(horizontal = LayoutSpacing.Screen, vertical = LayoutSpacing.CardGap)) {
                            ListItemRow(
                                avatar = null,
                                title = card.classEntity.name,
                                sub = "${card.studentCount} 名学生 · ${card.assignmentCount} 份作业",
                                onClick = {
                                    vm.switchClass(card.classEntity.id)
                                    nav.navigate("students/${card.classEntity.id}")
                                },
                                onLongClick = { openClassMenu(card) },
                                trailing = {
                                    Icon(
                                        AppIcons.ChevronRight,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                minHeight = LayoutSpacing.CardRowHeight
                            )
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(LayoutSpacing.FabMargin)) {
                HwtFab(AppIcons.Add, "新建班级") { newClassDialog = true }
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }

    if (newClassDialog) {
        NewClassDialog(
            onCreate = { name, prefix, digits, recycle ->
                vm.createClass(name, prefix, digits, recycle)
            },
            onDismiss = { newClassDialog = false }
        )
    }

    val ruleClass = ruleDialogClass
    val ruleEntity = classCards.firstOrNull { it.classEntity.id == ruleClass }?.classEntity
    if (ruleClass != null && ruleEntity != null) {
        ClassRuleDialog(
            initial = ruleEntity,
            onSave = { prefix, digits, recycle ->
                vm.updateClassRule(ruleClass, prefix, digits, recycle)
                ruleDialogClass = null
            },
            onDismiss = { ruleDialogClass = null }
        )
    }
}

@Composable
fun NewClassDialog(
    onCreate: (String, String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("2") }
    var recycle by remember { mutableStateOf(false) }
    FormDialog(title = "新建班级", onDismiss = onDismiss) {
        Text("班级名称", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        HwtTextField(name, { name = it }, "例：五班（3）")
        Spacer(Modifier.height(12.dp))
        SampleForm(prefix, { prefix = it }, digits, { digits = it }, recycle, { recycle = it })
        HwtButton(
            "创建",
            onClick = {
                if (name.isBlank()) { ToastBus.show("请输入班级名称"); return@HwtButton }
                onCreate(name, prefix, digits.toIntOrNull()?.coerceIn(1, 4) ?: 2, recycle)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ClassRuleDialog(
    initial: ClassEntity,
    onSave: (String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var prefix by remember { mutableStateOf(initial.prefix) }
    var digits by remember { mutableStateOf(initial.digits.toString()) }
    var recycle by remember { mutableStateOf(initial.recycle) }
    FormDialog(title = "学号规则", onDismiss = onDismiss) {
        SampleForm(prefix, { prefix = it }, digits, { digits = it }, recycle, { recycle = it })
        HwtButton(
            "保存",
            onClick = {
                onSave(prefix, digits.toIntOrNull()?.coerceIn(1, 4) ?: 2, recycle)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FormDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp)
        ) {
            Text(title, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SampleForm(
    prefix: String,
    onPrefix: (String) -> Unit,
    digits: String,
    onDigits: (String) -> Unit,
    recycle: Boolean,
    onRecycle: (Boolean) -> Unit
) {
    Text("学号前缀（可空）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    HwtTextField(prefix, onPrefix, "如：43")
    Spacer(Modifier.height(12.dp))
    Text("补零位数", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    HwtTextField(digits, { onDigits(it.filter { c -> c.isDigit() }.take(1)) }, "2", numeric = true)
    Spacer(Modifier.height(4.dp))
    val d = digits.toIntOrNull()?.coerceIn(1, 4) ?: 2
    val sample = listOf(1, 2, 3).joinToString("、") { prefix + it.toString().padStart(d, '0') }
    Text("示例学号：$sample…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("回收空号", fontSize = 14.sp, modifier = Modifier.weight(1f))
        HwtSwitch(recycle, onRecycle)
    }
    Spacer(Modifier.height(16.dp))
}
