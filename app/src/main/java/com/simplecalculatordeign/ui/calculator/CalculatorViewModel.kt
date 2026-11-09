package com.simplecalculatordeign.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simplecalculatordeign.AppContainer
import com.simplecalculatordeign.core.math.AngleMode
import com.simplecalculatordeign.core.math.CalcException
import com.simplecalculatordeign.core.math.CalcResult
import com.simplecalculatordeign.core.math.EvalOptions
import com.simplecalculatordeign.core.math.InputEditor
import com.simplecalculatordeign.core.math.MathEngine
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.data.repository.AppSettings
import com.simplecalculatordeign.data.repository.HistoryRepository
import com.simplecalculatordeign.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.coroutineContext

/** Everything a calculator key can do. */
sealed interface CalcAction {
    data class Digit(val digit: Char) : CalcAction
    data object Decimal : CalcAction
    data class Operator(val op: Char) : CalcAction
    data object Percent : CalcAction
    data object Factorial : CalcAction
    data object Square : CalcAction
    data object Power : CalcAction
    data object SquareRoot : CalcAction
    data object Reciprocal : CalcAction
    data object ToggleSign : CalcAction
    data object OpenParen : CalcAction
    data object CloseParen : CalcAction
    data class Function(val name: String) : CalcAction
    data class Constant(val symbol: Char) : CalcAction
    data object Clear : CalcAction
    data object Backspace : CalcAction
    data object Equals : CalcAction
    data object MemoryClear : CalcAction
    data object MemoryRecall : CalcAction
    data object MemoryAdd : CalcAction
    data object MemorySubtract : CalcAction
    data object ToggleAngle : CalcAction
}

data class CalculatorUiState(
    /** Raw expression in calculator syntax (plain numbers, × ÷ symbols). */
    val expression: String = "",
    /** Live preview value while typing (null when not evaluable). */
    val preview: BigDecimal? = null,
    val previewApproximate: Boolean = false,
    /** Final result shown after "=". */
    val result: CalcResult? = null,
    val error: String? = null,
    val memory: BigDecimal? = null,
    val justEvaluated: Boolean = false,
    val isComputing: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEGREES,
    /** Expression that produced [result], for "show steps". */
    val lastExpression: String? = null,
)

