package com.simplecalculatordeign.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simplecalculatordeign.AppContainer
import com.simplecalculatordeign.appContainer
import com.simplecalculatordeign.core.math.LocalMathTutor
import com.simplecalculatordeign.data.repository.AiRepository
import com.simplecalculatordeign.data.repository.AppSettings
import com.simplecalculatordeign.data.repository.ChatTurn
import com.simplecalculatordeign.data.repository.SettingsRepository
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.IconBadge
import com.simplecalculatordeign.ui.components.LoadingDots
import com.simplecalculatordeign.ui.rememberCopier
import com.simplecalculatordeign.ui.theme.CalcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val title: String? = null,
    val text: String,
    val steps: List<String> = emptyList(),
    val answer: String? = null,
    val source: String? = null,
    val isError: Boolean = false,
    /** Offline couldn't solve it and AI is available: offer to ask the AI. */
    val canAskAi: Boolean = false,
)

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val thinking: Boolean = false,
    val aiAvailable: Boolean = false,
)

class AssistantViewModel(
    private val ai: AiRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState(aiAvailable = ai.isConfigured))
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()
    private var settings = AppSettings()
    private var nextId = 1L

    init {
        viewModelScope.launch { settingsRepository.settings.collect { settings = it } }
    }

    fun send(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _state.value.thinking) return
        _state.update { it.copy(messages = it.messages + ChatMessage(nextId++, fromUser = true, text = q), thinking = true) }
        viewModelScope.launch {
            val tutor = LocalMathTutor(settings.formatOptions, settings.precision.coerceAtMost(60))
            val local = withContext(Dispatchers.Default) { runCatching { tutor.answer(q) }.getOrNull() }
            if (local != null && local.solved) {
                addReply(
                    ChatMessage(
                        nextId++, fromUser = false, title = local.title, text = "", steps = local.steps,
                        answer = local.finalAnswer, source = "Solved on-device · exact",
                        canAskAi = ai.isConfigured,
                    )
                )
            } else if (ai.isConfigured) {
                askAi(q)
            } else {
                addReply(
                    ChatMessage(
                        nextId++, fromUser = false, title = local?.title ?: "I couldn't understand that yet",
                        text = "The AI service isn't connected in this build, so I can only answer questions I can solve offline.",
                        steps = local?.steps.orEmpty(), source = "Offline tutor",
                    )
                )
            }
        }
    }

    fun askAiAbout(messageId: Long) {
        val index = _state.value.messages.indexOfFirst { it.id == messageId }
        val question = _state.value.messages.take(index).lastOrNull { it.fromUser }?.text ?: return
        _state.update { s -> s.copy(thinking = true, messages = s.messages.map { if (it.id == messageId) it.copy(canAskAi = false) else it }) }
        viewModelScope.launch { askAi("Explain in detail, step by step: $question") }
    }

    private suspend fun askAi(question: String) {
        val history = _state.value.messages.dropLast(1).filterNot { it.isError }.map { m ->
            ChatTurn(m.fromUser, if (m.fromUser) m.text else listOfNotNull(m.title, m.text.ifBlank { null }, m.steps.joinToString("\n").ifBlank { null }, m.answer).joinToString("\n"))
        }
        val result = ai.ask(question, history)
        result.onSuccess { answer ->
            addReply(ChatMessage(nextId++, fromUser = false, text = answer, source = "AI assistant · verify important results"))
        }.onFailure { e ->
            addReply(ChatMessage(nextId++, fromUser = false, text = e.message ?: "AI request failed", isError = true, source = "AI assistant"))
        }
    }

    private fun addReply(message: ChatMessage) {
        _state.update { it.copy(messages = it.messages + message, thinking = false) }
    }

    fun clear() = _state.update { it.copy(messages = emptyList()) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { AssistantViewModel(container.aiRepository, container.settingsRepository) }
        }
    }
}

private val SUGGESTIONS = listOf(
    "What is 15% of 2400?",
    "Solve 2x + 5 = 17",
    "x^2 - 5x + 6 = 0",
    "EMI for 500000 at 9% for 5 years",
    "Increase 1200 by 15%",
    "(250 + 150) × 5",
    "Percent change from 80 to 100",
    "Simple interest on 10000 at 5% for 3 years",
)

@Composable
fun AssistantScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: AssistantViewModel = viewModel(factory = AssistantViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.thinking) {
        val count = state.messages.size + (if (state.thinking) 1 else 0) + 1
        listState.animateScrollToItem((count - 1).coerceAtLeast(0))
    }

    AppScaffold(
        title = "AI Math Assistant",
        onBack = onBack,
        actions = {
            if (state.messages.isNotEmpty()) {
                IconButton(onClick = vm::clear) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear conversation") }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconBadge(Icons.Rounded.AutoAwesome, size = 64.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("Ask me any maths question", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        StatusPill(state.aiAvailable)
                    }
                }
                items(state.messages, key = { it.id }) { m ->
                    if (m.fromUser) UserBubble(m.text) else AssistantBubble(m, onAskAi = { vm.askAiAbout(m.id) })
                }
                if (state.thinking) {
                    item { Box(Modifier.padding(8.dp)) { LoadingDots() } }
                }
            }
            if (state.messages.isEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SUGGESTIONS.forEach { s ->
                        Text(
                            s,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { vm.send(s) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(1_000) },
                        placeholder = { Text("Type a problem, e.g. 20% off 3500") },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { vm.send(input); input = "" }),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = CalcTheme.colors.cardBorder),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CalcTheme.colors.accentGradient)
                            .clickable(enabled = input.isNotBlank() && !state.thinking) { vm.send(input); input = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(aiAvailable: Boolean) {
    val color = if (aiAvailable) CalcTheme.colors.success else MaterialTheme.colorScheme.secondary
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (aiAvailable) Icons.Rounded.Cloud else Icons.Rounded.CloudOff, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            if (aiAvailable) "Offline tutor + AI connected" else "Offline tutor · works without internet",
            style = MaterialTheme.typography.labelMedium, color = color,
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(CalcTheme.colors.accentGradient)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun AssistantBubble(m: ChatMessage, onAskAi: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .background(CalcTheme.colors.card)
            .padding(16.dp)
    ) {
        if (m.isError) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                Text(m.text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            AssistantContent(m, onAskAi)
        }
    }
}

@Composable
private fun AssistantContent(m: ChatMessage, onAskAi: () -> Unit) {
    val copy = rememberCopier()
    val colors = CalcTheme.colors
    Column {
        m.title?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        SelectionContainer {
            Column {
                if (m.text.isNotBlank()) {
                    Text(m.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                m.steps.forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        m.answer?.let { answer ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Answer: $answer",
                    style = MaterialTheme.typography.titleLarge.copy(brush = colors.accentGradient),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { copy("Answer", answer) }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy answer") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            m.source?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)) }
            if (m.canAskAi) TextButton(onClick = onAskAi) { Text("Ask AI to explain more") }
        }
    }
}
