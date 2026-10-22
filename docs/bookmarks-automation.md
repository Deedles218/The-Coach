# Bookmarks iOS automation

`BookmarksTests` covers Xray cases COA-9647 through COA-9666 and the no-subscription access check from COA-9667. Trial variants from COA-9667 and COA-9668 (expired subscription) are intentionally excluded by the agreed scope. The suite is iOS-only because the source cases are labelled iOS and the verified accessibility identifiers come from the iOS build.

Expired access has a different product contract and is not represented by the no-subscription test: My Bookmarks opens and displays saved cards, but opening a saved lesson is blocked by the subscription paywall. If COA-9668 is automated later, it must assert both stages instead of expecting the Bookmarks entry point itself to be paywalled.

## Runner

Run one case first:

```bash
mvn -Dtest=tests.BookmarksTests#emptyListNeverShowsTooltip \
  -Dplatform=ios \
  -Dios.udid=<UDID> \
  -Dios.deviceName=<device-name> \
  -Dios.bundleId=<bundle-id> \
  test
```

Run the dedicated suite after the fixture accounts are reset:

```bash
mvn -Dtest=suites.BookmarksSuite -Dplatform=ios test
```

For local locator work against an already authorized account, add `-Dcoach.bookmarks.useCurrentAccount=true` and run only one compatible method. Multi-account tests reject this mode.

## Secret-backed accounts

The content scenarios reuse seven subscribed account roles per app variant. The access-control scenario uses one additional `NOSUBSCRIPTION` role. Each role uses `COACH_BOOKMARKS_<ROLE>_EMAIL` and `COACH_BOOKMARKS_<ROLE>_OTP` (or matching lowercase `coach.bookmarks.<role>.email/otp` properties). Supply the male or female set that matches `ios.bundleId`; credentials are not stored in the repository.

| Role | Required initial state |
| --- | --- |
| `BASELINE` | Empty list and a saveable lesson; used by empty-state and reversible save/delete checks |
| `CONTENT` | Female: saved lesson and practice at known non-default slides. Male: two saved lessons because Stage 1 has no bookmarkable practice |
| `TOOLTIP` | One saved lesson; tooltip has never been shown |
| `RECURRING` | Repeating content saved from the later day/stage; adjacent-day state is known |
| `SWITCHA` | Exactly one bookmark, different from `SWITCHB` |
| `SWITCHB` | Exactly one bookmark, different from `SWITCHA` |
| `UPDATE` | Exactly one saved lesson; tooltip already consumed; update build configured |
| `NOSUBSCRIPTION` | Newly provisioned account with onboarding completed and no active, trial, lifetime, or expired entitlement; Today is available |

Cases that require a bookmarkable practice (`COA-9649`, `COA-9651`, `COA-9657`, `COA-9663`, `COA-9664`) must run with the Coach for Her fixture unless the selected men's program exposes an equivalent bookmarkable practice. The current men's Stage 1 does not.

The one `TOOLTIP` account is sufficient for the representative blank-area dismissal in COA-9653. Re-running the tooltip-first scenarios requires resetting the app-local tooltip state or reprovisioning that role; the suite never manufactures extra accounts.

The runner also supplies only the values needed by the selected method: `COACH_BOOKMARKS_LESSON_TITLE`, `SECOND_LESSON_TITLE`, `PRACTICE_TITLE`, `RECURRING_PRACTICE_TITLE`, first/middle/last or saved slide texts, `PROGRAM_TITLE`, `MODULE_TITLE`, `EMPTY_CATEGORY_TITLE`, `TOOLTIP_TEXT`, `KEGEL_TITLE`, `KEGEL_SPORT_TITLE`, `LESSON_ID`, `SAVED_SLIDE_NUMBER`, `PROGRAM_ID`, `PROGRAM_DAY`, `SAVED_DAY_TITLE`, and `PREVIOUS_DAY_TITLE`. System properties use the equivalent lower-camel names under `coach.bookmarks.*`.

COA-9662 reads an append-only log from `COACH_BOOKMARKS_ANALYTICS_LOG`; COA-9663 reads an append-only HTTP capture from `COACH_BOOKMARKS_NETWORK_LOG`. The tests checkpoint the file before the action and accept only evidence appended afterwards, preventing stale events or requests from passing. COA-9666 additionally requires `IOS_UPDATE_APP`/`ios.update.app` and intentionally removes then reinstalls the app during its reinstall phase.
