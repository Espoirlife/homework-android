package com.hwt.teacher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.ui.components.AppTopBar
import com.hwt.teacher.ui.components.Banner
import com.hwt.teacher.ui.components.BannerTone
import com.hwt.teacher.ui.components.DialogConfig
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtDialogHost
import com.hwt.teacher.ui.components.HwtSwitch
import com.hwt.teacher.ui.components.HwtTextField
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.SettingsViewModel

@Composable
fun WebdavScreen(nav: NavHostController, vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateCompat()

    var enabled by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogConfig?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enabled = settings.webdavEnabled
        url = settings.webdavUrl
        user = settings.webdavUsername
        loaded = true
    }
    LaunchedEffect(loaded) {
        if (loaded) pass = vm.webdavPassword()
    }

    fun toggleEnabled() {
        enabled = !enabled
        if (!enabled) vm.toggleWebdav(false)
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("WebDAV 配置", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RowField("启用 WebDAV 同步") {
                HwtSwitch(enabled) { toggleEnabled() }
            }
            if (!enabled) {
                Text(
                    "启用后可填写服务器信息。关闭时不会上传任何数据。",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("目录地址", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                Spacer(Modifier.height(4.dp))
                HwtTextField(url, { url = it }, "https://dav.jianguoyun.com/dav/hwt/", modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    "需以 / 结尾，备份文件直接放在该目录下。坚果云地址形如 https://dav.jianguoyun.com/dav/文件夹名/，密码需在坚果云「安全选项」中生成「应用密码」。",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("用户名", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                Spacer(Modifier.height(4.dp))
                HwtTextField(user, { user = it }, "账号", modifier = Modifier.padding(horizontal = 16.dp))
                Text("密码", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                            visualTransformation = if (pwVisible) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                    Spacer(Modifier.height(0.dp))
                    com.hwt.teacher.ui.components.IconButton(if (pwVisible) AppIcons.Visibility else AppIcons.Visibility, "显示密码") {
                        pwVisible = !pwVisible
                    }
                }
                Banner(
                    AppIcons.Lock,
                    "密码经 Android KeyStore 加密后存入 EncryptedSharedPreferences，不会写入备份文件。"
                )
                when (settings.webdavTested) {
                    "ok" -> Banner(AppIcons.Check, "连接正常，目录可写入。", BannerTone.Ok)
                    "fail" -> Banner(
                        AppIcons.Info,
                        settings.webdavLastError ?: "连接失败：请检查地址、用户名或密码。",
                        BannerTone.Err
                    )
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HwtButton("测试连接", onClick = { vm.testConnection(url, user, pass) }, filled = false, modifier = Modifier.weight(1f))
                    HwtButton("保存", onClick = { vm.saveWebdav(true, url, user, pass); nav.popBackStack() }, filled = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    HwtDialogHost(dialog) { dialog = null }
}
