package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.SpendingClass

/**
 * Resolves the classification used by analysis without rewriting the expense snapshot.
 * The stored value remains the fallback for records whose category is unavailable.
 */
fun Expense.effectiveSpendingClass(
    categorySpendingClassesById: Map<DocumentId, SpendingClass>
): SpendingClass = categorySpendingClassesById[categoryId] ?: spendingClass

fun Expense.withEffectiveSpendingClass(
    categorySpendingClassesById: Map<DocumentId, SpendingClass>
): Expense {
    val effectiveClass = effectiveSpendingClass(categorySpendingClassesById)
    return if (effectiveClass == spendingClass) this else copy(spendingClass = effectiveClass)
}

fun Iterable<Category>.spendingClassesById(): Map<DocumentId, SpendingClass> =
    associate { category -> category.id to category.defaultSpendingClass }
