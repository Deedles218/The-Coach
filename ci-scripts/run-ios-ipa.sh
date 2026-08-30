#!/usr/bin/env bash
set -euo pipefail

default_ipa="/Users/deedles/Downloads/The Coach.ipa"
ipa_path="${IOS_APP:-$default_ipa}"
project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

if [ "$#" -eq 0 ]; then
    echo "Usage: IOS_UDID=<connected-device-udid> $0 -Dtest=<test-or-suite> [Maven options]" >&2
    exit 2
fi

if [ ! -f "$ipa_path" ]; then
    echo "iOS IPA was not found: $ipa_path" >&2
    echo "Override it with IOS_APP=/path/to/app.ipa" >&2
    exit 1
fi

case "$ipa_path" in
    *.ipa) ;;
    *)
        echo "IOS_APP must point to an .ipa file for this launcher: $ipa_path" >&2
        exit 1
        ;;
esac

temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/the-coach-ipa.XXXXXX")"
cleanup() {
    rm -rf -- "$temp_dir"
}
trap cleanup EXIT INT TERM

unzip -q "$ipa_path" -d "$temp_dir"
app_path="$(find "$temp_dir/Payload" -maxdepth 1 -type d -name '*.app' -print -quit 2>/dev/null || true)"
if [ -z "$app_path" ] || [ ! -f "$app_path/Info.plist" ]; then
    echo "The IPA does not contain a valid Payload/*.app bundle: $ipa_path" >&2
    exit 1
fi

bundle_id="$(plutil -extract CFBundleIdentifier raw "$app_path/Info.plist")"
platform_name="$(plutil -extract DTPlatformName raw "$app_path/Info.plist" 2>/dev/null || true)"
supported_platforms="$(plutil -extract CFBundleSupportedPlatforms xml1 -o - "$app_path/Info.plist" 2>/dev/null || true)"
short_version="$(plutil -extract CFBundleShortVersionString raw "$app_path/Info.plist" 2>/dev/null || true)"
build_version="$(plutil -extract CFBundleVersion raw "$app_path/Info.plist" 2>/dev/null || true)"

if [ "$platform_name" != "iphoneos" ]; then
    echo "This launcher accepts a device IPA only; DTPlatformName=$platform_name" >&2
    echo "A Simulator run requires a separate .app built for iphonesimulator." >&2
    exit 1
fi

case "$supported_platforms" in
    *iPhoneOS*) ;;
    *)
        echo "The IPA is not marked for iPhoneOS: $ipa_path" >&2
        exit 1
        ;;
esac

if [ -n "${IOS_BUNDLE_ID:-}" ] && [ "$IOS_BUNDLE_ID" != "$bundle_id" ]; then
    echo "IOS_BUNDLE_ID=$IOS_BUNDLE_ID does not match the IPA Bundle ID $bundle_id" >&2
    exit 1
fi

echo "IPA: $ipa_path"
echo "Bundle ID: $bundle_id"
echo "Version: ${short_version:-unknown} (${build_version:-unknown})"
echo "Target: real iOS device (iPhoneOS)"

if [ -f "$app_path/embedded.mobileprovision" ]; then
    provisioning_plist="$temp_dir/embedded.mobileprovision.plist"
    if security cms -D -i "$app_path/embedded.mobileprovision" > "$provisioning_plist" 2>/dev/null; then
        if plutil -extract ProvisionedDevices xml1 -o - "$provisioning_plist" >/dev/null 2>&1; then
            echo "Provisioning: device-provisioned IPA"
        else
            echo "WARNING: the IPA has no ProvisionedDevices list; installation through Appium may fail."
            echo "Use an Ad Hoc/development/enterprise IPA, or install this exact build on the phone first."
        fi
    else
        echo "WARNING: embedded.mobileprovision could not be decoded; device installation may fail."
    fi
else
    echo "WARNING: the IPA has no embedded.mobileprovision; device installation may fail."
fi

cd "$project_dir"
maven_args=(
    "-Dplatform=ios"
    "-Dios.app=$ipa_path"
    "-Dios.bundleId=$bundle_id"
)

exec mvn test "${maven_args[@]}" "$@"
