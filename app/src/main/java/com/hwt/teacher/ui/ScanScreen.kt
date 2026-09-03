package com.hwt.teacher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.Grade
import com.hwt.teacher.data.Marks
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.IconButton
import com.hwt.teacher.ui.components.MarkGroupPanel
import com.hwt.teacher.ui.components.collectAsStateCompat
import com.hwt.teacher.ui.theme.ScanBg
import com.hwt.teacher.ui.theme.ScanToastBg
import com.hwt.teacher.ui.theme.ScanViewfinder
import com.hwt.teacher.ui.theme.StDone
import com.hwt.teacher.vm.ScanViewModel

@Composable
fun ScanScreen(nav: NavHostController, vm: ScanViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pick by vm.scanPick.collectAsStateCompat()
    val summary by vm.scanSummary.collectAsStateCompat()
    val lastMsg by vm.lastMessage.collectAsStateCompat()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Column(
        Modifier
            .fillMaxSize()
            .background(ScanBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(AppIcons.Close, "关闭") { nav.popBackStack() }
            Text("扫码录入", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = Color(0xFFE6E0E9))
        }
        if (!hasPermission) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("需要相机权限才能扫码", color = Color(0xFFE6E0E9), fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                HwtButton("授予权限", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Text(
                    if (summary.isEmpty()) "请至少选一项" else "扫到即标记为：$summary",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFCAC4D0)
                )
                Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    MarkGroupPanel(
                        "完成情况",
                        Completion.ALL.map { it to Completion.label(it) },
                        pick[Marks.GROUP_COMPLETION],
                        { vm.toggleScanPick(Marks.GROUP_COMPLETION, it) },
                        dark = true
                    )
                    MarkGroupPanel(
                        "订正情况",
                        Correction.ALL.map { it to Correction.label(it) },
                        pick[Marks.GROUP_CORRECTION],
                        { vm.toggleScanPick(Marks.GROUP_CORRECTION, it) },
                        dark = true
                    )
                    MarkGroupPanel(
                        "评级",
                        Grade.ALL.map { it to Grade.label(it) },
                        pick[Marks.GROUP_GRADE],
                        { vm.toggleScanPick(Marks.GROUP_GRADE, it) },
                        dark = true
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ScanViewfinder)
                    ) {
                        val previewView = remember {
                            PreviewView(context).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                        }
                        DisposableEffect(cameraFacing) {
                            val future = ProcessCameraProvider.getInstance(context)
                            var provider: ProcessCameraProvider? = null
                            future.addListener({
                                val p = future.get()
                                provider = p
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                                    val text = ScanDecoder.decode(image)
                                    image.close()
                                    if (text != null) vm.onScan(text)
                                }
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(cameraFacing)
                                    .build()
                                p.unbindAll()
                                p.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                            }, ContextCompat.getMainExecutor(context))
                            onDispose { provider?.unbindAll() }
                        }
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (lastMsg != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScanToastBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Check, null, tint = StDone, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(lastMsg ?: "", fontSize = 14.sp, color = Color(0xFFE6E0E9))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HwtButton("停止扫码", onClick = { nav.popBackStack() }, filled = true, tall = true, modifier = Modifier.weight(1f))
                    IconButton(AppIcons.Cameraswitch, "切换摄像头") {
                        cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                }
            }
        }
    }
}
