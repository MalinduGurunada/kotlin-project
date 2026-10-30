package com.example.personalfinanceapp.feature.category.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.Category

/**
 * Local monochrome icons used by the Category UI.
 *
 * The project does not include a Compose Material icons artifact, so these small vectors keep the
 * screen dependency-free while retaining the same visual language as Material outline icons.
 */
internal object CategoryIcons {
    val Add = materialIcon("Add") {
        moveTo(19f, 13f); lineTo(13f, 13f); lineTo(13f, 19f); lineTo(11f, 19f)
        lineTo(11f, 13f); lineTo(5f, 13f); lineTo(5f, 11f); lineTo(11f, 11f)
        lineTo(11f, 5f); lineTo(13f, 5f); lineTo(13f, 11f); lineTo(19f, 11f); close()
    }

    val ArrowBack = materialIcon("ArrowBack") {
        moveTo(20f, 11f); lineTo(7.83f, 11f); lineTo(13.42f, 5.41f); lineTo(12f, 4f)
        lineTo(4f, 12f); lineTo(12f, 20f); lineTo(13.42f, 18.59f); lineTo(7.83f, 13f)
        lineTo(20f, 13f); close()
    }

    val Edit = materialIcon("Edit") {
        moveTo(3f, 17.25f); lineTo(3f, 21f); lineTo(6.75f, 21f); lineTo(17.81f, 9.94f)
        lineTo(14.06f, 6.19f); close()
        moveTo(20.71f, 7.04f)
        curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
        lineTo(18.37f, 3.29f)
        curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
        lineTo(15.13f, 5.12f); lineTo(18.88f, 8.87f); close()
    }

    val Archive = materialIcon("Archive") {
        moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 7f); lineTo(4f, 7f); close()
        moveTo(5f, 8.5f); lineTo(19f, 8.5f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(11f, 10f); lineTo(13f, 10f); lineTo(13f, 13f); lineTo(15.5f, 13f)
        lineTo(12f, 16.5f); lineTo(8.5f, 13f); lineTo(11f, 13f); close()
    }

    val Restore = materialIcon("Restore") {
        moveTo(13f, 3f); curveTo(8.58f, 3f, 5f, 6.58f, 5f, 11f)
        lineTo(2f, 11f); lineTo(6f, 15f); lineTo(10f, 11f); lineTo(7f, 11f)
        curveTo(7f, 7.69f, 9.69f, 5f, 13f, 5f)
        curveTo(16.31f, 5f, 19f, 7.69f, 19f, 11f)
        curveTo(19f, 14.31f, 16.31f, 17f, 13f, 17f)
        lineTo(13f, 19f); curveTo(17.42f, 19f, 21f, 15.42f, 21f, 11f)
        curveTo(21f, 6.58f, 17.42f, 3f, 13f, 3f); close()
    }

    val ExpandMore = materialIcon("ExpandMore") {
        moveTo(7.41f, 8.59f); lineTo(12f, 13.17f); lineTo(16.59f, 8.59f)
        lineTo(18f, 10f); lineTo(12f, 16f); lineTo(6f, 10f); close()
    }

    val ExpandLess = materialIcon("ExpandLess") {
        moveTo(7.41f, 15.41f); lineTo(12f, 10.83f); lineTo(16.59f, 15.41f)
        lineTo(18f, 14f); lineTo(12f, 8f); lineTo(6f, 14f); close()
    }

    val ShoppingCart = materialIcon("ShoppingCart") {
        moveTo(7f, 18f); curveTo(5.9f, 18f, 5.01f, 18.9f, 5.01f, 20f)
        curveTo(5.01f, 21.1f, 5.9f, 22f, 7f, 22f)
        curveTo(8.1f, 22f, 9f, 21.1f, 9f, 20f)
        curveTo(9f, 18.9f, 8.1f, 18f, 7f, 18f)
        moveTo(1f, 2f); lineTo(1f, 4f); lineTo(3f, 4f); lineTo(6.6f, 11.59f)
        lineTo(5.24f, 14.04f); curveTo(5.09f, 14.32f, 5f, 14.65f, 5f, 15f)
        curveTo(5f, 16.1f, 5.9f, 17f, 7f, 17f); lineTo(19f, 17f); lineTo(19f, 15f)
        lineTo(7.42f, 15f); curveTo(7.28f, 15f, 7.17f, 14.89f, 7.17f, 14.75f)
        lineTo(8.1f, 13f); lineTo(15.55f, 13f)
        curveTo(16.3f, 13f, 16.96f, 12.58f, 17.3f, 11.97f)
        lineTo(20.88f, 5.5f); curveTo(20.95f, 5.34f, 21f, 5.17f, 21f, 5f)
        curveTo(21f, 4.45f, 20.55f, 4f, 20f, 4f); lineTo(5.21f, 4f); lineTo(4.27f, 2f); close()
        moveTo(17f, 18f); curveTo(15.9f, 18f, 15.01f, 18.9f, 15.01f, 20f)
        curveTo(15.01f, 21.1f, 15.9f, 22f, 17f, 22f)
        curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
        curveTo(19f, 18.9f, 18.1f, 18f, 17f, 18f)
    }

    val Bolt = materialIcon("Bolt") {
        moveTo(11f, 21f); lineTo(11f, 13f); lineTo(6f, 13f); lineTo(13f, 2f)
        lineTo(13f, 10f); lineTo(18f, 10f); close()
    }

