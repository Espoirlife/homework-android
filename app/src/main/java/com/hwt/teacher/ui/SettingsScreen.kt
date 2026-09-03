package com.hwt.teacher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.BuildConfig
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.util.DateUtil
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.DialogOption
import com.hwt.teacher.ui.components.GroupTitle
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtDivider
import com.hwt.teacher.ui.components.NavRow
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.ui.components.SelectPill
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavHostController, vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = vm.buildBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                vm.importBackup(text)
            }
        }
    }

    fun numOptions(values: List<Int>, suffix: String = "") =
        values.map { DialogOption(it.toString(), "$it$suffix") }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("设置")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GroupTitle("记录默认值")
            RowField("默认完成状态") {
                SelectPill(Completion.label(settings.defaultCompletion)) {
                    dialog = DialogConfig(
                        "默认完成状态",
                        DialogBody.Options(
                            Completion.ALL.map { DialogOption(it, Completion.label(it)) },
                            settings.defaultCompletion
                        ) { v -> vm.updateDefaults(v, settings.defaultCorrection) },
                        cancel = "关闭"
                    )
                }
            }
            RowField("默认订正状态") {
                SelectPill(Correction.label(settings.defaultCorrection)) {
                    dialog = DialogConfig(
                        "默认订正状态",
                        DialogBody.Options(
                            Correction.ALL.map { DialogOption(it, Correction.label(it)) },
                            settings.defaultCorrection
                        ) { v -> vm.updateDefaults(settings.defaultCompletion, v) },
                        cancel = "关闭"
                    )
                }
            }
            Text(
                "修改后立即作用于所有尚无记录的学生，已手动改过的记录不受影响。评级永远默认「未评」。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HwtDivider()
            GroupTitle("二维码")
            NavRow(AppIcons.Qr, "二维码打印", "排版参数、预览与打印当前班级贴纸") { nav.navigate("qr") }
            HwtDivider()
            GroupTitle("云同步")
            NavRow(
                AppIcons.Cloud,
                "WebDAV 配置",
                if (settings.webdavEnabled) {
                    if (settings.webdavUrl.isNotEmpty()) "已启用 · ${settings.webdavUrl}" else "已启用 · 尚未填写地址"
                } else {
                    "未启用"
                }
            ) { nav.navigate("webdav") }
            NavRow(
                AppIcons.Sync,
                "自动备份",
                if (settings.autoBackup) "开 · 上次上传 ${DateUtil.stamp(settings.lastBackupAt)}" else "关"
            ) { nav.navigate("backup") }
            HwtDivider()
            GroupTitle("数据管理")
            NavRow(AppIcons.CloudUp, "导出备份文件", "hwt-backup · JSON · 不含 WebDAV 密码") {
                exportLauncher.launch(DateUtil.backupName())
            }
            NavRow(AppIcons.CloudDown, "从文件恢复", "将覆盖当前本地数据") {
                dialog = DialogConfig(
                    "从文件恢复？",
                    DialogBody.Text("将校验 format（hwt-backup）与 version，校验通过后覆盖当前本地数据。"),
                    cancel = "取消", confirm = "选择文件",
                    onConfirm = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )
            }
            HwtDivider()
            GroupTitle("关于")
            RowField("版本") {
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}
