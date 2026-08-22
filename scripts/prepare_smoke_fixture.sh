#!/usr/bin/env bash

set -euo pipefail

fixture_script_dir="$(cd "$(dirname "$0")" && pwd)"
"${fixture_script_dir}/validate_smoke_test_data.sh"

case "${COACH_FIXTURE_RESET_MODE}" in
  backend_api)
    reset_script="${COACH_FIXTURE_RESET_SCRIPT:-}"
    if [[ -z "${reset_script}" || ! -x "${reset_script}" ]]; then
      echo "COACH_FIXTURE_RESET_MODE=backend_api requires an executable COACH_FIXTURE_RESET_SCRIPT" >&2
      exit 2
    fi

    "${reset_script}" \
      "${COACH_FIXTURE_ID}" \
      "${COACH_FIXTURE_ENVIRONMENT}" \
      "${COACH_DAILY_PLAN_FIXTURE_ID}" \
      "${COACH_KEGEL_FIXTURE_ID}" \
      "${COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID}"
    ;;
  prebuilt)
    echo "Prebuilt Smoke fixture selected; no backend data was changed."
    ;;
  clean_install)
    echo "Clean-install fixture mode selected; app state is reset by iOS capabilities."
    ;;
esac
