package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Test

/** Stored data from before wallets existed must load with every expense in the default Cash wallet. */
class WalletMigrationTest {
    @Test fun legacyLinesLandInCash() {
        val old = "2\tTea\t20.0\tFood\t5\tnote\n1\tMomo\t250.0\tFood"
        val book = ExpenseBook.deserialize(old)
        assertEquals(listOf(DEFAULT_WALLET_ID, DEFAULT_WALLET_ID), book.expenses.map { it.walletId })
        assertEquals(listOf("Tea", "Momo"), book.expenses.map { it.title })
        assertEquals(270.0, book.total(), 0.001)
    }

    @Test fun walletIdRoundTrips() {
        val book = ExpenseBook()
        book.add("a", 1.0, "Food", 1L, walletId = 3L)
        assertEquals(3L, ExpenseBook.deserialize(book.serialize()).expenses[0].walletId)
    }

    @Test fun addAndUpdateSetWallet() {
        val book = ExpenseBook()
        val e = book.add("a", 1.0, "Food", 1L)
        assertEquals(DEFAULT_WALLET_ID, e.walletId)
        book.update(e.id, "a", 1.0, "Food", walletId = 4L)
        assertEquals(4L, book.expenses[0].walletId)
        book.update(e.id, "a", 1.0, "Food")
        assertEquals(4L, book.expenses[0].walletId)
    }

    @Test fun reassignWalletMovesExpenses() {
        val book = ExpenseBook()
        book.add("a", 1.0, "Food", 1L, walletId = 2L)
        book.add("b", 1.0, "Food", 1L, walletId = 3L)
        assertEquals(1, book.reassignWallet(2L, DEFAULT_WALLET_ID))
        assertEquals(listOf(3L, DEFAULT_WALLET_ID), book.expenses.map { it.walletId })
    }

    @Test fun backupRoundTripsWalletsTransfersAndExpenseWallet() {
        val w = WalletBook()
        w.add("eSewa", WalletType.ESEWA, 100.0)
        w.transfer(DEFAULT_WALLET_ID, 2L, 10.0, 7L)
        val ex = listOf(Expense(1, "a", 5.0, "Food", 1, walletId = 2L))
        val data = Backup.parseFull(Backup.exportFull(ex, CategoryLimits.EMPTY, CategoryList.EMPTY, w))
        assertEquals(2L, data.expenses[0].walletId)
        assertEquals(w.wallets, data.wallets)
        assertEquals(w.transfers, data.transfers)
    }

    @Test fun oldBackupWithoutWalletsLoadsIntoCash() {
        val data = Backup.parseFull("""{"version":1,"expenses":[{"id":1,"title":"a","amount":1.0,"category":"Food","timestamp":1}]}""")
        assertEquals(DEFAULT_WALLET_ID, data.expenses[0].walletId)
        assertEquals(emptyList<Wallet>(), data.wallets)
    }

    @Test fun mergeMapsImportedWalletsByNameAndType() {
        val w = WalletBook()
        val imported = listOf(Wallet(1, "Cash", WalletType.CASH, 0.0), Wallet(7, "eSewa", WalletType.ESEWA, 50.0))
        val map = w.mergeWallets(imported)
        assertEquals(DEFAULT_WALLET_ID, map[1L])
        assertEquals(w.wallets.first { it.name == "eSewa" }.id, map[7L])
        assertEquals(2, w.wallets.size)
        assertEquals(map, w.mergeWallets(imported))
        assertEquals(2, w.wallets.size)
    }

    @Test fun csvIncludesWalletColumnWhenWalletsGiven() {
        val w = WalletBook()
        val csv = CsvExport.export(listOf(Expense(1, "a", 1.0, "Food", 0L)), java.time.ZoneOffset.UTC, w.wallets)
        assertEquals("date,title,category,amount,note,wallet", csv.lines()[0])
        assertEquals(",a,Food,1.0,,Cash", csv.lines()[1])
    }
}
