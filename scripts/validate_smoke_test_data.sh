#!/usr/bin/env bash

set -euo pipefail

fixture_scope="${SMOKE_DATA_SCOPE:-p0}"
normalized_fixture_scope="$(printf '%s' "${fixture_scope}" | tr '[:upper:]' '[:lower:]')"
case "${normalized_fixture_scope}" in
  p0|p1)
    ;;
  *)
    echo "SMOKE_DATA_SCOPE must be one of: p0, p1" >&2
    exit 2
    ;;
esac

required_variables=(
  COACH_EXISTING_PROGRESS_EMAIL
  COACH_EXISTING_PROGRESS_OTP
  COACH_DAILY_PLAN_DAY
  COACH_FIXTURE_RESET_MODE
)

if [[ "${normalized_fixture_scope}" == "p1" ]]; then
  required_variables+=(
    COACH_NO_PDF_ENTITLEMENT_EMAIL
    COACH_NO_PDF_ENTITLEMENT_OTP
    COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID
  )
fi

if [[ "${COACH_FIXTURE_RESET_MODE:-}" == "backend_api" ]]; then
  required_variables+=(
    COACH_FIXTURE_ID
    COACH_FIXTURE_ENVIRONMENT
    COACH_DAILY_PLAN_FIXTURE_ID
    COACH_KEGEL_FIXTURE_ID
  )
fi

missing_variables=()
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    missing_variables+=("${variable_name}")
  fi
done

if (( ${#missing_variables[@]} > 0 )); then
  echo "Smoke test data contract is incomplete. Missing secret-backed variables:" >&2
  printf ' - %s\n' "${missing_variables[@]}" >&2
  exit 2
fi

if [[ -n "${COACH_KEGEL_PLAYER_EMAIL:-}" || -n "${COACH_KEGEL_PLAYER_OTP:-}" ]]; then
  if [[ -z "${COACH_KEGEL_PLAYER_EMAIL:-}" || -z "${COACH_KEGEL_PLAYER_OTP:-}" ]]; then
    echo "Configure both COACH_KEGEL_PLAYER_EMAIL and COACH_KEGEL_PLAYER_OTP, or omit both to reuse existing-progress for Smoke Kegel" >&2
    exit 2
  fi
fi

case "${COACH_FIXTURE_RESET_MODE}" in
  backend_api|prebuilt|clean_install)
    ;;
  *)
    echo "COACH_FIXTURE_RESET_MODE must be one of: backend_api, prebuilt, clean_install" >&2
    exit 2
    ;;
esac

isolation_mode="${TEST_ISOLATION_MODE:-logout}"
normalized_isolation_mode="$(printf '%s' "${isolation_mode}" | tr '[:upper:]' '[:lower:]')"
case "${normalized_isolation_mode}" in
  logout|reinstall)
    ;;
  *)
    echo "TEST_ISOLATION_MODE must be one of: logout, reinstall" >&2
    exit 2
    ;;
esac

if [[ -n "${COACH_VALID_EMAIL_WITHOUT_PROGRESS:-}" \
   && "${COACH_EXISTING_PROGRESS_EMAIL}" == "${COACH_VALID_EMAIL_WITHOUT_PROGRESS}" ]]; then
  echo "Fixture account emails must be distinct for no-progress and existing-progress roles" >&2
  exit 2
fi

if [[ -n "${COACH_KEGEL_PLAYER_EMAIL:-}" \
   && ( "${COACH_EXISTING_PROGRESS_EMAIL}" == "${COACH_KEGEL_PLAYER_EMAIL}" \
     || ( -n "${COACH_VALID_EMAIL_WITHOUT_PROGRESS:-}" && "${COACH_VALID_EMAIL_WITHOUT_PROGRESS}" == "${COACH_KEGEL_PLAYER_EMAIL}" ) ) ]]; then
  echo "A dedicated Kegel-player account must be distinct from no-progress and existing-progress roles" >&2
  exit 2
fi

if [[ "${normalized_fixture_scope}" == "p1" \
   && ( "${COACH_NO_PDF_ENTITLEMENT_EMAIL:-}" == "${COACH_EXISTING_PROGRESS_EMAIL}" \
     || ( -n "${COACH_VALID_EMAIL_WITHOUT_PROGRESS:-}" && "${COACH_NO_PDF_ENTITLEMENT_EMAIL:-}" == "${COACH_VALID_EMAIL_WITHOUT_PROGRESS}" ) \
     || ( -n "${COACH_KEGEL_PLAYER_EMAIL:-}" && "${COACH_NO_PDF_ENTITLEMENT_EMAIL:-}" == "${COACH_KEGEL_PLAYER_EMAIL}" ) ) ]]; then
  echo "The no-PDF account must be distinct from the other fixture roles" >&2
  exit 2
fi

echo "Smoke test data contract is configured (credential values are not printed)."
