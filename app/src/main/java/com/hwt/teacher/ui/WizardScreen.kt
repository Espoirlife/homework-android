package com.hwt.teacher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.Banner
import com.hwt.teacher.ui.components.BannerTone
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogOption
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtDivider
import com.hwt.teacher.ui.components.NavRow
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.ui.components.SectionTitle
import com.hwt.teacher.ui.components.StatCard
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.ui.theme.ErrBannerBg
import com.hwt.teacher.ui.theme.StMiss
import com.hwt.teacher.vm.AppViewModel
import com.hwt.teacher.vm.WizardViewModel

@Composable
fun WizardScreen(nav: NavHostController, vm: WizardViewModel = hiltViewModel(), appVm: AppViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateCompat()
    val currentClass by appVm.currentClass.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME).let { idx ->
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else "名单.xlsx"
                }
            } ?: "名单.xlsx"
            context.contentResolver.openInputStream(uri)?.let { vm.loadSheet(name, it) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("导入学生名单", onBack = { nav.popBackStack() })
        Stepper(state.step)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (state.step) {
                1 -> {
                    SectionTitle("选择名单文件")
                    NavRow(AppIcons.Folder, state.fileName ?: "从文件选择器选取", if (state.fileName != null) "${state.rows.size} 行 · ${state.cols.size} 列" else "支持 .xlsx / .xls，只读取第一个工作表") {
                        fileLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*"))
                    }
                    Banner(
                        AppIcons.Info,
                        "名单只需一列姓名即可。表头写「姓名」和「备注」时能自动识别。"
                    )
                    if (currentClass != null) {
                        Text(
                            "导入到：${currentClass?.name}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        HwtButton("下一步", onClick = { vm.next() }, enabled = state.fileName != null, tall = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                2 -> {
                    SectionTitle("确认列对应关系")
                    state.cols.forEachIndexed { i, col ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dialog = DialogConfig(
                                        "该列用途",
                                        DialogBody.Options(
                                            listOf("姓名", "备注", "忽略").map { DialogOption(it, "${it}列") },
                                            col.role
                                        ) { role -> vm.pickCol(i, role) },
                                        cancel = "关闭"
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(col.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                col.samples.joinToString("、"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            RoleBadge(col.role)
                        }
                        HwtDivider(MaterialTheme.colorScheme.outlineVariant)
                    }
                    Text(
                        "点任意一行可重新指定该列的用途。未识别到姓名列时默认取第一列。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RowField("学号编号") {
                        Text("按当前班规则续号", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HwtButton("上一步", onClick = { vm.prev() }, filled = false, modifier = Modifier.weight(1f))
                        HwtButton("预览", onClick = { vm.next() }, filled = true, modifier = Modifier.weight(1f))
                    }
                }
                else -> {
                    val ok = state.preview.count { it.reason.isEmpty() }
                    val skipped = state.preview.size - ok
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("${state.preview.size}", "读到行数")
                        StatCard("$ok", "将导入")
                        StatCard("$skipped", "已跳过")
                    }
                    SectionTitle("逐行预览")
                    state.preview.forEachIndexed { i, r ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                            Text(
                                r.name.ifEmpty { "—" },
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                color = if (r.reason.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (r.note.isNotEmpty()) {
                                Text(r.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp), maxLines = 1)
                            }
                            when (r.reason) {
                                "empty" -> RoleBadge("空行", hit = false)
                                "dup" -> RoleBadge("重复", hit = false, danger = true)
                                "exist" -> RoleBadge("已存在", hit = false, danger = true)
                                else -> RoleBadge("✓", hit = true)
                            }
                        }
                    }
                    if (skipped > 0) {
                        Banner(AppIcons.Info, "空行与同名行会自动跳过，不会覆盖已有学生。", BannerTone.Err)
                    }
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HwtButton("上一步", onClick = { vm.prev() }, filled = false, modifier = Modifier.weight(1f))
                        HwtButton("导入 $ok 人", onClick = {
                            val c = currentClass
                            if (c != null) {
                                vm.commit(c.id)
                                nav.popBackStack()
                            }
                        }, filled = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}

@Composable
private fun Stepper(step: Int) {
    val names = listOf("选文件", "识别列", "预览确认")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        names.forEachIndexed { i, n ->
            if (i > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            val idx = i + 1
            val active = idx == step
            val done = idx < step
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> MaterialTheme.colorScheme.primary
                                done -> MaterialTheme.colorScheme.secondaryContainer
                                else -> Color(0x00000000)
                            }
                        )
                        .border(if (active || done) 0.dp else 1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (done) "✓" else "$idx",
                        fontSize = 12.sp,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else if (done) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    n,
                    fontSize = 12.sp,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String, hit: Boolean = true, danger: Boolean = false) {
    val bg = when {
        danger -> ErrBannerBg
        hit -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when {
        danger -> StMiss
        hit -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        fontSize = 11.sp,
        color = fg
    )
}
