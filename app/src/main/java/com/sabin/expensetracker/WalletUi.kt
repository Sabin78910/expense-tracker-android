package com.sabin.expensetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun walletLabel(w: Wallet): String =
    if (w.id == DEFAULT_WALLET_ID && w.name == "Cash") stringResource(R.string.wallet_cash) else w.name

@Composable
private fun typeLabel(t: WalletType) = stringResource(
    when (t) {
        WalletType.CASH -> R.string.wallet_cash
        WalletType.BANK -> R.string.wallet_bank
        WalletType.ESEWA -> R.string.wallet_esewa
        WalletType.KHALTI -> R.string.wallet_khalti
    }
)

/** Wallet chips for the expense form. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletPicker(wallets: List<Wallet>, selected: Long, onSelect: (Long) -> Unit) {
    FlowRow(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        wallets.forEach { w ->
            val label = walletLabel(w)
            val desc = stringResource(R.string.wallet_selected, label)
            FilterChip(
                selected = w.id == selected, onClick = { onSelect(w.id) }, label = { Text(label) },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = desc }
            )
        }
    }
}

/** Per-wallet balances with add, edit, delete and transfer actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletSection(
    wallets: WalletBook,
    expenses: List<Expense>,
    onChange: () -> Unit,
    onMoveExpenses: (from: Long, to: Long) -> Unit
) {
    var editing by remember { mutableStateOf<Wallet?>(null) }
    var adding by remember { mutableStateOf(false) }
    var transferring by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.wallets), style = MaterialTheme.typography.titleMedium)
        wallets.wallets.forEach { w ->
            val balance = formatNpr(wallets.balance(w.id, expenses))
            val desc = stringResource(R.string.wallet_balance, walletLabel(w), balance)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) { contentDescription = desc },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { editing = w }) { Text(walletLabel(w)) }
                Text(balance, style = MaterialTheme.typography.bodyLarge.merge(amountFieldTextStyle()))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { adding = true }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.add_wallet)) }
            OutlinedButton(onClick = { transferring = true }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.transfer)) }
        }
        Text(stringResource(R.string.transfer_note), style = MaterialTheme.typography.bodySmall)
    }
    if (adding) WalletDialog(null, wallets, expenses, { adding = false }, onChange, onMoveExpenses)
    editing?.let { WalletDialog(it, wallets, expenses, { editing = null }, onChange, onMoveExpenses) }
    if (transferring) TransferDialog(wallets, { transferring = false }, onChange)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletDialog(
    wallet: Wallet?,
    book: WalletBook,
    expenses: List<Expense>,
    onClose: () -> Unit,
    onChange: () -> Unit,
    onMoveExpenses: (Long, Long) -> Unit
) {
    var name by remember { mutableStateOf(wallet?.name ?: "") }
    var type by remember { mutableStateOf(wallet?.type ?: WalletType.BANK) }
    var opening by remember { mutableStateOf(wallet?.openingBalance?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalid = stringResource(R.string.wallet_invalid)
    val inUse = wallet?.let { w -> expenses.any { it.walletId == w.id } } == true
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(if (wallet == null) R.string.add_wallet else R.string.edit_wallet)) },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(MAX_WALLET_NAME_LENGTH).replace("\n", "").replace("\t", "") },
                    label = { Text(stringResource(R.string.wallet_name)) }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WalletType.entries.forEach { t ->
                        FilterChip(selected = t == type, onClick = { type = t }, label = { Text(typeLabel(t)) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                OutlinedTextField(opening, { opening = it }, label = { Text(stringResource(R.string.opening_balance)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.merge(amountFieldTextStyle()))
                if (inUse && wallet != null) {
                    val label = walletLabel(wallet)
                    val cash = book.wallet(DEFAULT_WALLET_ID)!!
                    if (wallet.id != DEFAULT_WALLET_ID) {
                        TextButton(onClick = { onMoveExpenses(wallet.id, DEFAULT_WALLET_ID); onChange() }) {
                            Text(stringResource(R.string.move_expenses_to, label, walletLabel(cash)))
                        }
                    }
                }
                val msg = error ?: if (inUse && wallet?.id != DEFAULT_WALLET_ID) stringResource(R.string.wallet_in_use, wallet?.let { walletLabel(it) } ?: "") else null
                msg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = if (opening.isBlank()) 0.0 else opening.toDoubleOrNull()
                val ok = value != null && if (wallet == null) book.add(name, type, value) != null
                else book.edit(wallet.id, name, type, value)
                if (ok) { onChange(); onClose() } else error = invalid
            }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = {
            Row {
                if (wallet != null && wallet.id != DEFAULT_WALLET_ID) {
                    TextButton(onClick = { if (book.remove(wallet.id, expenses)) { onChange(); onClose() } },
                        enabled = !inUse, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.delete)) }
                }
                TextButton(onClick = onClose, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransferDialog(book: WalletBook, onClose: () -> Unit, onChange: () -> Unit) {
    var from by remember { mutableStateOf(book.wallets.first().id) }
    var to by remember { mutableStateOf(book.wallets.getOrElse(1) { book.wallets.first() }.id) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.transfer)) },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    book.wallets.forEach { w ->
                        val label = stringResource(R.string.transfer_from, walletLabel(w))
                        FilterChip(selected = w.id == from, onClick = { from = w.id }, label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    book.wallets.forEach { w ->
                        val label = stringResource(R.string.transfer_to, walletLabel(w))
                        FilterChip(selected = w.id == to, onClick = { to = w.id }, label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                OutlinedTextField(amount, { amount = filterAmountInput(amount, it) }, label = { Text(stringResource(R.string.amount)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.merge(amountFieldTextStyle()))
                if (error) Text(stringResource(R.string.transfer_invalid), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (book.transfer(from, to, amount.toDoubleOrNull() ?: 0.0, System.currentTimeMillis())) { onChange(); onClose() }
                else error = true
            }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.done)) }
        },
        dismissButton = { TextButton(onClick = onClose, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) } }
    )
}
