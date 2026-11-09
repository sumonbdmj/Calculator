package com.simplecalculatordeign.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplecalculatordeign.ui.rememberCopier
import com.simplecalculatordeign.ui.rememberFeedback
import com.simplecalculatordeign.ui.theme.CalcTheme

/** Full-screen brand gradient. */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcTheme.colors.background),
        content = content,
    )
}

/** Standard screen frame: transparent scaffold over the gradient with a clean top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    if (titleContent != null) titleContent()
                    else Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        content = content,
    )
}

/** Gradient app name used on the calculator top bar and splash. */
@Composable
fun GradientTitle(text: String, style: TextStyle = MaterialTheme.typography.titleLarge) {
    Text(
        text = text,
        style = style.copy(brush = CalcTheme.colors.accentGradient, fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Rounded premium card with a hairline border. */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CalcTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (colors.isDark) 0.dp else 6.dp, shape, ambientColor = Color(0x1A4A00E0), spotColor = Color(0x224A00E0))
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Card with the brand gradient as background, white content. */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = CalcTheme.colors.display,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color(0x554A00E0), spotColor = Color(0x664A00E0))
            .clip(shape)
            .background(brush)
            .padding(contentPadding),
        content = content,
    )
}

enum class KeyStyle { Number, Function, Operator, Equals, Clear, Memory }

/**
 * Animated calculator key: springs down when pressed, gives sound/haptic feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalcKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KeyStyle = KeyStyle.Number,
    icon: ImageVector? = null,
    contentDescription: String = label,
    fontSize: Int = 26,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = CalcTheme.colors
    val feedback = rememberFeedback()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "keyScale")
    val shape = RoundedCornerShape(percent = 32)

    val (background, textColor) = when (style) {
        KeyStyle.Number -> SolidColor(colors.numberKey) to colors.numberKeyText
        KeyStyle.Function -> SolidColor(colors.functionKey) to colors.functionKeyText
        KeyStyle.Operator -> colors.operatorKey to Color.White
        KeyStyle.Equals -> colors.equalsKey to Color.White
        KeyStyle.Clear -> colors.clearKey to Color.White
        KeyStyle.Memory -> SolidColor(colors.memoryChip) to colors.memoryChipText
    }
    val elevated = style == KeyStyle.Operator || style == KeyStyle.Equals || style == KeyStyle.Clear

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (elevated) 8.dp else if (colors.isDark) 0.dp else 3.dp,
                shape = shape,
                ambientColor = if (elevated) Color(0x664A00E0) else Color(0x14000000),
                spotColor = if (elevated) Color(0x664A00E0) else Color(0x1A000000),
            )
            .clip(shape)
            .background(background)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = textColor),
                onClick = { feedback.click(); onClick() },
                onLongClick = onLongClick?.let { { feedback.heavy(); it() } },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size((fontSize + 2).dp))
        } else {
            Text(
                text = label,
                color = textColor,
                fontSize = fontSize.sp,
                fontWeight = if (style == KeyStyle.Number) FontWeight.Normal else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Big gradient action button. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = CalcTheme.colors.accentGradient,
) {
    val feedback = rememberFeedback()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (enabled) 10.dp else 0.dp, shape, ambientColor = Color(0x664A00E0), spotColor = Color(0x664A00E0))
            .clip(shape)
            .background(brush)
            .clickable(enabled = enabled) { feedback.click(); onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/** Round tonal icon button used in cards. */
@Composable
fun SoftIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
    )
}

/** Gradient circle with an icon, used for feature headers. */
@Composable
fun IconBadge(icon: ImageVector, brush: Brush = CalcTheme.colors.accentGradient, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.52f))
    }
}

/** Decimal number input that only accepts a valid number. */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    prefix: String? = null,
    allowNegative: Boolean = false,
    imeDone: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val cleaned = input.replace(",", "").replace(" ", "")
            val pattern = if (allowNegative) Regex("""-?\d*\.?\d*""") else Regex("""\d*\.?\d*""")
            if (cleaned.length <= 60 && pattern.matches(cleaned)) onValueChange(cleaned)
        },
        label = { Text(label) },
        singleLine = true,
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        prefix = if (prefix != null) { { Text(prefix) } } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (imeDone) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Next,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Label / value row for result cards, with an optional copy button. */
@Composable
fun ResultRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    copyValue: String? = null,
    valueColor: Color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
) {
    val copy = rememberCopier()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f, fill = false),
        )
        if (copyValue != null) {
            IconButton(onClick = { copy(label, copyValue) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Selectable pill used for modes, rates and categories. */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CalcTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(colors.accentGradient)
                else Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.cardBorder, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Three bouncing dots: the app's elegant loading indicator. */
@Composable
fun LoadingDots(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val offset by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 150, easing = LinearEasing), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .scale(0.6f + offset * 0.6f)
                    .alpha(0.4f + offset * 0.6f)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Info / warning banner. */
@Composable
fun InfoBanner(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