    val Car = materialIcon("Car") {
        moveTo(5f, 11f); lineTo(6.5f, 6.5f); curveTo(6.78f, 5.67f, 7.56f, 5f, 8.5f, 5f)
        lineTo(15.5f, 5f); curveTo(16.44f, 5f, 17.22f, 5.67f, 17.5f, 6.5f)
        lineTo(19f, 11f); curveTo(20.1f, 11f, 21f, 11.9f, 21f, 13f)
        lineTo(21f, 18f); lineTo(19f, 18f); lineTo(19f, 20f); lineTo(17f, 20f)
        lineTo(17f, 18f); lineTo(7f, 18f); lineTo(7f, 20f); lineTo(5f, 20f)
        lineTo(5f, 18f); lineTo(3f, 18f); lineTo(3f, 13f)
        curveTo(3f, 11.9f, 3.9f, 11f, 5f, 11f)
        moveTo(7.2f, 11f); lineTo(16.8f, 11f); lineTo(15.5f, 7f); lineTo(8.5f, 7f); close()
        moveTo(6.5f, 13f); curveTo(5.67f, 13f, 5f, 13.67f, 5f, 14.5f)
        curveTo(5f, 15.33f, 5.67f, 16f, 6.5f, 16f)
        curveTo(7.33f, 16f, 8f, 15.33f, 8f, 14.5f)
        curveTo(8f, 13.67f, 7.33f, 13f, 6.5f, 13f)
        moveTo(17.5f, 13f); curveTo(16.67f, 13f, 16f, 13.67f, 16f, 14.5f)
        curveTo(16f, 15.33f, 16.67f, 16f, 17.5f, 16f)
        curveTo(18.33f, 16f, 19f, 15.33f, 19f, 14.5f)
        curveTo(19f, 13.67f, 18.33f, 13f, 17.5f, 13f)
    }

    val Laptop = materialIcon("Laptop") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 16f); lineTo(4f, 16f); close()
        moveTo(2f, 18f); lineTo(22f, 18f); lineTo(22f, 20f); lineTo(2f, 20f); close()
    }

    val Coffee = materialIcon("Coffee") {
        moveTo(4f, 7f); lineTo(17f, 7f); lineTo(17f, 9f); lineTo(19f, 9f)
        curveTo(20.66f, 9f, 22f, 10.34f, 22f, 12f)
        curveTo(22f, 13.66f, 20.66f, 15f, 19f, 15f); lineTo(17f, 15f)
        curveTo(17f, 17.76f, 14.76f, 20f, 12f, 20f); lineTo(9f, 20f)
        curveTo(6.24f, 20f, 4f, 17.76f, 4f, 15f); close()
        moveTo(17f, 11f); lineTo(17f, 13f); lineTo(19f, 13f)
        curveTo(19.55f, 13f, 20f, 12.55f, 20f, 12f)
        curveTo(20f, 11.45f, 19.55f, 11f, 19f, 11f); close()
    }

    val Flight = materialIcon("Flight") {
        moveTo(21f, 16f); lineTo(13f, 13.5f); lineTo(13f, 19f); lineTo(15f, 21f)
        lineTo(9f, 21f); lineTo(7f, 13.5f); lineTo(3f, 12f); lineTo(3f, 9f)
        lineTo(9f, 11f); lineTo(9f, 4f); curveTo(9f, 2.9f, 9.9f, 2f, 11f, 2f)
        curveTo(12.1f, 2f, 13f, 2.9f, 13f, 4f); lineTo(13f, 11f); lineTo(21f, 8f); close()
    }

    val Fitness = materialIcon("Fitness") {
        moveTo(2f, 9f); lineTo(4f, 9f); lineTo(4f, 7f); lineTo(6f, 7f)
        lineTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 7f); lineTo(20f, 7f)
        lineTo(20f, 9f); lineTo(22f, 9f); lineTo(22f, 15f); lineTo(20f, 15f)
        lineTo(20f, 17f); lineTo(18f, 17f); lineTo(18f, 13f); lineTo(6f, 13f)
        lineTo(6f, 17f); lineTo(4f, 17f); lineTo(4f, 15f); lineTo(2f, 15f); close()
    }

    val Folder = materialIcon("Folder") {
        moveTo(3f, 5f); lineTo(10f, 5f); lineTo(12f, 7f); lineTo(21f, 7f)
        lineTo(21f, 19f); lineTo(3f, 19f); close()
    }
}

internal fun categoryIconFor(category: Category): ImageVector {
    val name = category.name.lowercase()
    return when {
        name.containsAny("grocery", "food", "market", "supermarket") -> CategoryIcons.ShoppingCart
        name.containsAny("utilit", "bill", "electric", "water", "power") -> CategoryIcons.Bolt
        name.containsAny("transport", "fuel", "ride", "car", "bus", "train") -> CategoryIcons.Car
        name.containsAny("electronic", "software", "gadget", "tech", "computer") -> CategoryIcons.Laptop
        name.containsAny("dining", "coffee", "restaurant", "takeout", "lunch", "dinner") -> CategoryIcons.Coffee
        name.containsAny("travel", "flight", "hotel", "trip") -> CategoryIcons.Flight
        name.containsAny("gym", "fitness", "sport", "workout") -> CategoryIcons.Fitness
        else -> CategoryIcons.Folder
    }
}

private fun String.containsAny(vararg keywords: String): Boolean = keywords.any(::contains)

private fun materialIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()
