package com.sabin.expensetracker

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** True when the user has turned system animations off. */
fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun BadgeShelf(unlocked: Set<Badge>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.badges_count, unlocked.size, Badge.values().size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Badge.values().forEach { b ->
                    val on = b in unlocked
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (on) "🏅" else "🔒")
                        Column {
                            Text(badgeTitle(b), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(if (on) R.string.unlocked_desc else R.string.locked_desc, badgeDescription(b)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

/** Announces [badges] with a confetti burst (the burst is skipped when animations are off). */
@Composable
fun UnlockCelebration(badges: Set<Badge>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val animate = remember { !animationsOff(context) }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (animate) t.animateTo(1f, tween(1500)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.badge_unlocked)) },
        text = {
            Box {
                Column { badges.forEach { Text("🏅 ${badgeTitle(it)}: ${badgeDescription(it)}") } }
                if (animate && t.value < 1f) {
                    val colors = listOf(Color(0xFFE53935), Color(0xFFFFB300), Color(0xFF43A047), Color(0xFF1E88E5))
                    Canvas(Modifier.matchParentSize()) {
                        repeat(24) { i ->
                            val angle = i * 0.2618f
                            val dist = size.maxDimension * t.value * (0.5f + (i % 3) * 0.25f)
                            drawCircle(
                                colors[i % colors.size].copy(alpha = 1f - t.value),
                                radius = 4.dp.toPx(),
                                center = Offset(size.width / 2 + cos(angle) * dist, size.height / 2 + sin(angle) * dist)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.nice)) } }
    )
}
