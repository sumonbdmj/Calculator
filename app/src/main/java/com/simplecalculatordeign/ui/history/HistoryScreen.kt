package com.simplecalculatordeign.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.simplecalculatordeign.core.math.MathEngine
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.data.db.HistoryEntity
import com.simplecalculatordeign.data.repository.HistoryRepository
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.EmptyState
import com.simplecalculatordeign.ui.components.PremiumCard
import com.simplecalculatordeign.ui.components.SoftIconButton
import com.simplecalculatordeign.ui.rememberCopier
import com.simplecalculatordeign.ui.theme.CalcTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<HistoryEntity>?> = query
        .flatMapLatest { repository.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
    fun clearAll() = viewModelScope.launch { repository.clear() }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(container.historyRepository) }
        }
    }
}

private fun dayLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun sameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (sameDay(now, then)) return "Today"
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(now, then)) return "Yesterday"
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(onReuse: (String) -> Unit, onShowSteps: (String) -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val query by vm.query.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    AppScaffold(
        title = "History",
        actions = {
            if (!items.isNullOrEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear all history", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it.take(100) },
                placeholder = { Text("Search calculations or results") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = CalcTheme.colors.cardBorder,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val list = items
            when {
                list == null -> Unit
                list.isEmpty() && query.isNotEmpty() -> EmptyState(Icons.Rounded.Search, "No matches", "Nothing in your history matches \"$query\".")
                list.isEmpty() -> EmptyState(
                    Icons.Rounded.History,
                    "No calculations yet",
                    "Every calculation you complete is saved here automatically — tap one to use it again.",
                )
                else -> {
                    val grouped = remember(list) { list.groupBy { dayLabel(it.timestamp) } }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        grouped.forEach { (day, entries) ->
                            stickyHeader(key = "h_$day") {
                                Text(
                                    day,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                            items(entries, key = { it.id }) { item ->
                                HistoryItem(
                                    item = item,
                                    onReuse = { onReuse(item.expression) },
                                    onSteps = { onShowSteps(item.expression) },
                                    onDelete = { vm.delete(item.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text("Clear all history?") },
            text = { Text("This permanently deletes every saved calculation. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); confirmClear = false }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HistoryItem(
    item: HistoryEntity,
    onReuse: () -> Unit,
    onSteps: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = LocalAppSettings.current.formatOptions
    val copy = rememberCopier()
    val resultText = remember(item.result, format) {
        item.result.toBigDecimalOrNull()?.let { NumberFormatter.format(it, format, item.approximate) } ?: item.result
    }
    val exprText = remember(item.expression, format) { NumberFormatter.formatExpression(item.expression, format.grouping) }
    val time = remember(item.timestamp) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.timestamp)) }

    PremiumCard(modifier = modifier.animateContentSize(), onClick = onReuse, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.source.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (MathEngine.hasOperation(item.expression)) {
                SoftIconButton(Icons.Rounded.Functions, "Show steps", onSteps, size = 34.dp)
                Spacer(Modifier.width(6.dp))
            }
            SoftIconButton(Icons.Rounded.ContentCopy, "Copy result", { copy("Result", item.result) }, size = 34.dp)
            Spacer(Modifier.width(6.dp))
            SoftIconButton(Icons.Rounded.DeleteOutline, "Delete", onDelete, tint = MaterialTheme.colorScheme.error, size = 34.dp)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(exprText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                (if (item.approximate) "≈ " else "= ") + resultText.let { if (it.length > 400) it.take(400) + "… (${it.length} chars, copy for all)" else it },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

