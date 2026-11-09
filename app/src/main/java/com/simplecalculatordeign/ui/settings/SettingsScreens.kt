package com.simplecalculatordeign.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplecalculatordeign.BuildConfig
import com.simplecalculatordeign.appContainer
import com.simplecalculatordeign.core.math.AngleMode
import com.simplecalculatordeign.core.math.GroupingStyle
import com.simplecalculatordeign.data.repository.ThemeMode
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.GradientTitle
import com.simplecalculatordeign.ui.components.IconBadge
import com.simplecalculatordeign.ui.components.PillChip
import com.simplecalculatordeign.ui.components.PremiumCard
import com.simplecalculatordeign.ui.components.SectionTitle
import com.simplecalculatordeign.ui.onboarding.AppLogo
import com.simplecalculatordeign.ui.theme.Brand
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpenAbout: () -> Unit, onOpenPrivacy: () -> Unit) {
    val container = LocalContext.current.appContainer
    val repo = container.settingsRepository
    val settings = LocalAppSettings.current
    val historyCount by container.historyRepository.count.collectAsStateWithLifecycle(initialValue = 0)
    val scope = rememberCoroutineScope()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    AppScaffold(title = "Settings") { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            SectionTitle("Appearance")
            PremiumCard {
                SettingHeader(Icons.Rounded.Brightness6, "Theme", "Choose light, dark or follow the system")
                Spacer(Modifier.height(12.dp))
                ChipRow {
                    PillChip("System", settings.themeMode == ThemeMode.SYSTEM, { scope.launch { repo.setTheme(ThemeMode.SYSTEM) } })
                    PillChip("Light", settings.themeMode == ThemeMode.LIGHT, { scope.launch { repo.setTheme(ThemeMode.LIGHT) } })
                    PillChip("Dark", settings.themeMode == ThemeMode.DARK, { scope.launch { repo.setTheme(ThemeMode.DARK) } })
                }
            }

            SectionTitle("Feedback")
            PremiumCard(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 18.dp)) {
                SwitchRow(Icons.Rounded.MusicNote, "Key sound", "Click sound on key press", settings.soundEnabled) {
                    scope.launch { repo.setSound(it) }
                }
                SwitchRow(Icons.Rounded.Vibration, "Vibration", "Haptic feedback on key press", settings.vibrationEnabled) {
                    scope.launch { repo.setVibration(it) }
                }
            }

            SectionTitle("Calculation")
            PremiumCard {
                SettingHeader(
                    Icons.Rounded.Tune, "Precision for endless decimals",
                    "Exact results (like 2^200) are always shown in full. This only applies to values such as 1 ÷ 3, √2, π or sin.",
                )
                Spacer(Modifier.height(12.dp))
                ChipRow {
                    listOf(20, 34, 50, 100, 200).forEach { p ->
                        PillChip("$p digits", settings.precision == p, { scope.launch { repo.setPrecision(p) } })
                    }
                }
                Spacer(Modifier.height(20.dp))
                SettingHeader(Icons.Rounded.Tag, "Decimal places shown", "Auto shows every digit. Fixed places are marked ≈ when rounding hides digits.")
                Spacer(Modifier.height(12.dp))
                ChipRow {
                    PillChip("Auto", settings.decimalPlaces == -1, { scope.launch { repo.setDecimalPlaces(-1) } })
                    listOf(0, 2, 4, 6, 8, 10).forEach { d ->
                        PillChip("$d", settings.decimalPlaces == d, { scope.launch { repo.setDecimalPlaces(d) } })
                    }
                }
                Spacer(Modifier.height(20.dp))
                SettingHeader(Icons.Rounded.FormatListNumbered, "Number format", "Digit grouping for results and expressions")
                Spacer(Modifier.height(12.dp))
                ChipRow {
                    PillChip("1,234,567", settings.grouping == GroupingStyle.INTERNATIONAL, { scope.launch { repo.setGrouping(GroupingStyle.INTERNATIONAL) } })
                    PillChip("12,34,567", settings.grouping == GroupingStyle.INDIAN, { scope.launch { repo.setGrouping(GroupingStyle.INDIAN) } })
                    PillChip("1234567", settings.grouping == GroupingStyle.NONE, { scope.launch { repo.setGrouping(GroupingStyle.NONE) } })
                }
                Spacer(Modifier.height(20.dp))
                SettingHeader(Icons.Rounded.Straighten, "Angle unit", "Used by sin, cos and tan")
                Spacer(Modifier.height(12.dp))
                ChipRow {
                    PillChip("Degrees", settings.angleMode == AngleMode.DEGREES, { scope.launch { repo.setAngleMode(AngleMode.DEGREES) } })
                    PillChip("Radians", settings.angleMode == AngleMode.RADIANS, { scope.launch { repo.setAngleMode(AngleMode.RADIANS) } })
                }
            }

            SectionTitle("Data")
            PremiumCard(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 18.dp)) {
                LinkRow(
                    Icons.Rounded.DeleteSweep, "Clear history",
                    if (historyCount == 0) "History is empty" else "$historyCount saved calculations",
                    enabled = historyCount > 0,
                    danger = true,
                ) { confirmClear = true }
            }

            SectionTitle("About")
            PremiumCard(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 18.dp)) {
                LinkRow(Icons.Rounded.Policy, "Privacy policy", "How your data is handled", onClick = onOpenPrivacy)
                LinkRow(Icons.Rounded.Info, "About application", "Features and technology", onClick = onOpenAbout)
                LinkRow(Icons.Rounded.Tag, "App version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", enabled = false) {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("All $historyCount saved calculations will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.historyRepository.clear() }
                    confirmClear = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun SettingHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger && enabled) tint else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------ about

@Composable
fun AboutScreen(onBack: () -> Unit) {
    AppScaffold(title = "About", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            AppLogo(96.dp)
            Spacer(Modifier.height(18.dp))
            GradientTitle("Simple Calculator Deign", MaterialTheme.typography.headlineSmall)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                "A premium calculator with unlimited-precision maths, a camera scanner that turns documents into reviewable calculations, step-by-step explanations and practical everyday tools.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            val features = listOf(
                "Exact BigInteger / BigDecimal engine — up to 100,000-digit results",
                "Scientific mode with degree & radian trigonometry",
                "On-device OCR (Google ML Kit) — images never leave your phone",
                "Step-by-step explanations for every calculation",
                "Percentage, discount, GST/VAT, loan EMI and unit converter",
                "Searchable history stored locally with Room",
                "Optional AI Math Assistant via your own secure backend",
            )
            PremiumCard {
                Text("Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                features.forEach {
                    Row(Modifier.padding(vertical = 5.dp)) {
                        Text("•", color = Brand.Purple, modifier = Modifier.width(18.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Info, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Built with", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Kotlin · Jetpack Compose · Material 3 · CameraX · ML Kit · Room · MVVM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ privacy

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val sections = listOf(
        "Summary" to "Simple Calculator Deign works offline. It has no ads, no analytics and no account. Your calculations and scanned images stay on your device.",
        "Calculations & history" to "Every completed calculation is stored in a private database inside the app on your device. You can delete single items or clear all history at any time from the History or Settings screens. Uninstalling the app removes all data. App backup is disabled so history is not copied to cloud backups.",
        "Camera & images" to "The camera is used only when you open the scanner. Captured or imported images are processed on-device by Google ML Kit text recognition (bundled model) and are kept only in memory for the current scan. They are not uploaded or saved unless you choose to share or save a PDF report.",
        "Sharing & export" to "When you share text or a PDF report, you choose the destination. PDF files you save are written only to the location you pick using the Android file picker.",
        "AI Math Assistant (optional)" to "The assistant answers many questions fully offline. Only when an AI backend has been configured by the app builder and the offline tutor cannot answer, the text of your question (and the recent conversation) is sent over HTTPS to that backend to generate an explanation. Images are never sent. No secret keys are stored in the app.",
        "Permissions" to "Camera — to scan documents (optional). Internet — only for the optional AI assistant. Vibrate — for key-press haptics, which you can turn off in Settings.",
        "Contact" to "Questions about this policy can be sent to the app developer through the store listing.",
    )
    AppScaffold(title = "Privacy policy", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sections.forEach { (title, body) ->
                PremiumCard {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

