package com.simplecalculatordeign.ui.scanner

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplecalculatordeign.core.math.CalcException
import com.simplecalculatordeign.core.math.CalcResult
import com.simplecalculatordeign.core.math.EvalOptions
import com.simplecalculatordeign.core.math.MathEngine
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.EmptyState
import com.simplecalculatordeign.ui.components.GradientButton
import com.simplecalculatordeign.ui.components.GradientCard
import com.simplecalculatordeign.ui.components.InfoBanner
import com.simplecalculatordeign.ui.components.LoadingDots
import com.simplecalculatordeign.ui.components.PremiumCard
import com.simplecalculatordeign.ui.components.SectionTitle
import com.simplecalculatordeign.ui.components.SoftIconButton
import com.simplecalculatordeign.ui.rememberCopier
import com.simplecalculatordeign.ui.theme.CalcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ===================================================================== Scan result

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ScanResultScreen(
    viewModel: ScannerViewModel,
    onBack: () -> Unit,
    onShowSteps: (String) -> Unit,
    onUseInCalculator: (String) -> Unit,
    onNewScan: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val format = LocalAppSettings.current.formatOptions
    val context = LocalContext.current
    val copy = rememberCopier()
    val scope = rememberCoroutineScope()
    var showImage by rememberSaveable { mutableStateOf(false) }
    var showText by rememberSaveable { mutableStateOf(false) }

    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { ScanExport.writePdf(state, format, it) } ?: error("no stream")
                    }.isSuccess
                }
                Toast.makeText(context, if (ok) "PDF saved" else "Could not save the PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppScaffold(
        title = "Scan results",
        onBack = onBack,
        actions = {
            IconButton(onClick = onNewScan) { Icon(Icons.Rounded.DocumentScanner, contentDescription = "New scan") }
        },
        bottomBar = {
            if (state.items.isNotEmpty() || state.numbers.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionPill(
                            if (state.savedToHistory) "Saved" else "Save to history",
                            if (state.savedToHistory) Icons.Rounded.CheckCircle else Icons.Rounded.Save,
                            enabled = !state.savedToHistory && state.validResults.isNotEmpty(),
                        ) { viewModel.saveToHistory() }
                        ActionPill("Copy", Icons.Rounded.ContentCopy) { copy("Scan results", ScanExport.buildText(state, format)) }
                        ActionPill("Share text", Icons.Rounded.Share) { ScanExport.shareText(context, ScanExport.buildText(state, format)) }
                        ActionPill("Share PDF", Icons.Rounded.PictureAsPdf) {
                            scope.launch {
                                val snapshot = state
                                val uri = withContext(Dispatchers.IO) {
                                    runCatching { ScanExport.createPdfFile(context, snapshot, format) }.getOrNull()
                                }
                                if (uri != null) ScanExport.shareFile(context, uri, "application/pdf")
                                else Toast.makeText(context, "Could not create the PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ActionPill("Save PDF", Icons.Rounded.SaveAlt) { savePdf.launch("scan_report.pdf") }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- summary header with original image
            item {
                GradientCard(contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val bmp = state.processedBitmap ?: state.sourceBitmap
                        if (bmp != null) {
                            val img = remember(bmp) { bmp.asImageBitmap() }
                            Image(
                                img, contentDescription = "Scanned image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showImage = true },
                            )
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${state.items.size} calculation${if (state.items.size == 1) "" else "s"} found",
                                style = MaterialTheme.typography.titleLarge, color = Color.White,
                            )
                            val uncertain = state.items.count { it.uncertain }
                            Text(
                                if (uncertain > 0) "$uncertain need your review" else "Review and edit before saving",
                                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                            )
                            if (bmp != null) {
                                Text(
                                    "Tap image to view",
                                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Functions,
                        "No calculations recognised",
                        "Nothing with + − × ÷ was found. Numbers alone are never calculated automatically — add an expression below, or select numbers to add up.",
                    )
                }
            } else {
                item { SectionTitle("Recognised calculations") }
                itemsIndexed(state.items, key = { _, it -> it.id }) { index, item ->
                    ScanItemCard(
                        index = index,
                        item = item,
                        onDraftChange = { viewModel.updateDraft(item.id, it) },
                        onRecalculate = { viewModel.recalculate(item.id) },
                        onRemove = { viewModel.removeItem(item.id) },
                        onToggleTotal = { viewModel.toggleInTotal(item.id) },
                        onSteps = { onShowSteps(item.expression) },
                        onUse = { onUseInCalculator(item.expression) },
                        showTotalToggle = state.validResults.size >= 2,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { viewModel.addExpression() }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add expression")
                    }
                    if (state.items.any { it.isEdited }) {
                        TextButton(onClick = { viewModel.recalculateAll() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Recalculate all")
                        }
                    }
                }
            }

            state.total?.let { total ->
                item {
                    PremiumCard {
                        Text("Total of selected results", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (state.totalApproximate) "≈ " else "") + NumberFormatter.format(total, format),
                                style = MaterialTheme.typography.headlineMedium.copy(brush = CalcTheme.colors.accentGradient),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            SoftIconButton(Icons.Rounded.ContentCopy, "Copy total", { copy("Total", NumberFormatter.plain(total)) })
                        }
                        Text(
                            "Untick a calculation above to leave it out of the total.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.numbers.isNotEmpty()) {
                item {
                    PremiumCard {
                        Text("Other numbers found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "These were not part of a calculation. Select any you want to add up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.numbers.forEachIndexed { i, n ->
                                FilterChip(
                                    selected = n.selected,
                                    onClick = { viewModel.toggleNumber(i) },
                                    label = {
                                        Text(
                                            (if (n.number.label != "Number") n.number.label.take(14) + ": " else "") +
                                                NumberFormatter.format(n.number.value, format),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingIcon = if (n.number.uncertain) {
                                        { Icon(Icons.Rounded.Warning, contentDescription = "Uncertain", modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                                )
                            }
                        }
                        state.selectedNumbersSum?.let { sum ->
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sum of selected", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(NumberFormatter.format(sum, format), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                SoftIconButton(Icons.Rounded.ContentCopy, "Copy sum", { copy("Sum", NumberFormatter.plain(sum)) }, size = 34.dp, modifier = Modifier.padding(start = 6.dp))
                            }
                            val expr = state.numbers.filter { it.selected }.joinToString("+") { NumberFormatter.plain(it.number.value).let { v -> if (v.startsWith("-")) "($v)" else v } }
                            TextButton(onClick = { viewModel.addExpression(expr) }) { Text("Add as a calculation") }
                        }
                    }
                }
            }

            item {
                PremiumCard {
                    Row(Modifier.fillMaxWidth().clickable { showText = !showText }, verticalAlignment = Alignment.CenterVertically) {
                        Text("Extracted text", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (state.ocrText.isNotBlank()) {
                            SoftIconButton(Icons.Rounded.ContentCopy, "Copy text", { copy("Extracted text", state.ocrText) }, size = 34.dp)
                        }
                        Icon(if (showText) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (showText) "Collapse" else "Expand")
                    }
                    AnimatedVisibility(showText) {
                        SelectionContainer {
                            Text(
                                state.ocrText.ifBlank { "No text was found in the image." },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImage) {
        val bmp = state.processedBitmap ?: state.sourceBitmap
        if (bmp != null) {
            ModalBottomSheet(onDismissRequest = { showImage = false }) {
                val img = remember(bmp) { bmp.asImageBitmap() }
                Image(
                    img, contentDescription = "Scanned image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .padding(16.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(primary.copy(alpha = if (enabled) 0.12f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = primary.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = primary.copy(alpha = if (enabled) 1f else 0.5f))
    }
}

@Composable
private fun ScanItemCard(
    index: Int,
    item: ScanItem,
    onDraftChange: (String) -> Unit,
    onRecalculate: () -> Unit,
    onRemove: () -> Unit,
    onToggleTotal: () -> Unit,
    onSteps: () -> Unit,
    onUse: () -> Unit,
    showTotalToggle: Boolean,
) {
    val format = LocalAppSettings.current.formatOptions
    val colors = CalcTheme.colors
    val copy = rememberCopier()
    PremiumCard(modifier = Modifier.animateContentSize(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.accentGradient),
                contentAlignment = Alignment.Center,
            ) { Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.width(10.dp))
            Text(
                "Read: \"${item.sourceText}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showTotalToggle && item.result != null) {
                Checkbox(checked = item.includeInTotal, onCheckedChange = { onToggleTotal() })
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
        if (item.uncertain) {
            Spacer(Modifier.height(8.dp))
            InfoBanner("Please verify: " + item.notes.joinToString("; "), Icons.Rounded.Warning, tint = colors.warning)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = item.draft,
            onValueChange = onDraftChange,
            label = { Text("Expression") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onRecalculate() }),
            trailingIcon = {
                if (item.isEdited) IconButton(onClick = onRecalculate) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Recalculate", tint = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        val result = item.result
        when {
            item.isEdited -> Text("Tap ↻ to recalculate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            item.error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(item.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            result != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                        Text(
                            NumberFormatter.formatExpression(item.expression, format.grouping) +
                                (if (result.approximate) " ≈ " else " = ") +
                                NumberFormatter.format(result.value, format, result.approximate),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, softWrap = false,
                        )
                    }
                }
                item.writtenAnswerMatches?.let { matches ->
                    Spacer(Modifier.height(6.dp))
                    InfoBanner(
                        if (matches) "Written answer ${item.writtenAnswer} is correct"
                        else "Written answer ${item.writtenAnswer} differs — correct result is ${NumberFormatter.format(result.value, format)}",
                        if (matches) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        tint = if (matches) colors.success else colors.warning,
                    )
                }
                if (result.steps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    result.steps.take(3).forEachIndexed { i, step ->
                        Text(
                            "Step ${i + 1}: ${NumberFormatter.formatStep(step, format)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (result.steps.size > 3) {
                        Text("+ ${result.steps.size - 3} more steps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSteps) {
                        Icon(Icons.Rounded.Functions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("Steps")
                    }
                    TextButton(onClick = { copy("Result", NumberFormatter.plain(result.value)) }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("Copy")
                    }
                    TextButton(onClick = onUse) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("Calculator")
                    }
                }
            }
        }
    }
}

// ===================================================================== Step by step

private sealed interface StepsState {
    data object Loading : StepsState
    data class Ready(val result: CalcResult) : StepsState
    data class Failed(val message: String) : StepsState
}

@Composable
fun StepByStepScreen(expression: String, onBack: () -> Unit, onUseInCalculator: (String) -> Unit) {
    val settings = LocalAppSettings.current
    val format = settings.formatOptions
    val copy = rememberCopier()
    val state by produceState<StepsState>(StepsState.Loading, expression, settings.precision, settings.angleMode) {
        value = withContext(Dispatchers.Default) {
            try {
                StepsState.Ready(
                    MathEngine.evaluate(
                        MathEngine.closeParentheses(expression),
                        EvalOptions(precision = settings.precision, angleMode = settings.angleMode, recordSteps = true),
                    )
                )
            } catch (e: CalcException) {
                StepsState.Failed(e.message ?: "Invalid expression")
            }
        }
    }

    AppScaffold(title = "Step-by-step", onBack = onBack) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GradientCard {
                    Text("Expression", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            NumberFormatter.formatExpression(expression, format.grouping),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1, softWrap = false,
                        )
                    }
                }
            }
            when (val s = state) {
                StepsState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingDots() }
                }
                is StepsState.Failed -> item {
                    InfoBanner(s.message, Icons.Rounded.ErrorOutline, tint = MaterialTheme.colorScheme.error)
                }
                is StepsState.Ready -> {
                    val result = s.result
                    item {
                        Text(
                            "Order of operations: brackets → powers & roots → × ÷ (left to right) → + − (left to right)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (result.steps.isEmpty()) {
                        item { InfoBanner("This is a single number — no calculation steps.", Icons.Rounded.Functions) }
                    }
                    itemsIndexedSteps(result, format)
                    item {
                        PremiumCard {
                            Text("Final result", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            val text = NumberFormatter.format(result.value, format, result.approximate)
                            Box(Modifier.horizontalScroll(rememberScrollState())) {
                                Text(
                                    (if (result.approximate) "≈ " else "") + if (text.length > 2000) text.take(2000) + "…" else text,
                                    style = MaterialTheme.typography.headlineMedium.copy(brush = CalcTheme.colors.accentGradient),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1, softWrap = false,
                                )
                            }
                            if (text.length > 2000) {
                                Text("Long result (${text.length} characters) — use Copy for every digit.", style = MaterialTheme.typography.bodySmall)
                            }
                            if (result.approximate) {
                                Text(
                                    "Non-terminating value rounded to ${settings.precision} significant digits after the decimal point.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GradientButton("Copy", onClick = { copy("Result", NumberFormatter.plain(result.value)) }, icon = Icons.Rounded.ContentCopy, modifier = Modifier.weight(1f))
                                GradientButton(
                                    "Calculator", onClick = { onUseInCalculator(expression) }, icon = Icons.Rounded.Calculate,
                                    modifier = Modifier.weight(1f), brush = CalcTheme.colors.equalsKey,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedSteps(
    result: CalcResult,
    format: com.simplecalculatordeign.core.math.FormatOptions,
) {
    itemsIndexed(result.steps) { i, step ->
        val colors = CalcTheme.colors
        Row(verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.accentGradient),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                if (i < result.steps.lastIndex) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(2.dp)
                            .height(28.dp)
                            .background(colors.cardBorder)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            PremiumCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Step ${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Box(Modifier.horizontalScroll(rememberScrollState())) {
                    Text(
                        NumberFormatter.formatStep(step, format),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, softWrap = false,
                    )
                }
            }
        }
    }
}
