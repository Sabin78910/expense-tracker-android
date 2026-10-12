package com.sabin.expensetracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ART = listOf(R.drawable.onboarding_add, R.drawable.onboarding_charts, R.drawable.onboarding_budget)

/** Three-page intro; [onDone] receives true when the user should land in the add-expense form. */
@Composable
fun OnboardingScreen(firstRun: FirstRun, onDone: (openForm: Boolean) -> Unit) {
    val flow = remember { OnboardingFlow(firstRun) }
    var page by remember { mutableIntStateOf(flow.page) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { flow.skip(); onDone(false) }) { Text(stringResource(R.string.skip)) }
        }
        Spacer(Modifier.weight(1f))
        Image(
            painterResource(ART[page]), contentDescription = null,
            modifier = Modifier.size(240.dp).clip(MaterialTheme.shapes.large)
        )
        Spacer(Modifier.height(24.dp))
        Text(onboardingTitle(page), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            onboardingBody(page), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        PagerDots(page, ART.size, Modifier.padding(bottom = 24.dp))
        Button(
            onClick = {
                val open = flow.next()
                if (flow.finished) onDone(open) else page = flow.page
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (flow.isLast) stringResource(R.string.add_first_expense) else stringResource(R.string.next)) }
    }
}

/** Page dots; the current page is a longer pill so position is not shown by colour alone. */
@Composable
private fun PagerDots(page: Int, count: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val current = i == page
            Box(
                Modifier.height(8.dp).width(if (current) 24.dp else 8.dp).clip(CircleShape)
                    .background(if (current) scheme.primary else scheme.outlineVariant)
            )
        }
    }
}
