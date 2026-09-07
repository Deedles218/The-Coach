#!/usr/bin/env bash

set -euo pipefail

required_variables=(
  COACH_EXISTING_PROGRESS_EMAIL
  COACH_EXISTING_PROGRESS_OTP
  COACH_VALID_EMAIL_WITHOUT_PROGRESS
  COACH_COA8231_EMAIL
  COACH_COA8231_OTP
  COACH_COA8231_UID
  COACH_COA8232_EMAIL
  COACH_COA8232_OTP
  COACH_COA8232_UID
  COACH_COA8517_EMAIL
  COACH_COA8517_OTP
  COACH_COA8517_UID
)

missing_variables=()
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    missing_variables+=("${variable_name}")
  fi
done

if (( ${#missing_variables[@]} > 0 )); then
  echo "Ready test-model data contract is incomplete. Missing variables:" >&2
  printf ' - %s\n' "${missing_variables[@]}" >&2
  exit 2
fi

if [[ "${COACH_EXISTING_PROGRESS_EMAIL}" == "${COACH_VALID_EMAIL_WITHOUT_PROGRESS}" ]]; then
  echo "Existing-progress and valid-no-progress account emails must be distinct" >&2
  exit 2
fi

echo "Ready test-model data contract is configured (credential values are not printed)."
