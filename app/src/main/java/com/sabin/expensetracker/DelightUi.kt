package com.sabin.expensetracker

import android.view.HapticFeedbackConstants
import android.view.View
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Haptic tick for a saved expense. */
fun confirmHaptic(view: View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    )
}

/** Springy "saved" pill; [token] changes to replay it. Snaps in when animations are off. */
@Composable
fun SaveConfirmation(token: Int, message: String, modifier: Modifier = Modifier) {
    val reduced = animationsOff(LocalContext.current)
    val scale = remember { Animatable(0f) }
    LaunchedEffect(token) {
        if (reduced) scale.snapTo(1f) else {
            scale.snapTo(0.6f)
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 400f))
        }
    }
    Surface(
        modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = scale.value.coerceIn(0f, 1f) }
            .semantics { contentDescription = message },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 6.dp
    ) { Text(message, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium) }
}

/** Bottom sheet shown when the daily streak reaches a milestone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakMilestoneSheet(days: Int, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔥", style = MaterialTheme.typography.displayMedium)
            Text(stringResource(R.string.streak_milestone_title, days), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.streak_milestone_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.close)) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 3x4 number pad; every key is at least 48dp. */
@Composable
fun NumberPad(onKey: (Char) -> Unit, modifier: Modifier = Modifier) {
    val backspace = stringResource(R.string.keypad_backspace)
    val rows = listOf("123", "456", "789", ".0$KEYPAD_BACKSPACE")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    FilledTonalButton(
                        onClick = { onKey(k) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).then(
                            if (k == KEYPAD_BACKSPACE) Modifier.semantics { contentDescription = backspace } else Modifier
                        )
                    ) { Text(if (k == KEYPAD_BACKSPACE) "⌫" else k.toString(), style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
    }
}
