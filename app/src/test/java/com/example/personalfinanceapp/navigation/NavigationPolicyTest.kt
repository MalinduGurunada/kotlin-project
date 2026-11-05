package com.example.personalfinanceapp.navigation

import com.example.personalfinanceapp.feature.auth.ui.AuthNavigationState
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun authenticatedAccountReplacementRequiresSessionUiReset() {
        assertFalse(authenticatedAccountChanged(null, "user-a"))
        assertFalse(authenticatedAccountChanged("user-a", "user-a"))
        assertTrue(authenticatedAccountChanged("user-a", "user-b"))
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
    fun unverifiedEmailTargetsVerificationInsteadOfFinancialFeatures() {
        assertEquals(
            AppNavigationTarget.EMAIL_VERIFICATION,
            startupTarget(AuthNavigationState.EmailVerificationRequired("user-1"))
        )
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
        assertEquals(
            AppNavigationTarget.RECURRING,
            managementTarget(ManagementDestination.RECURRING)
        )
        assertEquals(
            AppNavigationTarget.DUE_ITEMS,
            managementTarget(ManagementDestination.DUE_ITEMS)
        )
    }

    @Test
    fun transactionDetailRoutePreservesTransactionId() {
        assertEquals("transaction-42", TransactionDetail("transaction-42").transactionId)
    }

    @Test
    fun goalDetailRoutePreservesGoalId() {
        assertEquals("goal-m3", GoalDetail("goal-m3").goalId)
    }
}
