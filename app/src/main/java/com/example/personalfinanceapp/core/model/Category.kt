package com.example.personalfinanceapp.core.model

enum class SpendingClass {
    COMMITTED,
    DISCRETIONARY
}

data class Category(
    val id: DocumentId,
    val name: String,
    val defaultSpendingClass: SpendingClass,
    val archived: Boolean = false
)