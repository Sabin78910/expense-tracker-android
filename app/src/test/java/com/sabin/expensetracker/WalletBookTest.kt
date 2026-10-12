package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletBookTest {
    @Test fun newBookHasDefaultCash() {
        val b = WalletBook()
        assertEquals(1, b.wallets.size)
        assertEquals(Wallet(DEFAULT_WALLET_ID, "Cash", WalletType.CASH, 0.0), b.wallets[0])
    }

    @Test fun balanceIsOpeningMinusExpenses() {
        val b = WalletBook()
        val esewa = b.add("eSewa", WalletType.ESEWA, 1000.0)!!
        val ex = listOf(Expense(1, "a", 100.0, "Food", 1, walletId = esewa.id), Expense(2, "b", 50.0, "Food", 1))
        assertEquals(900.0, b.balance(esewa.id, ex), 0.001)
        assertEquals(-50.0, b.balance(DEFAULT_WALLET_ID, ex), 0.001)
    }

    @Test fun transfersMoveMoneyBetweenWallets() {
        val b = WalletBook()
        val bank = b.add("Bank", WalletType.BANK, 500.0)!!
        assertTrue(b.transfer(bank.id, DEFAULT_WALLET_ID, 200.0, 5L))
        assertEquals(300.0, b.balance(bank.id, emptyList()), 0.001)
        assertEquals(200.0, b.balance(DEFAULT_WALLET_ID, emptyList()), 0.001)
    }

    @Test fun invalidTransfersAreRejected() {
        val b = WalletBook()
        assertFalse(b.transfer(DEFAULT_WALLET_ID, DEFAULT_WALLET_ID, 5.0, 1L))
        assertFalse(b.transfer(DEFAULT_WALLET_ID, 99L, 5.0, 1L))
        assertFalse(b.transfer(DEFAULT_WALLET_ID, 99L, 0.0, 1L))
        assertTrue(b.transfers.isEmpty())
    }

    @Test fun transfersAreNeverSpending() {
        val b = WalletBook()
        val k = b.add("Khalti", WalletType.KHALTI, 0.0)!!
        b.transfer(DEFAULT_WALLET_ID, k.id, 100.0, 1L)
        val book = ExpenseBook()
        book.add("x", 30.0, "Food", 1L)
        assertEquals(30.0, book.total(), 0.001)
        assertEquals(30.0, book.totalsByCategory().values.sum(), 0.001)
    }

    @Test fun addRejectsBlankLongAndDuplicateNames() {
        val b = WalletBook()
        assertNull(b.add("  ", WalletType.BANK, 0.0))
        assertNull(b.add("x".repeat(MAX_WALLET_NAME_LENGTH + 1), WalletType.BANK, 0.0))
        assertNull(b.add("cash", WalletType.BANK, 0.0))
    }

    @Test fun editChangesNameTypeAndOpening() {
        val b = WalletBook()
        val w = b.add("Bank", WalletType.BANK, 10.0)!!
        assertTrue(b.edit(w.id, "NIC Asia", WalletType.BANK, 20.0))
        assertEquals(Wallet(w.id, "NIC Asia", WalletType.BANK, 20.0), b.wallet(w.id))
        assertFalse(b.edit(w.id, "Cash", WalletType.BANK, 20.0))
    }

    @Test fun walletWithExpensesCannotBeDeleted() {
        val b = WalletBook()
        val w = b.add("Bank", WalletType.BANK, 0.0)!!
        val ex = listOf(Expense(1, "a", 1.0, "Food", 1, walletId = w.id))
        assertFalse(b.remove(w.id, ex))
        assertTrue(b.remove(w.id, emptyList()))
        assertNull(b.wallet(w.id))
    }

    @Test fun lastWalletAndTransfersOfDeletedWalletHandled() {
        val b = WalletBook()
        val w = b.add("Bank", WalletType.BANK, 0.0)!!
        b.transfer(DEFAULT_WALLET_ID, w.id, 5.0, 1L)
        assertTrue(b.remove(w.id, emptyList()))
        assertTrue(b.transfers.isEmpty())
        assertFalse(b.remove(DEFAULT_WALLET_ID, emptyList()))
    }

    @Test fun serializeRoundTrips() {
        val b = WalletBook()
        b.add("My\tBank", WalletType.BANK, 12.5)
        b.transfer(DEFAULT_WALLET_ID, 2L, 3.0, 9L)
        val r = WalletBook.deserialize(b.serialize())
        assertEquals(b.wallets, r.wallets)
        assertEquals(b.transfers, r.transfers)
    }

    @Test fun deserializeEmptyOrCorruptGivesDefaultCash() {
        listOf("", "garbage", "W\tx").forEach {
            assertEquals(listOf(DEFAULT_WALLET_ID), WalletBook.deserialize(it).wallets.map { w -> w.id })
        }
    }
}
