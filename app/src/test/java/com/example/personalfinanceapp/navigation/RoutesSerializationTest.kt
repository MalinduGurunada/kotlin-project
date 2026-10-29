package com.example.personalfinanceapp.navigation

import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesSerializationTest {
    @Test
    fun everyTypedDestinationHasAGeneratedSerializer() {
        val routeSerializers = listOf(
            serializer<Login>(),
            serializer<Register>(),
            serializer<PasswordReset>(),
            serializer<Home>(),
            serializer<ExpenseList>(),
            serializer<IncomeList>(),
            serializer<ReceivableList>(),
            serializer<Overview>(),
            serializer<ActivityRoute>(),
            serializer<Goals>(),
            serializer<TransactionDetail>()
        )

        assertEquals(11, routeSerializers.size)
    }
}
