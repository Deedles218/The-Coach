# COA-9324: iOS partner-access E2E

Requirements: [COA-9324](https://the-coach.atlassian.net/browse/COA-9324),
[COA-9265](https://the-coach.atlassian.net/browse/COA-9265).
The current production offer setting was supplied by the feature owner:
every fifth opening for a new user on the new build, three displays total.
It is an assertion, not a configurable expectation that follows the app's counters.
The owner also clarified that one invitation has one stable URL and grants access
to only one unique recipient. Share hides the reminder for the current opening;
the first cold reopening must show it again until recipient activation.

| Method in `tests.PartnerAccessTests` | Assertions |
| --- | --- |
| `testOfferEveryFifthOpeningStopsAfterThreeDisplays` | Fresh buyer journey; absence on every other opening 1–21; offers exactly on 5, 10, 15; X dismisses without immediate redisplay; no fourth offer on 20 or 21. Actual terminate/activate calls preserve the app's counters. |
| `testCancelledPurchaseDoesNotExposeInvite` / COA-9687 | Fifth-opening offer → exact partner paywall → exact one-time product in native Sandbox sheet; two cancellations return to the same paywall; no invitation on Today or after restart. |
| `testPurchaseShareAndActivatePartnerAccess` / COA-9685, COA-9688 behavior | Unpaid R1 must hit a loaded paywall before purchase. Fifth-opening offer → Sandbox purchase → two native shares yield the same single HTTPS URL. After each share the reminder hides; the first cold reopening shows it again. The real link cold-starts production For Her; fresh membership reads must confirm Premium for the same UID, and R1's locked lesson must open before/after restart. Reopening the same link preserves R1 access. The buyer's reminder stays absent after activation. UI logout creates a distinct unpaid R2; the already used link is checked on both cold and warm opening. R2's lesson stays locked and fresh `/user/` reads remain unpaid throughout the delivery window. |

These are UI E2E checks with read-only, UID-verified membership evidence for R1/R2.
They do not assert backend `license_id`, transaction
uniqueness, `recipient_user_id`, lifetime metadata, invite email delivery,
analytics, refund/revoke, total database license-row count, or deferred
installation. A visible Today tab or Premium badge alone never proves activation.

## TestFlight prerequisites and isolation

- A dedicated iPhone with the matching production TestFlight apps. Installed on
  6 November 2026: For Him `1.14.0 (3582)`, For Her `2.1.0 (3583)`;
  bundle IDs `com.vamapps.The-Coach`, `com.vamapps.The-Coach-for-her`.
- Run **one method at a time**, sequentially. Reinstall For Him from TestFlight
  before **each method**, with no previous app openings or partner-offer counts.
  Appium's first launch is opening 1. Logout does not prove zero local counters.
  `partner.freshInstall=true` is an operator confirmation of this preparation,
  not independent evidence of an app-data reset.
- For COA-9685 prepare a dedicated unpaid For Her profile already on Today.
  Verify the unpaid baseline before buying anything; reinstall alone does not
  prove it. Both recipients are test-owned anonymous profiles. After confirming
  R1's persistent access, the case logs out R1 and creates R2 through the native
  Start / Goals flow and the observed English For Her 2.1.0 WebView questionnaire.
  The case finishes on R2; anonymous R1 has no reusable email login. Use this
  workflow only for these dedicated profiles. Server accounts are retained.
  A subscription-loading error does not satisfy the unpaid baseline.
  Choose a text lesson accessible from this account's current Today that is
  actually paywalled. A free introductory lesson must fail this precondition.
- Use the TestFlight/Sandbox Apple account with **no previous partner-product
  purchase**. Clear the Sandbox purchase history or use another Sandbox account between successful
  purchases. An “already purchased” result is a failure, not a new purchase.
  Apple supports repeating tests after clearing a Sandbox tester's history and
  signing out/in to clear the device cache. A new tester is another option.
  Plus-addresses such as `tester+1@example.com` must first be registered as
  separate Sandbox testers in App Store Connect, and the mail provider must
  support subaddressing. Credentials belong in the lab's secret store.
  See [Sandbox purchase history](https://developer.apple.com/documentation/storekit/testing-in-app-purchases-with-sandbox)
  and [creating Sandbox testers](https://developer.apple.com/help/app-store-connect/test-in-app-purchases/create-a-sandbox-apple-account/).
- Keep the remote offer configuration at 5/3 and the production partner product
  `male_partner_lifetime_34.99`. Do not modify Firebase from the test.
- Do not enable automatic alert acceptance/dismissal. Physical StoreKit sheets
  can require an Apple Account password or manual side-button/biometric
  confirmation. The observed native Sign In sheet is supported when
  `PARTNER_STORE_ACCOUNT` and `PARTNER_STORE_PASSWORD` are provided through the
  child process environment. The account must match the actual Apple prompt;
  mismatches and missing credentials fail before any password is entered.
  Credentials are never accepted as Java properties or Allure step arguments.
  Biometric/side-button requirements still need device support; initiating a
  transaction is not a pass.
- The tests preserve the final test-owned buyer/R2 state for diagnosis and close
  the Appium session. They do not delete server accounts or revoke access.
  A complete class/suite run without fresh preparation between methods is invalid.

## Commands

Offline checks:

```sh
mvn -q -DskipTests test-compile
mvn -q test -Dtest=tests.PartnerAccessContractUnitTests,tests.IOSPartnerPromoLocatorUnitTests
PYTHONPATH=scripts python3 -m unittest test_read_partner_recipient test_update_account
```

For the additional existing Daily Lesson / Module locator checks on this JDK:

```sh
mvn -q test \
  -Dtest=tests.PartnerAccessContractUnitTests,tests.IOSPartnerPromoLocatorUnitTests,tests.IOSDailyLessonLocatorUnitTests,tests.IOSModuleLessonLocatorUnitTests \
  -Djdk.xml.xpathExprOpLimit=200
```

JDK 8u512's default XPath expression limit rejects two pre-existing Module
locator expressions (101 operators, limit 100). The same two failures were
reproduced with the unchanged `iOSDailyPlanPageObject` from HEAD in a separate
temporary class directory. A limit of 200 lets the affected local DOM checks
execute; no existing tests or project-wide XML settings were changed.
See [Oracle's JAXP limits](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jaxp/jaxp.html).

Cadence check on the connected iPhone 11:

```sh
mvn test \
  -Dtest=tests.PartnerAccessTests#testOfferEveryFifthOpeningStopsAfterThreeDisplays \
  -Dplatform=ios -Dios.bundleId=com.vamapps.The-Coach \
  -Dios.udid=00008030-000929442E01802E \
  '-Dios.deviceName=iPhone Daria (2)' -Dios.platformVersion=18.0 \
  -Dios.noReset=true -Dios.fullReset=false -Dpartner.freshInstall=true \
  '-Donboarding.goal=Boost overall health'
```

Set `-Dappium.url=<server URL>` when using a dedicated server. The final purchase
and staged invitation checks used `http://127.0.0.1:4725/`; the earlier cadence
and cancellation runs used the project's default server.

For either payment method, use the same device/attach options and enable Sandbox:

```sh
-Dstorekit.sandbox=true
```

COA-9685 additionally needs:

```sh
-Dpurchase.allow=true \
'-Dpartner.recipientLesson=<previously locked lesson title>'
```

If Apple requests Sign In, inject `PARTNER_STORE_ACCOUNT` and
`PARTNER_STORE_PASSWORD` from the lab's secret store into Maven's child
environment. Do not put their values in source, command-line arguments or
reports. The feature suite does not change Apple password requirements.

If the protected lesson belongs to another available day, additionally set
`-Dpartner.recipientDay='<exact observed Day/Stage label>'`. The same day and
lesson are selected before purchase, after activation and after restart.

`src/test/resources/partner/ios-prod-3582.properties` freezes the paywall heading
and invitation title/action from the installed app's active Remote Config, plus the
For Her goal and the observed native `Partner's access`, `One-time charge`, and
TestFlight test-purchase notice. Override these
with `partner.paywallTitle`, `partner.inviteTitle`, `partner.inviteButton`, `partner.recipientGoal`,
or `partner.storeProduct` / `partner.storeOneTimeCharge` / `partner.storeTestNotice`
when the build or system language changes.
They are expected labels, not invented accessibility IDs. The observed
`GIFT HER ACCESS` action stays scoped to the shared partner promo locator.
Missing lesson inputs fail instead of selecting another subscription
or treating a free introductory lesson as evidence of activation.

On this iOS 18.0.1 TestFlight build, the native purchase sheet is absent from
the application's accessibility tree. The partner Page Object temporarily sets
`defaultActiveApplication=com.apple.springboard`, validates the partner product,
one-time charge and explicit test notice, and uses the observed `dismiss`/`close`
and `footer`/`Purchase` system controls. It restores the previous application
setting after cancellation/confirmation. Confirmation requires the explicit
`Your purchase was successful.` / `You’re all set` result and taps its native
`OK`; disappearance of the product sheet alone cannot prove completion.
Existing subscription helpers are unchanged.
See [XCUITest overlay settings](https://appium.github.io/appium-xcuitest-driver/latest/reference/settings/#defaultactiveapplication).

On physical iPhones, clipboard access requires WebDriverAgent in foreground;
the test uses a new marker before Copy to exclude a stale clipboard link.
After Copy, reminder absence is observed before any clipboard app switch.
The observed iOS 18 native Copy action is an accessibility cell.
Reading the clipboard leaves WDA in foreground; the test then performs the first
cold buyer reopening explicitly. This avoids an incidental foreground event
masking an incorrect reminder reappearance delay. If the lab uses another WDA
bundle ID, pass `-Dpartner.wdaBundleId=<installed WDA runner bundle ID>`.
See [XCUITest clipboard requirements](https://appium.github.io/appium-xcuitest-driver/latest/guides/clipboard/).

`iOSPartnerRecipientOnboardingPageObject` extends the existing native onboarding.
Its English WebView answers use observed question headers, choices and ratings.
The closeness question exposes none of its seven image choices to accessibility;
the handler uses the observed middle choice relative to WebView bounds on iPhone
11. This fallback needs revalidation for another screen layout; app-side
accessibility identifiers for those choices would remove the layout dependency.
Default onboarding in other suites keeps its existing behavior.

The recipient launch handler closes the observed subscription paywall and the
`Your Personal Guide Along the Way` introduction using its scoped close control.
The first invitation explicitly terminates For Her before asking iOS to resolve
the universal link, without forcing a target bundle. There is no automatic link
retry. The same UID must become Premium within the explicit membership wait;
closing startup windows alone never satisfies activation. R2's used-link check
then covers both a new app session and a warm opening, so a cold-start handling
problem cannot be the sole reason for a negative result.

`read_partner_recipient.py` copies current app identity/cache into a private
temporary directory, reuses the existing UID-bound auth-header reader, and issues
only a fresh `GET /api/v1/user/`. The result contains a UID hash and a strict
boolean membership state; tokens and raw UIDs are not exported. Missing auth,
unknown membership and driver/API errors fail the case, including the negative
delivery-window check. The bridge is tied to For Her 2.1.0 (3583) on the physical
iPhone and must be reviewed when updating the target build.

Use CoreTestCase's Surefire/Allure results, screenshot, page source and driver
diagnostics for failures. Invite links contain redeemable tokens; keep local
Appium/page-source/clipboard artifacts private. The deep-link call and URI
validation deliberately avoid embedding the invite in assertion messages.

## Device validation on 6 November 2026

| Check | Result | Local evidence |
| --- | --- | --- |
| Cadence and total cap | PASS: openings 1–21; displays on 5, 10, 15; no fourth display. 993.506 seconds. | `target/partner-access-evidence/cadence-pass/` |
| Cancel purchase | PASS: two native TestFlight cancellations, same paywall restored, no invite before/after restart. 488.469 seconds. | `target/partner-access-evidence/cancel-pass/` |
| Complete purchase E2E | **Not a complete PASS.** The real Sandbox purchase succeeded in attempt 3, but the JUnit run stopped at the unhandled native success notice. The success-acknowledgement and native Copy handlers were subsequently corrected and exercised on that same transaction. No new purchase was used to turn the staged checks into a complete JUnit pass. | `target/partner-access-evidence/purchase-attempt-3/`, `target/partner-access-evidence/invite-continuation-attempt-1/` |
| Stable link and reminder before activation | PASS in the staged continuation: two native shares copied the same HTTPS URL; the reminder hid after each share and returned on the first cold buyer reopening. The shared URL matched the backend's `invite_url`. | `target/partner-access-evidence/invite-continuation-attempt-2/`, `target/partner-access-evidence/invite-url-comparison.json` |
| First recipient, first cold link | **FAIL observed:** the universal link opened production For Her, but the original unpaid UID did not get Premium. Closing the subscription and live-coach startup windows still left membership false and the license in `created`. | `target/partner-access-evidence/recipient-after-startup-promos.json`, `target/partner-access-evidence/license-after-closing-startup-paywall.json` |
| First recipient, warm link | PASS in a separate diagnostic: opening the exact same link with For Her already initialized activated the same recipient. | `target/partner-access-evidence/warm-invite-diagnostic.json` |
| Remaining invitation lifecycle | PASS after that warm activation: the previously locked lesson opened before/after restart; repeat use retained R1's access; the buyer's reminder stayed absent across two restarts. A distinct unpaid R2 remained unpaid with its lesson locked after both cold and warm use of the same invitation. | `target/partner-access-evidence/recipient-lifecycle-continuation/`, `target/partner-access-evidence/used-invite-warm-continuation/` |
| Final read-only license diagnostic | Same license and invitation URL hashes as before activation; `status=activated`, `recipient_activated=true`. Buyer userdata no longer had `partner_invite_message`; current R2 membership remained false. This is additional diagnostic evidence, not an assertion of total license-row count. | `target/partner-access-evidence/license-after-activation-and-reuse.json` |
| Affected offline checks | PASS: 53 Java checks with the bounded XPath setting and 16 Python checks. The final feature-only rerun passed 25 Java checks; current sources also compiled. | `target/partner-access-offline-final.log`, `target/partner-access-current-contracts.log`, `target/partner-cold-warm-invite-compile.log` |

The isolated Allure snapshot/report is in
`target/partner-access-evidence/allure-results-current/` and
`target/partner-access-report/`. It retains the latest complete JUnit result per
test: 53 passed local Java checks, two passed device E2Es and the broken purchase
attempt. Staged continuations have their own logs, screenshots and safe membership
attachments; they do not replace that failed complete run.

The cold/warm difference needs investigation in the app/Branch invitation
handling, including delivery of `invite_token` to `/user/` during startup.
The root cause has not been isolated. The main E2E preserves the first cold link
and fails if that original recipient does not become Premium; it does not
repeat the link to make activation pass. The separately fixed native success
notice and Copy locators were test implementation failures.

The original recipient had active lifetime access and no linked email. After
the owner's explicit approval, the app was logged out and a new anonymous
recipient was prepared on Today. A fresh read-only `GET /api/v1/user/`, with
authorization matched to the app's current UID, returned
`subscription.active=false`. The Day 1 lesson `Awareness and Being in the Moment`
opened a fully loaded subscription paywall, both during preparation and again
inside the purchase test. The safe account-state evidence is saved in
`target/partner-access-evidence/recipient-new-prerequisite.json`.

## Changed files

| File | Change |
| --- | --- |
| `src/test/java/tests/PartnerAccessTests.java` | Three isolated device cases and shared invitation lifecycle assertions. |
| `src/test/java/lib/ui/ios/iOSPartnerAccessPageObject.java` | Offer, native Sandbox purchase, share/reminder, universal-link and recipient-content flows. |
| `src/test/java/lib/ui/ios/iOSPartnerRecipientOnboardingPageObject.java` | Observed For Her questionnaire for the second unpaid recipient. |
| `src/test/java/lib/PartnerRecipientEvidence.java` | UID-verified membership capture, activation wait and negative delivery window. |
| `scripts/read_partner_recipient.py` | Read-only physical-device membership bridge. |
| `scripts/test_read_partner_recipient.py` | Bridge checks for identity, strict state, auth rejection and temporary-data cleanup. |
| `src/test/java/tests/PartnerAccessContractUnitTests.java` | URL, native account and purchase/absence guard checks. |
| `src/test/resources/partner/ios-prod-3582.properties` | Observed labels and Sandbox expectations for the target builds. |
| `src/test/java/lib/ui/ios/iOSCoachFlowPageObject.java` | Reuse the existing partner promo title locator. |
| `src/test/java/lib/ui/ios/iOSDailyPlanPageObject.java` | Open a specifically selected lesson using the existing lesson flow. |
| `src/test/java/lib/ui/ios/iOSOnboardingPageObject.java` | Default-preserving questionnaire hook and reusable goal/tap helpers. |
| `docs/partner-access-e2e.md` | Requirements, isolation, commands, validation evidence and remaining cold-start issue. |
