package com.example.personalfinanceapp.feature.category.ui

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.SpendingClass
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryIconsTest {

    @Test
    fun categoryIconFor_mapsKeywordsToExpectedIcons() {
        val mappings = listOf(
            "Grocery Store" to "ShoppingCart",
            "Fast Food" to "ShoppingCart",
            "Electric Bill" to "Bolt",
            "Public Transport" to "Car",
            "Electronics" to "Laptop",
            "Coffee Shop" to "Coffee",
            "Flight Ticket" to "Flight",
            "Gym Membership" to "Fitness",
            "Random Category" to "Folder"
        )

        mappings.forEachIndexed { index, (categoryName, expectedIconName) ->
            val category = Category(
                id = "category-$index",
                name = categoryName,
                defaultSpendingClass = SpendingClass.DISCRETIONARY
            )

            assertEquals(expectedIconName, categoryIconFor(category).name)
        }
    }
}
