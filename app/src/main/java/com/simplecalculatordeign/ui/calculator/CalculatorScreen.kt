package com.simplecalculatordeign.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.CalcKey
import com.simplecalculatordeign.ui.components.GradientTitle
import com.simplecalculatordeign.ui.components.IconBadge
import com.simplecalculatordeign.ui.components.KeyStyle
import com.simplecalculatordeign.ui.navigation.Routes
import com.simplecalculatordeign.ui.theme.Brand
import com.simplecalculatordeign.ui.theme.CalcTheme

/** Main (basic) calculator screen. */
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onNavigate: (String) -> Unit,
    onShowSteps: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showModes by rememberSaveable { mutableStateOf(false) }
    val act: (CalcAction) -> Unit = { viewModel.onAction(it, "calculator") }

    AppScaffold(
        title = "Simple Calculator Deign",
        titleContent = { GradientTitle("Simple Calculator Deign") },
        actions = {
            IconButton(onClick = { onNavigate(Routes.ASSISTANT) }) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Math Assistant", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onNavigate(Routes.SCANNER) }) {
                Icon(Icons.Rounded.DocumentScanner, contentDescription = "Camera scanner", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showModes = true }) {
                Icon(Icons.Rounded.Dashboard, contentDescription = "Calculator modes", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .padding(bottom = 8.dp)
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CalculatorDisplay(
                        state = state,
                        onBackspace = { act(CalcAction.Backspace) },
                        onClear = { act(CalcAction.Clear) },
                        onShowSteps = onShowSteps,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        largeFont = 44,
                    )
                    BasicKeypad(act, onScanner = { onNavigate(Routes.SCANNER) }, modifier = Modifier.weight(1.25f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    CalculatorDisplay(
                        state = state,
                        onBackspace = { act(CalcAction.Backspace) },
                        onClear = { act(CalcAction.Clear) },
                        onShowSteps = onShowSteps,
                        modifier = Modifier.weight(0.34f),
                    )
                    Spacer(Modifier.height(10.dp))
                    BasicKeypad(act, onScanner = { onNavigate(Routes.SCANNER) }, modifier = Modifier.weight(0.66f))
                }
            }
        }
    }

    if (showModes) {
        ModeMenuSheet(
            onDismiss = { showModes = false },
            onSelect = { route -> showModes = false; onNavigate(route) },
        )
    }
}

@Composable
fun KeyRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), content = content)
}

private fun RowScope.keyModifier(weight: Float = 1f) = Modifier.weight(weight).fillMaxHeight()

@Composable
fun BasicKeypad(act: (CalcAction) -> Unit, onScanner: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        // Memory + scanner shortcut
        KeyRow(Modifier.weight(0.5f)) {
            CalcKey("MC", { act(CalcAction.MemoryClear) }, keyModifier(), KeyStyle.Memory, fontSize = 15, contentDescription = "Memory clear")
            CalcKey("MR", { act(CalcAction.MemoryRecall) }, keyModifier(), KeyStyle.Memory, fontSize = 15, contentDescription = "Memory recall")
            CalcKey("M+", { act(CalcAction.MemoryAdd) }, keyModifier(), KeyStyle.Memory, fontSize = 15, contentDescription = "Memory add")
            CalcKey("M−", { act(CalcAction.MemorySubtract) }, keyModifier(), KeyStyle.Memory, fontSize = 15, contentDescription = "Memory subtract")
            CalcKey("Scan", onScanner, keyModifier(), KeyStyle.Operator, icon = Icons.Rounded.DocumentScanner, fontSize = 18, contentDescription = "Open camera scanner")
        }
        // Functions
        KeyRow(Modifier.weight(0.62f)) {
            CalcKey("%", { act(CalcAction.Percent) }, keyModifier(), KeyStyle.Function, fontSize = 19, contentDescription = "Percent")
            CalcKey("√", { act(CalcAction.SquareRoot) }, keyModifier(), KeyStyle.Function, fontSize = 19, contentDescription = "Square root")
            CalcKey("x²", { act(CalcAction.Square) }, keyModifier(), KeyStyle.Function, fontSize = 19, contentDescription = "Square")
            CalcKey("xʸ", { act(CalcAction.Power) }, keyModifier(), KeyStyle.Function, fontSize = 19, contentDescription = "Power")
            CalcKey("1/x", { act(CalcAction.Reciprocal) }, keyModifier(), KeyStyle.Function, fontSize = 17, contentDescription = "Reciprocal")
        }
        KeyRow(Modifier.weight(1f)) {
            CalcKey("AC", { act(CalcAction.Clear) }, keyModifier(), KeyStyle.Clear, fontSize = 22, contentDescription = "All clear")
            CalcKey("(", { act(CalcAction.OpenParen) }, keyModifier(), KeyStyle.Function, contentDescription = "Open parenthesis")
            CalcKey(")", { act(CalcAction.CloseParen) }, keyModifier(), KeyStyle.Function, contentDescription = "Close parenthesis")
            CalcKey("÷", { act(CalcAction.Operator('÷')) }, keyModifier(), KeyStyle.Operator, fontSize = 30, contentDescription = "Divide")
        }
        DigitRow('7', '8', '9', '×', "Multiply", act, Modifier.weight(1f))
        DigitRow('4', '5', '6', '-', "Minus", act, Modifier.weight(1f))
        DigitRow('1', '2', '3', '+', "Plus", act, Modifier.weight(1f))
        KeyRow(Modifier.weight(1f)) {
            CalcKey("±", { act(CalcAction.ToggleSign) }, keyModifier(), KeyStyle.Number, contentDescription = "Toggle sign")
            CalcKey("0", { act(CalcAction.Digit('0')) }, keyModifier())
            CalcKey(".", { act(CalcAction.Decimal) }, keyModifier(), contentDescription = "Decimal point")
            CalcKey("=", { act(CalcAction.Equals) }, keyModifier(), KeyStyle.Equals, fontSize = 32, contentDescription = "Equals")
        }
    }
}

