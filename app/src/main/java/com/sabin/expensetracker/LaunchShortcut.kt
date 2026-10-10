package com.sabin.expensetracker

/** The add form opens on a launcher-shortcut/widget launch or after choosing it in onboarding. */
fun shouldOpenForm(extraOpenForm: Boolean, afterOnboarding: Boolean): Boolean = extraOpenForm || afterOnboarding
