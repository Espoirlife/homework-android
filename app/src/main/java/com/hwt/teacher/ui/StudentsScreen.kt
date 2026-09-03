package com.hwt.teacher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.data.StudentView
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogMenuItem
import com.hwt.teacher.ui.components.EmptyState
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.LayoutSpacing
import com.hwt.teacher.ui.components.ListItemRow
import com.hwt.teacher.ui.components.SectionTitle
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.StudentsViewModel
import kotlinx.coroutines.launch

/** 班级页的下一级：某个班级的学生名单（FR-2.1）。 */
@Composable
fun StudentsScreen(nav: NavHostController, vm: StudentsViewModel = hiltViewModel()) {
    val classEntity by vm.classEntity.collectAsStateCompat()
    val students by vm.students.collectAsStateCompat()
    val ready by vm.ready.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }
    val scope = rememberCoroutineScope()

    fun openStudentMenu(s: StudentView) {
        dialog = DialogConfig(
            title = "${s.code}\u3000${s.student.name}",
            body = DialogBody.Menu(
                items = listOf(
                    DialogMenuItem("stu-name", AppIcons.Groups, "修改姓名", s.student.name),
                    DialogMenuItem("stu-no", AppIcons.Qr, "修改学号序号", "当前序号 ${s.student.seq}，学号 ${s.code}"),
                    DialogMenuItem("stu-note", AppIcons.Info, "修改备注", s.student.note.ifEmpty { "暂无备注" }),
                    DialogMenuItem("stu-del", AppIcons.Delete, "删除学生", "会同时删除其全部作业记录", danger = true)
                ),
                onPick = { act ->
                    when (act) {
                        "stu-name" -> dialog = DialogConfig(
                            "修改姓名",
                            DialogBody.Input(s.student.name, "学生姓名", "学号 ${s.code} 保持不变") {
                                if (it.isEmpty()) { ToastBus.show("请输入姓名"); false }
                                else { vm.updateStudentName(s.student.id, it); true }
                            },
                            confirm = "确定"
                        )
                        "stu-no" -> dialog = DialogConfig(
                            "修改学号序号",
                            DialogBody.Input(s.student.seq.toString(), "序号（1-9999）", "当前学号 ${s.code}。已打印的二维码不受影响。", numeric = true) {
                                val n = it.toIntOrNull()
                                if (n == null) { ToastBus.show("序号请输入数字"); false }
                                else if (n < 1 || n > 9999) { ToastBus.show("序号需在 1-9999 之间"); false }
                                else {
                                    scope.launch {
                                        val err = vm.updateStudentSeq(s.student.id, n)
                                        if (err != null) ToastBus.show(err)
                                    }
                                    true
                                }
                            },
                            confirm = "确定"
                        )
                        "stu-note" -> dialog = DialogConfig(
                            "修改备注",
                            DialogBody.Input(s.student.note, "如：组长、需重点关注", "留空即清除备注") {
                                vm.updateStudentNote(s.student.id, it); true
                            },
                            confirm = "确定"
                        )
                        "stu-del" -> dialog = DialogConfig(
                            "删除${s.student.name}？",
                            DialogBody.Text("学号 ${s.code} 的全部作业记录会一并删除，不可撤销。"),
                            cancel = "取消", confirm = "删除",
                            onConfirm = { vm.deleteStudent(s.student.id) }
                        )
                    }
                }
            ),
            cancel = "关闭"
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(classEntity?.name ?: "学生名单", onBack = { nav.popBackStack() })
        if (!ready) {
            Box(Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (students.isEmpty()) {
                    item {
                        EmptyState(AppIcons.Groups, "名单是空的", "可以从 Excel 导入，或逐个添加")
                    }
                } else {
                    item { SectionTitle("共 ${students.size} 人") }
                    items(students, key = { it.student.id }) { s ->
                        ListItemRow(
                            avatar = s.code,
                            title = s.student.name,
                            sub = s.student.note.ifEmpty { null },
                            onClick = { openStudentMenu(s) }
                        )
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(LayoutSpacing.Screen),
                        horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.ButtonGap)
                    ) {
                        HwtButton("Excel 导入", onClick = { nav.navigate("wizard") }, filled = false, modifier = Modifier.weight(1f))
                        HwtButton("添加学生", onClick = {
                            dialog = DialogConfig(
                                "添加学生",
                                DialogBody.Input("", "学生姓名", "学号将自动编排") {
                                    if (it.isEmpty()) { ToastBus.show("请输入姓名"); false }
                                    else { vm.addStudent(it); true }
                                },
                                confirm = "确定"
                            )
                        }, filled = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}
