package com.xlr8.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlr8.app.BuildConfig
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.ui.theme.ThemeMode

private val QUALITIES = listOf("best", "1080", "720", "480", "360", "worst")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val out = context.contentResolver.openOutputStream(uri)
            if (out != null) viewModel.exportTo(out) { ok ->
                Toast.makeText(context, if (ok) "Backup exported" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val input = context.contentResolver.openInputStream(uri)
            if (input != null) viewModel.importFrom(input) { ok ->
                Toast.makeText(context, if (ok) "Backup restored" else "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
        )

        SectionTitle("Appearance")
        ThemeModeRow(settings.themeMode, viewModel::setThemeMode)
        SwitchRow("Material You dynamic color", settings.dynamicColor, viewModel::setDynamicColor)
        SwitchRow("AMOLED black (dark themes)", settings.amoled, viewModel::setAmoled)

        SectionTitle("Playback")
        ChoiceRow(
            label = "Default quality",
            options = QUALITIES,
            selected = settings.defaultQuality,
            onSelect = viewModel::setDefaultQuality,
        )
        ChoiceRow(
            label = "Default audio",
            options = TranslationType.entries.map { it.name },
            selected = settings.defaultTranslation.name,
            onSelect = { name ->
                viewModel.setDefaultTranslation(
                    if (name == TranslationType.DUB.name) TranslationType.DUB else TranslationType.SUB,
                )
            },
        )

        SectionTitle("Data")
        ActionRow("Clear watch history") { viewModel.clearHistory() }
        ActionRow("Clear source & search cache") { viewModel.clearCache() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { exportLauncher.launch("xlr8-backup.json") }) { Text("Export backup") }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import backup") }
        }
        Text(
            "Backup is a plain JSON file you save and restore yourself. There is no server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionTitle("Privacy")
        Text(
            text = PRIVACY_STATEMENT,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionTitle("About")
        Text(
            "XLR8 v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        Text(
            "github.com/xlr8-bl/ani-cli-app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Box {
            FilterChip(selected = true, onClick = { expanded = true }, label = { Text(selected) })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private const val PRIVACY_STATEMENT =
    "XLR8 has no accounts, no login, and no email. There are no analytics, no crash-reporting, " +
        "no ad SDKs, and no trackers of any kind. Everything — your history, watchlist, downloads " +
        "and settings — stays on this device. The only network calls are to AniList (metadata), " +
        "AllAnime (streams), and the image/stream CDNs they reference. Uninstalling the app wipes " +
        "everything, because the device was only ever the account."
