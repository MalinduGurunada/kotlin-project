package com.example.personalfinanceapp.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Login

@Serializable
data object Register

@Serializable
data object PasswordReset

@Serializable
data object Home

@Serializable
data object Category

@Serializable
data object ExpenseList

@Serializable
data object IncomeList

@Serializable
data object ReceivableList

@Serializable
data object RecurringCommitments

@Serializable
data object DueItems

@Serializable
data object Overview

@Serializable
data object Activity

@Serializable
data object Goals

@Serializable
data class TransactionDetail(val transactionId: String)
