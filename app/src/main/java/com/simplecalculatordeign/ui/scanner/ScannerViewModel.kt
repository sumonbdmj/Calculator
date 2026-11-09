package com.simplecalculatordeign.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simplecalculatordeign.AppContainer
import com.simplecalculatordeign.core.math.CalcException
import com.simplecalculatordeign.core.math.CalcResult
import com.simplecalculatordeign.core.math.DetectedNumber
import com.simplecalculatordeign.core.math.EvalOptions
import com.simplecalculatordeign.core.math.ExpressionExtractor
import com.simplecalculatordeign.core.math.MathEngine
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.data.db.HistoryEntity
import com.simplecalculatordeign.data.repository.AppSettings
import com.simplecalculatordeign.data.repository.HistoryRepository
import com.simplecalculatordeign.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/** One recognised (or user-added) calculation on the result screen. */
data class ScanItem(
    val id: Long,
    val sourceText: String,
    /** Expression as last calculated. */
    val expression: String,
    /** Text currently in the edit field (may differ from [expression] until Recalculate). */
    val draft: String,
    val writtenAnswer: String?,
    val uncertain: Boolean,
    val notes: List<String>,
    val result: CalcResult? = null,
    val error: String? = null,
    val includeInTotal: Boolean = true,
) {
    val isEdited: Boolean get() = draft != expression
    /** null = no written answer to compare; true = matches. */
    val writtenAnswerMatches: Boolean?
        get() {
            val written = writtenAnswer?.toBigDecimalOrNull() ?: return null
            val r = result ?: return null
            return written.compareTo(r.value) == 0 ||
                (r.approximate && written.subtract(r.value).abs() < BigDecimal("0.01"))
        }
}

data class SelectableNumber(val number: DetectedNumber, val selected: Boolean = false)

enum class ScanStage { Idle, Editing, Processing, Done, Failed }

data class ScannerUiState(
    val sourceBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val crop: CropRect = CropRect(),
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val grayscale: Boolean = false,
    val stage: ScanStage = ScanStage.Idle,
    val statusText: String = "",
    val ocrText: String = "",
    val items: List<ScanItem> = emptyList(),
    val numbers: List<SelectableNumber> = emptyList(),
    val error: String? = null,
    val savedToHistory: Boolean = false,
    val busy: Boolean = false,
) {
    val validResults: List<ScanItem> get() = items.filter { it.result != null && it.error == null }
    val total: BigDecimal?
        get() {
            val included = validResults.filter { it.includeInTotal }
            if (included.size < 2) return null
            return included.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.result!!.value) }
        }
    val totalApproximate: Boolean get() = validResults.any { it.includeInTotal && it.result?.approximate == true }
    val selectedNumbersSum: BigDecimal?
        get() = numbers.filter { it.selected }.takeIf { it.isNotEmpty() }
            ?.fold(BigDecimal.ZERO) { acc, n -> acc.add(n.number.value) }
}

