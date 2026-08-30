#!/usr/bin/env bash
set -euo pipefail

default_app="/Users/deedles/Downloads/The Coach.app"
default_simulator_udid="00CA21E8-4A92-4607-A941-E5FD2E29DAC5"
default_simulator_name="iPhone 17 Pro"
default_platform_version="26.5"

app_path="${IOS_SIMULATOR_APP:-$default_app}"
simulator_udid="${IOS_SIMULATOR_UDID:-$default_simulator_udid}"
simulator_name="${IOS_SIMULATOR_DEVICE_NAME:-$default_simulator_name}"
platform_version="${IOS_SIMULATOR_PLATFORM_VERSION:-$default_platform_version}"
project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

if [ "$#" -eq 0 ]; then
    echo "Usage: IOS_SIMULATOR_APP=/path/to/app.app $0 -Dtest=<test-or-suite> [Maven options]" >&2
    exit 2
fi

if [ ! -d "$app_path" ]; then
    echo "iOS Simulator .app was not found: $app_path" >&2
    echo "Build or provide a separate .app compiled for iphonesimulator." >&2
    echo "The device IPA /Users/deedles/Downloads/The Coach.ipa cannot be used here." >&2
    exit 1
fi

case "$app_path" in
    *.app) ;;
    *)
        echo "IOS_SIMULATOR_APP must point to an .app bundle: $app_path" >&2
        exit 1
        ;;
esac

if [ ! -f "$app_path/Info.plist" ]; then
    echo "The Simulator app does not contain Info.plist: $app_path" >&2
    exit 1
fi

bundle_id="$(plutil -extract CFBundleIdentifier raw "$app_path/Info.plist")"
platform_name="$(plutil -extract DTPlatformName raw "$app_path/Info.plist" 2>/dev/null || true)"
supported_platforms="$(plutil -extract CFBundleSupportedPlatforms xml1 -o - "$app_path/Info.plist" 2>/dev/null || true)"
short_version="$(plutil -extract CFBundleShortVersionString raw "$app_path/Info.plist" 2>/dev/null || true)"
build_version="$(plutil -extract CFBundleVersion raw "$app_path/Info.plist" 2>/dev/null || true)"

if [ "$platform_name" != "iphonesimulator" ]; then
    echo "This launcher accepts a Simulator .app only; DTPlatformName=$platform_name" >&2
    echo "The supplied IPA is a device build (iphoneos), not a Simulator build." >&2
    exit 1
fi

case "$supported_platforms" in
    *iPhoneSimulator*) ;;
    *)
        echo "The app is not marked for iPhoneSimulator: $app_path" >&2
        exit 1
        ;;
esac

device_line="$(xcrun simctl list devices available | grep -F "($simulator_udid)" || true)"
if [ -z "$device_line" ]; then
    echo "iOS Simulator UDID is not available: $simulator_udid" >&2
    echo "Use: xcrun simctl list devices available" >&2
    exit 1
fi

case "$device_line" in
    *Booted*) ;;
    *)
        xcrun simctl boot "$simulator_udid"
        ;;
esac
xcrun simctl bootstatus "$simulator_udid" -b

echo "Simulator app: $app_path"
echo "Bundle ID: $bundle_id"
echo "Version: ${short_version:-unknown} (${build_version:-unknown})"
echo "Target: $simulator_name ($simulator_udid), iOS $platform_version"

cd "$project_dir"
maven_args=(
    "-Dplatform=ios"
    "-Dios.app=$app_path"
    "-Dios.bundleId=$bundle_id"
    "-Dios.deviceName=$simulator_name"
    "-Dios.platformVersion=$platform_version"
    "-Dios.udid=$simulator_udid"
)

exec mvn test "${maven_args[@]}" "$@"
