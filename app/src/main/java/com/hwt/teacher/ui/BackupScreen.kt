package com.hwt.teacher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import com.hwt.teacher.util.CloudFile
import com.hwt.teacher.util.DateUtil
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.Banner
import com.hwt.teacher.ui.components.DialogBody
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.GroupTitle
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtDivider
import com.hwt.teacher.ui.components.HwtSwitch
import com.hwt.teacher.ui.components.HwtTextButton
import com.hwt.teacher.ui.components.IconButton
import com.hwt.teacher.ui.components.NavRow
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(nav: NavHostController, vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateCompat()

    var dialog by remember { mutableStateOf<DialogConfig?>(null) }
    var cloudFiles by remember { mutableStateOf<List<CloudFile>>(emptyList()) }

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

    fun refreshCloud() {
        if (!settings.webdavEnabled) return
        vm.listCloud { cloudFiles = it }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("备份管理", onBack = { nav.popBackStack() }, actions = {
            IconButton(AppIcons.CloudUp, "立即上传") { vm.uploadNow(); refreshCloud() }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GroupTitle("自动备份")
            RowField("变更后自动上传") {
                HwtSwitch(settings.autoBackup) { vm.updateAutoBackup(it) }
            }
            Text(
                "停止操作 30 秒后合并上传一次，且两次自动上传至少间隔 5 分钟。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RowField("上次上传") {
                Text(DateUtil.stamp(settings.lastBackupAt), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            if (settings.webdavLastError != null) {
                Banner(AppIcons.Info, "最近备份失败：${settings.webdavLastError}", com.hwt.teacher.ui.components.BannerTone.Err)
            }
            HwtDivider()
            GroupTitle("本地文件")
            NavRow(AppIcons.CloudUp, "导出备份文件", "选择保存位置后写入 JSON") {
                exportLauncher.launch(DateUtil.backupName())
            }
            NavRow(AppIcons.CloudDown, "从文件恢复", "校验 format 后覆盖本地数据") {
                dialog = DialogConfig(
                    "从文件恢复？",
                    DialogBody.Text("将校验 format（hwt-backup）与 version，校验通过后覆盖当前本地数据。"),
                    cancel = "取消", confirm = "选择文件",
                    onConfirm = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
            HwtDivider()
            GroupTitle("云端备份")
            if (!settings.webdavEnabled) {
                Text(
                    "尚未启用 WebDAV，只能使用本地导出 / 导入。",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (cloudFiles.isEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("云端还没有备份，修改数据后会自动上传一份", modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HwtTextButton("刷新") { refreshCloud() }
                }
            } else {
                cloudFiles.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (f.latest) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                AppIcons.FilePresent,
                                null,
                                tint = if (f.latest) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(
                                "${formatSize(f.size)} · ${DateUtil.stamp(f.lastModified)}${if (f.latest) " · 最新" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HwtTextButton("恢复") {
                            dialog = DialogConfig(
                                "从云端恢复？",
                                DialogBody.Text("${f.name}\n将覆盖当前本地数据。"),
                                cancel = "取消", confirm = "恢复",
                                onConfirm = { vm.restoreFromCloud(f.name); refreshCloud() }
                            )
                        }
                    }
                }
            }
            Banner(
                AppIcons.Info,
                "备份为 hwt-backup 格式（version 1），包含设置、班级、学生、作业与记录，不包含 WebDAV 密码。"
            )
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
