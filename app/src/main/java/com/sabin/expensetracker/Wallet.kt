package com.sabin.expensetracker

const val DEFAULT_WALLET_ID = 1L
const val MAX_WALLET_NAME_LENGTH = 20

enum class WalletType { CASH, BANK, ESEWA, KHALTI }

data class Wallet(val id: Long, val name: String, val type: WalletType, val openingBalance: Double)

/** Money moved between two wallets; never counted as spending. */
data class Transfer(val id: Long, val fromId: Long, val toId: Long, val amount: Double, val timestamp: Long)

/** Pure, testable wallet logic (no Android dependencies). Always contains the default Cash wallet. */
class WalletBook(initialWallets: List<Wallet> = emptyList(), initialTransfers: List<Transfer> = emptyList()) {
    private val items = initialWallets.toMutableList()
    private val moves = initialTransfers.toMutableList()
    private var nextId = 0L
    private var nextTransferId = (moves.maxOfOrNull { it.id } ?: 0L) + 1

    init {
        if (items.none { it.id == DEFAULT_WALLET_ID }) items.add(0, Wallet(DEFAULT_WALLET_ID, "Cash", WalletType.CASH, 0.0))
        nextId = items.maxOf { it.id } + 1
    }

    val wallets: List<Wallet> get() = items.toList()
    val transfers: List<Transfer> get() = moves.toList()

    fun wallet(id: Long): Wallet? = items.firstOrNull { it.id == id }

    private fun validName(raw: String, ignoreId: Long? = null): String? {
        val n = raw.trim()
        if (n.isEmpty() || n.length > MAX_WALLET_NAME_LENGTH || '\n' in n || '\t' in n) return null
        if (items.any { it.id != ignoreId && it.name.equals(n, ignoreCase = true) }) return null
        return n
    }

    /** Adds a wallet; null if the name is blank, too long or a case-insensitive duplicate. */
    fun add(name: String, type: WalletType, openingBalance: Double): Wallet? {
        val n = validName(name) ?: return null
        if (!openingBalance.isFinite()) return null
        return Wallet(nextId++, n, type, openingBalance).also { items.add(it) }
    }

    fun edit(id: Long, name: String, type: WalletType, openingBalance: Double): Boolean {
        val i = items.indexOfFirst { it.id == id }
        val n = validName(name, id)
        if (i < 0 || n == null || !openingBalance.isFinite()) return false
        items[i] = Wallet(id, n, type, openingBalance)
        return true
    }

    /**
     * Deletes wallet [id] and its transfers. Refused (false) while [expenses] still use it,
     * and for the default Cash wallet.
     */
    fun remove(id: Long, expenses: List<Expense>): Boolean {
        if (id == DEFAULT_WALLET_ID || items.none { it.id == id } || expenses.any { it.walletId == id }) return false
        items.removeAll { it.id == id }
        moves.removeAll { it.fromId == id || it.toId == id }
        return true
    }

    fun transfer(fromId: Long, toId: Long, amount: Double, timestamp: Long): Boolean {
        if (fromId == toId || wallet(fromId) == null || wallet(toId) == null) return false
        if (!amount.isFinite() || amount <= 0) return false
        moves.add(Transfer(nextTransferId++, fromId, toId, amount, timestamp))
        return true
    }

    /** Opening balance − expenses ± transfers. */
    fun balance(id: Long, expenses: List<Expense>): Double {
        val opening = wallet(id)?.openingBalance ?: 0.0
        return opening - expenses.filter { it.walletId == id }.sumOf { it.amount } +
            moves.filter { it.toId == id }.sumOf { it.amount } - moves.filter { it.fromId == id }.sumOf { it.amount }
    }

    /**
     * Merges [imported] wallets (matched by name, case-insensitive; new ones are added) and returns a map
     * from imported id to local id. The default id 1 always maps to Cash.
     */
    fun mergeWallets(imported: List<Wallet>): Map<Long, Long> {
        val map = mutableMapOf<Long, Long>()
        imported.forEach { w ->
            val existing = items.firstOrNull { it.name.equals(w.name.trim(), ignoreCase = true) }
            val local = existing ?: add(w.name, w.type, w.openingBalance)
            map[w.id] = local?.id ?: DEFAULT_WALLET_ID
        }
        return map
    }

    /** Adds imported transfers whose wallets map through [idMap], skipping exact duplicates. */
    fun mergeTransfers(imported: List<Transfer>, idMap: Map<Long, Long>) {
        imported.forEach { t ->
            val from = idMap[t.fromId] ?: return@forEach
            val to = idMap[t.toId] ?: return@forEach
            if (moves.none { it.fromId == from && it.toId == to && it.amount == t.amount && it.timestamp == t.timestamp }) {
                transfer(from, to, t.amount, t.timestamp)
            }
        }
    }

    /** Lines: `W id name type opening` and `T id from to amount timestamp`, tab-separated. */
    fun serialize(): String =
        (items.map { listOf("W", it.id, ExpenseBook.escape(it.name), it.type.name, it.openingBalance).joinToString("\t") } +
            moves.map { listOf("T", it.id, it.fromId, it.toId, it.amount, it.timestamp).joinToString("\t") })
            .joinToString("\n")

    companion object {
        fun deserialize(data: String): WalletBook {
            val wallets = mutableListOf<Wallet>()
            val transfers = mutableListOf<Transfer>()
            data.lines().forEach { line ->
                val f = line.split("\t")
                if (f[0] == "W" && f.size == 5) {
                    val id = f[1].toLongOrNull() ?: return@forEach
                    val type = WalletType.entries.firstOrNull { it.name == f[3] } ?: return@forEach
                    val opening = f[4].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return@forEach
                    wallets.add(Wallet(id, ExpenseBook.unescape(f[2]), type, opening))
                } else if (f[0] == "T" && f.size == 6) {
                    val id = f[1].toLongOrNull() ?: return@forEach
                    val from = f[2].toLongOrNull() ?: return@forEach
                    val to = f[3].toLongOrNull() ?: return@forEach
                    val amount = f[4].toDoubleOrNull() ?: return@forEach
                    val ts = f[5].toLongOrNull() ?: return@forEach
                    transfers.add(Transfer(id, from, to, amount, ts))
                }
            }
            return WalletBook(wallets, transfers)
        }
    }
}
