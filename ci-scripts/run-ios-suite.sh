#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
project_dir="$(CDPATH= cd -- "$script_dir/.." && pwd)"

usage() {
    cat <<'EOF'
Usage: IOS_SIMULATOR_APP=/path/to/app.app ./ci-scripts/run-ios-suite.sh <suite> [Maven options]

Suites:
  smoke              suites.SmokeSuite; validates P0 smoke data
  release-smoke      suites.ReleaseSmokeSuite; validates P0 smoke data
  ios-parity-smoke   suites.IOSParitySmokeSuite; validates P0 smoke data
  explore            suites.ExploreParitySuite; validates P0 smoke data
  modules            suites.ModulesSuite; validates P0 smoke data
  test-model         suites.TestModelAutomationSuite; validates test-model data
  localization       suites.LocalizationSuite; requires an existing-progress account
  male-start-screen  suites.MaleBuildSuite
  onboarding-goal    suites.OnboardingGoalProgramSuite
  onboarding-paywall suites.OnboardingPaywallSuite
  onboarding-slides  suites.OnboardingSlidesSuite
  push-permission    suites.PushPermissionSuite
  storekit-purchase  suites.StoreKitPurchaseSuite

Set IOS_APP_VARIANT=male or IOS_APP_VARIANT=female to use
COACH_MALE_EXISTING_PROGRESS_* or COACH_FEMALE_EXISTING_PROGRESS_* for suites
that need the existing-progress role. The launcher never prints credential values.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    usage
    exit 0
fi

if [[ $# -lt 1 ]]; then
    usage >&2
    exit 2
fi

suite_name="$1"
shift

selectVariantExistingProgressAccount() {
    local prefix
    case "${IOS_APP_VARIANT:-}" in
        "")
            return
            ;;
        male)
            prefix="COACH_MALE_EXISTING_PROGRESS"
            ;;
        female)
            prefix="COACH_FEMALE_EXISTING_PROGRESS"
            ;;
        *)
            echo "IOS_APP_VARIANT must be male or female when it is set" >&2
            exit 2
            ;;
    esac

    local email_variable="${prefix}_EMAIL"
    local otp_variable="${prefix}_OTP"
    if [[ -z "${!email_variable:-}" || -z "${!otp_variable:-}" ]]; then
        echo "Missing variant-specific existing-progress account for IOS_APP_VARIANT=${IOS_APP_VARIANT}" >&2
        echo "Configure ${email_variable} and ${otp_variable}; values are not printed." >&2
        exit 2
    fi

    export COACH_EXISTING_PROGRESS_EMAIL="${!email_variable}"
    export COACH_EXISTING_PROGRESS_OTP="${!otp_variable}"
}

requireExistingProgressAccount() {
    local missing=()
    local variable_name
    for variable_name in COACH_EXISTING_PROGRESS_EMAIL COACH_EXISTING_PROGRESS_OTP; do
        if [[ -z "${!variable_name:-}" ]]; then
            missing+=("$variable_name")
        fi
    done
    if (( ${#missing[@]} > 0 )); then
        echo "Existing-progress account is required for this suite. Missing:" >&2
        printf ' - %s\n' "${missing[@]}" >&2
        exit 2
    fi
}

selectVariantExistingProgressAccount

case "$suite_name" in
    smoke)
        "$project_dir/scripts/validate_smoke_test_data.sh"
        suite_class="suites.SmokeSuite"
        ;;
    release-smoke)
        "$project_dir/scripts/validate_smoke_test_data.sh"
        suite_class="suites.ReleaseSmokeSuite"
        ;;
    ios-parity-smoke)
        "$project_dir/scripts/validate_smoke_test_data.sh"
        suite_class="suites.IOSParitySmokeSuite"
        ;;
    explore)
        "$project_dir/scripts/validate_smoke_test_data.sh"
        suite_class="suites.ExploreParitySuite"
        ;;
    modules)
        "$project_dir/scripts/validate_smoke_test_data.sh"
        suite_class="suites.ModulesSuite"
        ;;
    test-model)
        "$project_dir/scripts/validate_test_model_data.sh"
        suite_class="suites.TestModelAutomationSuite"
        ;;
    localization)
        requireExistingProgressAccount
        suite_class="suites.LocalizationSuite"
        ;;
    male-start-screen)
        suite_class="suites.MaleBuildSuite"
        ;;
    onboarding-goal)
        suite_class="suites.OnboardingGoalProgramSuite"
        ;;
    onboarding-paywall)
        suite_class="suites.OnboardingPaywallSuite"
        ;;
    onboarding-slides)
        suite_class="suites.OnboardingSlidesSuite"
        ;;
    push-permission)
        suite_class="suites.PushPermissionSuite"
        ;;
    storekit-purchase)
        suite_class="suites.StoreKitPurchaseSuite"
        ;;
    *)
        echo "Unknown iOS suite: $suite_name" >&2
        usage >&2
        exit 2
        ;;
esac

exec "$script_dir/run-ios-simulator.sh" "-Dtest=${suite_class}" "$@"
