package com.hwt.teacher.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.Grade
import com.hwt.teacher.data.Marks
import com.hwt.teacher.ui.theme.ErrBannerBg
import com.hwt.teacher.ui.theme.MdPrimary
import com.hwt.teacher.ui.theme.OkBannerBg
import com.hwt.teacher.ui.theme.OkBannerFg
import com.hwt.teacher.ui.theme.StDone
import com.hwt.teacher.ui.theme.StMiss
import com.hwt.teacher.ui.theme.StNone
import com.hwt.teacher.ui.theme.StPartial
import com.hwt.teacher.ui.theme.StPending
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

object LayoutSpacing {
    val Screen = 16.dp
    val SectionTop = 16.dp
    val SectionBottom = 8.dp
    val CardGap = 8.dp
    val CardRowHeight = 72
    val ButtonGap = 12.dp
    val FabMargin = 16.dp
    val FabListBottom = 88.dp
}

// ---------- 顶栏 ----------

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null || leadingIcon != null) {
            IconButton(
                icon = leadingIcon ?: Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                onClick = onBack ?: {}
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

@Composable
fun IconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun ClassSelector(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

// ---------- 页签 ----------

@Composable
fun HwtTabRow(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        tabs.forEach { (key, label) ->
            val active = key == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = LayoutSpacing.Screen, end = LayoutSpacing.Screen, top = LayoutSpacing.SectionTop, bottom = LayoutSpacing.SectionBottom),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = LayoutSpacing.Screen, end = LayoutSpacing.Screen, top = LayoutSpacing.SectionTop, bottom = LayoutSpacing.SectionBottom),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

// ---------- 列表项 ----------

@Composable
fun Avatar(text: String, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItemRow(
    avatar: String?,
    title: String,
    sub: String?,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    minHeight: Int = 64
) {
    val base = Modifier
        .fillMaxWidth()
        .height(minHeight.dp)
    val clickMod = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }
    Row(
        modifier = base.then(clickMod).padding(horizontal = LayoutSpacing.Screen, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatar != null) {
            Avatar(avatar)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (title.isNotEmpty()) {
                Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub != null) Spacer(Modifier.height(2.dp))
            }
            if (sub != null) {
                Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun HwtDivider(color: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun PillCount(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

// ---------- 卡片 ----------

@Composable
fun HwtCard(
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (animate) Modifier.animateContentSize() else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) { content() }
}

// ---------- 状态标 / 选择块 ----------

fun markColor(kind: String, value: String?): Color = when (kind) {
    Marks.GROUP_COMPLETION -> when (value) {
        Completion.DONE -> StDone
        Completion.PARTIAL -> StPartial
        else -> StMiss
    }
    Marks.GROUP_CORRECTION -> if (value == Correction.FIXED) StDone else StPending
    else -> if (value.isNullOrEmpty()) StNone else MdPrimary
}

fun markLabel(kind: String, value: String?): String = when (kind) {
    Marks.GROUP_COMPLETION -> Completion.mark(value)
    Marks.GROUP_CORRECTION -> Correction.mark(value)
    else -> Grade.mark(value)
}

@Composable
fun MarkBox(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 13.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarkChip(
    kind: String,
    value: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val color = markColor(kind, value)
    MarkBox(
        text = markLabel(kind, value),
        color = color,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

@Composable
fun StatusChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MarkGroupPanel(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onToggle: (String) -> Unit,
    dark: Boolean = false
) {
    Column {
        Text(
            label,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            fontSize = 12.sp,
            color = if (dark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (v, text) ->
                StatusChip(text, selected == v) { onToggle(v) }
            }
        }
    }
}

// ---------- 统计卡片 ----------

@Composable
fun RowScope.StatCard(value: String, label: String, compact: Boolean = false) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(if (compact) 6.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = if (compact) 20.sp else 24.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = if (compact) 26.sp else 30.sp
        )
        Text(
            label,
            fontSize = if (compact) 11.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- 按钮 ----------

@Composable
fun HwtButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    enabled: Boolean = true,
    tall: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    val bg = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor = if (filled) Color.Transparent else MaterialTheme.colorScheme.outline
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = 0.38f))
            .border(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.38f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(if (tall) 48.dp else 40.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (enabled) fg else fg.copy(alpha = 0.38f))
    }
}

@Composable
fun HwtTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .height(32.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    }
}

// ---------- FAB ----------

@Composable
fun HwtFab(icon: ImageVector, text: String? = null, onClick: () -> Unit) {
    if (text == null) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
        }
    } else {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
            text = { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ---------- 空态 / Banner / 导航行 ----------

@Composable
fun EmptyState(icon: ImageVector, title: String, sub: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(sub, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

enum class BannerTone { Info, Ok, Err }

@Composable
fun Banner(icon: ImageVector, text: String, tone: BannerTone = BannerTone.Info) {
    val bg = when (tone) {
        BannerTone.Ok -> OkBannerBg
        BannerTone.Err -> ErrBannerBg
        BannerTone.Info -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when (tone) {
        BannerTone.Ok -> OkBannerFg
        BannerTone.Err -> StMiss
        BannerTone.Info -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, lineHeight = 18.sp, color = fg)
    }
}

@Composable
fun NavRow(icon: ImageVector, title: String, sub: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    sub,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun RowField(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        value()
    }
}

@Composable
fun SelectPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun HwtSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val w = 52.dp; val h = 32.dp; val knob = 24.dp
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 4.dp)
                .size(knob)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ---------- 底部导航 ----------

data class NavItem(val key: String, val label: String, val icon: ImageVector)

@Composable
fun BottomNavBar(items: List<NavItem>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .height(80.dp)
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)
    ) {
        items.forEach { item ->
            val active = item.key == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(item.key) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 32.dp)
                        .clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        null,
                        tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    item.label,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- 输入框 ----------

@Composable
fun HwtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .height(48.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = if (numeric) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------- Toast ----------

object ToastBus {
    private val _msg = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val msg: kotlinx.coroutines.flow.StateFlow<String?> = _msg
    fun show(m: String) { _msg.value = m }
    fun clear() { _msg.value = null }
}

@Composable
fun BoxScope.ToastHost() {
    val msg by ToastBus.msg.collectAsStateCompat()
    if (msg != null) {
        LaunchedEffect(msg) {
            delay(1800)
            ToastBus.clear()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF322F35))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(msg ?: "", fontSize = 13.sp, color = Color(0xFFF5EFF7))
        }
    }
}

@Composable
fun <T> StateFlow<T>.collectAsStateCompat(): State<T> {
    val state = remember(this) { mutableStateOf(value) }
    LaunchedEffect(this) { this@collectAsStateCompat.collect { state.value = it } }
    return state
}

// ---------- 对话框 ----------

data class DialogOption(val v: String, val label: String)

data class DialogMenuItem(val act: String, val icon: ImageVector, val label: String, val sub: String, val danger: Boolean = false)

sealed interface DialogBody {
    data class Text(val text: String) : DialogBody
    data class Options(val options: List<DialogOption>, val value: String, val onPick: (String) -> Unit) : DialogBody
    data class Menu(val items: List<DialogMenuItem>, val onPick: (String) -> Unit) : DialogBody
    data class Input(
        val value: String,
        val placeholder: String,
        val hint: String,
        val numeric: Boolean = false,
        val onOk: (String) -> Boolean
    ) : DialogBody
}

data class DialogConfig(
    val title: String,
    val body: DialogBody,
    val cancel: String = "取消",
    val confirm: String? = null,
    val onConfirm: (() -> Unit)? = null
)

@Composable
fun HwtDialogHost(config: DialogConfig?, onDismiss: () -> Unit) {
    if (config == null) return
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(config.title, fontSize = 18.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(12.dp))
            when (val body = config.body) {
                is DialogBody.Text -> {
                    Text(body.text, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                }
                is DialogBody.Options -> {
                    body.options.forEach { opt ->
                        val sel = opt.v == body.value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { body.onPick(opt.v); onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(opt.label, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                is DialogBody.Menu -> {
                    body.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { body.onPick(item.act) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                item.icon,
                                null,
                                tint = if (item.danger) StMiss else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.label, fontSize = 15.sp, color = if (item.danger) StMiss else MaterialTheme.colorScheme.onSurface)
                                if (item.sub.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(item.sub, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                is DialogBody.Input -> {
                    var text by remember { mutableStateOf(body.value) }
                    HwtTextField(text, { text = it }, body.placeholder, numeric = body.numeric)
                    if (body.hint.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(body.hint, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        HwtTextButton(config.cancel) { onDismiss() }
                        if (config.confirm != null) {
                            Spacer(Modifier.width(8.dp))
                            HwtButton(config.confirm, onClick = {
                                if (body.onOk(text.trim()) != false) onDismiss()
                            })
                        }
                    }
                    return@Column
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                HwtTextButton(config.cancel) { onDismiss() }
                if (config.confirm != null && config.onConfirm != null) {
                    Spacer(Modifier.width(8.dp))
                    HwtButton(config.confirm, onClick = { config.onConfirm.invoke(); onDismiss() })
                }
            }
        }
    }
}
