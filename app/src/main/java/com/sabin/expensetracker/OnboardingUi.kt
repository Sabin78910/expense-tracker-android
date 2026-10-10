package com.sabin.expensetracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Image(painterResource(ART[page]), contentDescription = null, modifier = Modifier.size(240.dp))
        Spacer(Modifier.height(24.dp))
        Text(onboardingTitle(page), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            onboardingBody(page), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val open = flow.next()
                if (flow.finished) onDone(open) else page = flow.page
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (flow.isLast) stringResource(R.string.add_first_expense) else stringResource(R.string.next)) }
    }
}
