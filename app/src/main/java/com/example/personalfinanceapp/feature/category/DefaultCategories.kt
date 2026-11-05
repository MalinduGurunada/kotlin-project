package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.SpendingClass

/**
 * Appendix E categories that every account starts with.
 *
 * Stable document IDs make seeding idempotent even when a category is renamed,
 * reclassified or archived later.
 */
internal val APPENDIX_E_DEFAULT_CATEGORIES = listOf(
    Category("default_rent_housing", "Rent & Housing", SpendingClass.COMMITTED),
    Category("default_utilities", "Utilities", SpendingClass.COMMITTED),
    Category("default_subscriptions", "Subscriptions", SpendingClass.COMMITTED),
    Category("default_gym", "Gym", SpendingClass.COMMITTED),
    Category("default_groceries", "Groceries", SpendingClass.COMMITTED),
    Category("default_transport_commute", "Transport (commute)", SpendingClass.COMMITTED),
    Category("default_rides", "Rides (PickMe, Uber)", SpendingClass.DISCRETIONARY),
    Category("default_food_delivery", "Food Delivery (UberEats)", SpendingClass.DISCRETIONARY),
    Category("default_coffee_dining", "Coffee & Dining Out", SpendingClass.DISCRETIONARY),
    Category("default_shopping_gadgets", "Shopping & Gadgets", SpendingClass.DISCRETIONARY),
    Category("default_entertainment", "Entertainment", SpendingClass.DISCRETIONARY),
    Category("default_other", "Other / Uncategorised", SpendingClass.DISCRETIONARY)
)

internal fun missingDefaultCategories(
    existingIds: Set<DocumentId>,
    existingNames: Set<String>
): List<Category> {
    val normalizedNames = existingNames.mapTo(mutableSetOf(), ::normalizeCategoryName)

    return APPENDIX_E_DEFAULT_CATEGORIES.filter { defaultCategory ->
        defaultCategory.id !in existingIds &&
            normalizeCategoryName(defaultCategory.name) !in normalizedNames
    }
}

internal fun normalizeCategoryName(name: String): String = name.trim().lowercase()
