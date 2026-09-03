package com.hwt.teacher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hwt.teacher.ui.components.HwtButton
import com.hwt.teacher.ui.components.HwtSwitch
import com.hwt.teacher.ui.components.HwtTextField
import com.hwt.teacher.ui.components.RowField
import com.hwt.teacher.vm.AppViewModel

@Composable
fun OnboardingScreen(vm: AppViewModel = hiltViewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var prefix by rememberSaveable { mutableStateOf("") }
    var digits by rememberSaveable { mutableStateOf("2") }
    var recycle by rememberSaveable { mutableStateOf(false) }

    val d = digits.toIntOrNull()?.coerceIn(1, 4) ?: 2
    val sample = listOf(1, 2, 3).joinToString("、") { prefix + it.toString().padStart(d, '0') }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(AppIcons.School, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("先建一个班级", fontSize = 24.sp, lineHeight = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "建好班级后就能导入学生名单、布置作业。学号会按下面的规则自动编排，之后可以随时修改。",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Text("班级名称", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        HwtTextField(name, { name = it }, "例：三班（2）")
        Spacer(Modifier.height(16.dp))
        Text("备注（可选）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        HwtTextField(note, { note = it }, "例：2025 秋季 · 数学")
        Spacer(Modifier.height(16.dp))
        Text("学号编号规则", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                HwtTextField(prefix, { prefix = it }, "前缀（可空）")
            }
            Box(Modifier.weight(1f)) {
                HwtTextField(digits, { digits = it.filter { c -> c.isDigit() }.take(1) }, "补零位数", numeric = true)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("示例：$sample…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        RowField("回收空号") {
            HwtSwitch(recycle) { recycle = it }
        }
        Spacer(Modifier.height(24.dp))
        HwtButton(
            text = "创建并开始",
            onClick = {
                if (name.isBlank()) {
                    com.hwt.teacher.ui.components.ToastBus.show("请先填写班级名称")
                    return@HwtButton
                }
                vm.completeOnboarding(name, note, prefix, digits.toIntOrNull()?.coerceIn(1, 4) ?: 2, recycle)
            },
            filled = true,
            tall = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
