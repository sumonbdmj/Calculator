package com.simplecalculatordeign.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simplecalculatordeign.core.math.MeasureUnit
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.core.math.UnitCategory
import com.simplecalculatordeign.core.math.UnitConverter
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.components.NumberField
import com.simplecalculatordeign.ui.components.PillChip
import com.simplecalculatordeign.ui.components.PremiumCard
import com.simplecalculatordeign.ui.components.SectionTitle
import com.simplecalculatordeign.ui.components.SoftIconButton
import com.simplecalculatordeign.ui.theme.Brand

@Composable
fun UnitConverterScreen(onBack: () -> Unit, onUseInCalculator: (String) -> Unit) {
    var category by rememberSaveable { mutableStateOf(UnitCategory.LENGTH) }
    val units = UnitConverter.units.getValue(category)
    var fromId by rememberSaveable(category) { mutableStateOf(units.first().id) }
    var toId by rememberSaveable(category) { mutableStateOf(units[1].id) }
    var input by rememberSaveable { mutableStateOf("1") }
    val format = LocalAppSettings.current.formatOptions

    val from = units.firstOrNull { it.id == fromId } ?: units.first()
    val to = units.firstOrNull { it.id == toId } ?: units[1]
    val value = input.toAmount()

    ToolFrame(
        "Unit Converter", Icons.Rounded.Straighten, Brush.linearGradient(listOf(Brand.Pink, Brand.Coral)),
        "Precise conversions, including Bangladeshi land and weight units.", onBack,
    ) {
        ChipsRow { UnitCategory.entries.forEach { c -> PillChip(c.title, category == c, { category = c }) } }
        PremiumCard {
            NumberField("Value", input, { input = it }, allowNegative = category == UnitCategory.TEMPERATURE, imeDone = true)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UnitPicker("From", from, units, Modifier.weight(1f)) { fromId = it.id }
                SoftIconButton(Icons.Rounded.SwapVert, "Swap units", {
                    val f = fromId; fromId = toId; toId = f
                }, modifier = Modifier.padding(horizontal = 8.dp))
                UnitPicker("To", to, units, Modifier.weight(1f)) { toId = it.id }
            }
        }
        if (value != null) {
            val result = UnitConverter.convert(value, from, to, category)
            ResultHero(
                "${NumberFormatter.format(value, format)} ${from.symbol} =",
                "${NumberFormatter.format(result, format)} ${to.symbol}",
                NumberFormatter.plain(result),
            )
            TextButton(onClick = { onUseInCalculator(NumberFormatter.plain(result)) }) {
                Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Use in calculator")
            }
            SectionTitle("All ${category.title.lowercase()} units")
            PremiumCard {
                units.forEach { u ->
                    val converted = UnitConverter.convert(value, from, u, category)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { toId = u.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.bodyMedium)
                            Text(u.symbol, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            NumberFormatter.format(converted, format),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (u.id == to.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.3f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitPicker(label: String, selected: MeasureUnit, units: List<MeasureUnit>, modifier: Modifier, onSelect: (MeasureUnit) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { open = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            units.forEach { u ->
                DropdownMenuItem(
                    text = { Text("${u.name} (${u.symbol})") },
                    onClick = { onSelect(u); open = false },
                )
            }
        }
    }
}

