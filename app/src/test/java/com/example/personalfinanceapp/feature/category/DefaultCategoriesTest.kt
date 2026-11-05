package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.SpendingClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {

    @Test
    fun noExistingCategories_returnsAllAppendixEDefaults() {
        val missing = missingDefaultCategories(emptySet(), emptySet())

        assertEquals(12, missing.size)
        assertEquals(12, missing.map { it.id }.toSet().size)
        assertEquals(
            listOf(
                "Rent & Housing" to SpendingClass.COMMITTED,
                "Utilities" to SpendingClass.COMMITTED,
                "Subscriptions" to SpendingClass.COMMITTED,
                "Gym" to SpendingClass.COMMITTED,
                "Groceries" to SpendingClass.COMMITTED,
                "Transport (commute)" to SpendingClass.COMMITTED,
                "Rides (PickMe, Uber)" to SpendingClass.DISCRETIONARY,
                "Food Delivery (UberEats)" to SpendingClass.DISCRETIONARY,
                "Coffee & Dining Out" to SpendingClass.DISCRETIONARY,
                "Shopping & Gadgets" to SpendingClass.DISCRETIONARY,
                "Entertainment" to SpendingClass.DISCRETIONARY,
                "Other / Uncategorised" to SpendingClass.DISCRETIONARY
            ),
            missing.map { it.name to it.defaultSpendingClass }
        )
    }

    @Test
    fun allDefaultsAlreadyExist_returnsNothing() {
        val missing = missingDefaultCategories(
            existingIds = APPENDIX_E_DEFAULT_CATEGORIES.mapTo(mutableSetOf()) { it.id },
            existingNames = APPENDIX_E_DEFAULT_CATEGORIES.mapTo(mutableSetOf()) { it.name }
        )

        assertTrue(missing.isEmpty())
    }

    @Test
    fun renamedOrArchivedDefault_isNotRecreatedWhenItsStableIdExists() {
        val missing = missingDefaultCategories(
            existingIds = setOf("default_groceries"),
            existingNames = setOf("Weekly food")
        )

        assertTrue(missing.none { it.id == "default_groceries" })
        assertEquals(11, missing.size)
    }

    @Test
    fun matchingCustomName_isNotDuplicatedIgnoringWhitespaceAndCase() {
        val missing = missingDefaultCategories(
            existingIds = setOf("custom-utilities"),
            existingNames = setOf("  uTiLiTiEs  ")
        )

        assertTrue(missing.none { it.name == "Utilities" })
        assertEquals(11, missing.size)
    }
}
