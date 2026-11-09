package com.simplecalculatordeign.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplecalculatordeign.core.math.AngleMode
import com.simplecalculatordeign.core.math.FormatOptions
import com.simplecalculatordeign.core.math.MathEngine
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.components.GradientCard
import com.simplecalculatordeign.ui.rememberCopier
import com.simplecalculatordeign.ui.rememberFeedback
import com.simplecalculatordeign.ui.theme.CalcTheme
import java.math.BigDecimal

/** Results longer than this are previewed in the display and opened in full in a sheet. */
private const val INLINE_LIMIT = 2_500

private fun fontSizeFor(length: Int, large: Int): Int = when {
    length <= 9 -> large
    length <= 13 -> (large * 0.82).toInt()
    length <= 18 -> (large * 0.66).toInt()
    else -> (large * 0.54).toInt()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorDisplay(
    state: CalculatorUiState,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onShowSteps: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAngle: Boolean = false,
    onToggleAngle: () -> Unit = {},
    largeFont: Int = 54,
) {
    val settings = LocalAppSettings.current
    val format: FormatOptions = settings.formatOptions
    val colors = CalcTheme.colors
    val copy = rememberCopier()
    val feedback = rememberFeedback()
    var showFull by rememberSaveable { mutableStateOf(false) }

    val result = state.result
    val fullValue: BigDecimal? = if (state.justEvaluated) result?.value else state.preview
    val approximate = if (state.justEvaluated) result?.approximate == true else state.previewApproximate
    val formatted = remember(fullValue, format, approximate) {
        fullValue?.let { NumberFormatter.formatDetailed(it, format, approximate) }
    }
    val expressionText = remember(state.expression, format.grouping, state.justEvaluated, state.lastExpression) {
        val source = if (state.justEvaluated) state.lastExpression.orEmpty() else state.expression
        NumberFormatter.formatExpression(source, format.grouping)
    }

    GradientCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        // ---- status row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.memory != null) StatusChip("M")
            if (showAngle) {
                StatusChip(if (state.angleMode == AngleMode.DEGREES) "DEG" else "RAD", onClick = onToggleAngle)
            }
            Spacer(Modifier.weight(1f))
            if (state.justEvaluated && result != null && state.lastExpression != null && MathEngine.hasOperation(state.lastExpression)) {
                DisplayIcon(Icons.Rounded.Functions, "Show steps") { onShowSteps(state.lastExpression) }
            }
            if (fullValue != null) {
                DisplayIcon(Icons.Rounded.ContentCopy, "Copy result") { copy("Result", NumberFormatter.plain(fullValue)) }
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .combinedClickable(
                        onClick = { feedback.click(); onBackspace() },
                        onLongClick = { feedback.heavy(); onClear() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Backspace (hold to clear)", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.weight(1f, fill = true))

        // ---- expression line (scrolls horizontally, always shows the end)
        val exprScroll = rememberScrollState()
        LaunchedEffect(expressionText, exprScroll.maxValue) { exprScroll.scrollTo(exprScroll.maxValue) }
        val showExpressionLarge = !state.justEvaluated
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(exprScroll),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = expressionText.ifEmpty { if (showExpressionLarge) "0" else "" },
                color = if (showExpressionLarge) colors.displayText else colors.displaySubtle,
                fontSize = if (showExpressionLarge) fontSizeFor(expressionText.length, largeFont).sp else 20.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.height(4.dp))

        // ---- result / preview / error
        AnimatedContent(
            targetState = Triple(state.error, formatted?.text, state.justEvaluated),
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
            label = "result",
        ) { (error, text, evaluated) ->
            when {
                error != null -> Text(error, color = Color(0xFFFFD27A), style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                state.isComputing -> Text("Calculating…", color = colors.displaySubtle, style = MaterialTheme.typography.titleMedium)
                text != null -> {
                    val prefix = if (approximate || formatted?.roundedForDisplay == true) "≈ " else "= "
                    val tooLong = text.length > INLINE_LIMIT
                    val shown = if (tooLong) text.take(INLINE_LIMIT / 3) + "…" else text
                    val scroll = rememberScrollState()
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = prefix + shown,
                                color = if (evaluated) colors.displayText else colors.displaySubtle,
                                fontSize = if (evaluated) fontSizeFor(shown.length + 2, largeFont).sp else 26.sp,
                                fontWeight = if (evaluated) FontWeight.Normal else FontWeight.Light,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        if (tooLong || text.length > 18) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val digitCount = text.count { it.isDigit() }
                                Text(
                                    "%,d digits".format(digitCount) + if (tooLong) " · preview" else " · scroll ↔",
                                    color = colors.displaySubtle,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                TextButton(onClick = { showFull = true }) {
                                    Icon(Icons.Rounded.OpenInFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("View full", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (approximate && evaluated) {
                            Text(
                                "Rounded to ${settings.precision} significant digits (non-terminating)",
                                color = colors.displaySubtle,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                else -> Spacer(Modifier.height(30.dp))
            }
        }
    }

    if (showFull && fullValue != null && formatted != null) {
        FullResultSheet(
            text = formatted.text,
            plain = NumberFormatter.plain(fullValue),
            approximate = approximate,
            onDismiss = { showFull = false },
        )
    }
}

@Composable
private fun StatusChip(text: String, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DisplayIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

/** Shows an arbitrarily long result efficiently (lazy list of chunks), fully selectable and copyable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullResultSheet(text: String, plain: String, approximate: Boolean, onDismiss: () -> Unit) {
    val copy = rememberCopier()
    val chunks = remember(text) { text.chunked(40) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Full result", style = MaterialTheme.typography.titleLarge)
            Text(
                "%,d characters".format(text.length) + if (approximate) " · rounded (non-terminating)" else " · exact",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { copy("Result", plain) }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy all digits")
            }
            SelectionContainer {
                LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
                    itemsIndexed(chunks) { index, line ->
                        Row {
                            Text(
                                "${index * 40 + 1}".padStart(6),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(56.dp),
                            )
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
