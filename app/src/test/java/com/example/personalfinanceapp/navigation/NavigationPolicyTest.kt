package com.example.personalfinanceapp.navigation

import com.example.personalfinanceapp.feature.auth.ui.AuthNavigationState
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationPolicyTest {

    @Test
    fun authenticatedStartupTargetsOverview() {
        assertEquals(
            AppNavigationTarget.OVERVIEW,
            startupTarget(AuthNavigationState.Authenticated("user-1"))
        )
    }

    @Test
    fun unauthenticatedStartupTargetsLogin() {
        assertEquals(
            AppNavigationTarget.LOGIN,
            startupTarget(AuthNavigationState.Unauthenticated)
        )
    }

    @Test
    fun incompleteRegistrationTargetsSetupAndResolvingDoesNotNavigate() {
        assertEquals(
            AppNavigationTarget.REGISTRATION_SETUP,
            startupTarget(AuthNavigationState.RegistrationSetup)
        )
        assertNull(startupTarget(AuthNavigationState.Resolving))
    }

    @Test
    fun bottomNavigationTargetsEveryDashboardDestination() {
        assertEquals(AppNavigationTarget.OVERVIEW, dashboardTarget(NavDestination.OVERVIEW))
        assertEquals(AppNavigationTarget.ACTIVITY, dashboardTarget(NavDestination.ACTIVITY))
        assertEquals(AppNavigationTarget.GOALS, dashboardTarget(NavDestination.GOALS))
        assertEquals(AppNavigationTarget.MORE, dashboardTarget(NavDestination.MORE))
    }

    @Test
    fun managementNavigationKeepsEveryFeatureAccessible() {
        assertEquals(
            AppNavigationTarget.EXPENSES,
            managementTarget(ManagementDestination.EXPENSES)
        )
        assertEquals(
            AppNavigationTarget.CATEGORIES,
            managementTarget(ManagementDestination.CATEGORIES)
        )
        assertEquals(
            AppNavigationTarget.INCOME,
            managementTarget(ManagementDestination.INCOME)
        )
        assertEquals(
            AppNavigationTarget.RECEIVABLES,
            managementTarget(ManagementDestination.RECEIVABLES)
        )
    }

    @Test
    fun transactionDetailRoutePreservesTransactionId() {
        assertEquals("transaction-42", TransactionDetail("transaction-42").transactionId)
    }
}
