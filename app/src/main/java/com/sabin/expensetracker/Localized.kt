package com.sabin.expensetracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Display name for a stored category key (the key itself stays English in saved data). */
@Composable
fun categoryLabel(category: String): String = stringResource(
    when (category) {
        "Food" -> R.string.cat_food
        "Transport" -> R.string.cat_transport
        "Bills" -> R.string.cat_bills
        "Shopping" -> R.string.cat_shopping
        else -> R.string.cat_other
    }
)

@Composable
fun badgeTitle(b: Badge): String = stringResource(
    when (b) {
        Badge.FIRST_EXPENSE -> R.string.badge_first_title
        Badge.STREAK_7 -> R.string.badge_streak_title
        Badge.NO_SPEND_DAY -> R.string.badge_nospend_title
        Badge.BUDGET_KEPT_MONTH -> R.string.badge_budget_title
        Badge.FIRST_BACKUP -> R.string.badge_backup_title
    }
)

@Composable
fun badgeDescription(b: Badge): String = stringResource(
    when (b) {
        Badge.FIRST_EXPENSE -> R.string.badge_first_desc
        Badge.STREAK_7 -> R.string.badge_streak_desc
        Badge.NO_SPEND_DAY -> R.string.badge_nospend_desc
        Badge.BUDGET_KEPT_MONTH -> R.string.badge_budget_desc
        Badge.FIRST_BACKUP -> R.string.badge_backup_desc
    }
)

@Composable
fun onboardingTitle(page: Int): String = stringResource(
    listOf(R.string.ob1_title, R.string.ob2_title, R.string.ob3_title)[page]
)

@Composable
fun onboardingBody(page: Int): String = stringResource(
    listOf(R.string.ob1_body, R.string.ob2_body, R.string.ob3_body)[page]
)
