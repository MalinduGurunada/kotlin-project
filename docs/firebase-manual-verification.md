# M04/M07 manual verification

Use a Google Play-enabled emulator or physical Android device. Do not use `force-stop` for
background FCM checks; swipe the app away for a terminated-state check instead.

## PBD-38: Google authentication

1. In Firebase Console, open **Build > Authentication > Sign-in method** and confirm
   **Google** is enabled.
2. In **Project settings > Your apps > Android app**, confirm the package is
   `com.example.personalfinanceapp` and that the debug SHA-1 and SHA-256 fingerprints are
   registered. Keep the repository's local `app/google-services.json`; do not paste its
   contents into reports.
3. Install and open the debug app, tap **Continue with Google**, choose account A, and
   verify Overview opens. In **Authentication > Users**, record the UID as evidence.
4. Sign out. Start Google sign-in again, close the account chooser, and verify the login
   screen remains usable with no authenticated session.
5. Disable device networking, retry Google sign-in, and verify a recoverable error is
   shown. Restore networking and verify a later attempt succeeds.
6. Conflict check: register/sign in with email-password using a Google-address email,
   sign out, then try Google with that same address. Depending on the Firebase
   one-account-per-email setting, verify either the existing identity is reused or a
   controlled error is shown; verify that no duplicate authenticated session is created.
7. Sign out account A, choose account B in the Google chooser, and verify the displayed
   data is scoped to B. Sign out B and sign in as A again to confirm switching remains
   possible.

## PBD-49/PBD-50/PBD-51: FCM and notification navigation

1. Sign in and grant the Android notification permission when prompted. On the **More /
   Preferences** screen, leave **Notifications** enabled.
2. In Firebase Console, open **Build > Firestore Database > Data** and locate
   `users/{uid}/fcmRegistrations/{registrationHash}`. Copy the `installationId` field.
   Its presence is evidence of registration only, not delivery.
3. Open **DevOps & Engagement > Messaging**. Choose **Create your first campaign >
   Firebase Notification messages > Create**, or **New campaign > Notifications**.
4. Set title `FinClarity PBD-51` and body `Foreground notification test`. In **Advanced
   options > Custom data**, add exactly:

   | Key | Value |
   | --- | --- |
   | `type` | `finance_review` |
   | `destination` | `activity` |
   | `userId` | the signed-in Firebase UID |
   | `reviewId` | `pbd51-foreground-001` |
   | `title` | `FinClarity PBD-51` |
   | `body` | `Foreground notification test` |

5. Keep FinClarity visible. Choose **Send test message**, paste the installation ID, and
   select **Test**. Record a screenshot and time only if the notification actually appears.
6. Change `reviewId` to `pbd51-background-001` and body to `Background notification
   test`. Put the app in the background with Home, send another test, tap the notification,
   and verify FinClarity opens the Activity screen.
7. Swipe FinClarity away (do not force-stop it), change `reviewId` to
   `pbd51-terminated-001`, send again, tap it, and verify the Activity screen opens after
   authentication resolves.
8. Cross-account safety: sign in as account B, send a payload whose `userId` is account
   A's UID, and verify the app does not navigate to A's notification destination.
9. Preference lifecycle: turn **Notifications** off and verify A's Firestore registration
   document is removed. A test to the old ID must not be counted as successful without a
   device notification. Turn notifications on, grant permission if asked, and verify the
   registration document is recreated before sending again.
10. Account lifecycle: sign out and verify the old user's registration document is
    removed. Sign in as another account and verify registration exists only under that UID.
11. On Android 13+, repeat once with permission denied. Verify no app notification is
    displayed; enabling notifications from More should request permission again when the
    OS allows another prompt.

For each delivery check, retain the device state, UID, `reviewId`, send time, screenshot,
and observed destination. Firebase Console acceptance alone is not delivery evidence.