@Composable
fun DigitRow(a: Char, b: Char, c: Char, op: Char, opName: String, act: (CalcAction) -> Unit, modifier: Modifier = Modifier) {
    KeyRow(modifier) {
        CalcKey(a.toString(), { act(CalcAction.Digit(a)) }, keyModifier())
        CalcKey(b.toString(), { act(CalcAction.Digit(b)) }, keyModifier())
        CalcKey(c.toString(), { act(CalcAction.Digit(c)) }, keyModifier())
        CalcKey(if (op == '-') "−" else op.toString(), { act(CalcAction.Operator(op)) }, keyModifier(), KeyStyle.Operator, fontSize = 30, contentDescription = opName)
    }
}

// ------------------------------------------------------------------ mode menu

private data class ModeItem(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val brush: Brush)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeMenuSheet(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val modes = listOf(
        ModeItem("Basic", "Everyday maths", Icons.Rounded.Calculate, Routes.CALCULATOR, Brush.linearGradient(listOf(Brand.Violet, Brand.Blue))),
        ModeItem("Scientific", "sin, log, π, n!", Icons.Rounded.Science, Routes.SCIENTIFIC, Brush.linearGradient(listOf(Brand.DeepPurple, Brand.Pink))),
        ModeItem("Percentage", "Of, change, increase", Icons.Rounded.Percent, Routes.PERCENTAGE, Brush.linearGradient(listOf(Brand.Cyan, Brand.Blue))),
        ModeItem("Discount", "Sale price & savings", Icons.Rounded.LocalOffer, Routes.DISCOUNT, Brush.linearGradient(listOf(Brand.Amber, Brand.Orange))),
        ModeItem("GST / VAT", "Add or remove tax", Icons.Rounded.Receipt, Routes.VAT, Brush.linearGradient(listOf(Brand.Green, Brand.Teal))),
        ModeItem("Loan / EMI", "Monthly instalments", Icons.Rounded.AccountBalance, Routes.LOAN, Brush.linearGradient(listOf(Brand.Indigo, Brand.Purple))),
        ModeItem("Unit Converter", "Length, weight, land…", Icons.Rounded.Straighten, Routes.UNITS, Brush.linearGradient(listOf(Brand.Pink, Brand.Coral))),
        ModeItem("AI Assistant", "Explain any problem", Icons.Rounded.AutoAwesome, Routes.ASSISTANT, Brush.linearGradient(listOf(Brand.Purple, Brand.Cyan))),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Calculator modes", style = MaterialTheme.typography.titleLarge)
            Text("Choose the right tool for the job", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            modes.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    pair.forEach { mode -> ModeTile(mode, Modifier.weight(1f)) { onSelect(mode.route) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeTile(mode: ModeItem, modifier: Modifier, onClick: () -> Unit) {
    val colors = CalcTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(colors.functionKey.copy(alpha = if (colors.isDark) 0.6f else 0.55f))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        IconBadge(mode.icon, mode.brush, size = 42.dp)
        Spacer(Modifier.height(10.dp))
        Text(mode.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
