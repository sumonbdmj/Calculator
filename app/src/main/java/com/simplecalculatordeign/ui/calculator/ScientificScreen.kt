package com.simplecalculatordeign.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplecalculatordeign.core.math.AngleMode
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.CalcKey
import com.simplecalculatordeign.ui.components.KeyStyle
import com.simplecalculatordeign.ui.components.PillChip

/** Scientific calculator: shares state (expression, memory, history) with the basic calculator. */
@Composable
fun ScientificScreen(
    viewModel: CalculatorViewModel,
    onBack: () -> Unit,
    onShowSteps: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val act: (CalcAction) -> Unit = { viewModel.onAction(it, "scientific") }

    AppScaffold(
        title = "Scientific",
        onBack = onBack,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(end = 8.dp)) {
                PillChip("DEG", state.angleMode == AngleMode.DEGREES, { if (state.angleMode != AngleMode.DEGREES) act(CalcAction.ToggleAngle) })
                PillChip("RAD", state.angleMode == AngleMode.RADIANS, { if (state.angleMode != AngleMode.RADIANS) act(CalcAction.ToggleAngle) })
            }
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalculatorDisplay(
                        state, { act(CalcAction.Backspace) }, { act(CalcAction.Clear) }, onShowSteps,
                        Modifier.weight(0.8f).fillMaxHeight(), showAngle = true, onToggleAngle = { act(CalcAction.ToggleAngle) }, largeFont = 40,
                    )
                    ScientificKeypad(act, Modifier.weight(1.4f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    CalculatorDisplay(
                        state, { act(CalcAction.Backspace) }, { act(CalcAction.Clear) }, onShowSteps,
                        Modifier.weight(0.27f), showAngle = true, onToggleAngle = { act(CalcAction.ToggleAngle) }, largeFont = 46,
                    )
                    Spacer(Modifier.height(10.dp))
                    ScientificKeypad(act, Modifier.weight(0.73f))
                }
            }
        }
    }
}

private fun RowScope.k() = Modifier.weight(1f).fillMaxHeight()

@Composable
fun ScientificKeypad(act: (CalcAction) -> Unit, modifier: Modifier = Modifier) {
    val fn = 17
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SciRow(Modifier.weight(0.7f)) {
            CalcKey("sin", { act(CalcAction.Function("sin")) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Sine")
            CalcKey("cos", { act(CalcAction.Function("cos")) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Cosine")
            CalcKey("tan", { act(CalcAction.Function("tan")) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Tangent")
            CalcKey("π", { act(CalcAction.Constant('π')) }, k(), KeyStyle.Function, fontSize = 20, contentDescription = "Pi")
            CalcKey("e", { act(CalcAction.Constant('e')) }, k(), KeyStyle.Function, fontSize = 20, contentDescription = "Euler's number")
        }
        SciRow(Modifier.weight(0.7f)) {
            CalcKey("log", { act(CalcAction.Function("log")) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Log base 10")
            CalcKey("ln", { act(CalcAction.Function("ln")) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Natural log")
            CalcKey("x!", { act(CalcAction.Factorial) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Factorial")
            CalcKey("√", { act(CalcAction.SquareRoot) }, k(), KeyStyle.Function, fontSize = 20, contentDescription = "Square root")
            CalcKey("1/x", { act(CalcAction.Reciprocal) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Reciprocal")
        }
        SciRow(Modifier.weight(0.7f)) {
            CalcKey("MC", { act(CalcAction.MemoryClear) }, k(), KeyStyle.Memory, fontSize = 14)
            CalcKey("MR", { act(CalcAction.MemoryRecall) }, k(), KeyStyle.Memory, fontSize = 14)
            CalcKey("M+", { act(CalcAction.MemoryAdd) }, k(), KeyStyle.Memory, fontSize = 14)
            CalcKey("M−", { act(CalcAction.MemorySubtract) }, k(), KeyStyle.Memory, fontSize = 14)
            CalcKey("xʸ", { act(CalcAction.Power) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Power")
        }
        SciRow(Modifier.weight(1f)) {
            CalcKey("AC", { act(CalcAction.Clear) }, k(), KeyStyle.Clear, fontSize = 20)
            CalcKey("(", { act(CalcAction.OpenParen) }, k(), KeyStyle.Function, fontSize = 22)
            CalcKey(")", { act(CalcAction.CloseParen) }, k(), KeyStyle.Function, fontSize = 22)
            CalcKey("%", { act(CalcAction.Percent) }, k(), KeyStyle.Function, fontSize = 20)
            CalcKey("÷", { act(CalcAction.Operator('÷')) }, k(), KeyStyle.Operator, fontSize = 26, contentDescription = "Divide")
        }
        SciRow(Modifier.weight(1f)) {
            CalcKey("7", { act(CalcAction.Digit('7')) }, k(), fontSize = 23)
            CalcKey("8", { act(CalcAction.Digit('8')) }, k(), fontSize = 23)
            CalcKey("9", { act(CalcAction.Digit('9')) }, k(), fontSize = 23)
            CalcKey("x²", { act(CalcAction.Square) }, k(), KeyStyle.Function, fontSize = fn, contentDescription = "Square")
            CalcKey("×", { act(CalcAction.Operator('×')) }, k(), KeyStyle.Operator, fontSize = 26, contentDescription = "Multiply")
        }
        SciRow(Modifier.weight(1f)) {
            CalcKey("4", { act(CalcAction.Digit('4')) }, k(), fontSize = 23)
            CalcKey("5", { act(CalcAction.Digit('5')) }, k(), fontSize = 23)
            CalcKey("6", { act(CalcAction.Digit('6')) }, k(), fontSize = 23)
            CalcKey("±", { act(CalcAction.ToggleSign) }, k(), fontSize = 22, contentDescription = "Toggle sign")
            CalcKey("−", { act(CalcAction.Operator('-')) }, k(), KeyStyle.Operator, fontSize = 26, contentDescription = "Minus")
        }
        SciRow(Modifier.weight(1f)) {
            CalcKey("1", { act(CalcAction.Digit('1')) }, k(), fontSize = 23)
            CalcKey("2", { act(CalcAction.Digit('2')) }, k(), fontSize = 23)
            CalcKey("3", { act(CalcAction.Digit('3')) }, k(), fontSize = 23)
            CalcKey("⌫", { act(CalcAction.Backspace) }, k(), KeyStyle.Number, icon = Icons.AutoMirrored.Rounded.Backspace, fontSize = 20, contentDescription = "Backspace")
            CalcKey("+", { act(CalcAction.Operator('+')) }, k(), KeyStyle.Operator, fontSize = 26, contentDescription = "Plus")
        }
        SciRow(Modifier.weight(1f)) {
            CalcKey("0", { act(CalcAction.Digit('0')) }, Modifier.weight(2.09f).fillMaxHeight(), fontSize = 23)
            CalcKey(".", { act(CalcAction.Decimal) }, k(), fontSize = 23, contentDescription = "Decimal point")
            CalcKey("=", { act(CalcAction.Equals) }, Modifier.weight(2.09f).fillMaxHeight(), KeyStyle.Equals, fontSize = 28, contentDescription = "Equals")
        }
    }
}

@Composable
private fun SciRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), content = content)
}
