package com.xlr8.app.ui.easter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The hidden menu unlocked by tapping the XLR8 logo eight times. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretMenuScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("XLR8 Secret Menu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

            Text(
                text = "“${EasterEggs.BLEACH_MANTRA}”",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
            )

            Section("The REAL Big Three ✊")
            EasterEggs.REAL_BIG_THREE.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
            Text(
                "(yes, Bleach. no, not that one.)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Section("Certified One Piece takes")
            EasterEggs.ONE_PIECE_ROASTS.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }

            Section("On Naruto (no glazing)")
            EasterEggs.NARUTO_TAKES.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }

            Section("Credits")
            EasterEggs.FRIEND_GROUP_CREDITS.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }

            Text(
                "Built for the group chat. No accounts, no trackers, all vibes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}