class CalculatorViewModel(
    private val history: HistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    private var settings = AppSettings()
    private var previewJob: Job? = null
    private var equalsJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                settings = s
                _state.update { it.copy(angleMode = s.angleMode) }
                refreshPreview()
            }
        }
    }

    private fun options(recordSteps: Boolean = false, check: () -> Unit = {}) = EvalOptions(
        precision = settings.precision,
        angleMode = _state.value.angleMode,
        recordSteps = recordSteps,
        checkCancelled = check,
    )

    fun onAction(action: CalcAction, source: String = "calculator") {
        val s = _state.value
        // After "=", typing a number starts a new calculation; an operator continues from the result.
        val startsNew = s.justEvaluated && when (action) {
            is CalcAction.Digit, CalcAction.Decimal, is CalcAction.Constant, is CalcAction.Function,
            CalcAction.SquareRoot, CalcAction.OpenParen -> true
            else -> false
        }
        val expr = if (startsNew) "" else s.expression

        val newExpr: String = when (action) {
            is CalcAction.Digit -> InputEditor.appendDigit(expr, action.digit)
            CalcAction.Decimal -> InputEditor.appendDecimal(expr)
            is CalcAction.Operator -> InputEditor.appendOperator(expr, action.op)
            CalcAction.Percent -> InputEditor.appendPostfix(expr, '%')
            CalcAction.Factorial -> InputEditor.appendPostfix(expr, '!')
            CalcAction.Square -> InputEditor.appendPostfix(expr, '²')
            CalcAction.Power -> InputEditor.appendPowerOperator(expr)
            CalcAction.SquareRoot -> InputEditor.appendFunction(expr, "√")
            CalcAction.Reciprocal -> InputEditor.reciprocal(expr)
            CalcAction.ToggleSign -> InputEditor.toggleSign(expr)
            CalcAction.OpenParen -> InputEditor.openParenthesis(expr)
            CalcAction.CloseParen -> InputEditor.closeParenthesis(expr)
            is CalcAction.Function -> InputEditor.appendFunction(expr, action.name)
            is CalcAction.Constant -> InputEditor.appendConstant(expr, action.symbol)
            CalcAction.Backspace -> if (s.justEvaluated) "" else InputEditor.backspace(expr)
            CalcAction.Clear -> {
                equalsJob?.cancel()
                _state.update { it.copy(expression = "", preview = null, result = null, error = null, justEvaluated = false, isComputing = false) }
                return
            }
            CalcAction.Equals -> { evaluate(source); return }
            CalcAction.MemoryClear -> { _state.update { it.copy(memory = null) }; return }
            CalcAction.MemoryRecall -> {
                val m = s.memory ?: return
                InputEditor.appendNumber(expr.takeUnless { s.justEvaluated } ?: "", NumberFormatter.plain(m))
            }
            CalcAction.MemoryAdd, CalcAction.MemorySubtract -> {
                val value = currentValue() ?: return
                val base = s.memory ?: BigDecimal.ZERO
                val updated = if (action == CalcAction.MemoryAdd) base.add(value) else base.subtract(value)
                _state.update { it.copy(memory = updated) }
                return
            }
            CalcAction.ToggleAngle -> {
                val next = if (s.angleMode == AngleMode.DEGREES) AngleMode.RADIANS else AngleMode.DEGREES
                _state.update { it.copy(angleMode = next) }
                viewModelScope.launch { settingsRepository.setAngleMode(next) }
                return
            }
        }
        if (newExpr == s.expression) return // key had no effect (e.g. ")" with nothing open)
        _state.update {
            it.copy(expression = newExpr, justEvaluated = false, result = null, error = null)
        }
        refreshPreview()
    }

    /** Value used by M+ / M−: the shown result, otherwise the live preview. */
    private fun currentValue(): BigDecimal? {
        val s = _state.value
        return s.result?.value?.takeIf { s.justEvaluated } ?: s.preview
            ?: MathEngine.tryEvaluate(MathEngine.prepareForPreview(s.expression), options())?.value
    }

    private fun refreshPreview() {
        previewJob?.cancel()
        val expr = _state.value.expression
        if (expr.isEmpty() || _state.value.justEvaluated || !MathEngine.hasOperation(expr)) {
            _state.update { it.copy(preview = null, previewApproximate = false) }
            return
        }
        previewJob = viewModelScope.launch {
            delay(30) // coalesce very fast typing
            val ctx = coroutineContext
            val result = withContext(Dispatchers.Default) {
                MathEngine.tryEvaluate(MathEngine.prepareForPreview(expr), options { ctx.ensureActive() })
            }
            _state.update {
                if (it.expression != expr) it
                else it.copy(preview = result?.value, previewApproximate = result?.approximate == true)
            }
        }
    }

    private fun evaluate(source: String) {
        val expr = _state.value.expression
        if (expr.isBlank()) return
        if (_state.value.justEvaluated) return
        previewJob?.cancel()
        equalsJob?.cancel()
        val toEvaluate = MathEngine.closeParentheses(expr)
        _state.update { it.copy(isComputing = true, error = null) }
        equalsJob = viewModelScope.launch {
            val ctx = coroutineContext
            val outcome = withContext(Dispatchers.Default) {
                try {
                    Result.success(MathEngine.evaluate(toEvaluate, options(recordSteps = false) { ctx.ensureActive() }))
                } catch (e: CalcException) {
                    Result.failure(e)
                }
            }
            outcome.onSuccess { result ->
                val plain = NumberFormatter.plain(result.value)
                _state.update {
                    it.copy(
                        expression = plain,
                        result = result,
                        lastExpression = toEvaluate,
                        preview = null,
                        justEvaluated = true,
                        isComputing = false,
                        error = null,
                    )
                }
                if (MathEngine.hasOperation(toEvaluate)) {
                    history.add(toEvaluate, plain, result.approximate, source)
                }
            }.onFailure { e ->
                _state.update { it.copy(isComputing = false, error = e.message ?: "Invalid expression") }
            }
        }
    }

    /** Puts a saved or scanned expression into the calculator. */
    fun loadExpression(expression: String) {
        equalsJob?.cancel()
        _state.update {
            it.copy(expression = expression.take(InputEditor.MAX_LENGTH), result = null, error = null, justEvaluated = false, isComputing = false)
        }
        refreshPreview()
    }

    /** Inserts a number (e.g. from the unit converter) at the cursor. */
    fun insertNumber(number: String) {
        val s = _state.value
        val base = if (s.justEvaluated) "" else s.expression
        loadExpression(InputEditor.appendNumber(base, number))
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CalculatorViewModel(container.historyRepository, container.settingsRepository) }
        }
    }
}
