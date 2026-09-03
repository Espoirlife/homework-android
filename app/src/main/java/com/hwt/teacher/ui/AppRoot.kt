package com.hwt.teacher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hwt.teacher.ui.components.BottomNavBar
import com.hwt.teacher.ui.components.NavItem
import com.hwt.teacher.ui.components.ToastHost
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.vm.AppViewModel
import com.hwt.teacher.vm.ReportViewModel

@Composable
fun AppRoot(vm: AppViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateCompat()
    Box(Modifier.fillMaxSize()) {
        if (!settings.onboarded) {
            InsetScreen { OnboardingScreen() }
        } else {
            MainNav()
        }
        ToastHost()
    }
}

@Composable
private fun InsetScreen(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) { content() }
}

@Composable
fun MainNav(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            Box(Modifier.fillMaxSize().statusBarsPadding()) { MainTabs(nav) }
        }
        composable("entry/{assignmentId}") {
            InsetScreen { EntryScreen(nav) }
        }
        composable("students/{classId}") {
            InsetScreen { StudentsScreen(nav) }
        }
        composable("scan/{assignmentId}") {
            ScanScreen(nav)
        }
        composable("qr") {
            InsetScreen { QrPrintScreen(nav) }
        }
        composable("webdav") {
            InsetScreen { WebdavScreen(nav) }
        }
        composable("backup") {
            InsetScreen { BackupScreen(nav) }
        }
        composable("wizard") {
            InsetScreen { WizardScreen(nav) }
        }
    }
}

@Composable
fun MainTabs(nav: NavHostController) {
    hiltViewModel<ReportViewModel>()
    var tab by rememberSaveable { mutableStateOf("class") }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                "homework" -> HomeworkScreen(
                    onOpenEntry = { id -> nav.navigate("entry/$id") },
                    onAddAssignment = { id -> nav.navigate("entry/$id") }
                )
                "report" -> ReportScreen(
                    initialPersonId = null,
                    onPersonConsumed = { }
                )
                "settings" -> SettingsScreen(nav)
                else -> ClassStudentsScreen(nav = nav)
            }
        }
        BottomNavBar(
            items = listOf(
                NavItem("class", "班级", Icons.Filled.Groups),
                NavItem("homework", "作业", Icons.AutoMirrored.Filled.Assignment),
                NavItem("report", "报表", Icons.Filled.BarChart),
                NavItem("settings", "设置", Icons.Filled.Settings)
            ),
            selected = tab,
            onSelect = { tab = it }
        )
    }
}

// 统一图标常量供各屏幕使用
object AppIcons {
    val Close = Icons.Filled.Close
    val Add = Icons.Filled.Add
    val Qr = Icons.Filled.QrCode
    val Print = Icons.Filled.Print
    val Check = Icons.Filled.Check
    val School = Icons.Filled.School
    val Groups = Icons.Filled.Groups
    val Assignment = Icons.AutoMirrored.Filled.Assignment
    val Folder = Icons.Filled.Folder
    val Cloud = Icons.Filled.Cloud
    val CloudUp = Icons.Filled.CloudUpload
    val CloudDown = Icons.Filled.CloudDownload
    val Restore = Icons.Filled.Restore
    val Visibility = Icons.Filled.Visibility
    val Delete = Icons.Filled.Delete
    val Edit = Icons.Filled.Edit
    val ChevronRight = Icons.Filled.ChevronRight
    val Info = Icons.Filled.Info
    val Lock = Icons.Filled.Lock
    val Sync = Icons.Filled.Sync
    val FilePresent = Icons.Filled.FilePresent
    val Cameraswitch = Icons.Filled.Cameraswitch
}
