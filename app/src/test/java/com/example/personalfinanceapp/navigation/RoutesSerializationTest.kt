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
            serializer<Category>(),
            serializer<ExpenseList>(),
            serializer<IncomeList>(),
            serializer<ReceivableList>(),
            serializer<RecurringCommitments>(),
            serializer<DueItems>(),
            serializer<FinancialSummaries>(),
            serializer<Overview>(),
            serializer<ActivityRoute>(),
            serializer<Goals>(),
            serializer<GoalDetail>(),
            serializer<TransactionDetail>()
        )

        assertEquals(16, routeSerializers.size)
    }

    @Test
    fun goalDetailRoutePreservesGoalId() {
        val route = GoalDetail(goalId = "macbook-m3")
        assertEquals("macbook-m3", route.goalId)
    }
}
