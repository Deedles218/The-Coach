# Onboarding paywall test report — 2026-08-30

## Scope and environment

- Android: The Coach `1.40.8`, Android 16 / API 36 Google Play emulator,
  approved license-tester account.
- iOS: TestFlight `1.13.29 (3398)`, iPhone 11, iOS `18.0.1`.
- Journey: fresh anonymous user, first `START NOW`, goal `Boost overall
  health`, runtime questionnaire, onboarding paywall.

## Execution result

| Case | Android | iOS |
| --- | --- | --- |
| Top visual plan purchase | Test purchase sheet and test marker confirmed; Subscribe was tapped. Blocked by Google Play: billing is paused for the account/device region. | Top plan and TestFlight test-only sheet reached. Completion is blocked by Apple Account authentication in system UI that Appium cannot access reliably. |
| Bottom visual plan purchase | Test purchase sheet and test marker confirmed; Subscribe was tapped. Blocked by the same Google Play regional restriction. | Not completed: the TestFlight system confirmation requires manual authentication, and a later Restore made the device entitled. |
| Terms & Privacy | PASS after adding a ClickableSpan-relative tap and a destination check outside the paywall. | Not independently completed. Restore activated the device entitlement, so a subsequent fresh profile bypassed the paywall and opened Today. |
| Exact renewal disclosure | FAIL. Actual Android copy is price/trial-specific and does not equal the required text. | PASS. Normalized text exactly equals the requirement. |
| Restore | FAIL. No visible/enabled Restore control is exposed on the loaded Android paywall. | PASS. Restore was visible/enabled and closed the paywall to a visible Today tab. |

Evidence is stored under `target/onboarding-paywall-evidence/runs`. A PASS is
reported only when a destination or postcondition is observed; purchase-sheet
initiation alone is not counted as a completed purchase.

## Release assessment

Android `1.40.8` is **No-Go against the supplied paywall acceptance criteria**:
the required disclosure is different and Restore is absent. Purchase
completion is additionally unverified because Google Play blocks billing in
the current region.

iOS purchase readiness is **insufficient evidence**. Disclosure and Restore
are confirmed, but unattended top/bottom purchase completion needs a
development-signed StoreKit Test build or manual TestFlight confirmation. The
active restored entitlement must be cleared before another paywall-only case.

## Recommended additional paywall checks

### P0 — purchase correctness

1. Assert the selected product identifier, plan period, localized price,
   introductory trial and charged amount before confirmation.
2. Verify the transaction/receipt or a backend entitlement for the selected
   SKU. Reaching Today alone is not proof that the intended plan was bought.
3. Verify idempotency: double tap, delayed callback and app relaunch must not
   produce duplicate transactions or conflicting UI state.
4. Cover user cancellation, payment failure, pending/interrupted purchase,
   offline mode and product-loading failure with retry.

### P1 — subscription lifecycle and restore

1. Restore with no history, active subscription, expired subscription,
   refunded/revoked subscription and a network/server error.
2. Verify entitlement survives relaunch and account reconnect, and disappears
   after expiry/revocation according to product rules.
3. Verify top and bottom plans use distinct expected product identifiers and
   that visual selection cannot purchase the previously selected SKU.
4. Exercise StoreKit/Play renewal, grace period, billing retry and cancellation
   events, including backend notifications.

### P1 — legal and commercial content

1. Assert Privacy Policy and Terms of Service separately, including exact HTTPS
   host/path and successful page load; also verify back/close navigation.
2. Validate price, currency, billing period, trial eligibility, post-trial
   charge, auto-renewal and cancellation copy as one internally consistent
   contract.
3. Cover a returning user who is ineligible for the introductory trial.

### P2 — UX, accessibility and observability

1. Check loading skeleton, disabled CTA before products load, retry state,
   orientation/safe areas and small/large text sizes.
2. Require stable accessibility IDs for both plan cards, selected state,
   Privacy, Terms, Restore and purchase CTA; verify labels and tap targets.
3. Add localization/storefront coverage for at least the primary markets and
   longest translations.
4. Assert analytics for paywall impression, plan selection, CTA tap, store
   sheet opened, purchase result, restore result and legal-link clicks without
   logging credentials or full receipts.

## iOS automation without repeated passwords

For unattended regression, use a development-signed QA build with a
`.storekit` configuration and `SKTestSession.disableDialogs = true`. Clear
transactions before each case and drive StoreKit success/error/lifecycle
states programmatically. Appium remains responsible for the app-owned UI
journey; StoreKit Test/XCTest owns transaction state.

TestFlight should remain a small real-integration smoke suite. Use a dedicated
Sandbox Apple Account and reset its purchase history between scenarios, but do
not put Apple credentials in Appium, source control or CI logs.
