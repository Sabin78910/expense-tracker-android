package com.sabin.expensetracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Section title used above groups of settings fields. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

/** Rounded decimal amount field with tabular figures that scrolls itself above the keyboard when focused. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    OutlinedTextField(
        value, onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(FIELD_RADIUS_DP.dp),
        textStyle = MaterialTheme.typography.bodyLarge.merge(amountFieldTextStyle()),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth().imePadding().bringIntoViewRequester(requester)
            .onFocusEvent { if (it.isFocused) scope.launch { requester.bringIntoView() } }
    )
}
