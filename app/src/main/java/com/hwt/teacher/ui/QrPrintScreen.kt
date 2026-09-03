package com.hwt.teacher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.util.QrCodec
import com.hwt.teacher.util.QrGenerator
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogOption
import com.hwt.teacher.ui.components.EmptyState
import com.hwt.teacher.ui.components.GroupTitle
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtDivider
import com.hwt.teacher.ui.components.HwtSwitch
import com.hwt.teacher.ui.components.IconButton
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.ui.components.SelectPill
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.QrViewModel

@Composable
fun QrPrintScreen(nav: NavHostController, vm: QrViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val classes by vm.classes.collectAsStateCompat()
    val currentClass by vm.currentClass.collectAsStateCompat()
    val settings by vm.settings.collectAsStateCompat()
    val students by vm.students.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }

    fun numOptions(values: List<Int>, suffix: String = "") =
        values.map { DialogOption(it.toString(), "$it$suffix") }

    val rows = QrSheetLayout.rowsPerPage(settings.qrPerRow, settings.qrMargin, settings.qrWithNo)
    val per = settings.qrPerRow * rows
    val page = students.take(per)
    val cutColor = MaterialTheme.colorScheme.outline

    fun doPrint() {
        if (currentClass == null || students.isEmpty()) return
        QrPrintHelper.print(context, currentClass!!, students, settings.qrPerRow, settings.qrMargin, settings.qrLevel, settings.qrWithNo)
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("二维码打印", onBack = { nav.popBackStack() }, actions = {
            IconButton(AppIcons.Print, "打印") { doPrint() }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RowField("班级") {
                SelectPill(currentClass?.name ?: "无") {
                    dialog = DialogConfig(
                        "切换班级",
                        DialogBody.Options(
                            classes.map { DialogOption(it.id, it.name) },
                            currentClass?.id ?: "",
                            onPick = { vm.switchClass(it) }
                        ),
                        cancel = "关闭"
                    )
                }
            }
            HwtDivider()
            GroupTitle("排版参数")
            RowField("每行个数") {
                SelectPill(settings.qrPerRow.toString()) {
                    dialog = DialogConfig(
                        "每行个数",
                        DialogBody.Options(numOptions((2..8).toList()), settings.qrPerRow.toString()) { v ->
                            vm.updateParams(v.toInt(), settings.qrMargin, settings.qrLevel, settings.qrWithNo)
                        },
                        cancel = "关闭"
                    )
                }
            }
            RowField("纸张大小") {
                Text("A4", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            RowField("容错级别") {
                SelectPill(settings.qrLevel) {
                    dialog = DialogConfig(
                        "容错级别",
                        DialogBody.Options(listOf("L", "M", "Q", "H").map { DialogOption(it, it) }, settings.qrLevel) { v ->
                            vm.updateParams(settings.qrPerRow, settings.qrMargin, v, settings.qrWithNo)
                        },
                        cancel = "关闭"
                    )
                }
            }
            RowField("页边距") {
                SelectPill("${settings.qrMargin} mm") {
                    dialog = DialogConfig(
                        "页边距",
                        DialogBody.Options(numOptions(QrSheetLayout.MARGIN_OPTIONS, " mm"), settings.qrMargin.toString()) { v ->
                            vm.updateParams(settings.qrPerRow, v.toInt(), settings.qrLevel, settings.qrWithNo)
                        },
                        cancel = "关闭"
                    )
                }
            }
            RowField("包含学号") {
                HwtSwitch(settings.qrWithNo) {
                    vm.updateParams(settings.qrPerRow, settings.qrMargin, settings.qrLevel, it)
                }
            }
            HwtDivider()
            if (students.isEmpty()) {
                EmptyState(AppIcons.Qr, "没有学生", "先导入名单才能生成贴纸")
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    page.chunked(settings.qrPerRow).forEach { rowStudents ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowStudents.forEach { s ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .drawBehind {
                                            drawRect(
                                                color = cutColor,
                                                style = Stroke(
                                                    width = 1f,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                                )
                                            )
                                        }
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val code = QrCodec.encode(currentClass!!.id, s.student.id, s.code, s.student.name)
                                    val bmp = remember(code, settings.qrLevel) { QrGenerator.generate(code, 160, settings.qrLevel) }
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    )
                                    Text(s.student.name, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1)
                                    if (settings.qrWithNo) {
                                        Text(s.code, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            repeat(settings.qrPerRow - rowStudents.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Text(
                    "A4 排版预览 · ${settings.qrPerRow} × $rows = $per 枚 / 页 · 共 ${students.size} 人 · 虚线为裁切边",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HwtButton("预览", onClick = { doPrint() }, filled = false, modifier = Modifier.weight(1f))
                HwtButton("打印", onClick = { doPrint() }, filled = true, modifier = Modifier.weight(1f))
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}