class ScannerViewModel(
    private val history: HistoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val ocr = OcrService()
    private var settings = AppSettings()
    private var nextId = 1L
    private var job: Job? = null

    init {
        viewModelScope.launch { settingsRepository.settings.collect { settings = it } }
    }

    private fun options(steps: Boolean = true) = EvalOptions(precision = settings.precision, angleMode = settings.angleMode, recordSteps = steps)

    // ---------------------------------------------------------------- image input & edits

    fun setImage(bitmap: Bitmap) {
        job?.cancel()
        _state.value = ScannerUiState(sourceBitmap = bitmap, stage = ScanStage.Editing)
    }

    fun setImageError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun rotate(degrees: Int) {
        val src = _state.value.sourceBitmap ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) { ImageUtils.rotate(src, degrees) }
            _state.update { it.copy(sourceBitmap = rotated, crop = CropRect(), busy = false) }
        }
    }

    fun setCrop(crop: CropRect) = _state.update { it.copy(crop = crop) }
    fun setBrightness(value: Float) = _state.update { it.copy(brightness = value) }
    fun setContrast(value: Float) = _state.update { it.copy(contrast = value) }
    fun setGrayscale(value: Boolean) = _state.update { it.copy(grayscale = value) }
    fun resetEdits() = _state.update { it.copy(crop = CropRect(), brightness = 0f, contrast = 1f, grayscale = false) }

    // ---------------------------------------------------------------- OCR

    fun startRecognition() {
        val s = _state.value
        val src = s.sourceBitmap ?: return
        job?.cancel()
        _state.update { it.copy(stage = ScanStage.Processing, statusText = "Preparing image…", error = null, savedToHistory = false) }
        job = viewModelScope.launch {
            try {
                val processed = withContext(Dispatchers.Default) {
                    ImageUtils.applyEdits(src, s.crop, s.brightness, s.contrast, s.grayscale)
                }
                _state.update { it.copy(processedBitmap = processed, statusText = "Reading text…") }
                val output = ocr.recognize(processed)
                _state.update { it.copy(statusText = "Finding calculations…") }
                val (items, numbers) = withContext(Dispatchers.Default) {
                    val extraction = ExpressionExtractor.extract(output.lines)
                    val items = extraction.expressions.map { e ->
                        evaluateItem(
                            ScanItem(
                                id = nextId++,
                                sourceText = e.sourceText,
                                expression = e.expression,
                                draft = e.expression,
                                writtenAnswer = e.writtenAnswer,
                                uncertain = e.uncertain,
                                notes = e.notes,
                            )
                        )
                    }
                    items to extraction.numbers.map { SelectableNumber(it) }
                }
                _state.update {
                    it.copy(stage = ScanStage.Done, ocrText = output.fullText, items = items, numbers = numbers, statusText = "")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(stage = ScanStage.Failed, error = "Text recognition failed: ${e.localizedMessage ?: "unknown error"}")
                }
            } catch (e: OutOfMemoryError) {
                _state.update { it.copy(stage = ScanStage.Failed, error = "The image is too large for this device. Try cropping it.") }
            }
        }
    }

    private fun evaluateItem(item: ScanItem): ScanItem = try {
        val result = MathEngine.evaluate(MathEngine.closeParentheses(item.expression), options())
        item.copy(result = result, error = null)
    } catch (e: CalcException) {
        item.copy(result = null, error = e.message)
    }

    // ---------------------------------------------------------------- review & edit

    fun updateDraft(id: Long, text: String) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == id) it.copy(draft = text.take(2_000)) else it })
    }

    fun recalculate(id: Long) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val normalized = ExpressionExtractor.normalize(item.draft, mutableListOf()).replace(" ", "")
            val updated = withContext(Dispatchers.Default) {
                evaluateItem(item.copy(expression = normalized, draft = normalized, uncertain = false, notes = emptyList()))
            }
            _state.update { s -> s.copy(items = s.items.map { if (it.id == id) updated else it }, savedToHistory = false) }
        }
    }

    fun recalculateAll() = _state.value.items.filter { it.isEdited }.forEach { recalculate(it.id) }

    fun addExpression(expression: String = "") {
        val item = ScanItem(
            id = nextId++, sourceText = "Added manually", expression = expression, draft = expression,
            writtenAnswer = null, uncertain = false, notes = emptyList(),
        )
        val evaluated = if (expression.isNotBlank()) evaluateItem(item) else item.copy(error = "Enter an expression")
        _state.update { it.copy(items = it.items + evaluated) }
    }

    fun removeItem(id: Long) = _state.update { s -> s.copy(items = s.items.filterNot { it.id == id }) }

    fun toggleInTotal(id: Long) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == id) it.copy(includeInTotal = !it.includeInTotal) else it })
    }

    fun toggleNumber(index: Int) = _state.update { s ->
        s.copy(numbers = s.numbers.mapIndexed { i, n -> if (i == index) n.copy(selected = !n.selected) else n })
    }

    fun saveToHistory() {
        val s = _state.value
        val entries = s.validResults.map {
            HistoryEntity(
                expression = it.expression,
                result = NumberFormatter.plain(it.result!!.value),
                approximate = it.result.approximate,
                source = "scanner",
            )
        }
        if (entries.isEmpty()) return
        viewModelScope.launch {
            history.addAll(entries)
            _state.update { it.copy(savedToHistory = true) }
        }
    }

    override fun onCleared() {
        ocr.close()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ScannerViewModel(container.historyRepository, container.settingsRepository) }
        }
    }
}
