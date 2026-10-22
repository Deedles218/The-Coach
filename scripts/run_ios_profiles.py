#!/usr/bin/env python3
"""Run explicit iOS profiles with preflight gates and separate Allure results."""
import argparse
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
import sys
from datetime import datetime
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
EXISTING = ("COACH_EXISTING_PROGRESS_EMAIL", "COACH_EXISTING_PROGRESS_OTP")
PREFLIGHT = "tests.IOSRegressionPreflightTests#"
PROFILES = {
    "regression": dict(test="suites.IOSRegressionSuite", mode="existing", validator="smoke",
                       gate=PREFLIGHT + "testExistingProgressFixtureAndExplore"),
    "preflight": dict(test=PREFLIGHT + "testExistingProgressFixtureAndExplore", mode="existing", validator="smoke"),
    "smoke": dict(test="suites.SmokeSuite", mode="clean", validator="smoke", onboarding=True),
    "release-smoke": dict(test="suites.ReleaseSmokeSuite", mode="update", validator="smoke",
                          required=("COACH_EXPECTED_ACCOUNT_UID",)),
    "ios-parity-smoke": dict(test="suites.IOSParitySmokeSuite", mode="existing", validator="smoke",
                             gate=PREFLIGHT + "testExistingProgressFixtureAndExplore"),
    "explore": dict(test="suites.ExploreParitySuite", mode="existing", required=EXISTING,
                    gate=PREFLIGHT + "testExploreCatalogIsReady"),
    "authorization": dict(test="tests.CoachAuthorizationTests", mode="existing",
                          required=EXISTING + ("COACH_VALID_EMAIL_WITHOUT_PROGRESS",)),
    "profile": dict(test="tests.CoachProfileTests", mode="existing", required=EXISTING),
    "daily-plan": dict(test="tests.DailyPlanTests", mode="existing", validator="smoke",
                       gate=PREFLIGHT + "testExistingProgressFixtureAndExplore"),
    "kegel": dict(test="tests.KegelExerciseTests", mode="existing", validator="smoke",
                  required=("COACH_KEGEL_PLAYER_EMAIL", "COACH_KEGEL_PLAYER_OTP")),
    "kegel-october": dict(test="suites.KegelOctoberCasesSuite", mode="existing",
                          required=tuple("COACH_" + case + "_" + field
                                         for case in ("COA8094", "COA8096", "COA8170", "COA8171")
                                         for field in ("EMAIL", "OTP")) + ("COACH_COA7937_EMAIL", "COACH_COA7937_LINK"),
                          files=("coach.kegelUi.fixture",)),
    "modules": dict(test="suites.ModulesSuite", mode="existing",
                    required=("COACH_COA9044_EMAIL", "COACH_COA9044_OTP", "COACH_COA9044_UID"),
                    properties={"coach.modules.enabled": "true"},
                    required_properties=("coach.modules.programId",)),
    "test-model": dict(test="suites.TestModelAutomationSuite", mode="existing", validator="test-model"),
    "localization": dict(test="suites.LocalizationSuite", mode="installed",
                         files=("localization.fixture",),
                         configuration_gate="tests.LocalizationConfigurationTests"),
    "male-start-screen": dict(test="suites.MaleBuildSuite", mode="existing", required=EXISTING),
    "onboarding-goal": dict(test="suites.OnboardingGoalProgramSuite", mode="installed", fresh=True),
    "onboarding-paywall": dict(test="suites.OnboardingPaywallSuite", mode="installed", fresh=True,
                               required_true=("storekit.sandbox", "purchase.allow")),
    "onboarding-slides": dict(test="suites.OnboardingSlidesSuite", mode="installed",
                              files=("onboarding.slides.fixture",), fresh=True),
    "push-permission": dict(test="suites.PushPermissionSuite", mode="clean", onboarding=True),
    "storekit-purchase": dict(test="suites.StoreKitPurchaseSuite", mode="installed", onboarding=True,
                              required_true=("storekit.sandbox", "storekit.allowPurchases")),
    "video-player": dict(test="tests.VideoPlayerFeatureTests", mode="existing",
                         required=("COACH_COA9551_EMAIL", "COACH_COA9551_OTP"),
                         required_properties=("coach.video.lessonTitle", "coach.video.secondLessonTitle")),
    "program-switch": dict(test="tests.ProgramSwitchFeatureTests", mode="clean",
                           required_properties=("coach.program.target",)),
}

ENV_PROPERTIES = {
    "COACH_EXISTING_PROGRESS_EMAIL": "coach.existingProgress.email",
    "COACH_EXISTING_PROGRESS_OTP": "coach.existingProgress.otp",
    "COACH_VALID_EMAIL_WITHOUT_PROGRESS": "coach.validEmailWithoutProgress",
    "COACH_DAILY_PLAN_DAY": "coach.fixture.dailyPlan.day",
    "COACH_FIXTURE_RESET_MODE": "coach.fixture.reset.mode",
    "COACH_FIXTURE_ID": "coach.fixture.id",
    "COACH_FIXTURE_ENVIRONMENT": "coach.fixture.environment",
    "COACH_DAILY_PLAN_FIXTURE_ID": "coach.fixture.dailyPlan.id",
    "COACH_KEGEL_FIXTURE_ID": "coach.fixture.kegel.id",
    "COACH_KEGEL_PLAYER_EMAIL": "coach.kegelPlayer.email",
    "COACH_KEGEL_PLAYER_OTP": "coach.kegelPlayer.otp",
    "TEST_ISOLATION_MODE": "test.isolation.mode",
}
for _case in ("COA8231", "COA8232", "COA8511", "COA8512", "COA8517",
              "COA9044", "COA9551", "COA8094", "COA8096", "COA8170", "COA8171", "COA7937"):
    for _field in ("EMAIL", "OTP"):
        ENV_PROPERTIES["COACH_" + _case + "_" + _field] = "coach." + _case.lower() + "." + _field.lower()


class PreflightError(RuntimeError):
    pass


def properties(arguments):
    values = {}
    for argument in arguments:
        if argument.startswith("-D"):
            key, separator, value = argument[2:].partition("=")
            values[key] = value if separator else "true"
        elif argument not in ("-q", "-e", "-B", "-ntp"):
            raise PreflightError("Unsupported Maven option; use -Dkey=value, -q, -e, -B or -ntp")
    if "test" in values:
        raise PreflightError("The profile owns -Dtest. Use an explicit profile instead of package discovery.")
    if values.get("platform", "ios") != "ios":
        raise PreflightError("The iOS launcher requires platform=ios")
    for key in values:
        if key.startswith("coach.run.") or key == "allure.results.directory":
            raise PreflightError("The launcher owns run metadata and Allure directories; use --results-root")
    return values


def load_app(path):
    bundle = Path(path).expanduser().resolve()
    if bundle.suffix != ".app" or not (bundle / "Info.plist").is_file():
        raise PreflightError("Provide an existing Simulator .app with Info.plist via IOS_SIMULATOR_APP")
    with (bundle / "Info.plist").open("rb") as source:
        info = plistlib.load(source)
    if info.get("DTPlatformName") != "iphonesimulator" or "iPhoneSimulator" not in info.get("CFBundleSupportedPlatforms", []):
        raise PreflightError("This profile requires an iphonesimulator .app, not a device IPA/.app")
    if not info.get("CFBundleIdentifier"):
        raise PreflightError("The app has no CFBundleIdentifier")
    return bundle, info


def environment_for(variant, supplied, props):
    env = dict(supplied)
    if variant not in ("male", "female"):
        raise PreflightError("IOS_APP_VARIANT must be male or female")
    prefix = "COACH_" + variant.upper() + "_EXISTING_PROGRESS"
    if env.get(prefix + "_EMAIL") or env.get(prefix + "_OTP"):
        for field in ("EMAIL", "OTP"):
            env["COACH_EXISTING_PROGRESS_" + field] = env.get(prefix + "_" + field, "")
    for variable, key in ENV_PROPERTIES.items():
        if key in props:
            env[variable] = props[key]
    if env.get("COACH_ACCOUNT_VARIANT", variant).lower() != variant:
        raise PreflightError("COACH_ACCOUNT_VARIANT disagrees with the app variant")
    env["IOS_APP_VARIANT"] = variant
    env["TEST_ISOLATION_MODE"] = env.get("TEST_ISOLATION_MODE", "logout").strip().lower()
    return env


def redact(text, env, props=None):
    secrets = [value for key, value in env.items()
               if key.endswith(("_EMAIL", "_OTP", "_LINK")) and len(value) > 1]
    for key, value in (props or {}).items():
        if key.endswith((".email", ".otp", ".link")) and len(value) > 1:
            secrets.append(value)
    for secret in sorted(set(secrets), key=len, reverse=True):
        text = text.replace(secret, "<redacted>")
    return re.sub(r"[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}", "<redacted-email>", text)


def configured(props, env, key, variable=None, default=None):
    return props.get(key, env.get(variable, default) if variable else default)


def validate_profile(name, props, env, info):
    profile = PROFILES[name]
    clean = profile["mode"] == "clean"
    for key, expected in (("ios.fullReset", str(clean).lower()), ("ios.noReset", str(not clean).lower())):
        if key in props and props[key].lower() != expected:
            raise PreflightError(name + " requires " + key + "=" + expected)
    if env["TEST_ISOLATION_MODE"] != "logout":
        raise PreflightError(name + " requires TEST_ISOLATION_MODE=logout; reset is owned by the profile")
    if profile["mode"] == "installed" and (props.get("ios.app") or env.get("IOS_APP")):
        raise PreflightError(name + " attaches by bundle ID: omit ios.app and IOS_APP")
    if profile["mode"] != "installed" and not info:
        raise PreflightError(name + " requires IOS_SIMULATOR_APP")
    missing = [key for key in profile.get("required", ()) if not env.get(key, "").strip()]
    if name == "localization" and props.get("localization.scenarios", "welcome,dashboard,switch") != "welcome":
        missing.extend(key for key in EXISTING if not env.get(key, "").strip())
    if missing:
        raise PreflightError(name + ": missing " + ", ".join(missing))
    for key in profile.get("required_properties", ()):
        if not props.get(key, "").strip():
            raise PreflightError(name + ": configure -D" + key)
    for key, expected in profile.get("properties", {}).items():
        if props.get(key, expected) != expected:
            raise PreflightError(name + " requires " + key + "=" + expected)
    for key in profile.get("required_true", ()):
        variable = key.replace(".", "_").upper()
        if str(configured(props, env, key, variable, "false")).lower() != "true":
            raise PreflightError(name + ": explicitly configure " + key + "=true after preparing Sandbox")
    for key in profile.get("files", ()):
        path = Path(props[key]).expanduser() if props.get(key) else None
        if path is not None and not path.is_absolute():
            path = ROOT / path
        if path is None or not path.is_file():
            raise PreflightError(name + ": configure -D" + key + "=<existing fixture file>")
    if profile.get("fresh") and env.get("IOS_FRESH_START_PREPARED") != "true":
        raise PreflightError(name + " requires a manually prepared fresh installed build and IOS_FRESH_START_PREPARED=true")
    if profile.get("onboarding") and not configured(props, env, "ios.onboarding.steps", "IOS_ONBOARDING_STEPS"):
        raise PreflightError(name + ": configure IOS_ONBOARDING_STEPS from the observed onboarding")
    if name == "modules" and info.get("CFBundleIdentifier") != "com.vamapps.preprod.The-Coach":
        raise PreflightError("modules supports the preprod iOS build only")
    if name == "localization":
        expected_variant = props.get("localization.variant", "male")
        if expected_variant.replace("-legacy", "") != env["IOS_APP_VARIANT"]:
            raise PreflightError("localization.variant disagrees with the selected app variant")
    if name == "male-start-screen" and env["IOS_APP_VARIANT"] != "male":
        raise PreflightError("male-start-screen requires the male app variant")
    if profile.get("validator"):
        script = "validate_smoke_test_data.sh" if profile["validator"] == "smoke" else "validate_test_model_data.sh"
        result = subprocess.run(["bash", str(ROOT / "scripts" / script)],
                                env=env, cwd=ROOT, capture_output=True, text=True)
        if result.returncode:
            raise PreflightError(name + ": " + redact(result.stderr.strip(), env, props))
    if profile["mode"] == "update":
        update = configured(props, env, "ios.update.app", "IOS_UPDATE_APP")
        if not update:
            raise PreflightError("release-smoke requires IOS_UPDATE_APP or -Dios.update.app")
        _, newer = load_app(update)
        if newer["CFBundleIdentifier"] != info["CFBundleIdentifier"]:
            raise PreflightError("Update builds must have the same bundle ID")
        if newer.get("CFBundleVersion") == info.get("CFBundleVersion"):
            raise PreflightError("Update builds must have different CFBundleVersion values")
        for build in (info, newer):
            if not re.fullmatch(r"[0-9]+(?:\.[0-9]+){0,2}", str(build.get("CFBundleVersion", ""))):
                raise PreflightError("Update evidence requires real numeric CFBundleVersion values, not CI placeholders")


def validate_runtime(props, env, device, simulator, bundle_id, installed):
    if not shutil.which("mvn"):
        raise PreflightError("mvn is not available in PATH")
    if simulator:
        result = subprocess.run(["xcrun", "simctl", "list", "devices", "available", "--json"],
                                capture_output=True, text=True)
        if result.returncode:
            raise PreflightError("xcrun simctl is unavailable; check Xcode selection and setup")
        devices = [entry for group in json.loads(result.stdout)["devices"].values() for entry in group]
        if not any(entry["udid"] == device for entry in devices):
            raise PreflightError("The configured Simulator UDID is not available")
        if installed:
            result = subprocess.run(["xcrun", "simctl", "get_app_container", device, bundle_id, "app"],
                                    capture_output=True, text=True)
            if result.returncode:
                raise PreflightError("The installed-build profile requires the app to be installed in the booted Simulator")
    url = configured(props, env, "appium.url", "APPIUM_URL", "http://127.0.0.1:4723/").rstrip("/") + "/status"
    try:
        with urllib.request.urlopen(url, timeout=5) as response:
            ready = json.load(response).get("value", {}).get("ready")
        if ready is not True:
            raise PreflightError("Appium is not ready")
    except (OSError, ValueError) as error:
        raise PreflightError("Appium /status is unavailable; start the server at the configured appium.url") from error


def command_for(name, props, env, info, device, results, git_sha, selector=None):
    profile = PROFILES[name]
    clean = profile["mode"] == "clean"
    controlled = {
        "platform": "ios", "test": selector or profile["test"],
        "ios.fullReset": str(clean).lower(), "ios.noReset": str(not clean).lower(),
        "ios.udid": device, "ios.bundleId": info["CFBundleIdentifier"],
        "allure.results.directory": str(results),
        "coach.run.runProfile": name, "coach.run.testGitSha": git_sha,
        "coach.run.appVersion": str(info.get("CFBundleShortVersionString", "unknown")),
        "coach.run.appBuild": str(info.get("CFBundleVersion", "unknown")),
        "coach.run.appVariant": env["IOS_APP_VARIANT"],
        "coach.run.testGitDirty": env.get("COACH_RUN_GIT_DIRTY", "unknown"),
        "coach.run.resetPolicy": "fullReset=" + str(clean).lower() + ",noReset=" + str(not clean).lower(),
    }
    controlled.update(profile.get("properties", {}))
    if re.fullmatch(r"[A-Fa-f0-9-]{36}", device):
        controlled["ios.deviceName"] = configured(props, env, "ios.deviceName", "IOS_SIMULATOR_DEVICE_NAME", "iPhone 17 Pro")
        controlled["ios.platformVersion"] = configured(props, env, "ios.platformVersion", "IOS_SIMULATOR_PLATFORM_VERSION", "26.5")
        controlled["ios.isHeadless"] = props.get("ios.isHeadless", env.get("IOS_IS_HEADLESS", "true"))
    values = dict(props)
    values.update(controlled)
    arguments = ["-D" + key + "=" + value for key, value in values.items()]
    if profile["mode"] == "installed":
        return ["mvn", "test"] + arguments
    return ["bash", str(ROOT / "ci-scripts/run-ios-simulator.sh")] + arguments


def execute(command, env, log_path, props):
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True)
        try:
            for line in process.stdout:
                safe_line = redact(line, env, props)
                log.write(safe_line)
                log.flush()
                print(safe_line, end="", flush=True)
            return process.wait()
        except KeyboardInterrupt:
            process.terminate()
            process.wait()
            raise


def generate_report(allure, results, report, history=None):
    if history:
        # Copy only Allure's history data into this new run. Never alter the old report.
        shutil.copytree(history, results[0] / "history")
    return subprocess.call([allure, "generate"] + [str(path) for path in results]
                           + ["-o", str(report)], cwd=ROOT)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", choices=list(PROFILES) + ["full"])
    parser.add_argument("--profiles", default="regression",
                        help="Comma-separated profiles for full; isolated profiles require explicit selection")
    parser.add_argument("--plan", action="store_true", help="Show selections and contracts without accessing accounts/devices")
    parser.add_argument("--check", action="store_true", help="Validate config, app, device and Appium without opening sessions")
    parser.add_argument("--results-root")
    parser.add_argument("--report", action="store_true", help="Generate one report from selected profiles and gates")
    parser.add_argument("--history-from", help="Previous Allure report with a history directory; requires --report")
    args, maven = parser.parse_known_args(argv)
    try:
        props = properties(maven)
        selected = args.profiles.split(",") if args.profile == "full" else [args.profile]
        if len(set(selected)) != len(selected) or any(name not in PROFILES for name in selected):
            raise PreflightError("Select unique known profiles using --profiles")
        if args.profile != "full" and args.profiles != "regression":
            raise PreflightError("--profiles is only valid for full")
        for name in selected:
            profile = PROFILES[name]
            print(name + ": " + profile["test"] + "; mode=" + profile["mode"]
                  + ("; UI preflight gate" if profile.get("gate") else ""), flush=True)
            if args.plan:
                print("  required: " + ", ".join(profile.get("required", ()))
                      + "; validator=" + profile.get("validator", "profile")
                      + "; fixtures=" + ", ".join(profile.get("files", ())))
        if args.plan:
            return 0
        history = None
        if args.history_from:
            history = Path(args.history_from).expanduser().resolve() / "history"
            if not args.report or not history.is_dir():
                raise PreflightError("--history-from requires --report and a previous report/history directory")
        app_path = props.get("ios.app") or os.environ.get("IOS_SIMULATOR_APP") or os.environ.get("IOS_APP")
        app, info = load_app(app_path) if app_path else (None, {})
        bundle_id = props.get("ios.bundleId") or os.environ.get("IOS_BUNDLE_ID") or info.get("CFBundleIdentifier")
        if not bundle_id:
            raise PreflightError("Supply IOS_SIMULATOR_APP or an explicit IOS_BUNDLE_ID for installed profiles")
        if info and bundle_id != info["CFBundleIdentifier"]:
            raise PreflightError("Configured bundle ID disagrees with the supplied app")
        info.setdefault("CFBundleIdentifier", bundle_id)
        variant = "female" if "her" in bundle_id.lower() else "male"
        requested_variant = os.environ.get("IOS_APP_VARIANT", variant)
        if requested_variant != variant:
            raise PreflightError("IOS_APP_VARIANT disagrees with the app bundle ID")
        env = environment_for(variant, os.environ, props)
        if app:
            env["IOS_SIMULATOR_APP"] = str(app)
        device = props.get("ios.udid") or env.get("IOS_SIMULATOR_UDID") or env.get("IOS_UDID") or "00CA21E8-4A92-4607-A941-E5FD2E29DAC5"
        simulator = bool(re.fullmatch(r"[A-Fa-f0-9-]{36}", device))
        if not simulator and any(PROFILES[name]["mode"] != "installed" for name in selected):
            raise PreflightError("Local .app profiles require a Simulator UDID")
        env["IOS_SIMULATOR_UDID"] = device
        errors = []
        for name in selected:
            try:
                validate_profile(name, props, env, info if app else {})
            except PreflightError as error:
                errors.append(str(error))
        if errors:
            raise PreflightError("\n".join(errors))
        allure = env.get("ALLURE_BIN") or shutil.which("allure")
        if args.report and (not allure or not os.access(allure, os.X_OK)):
            raise PreflightError("Configure an executable ALLURE_BIN before requesting --report")
        validate_runtime(props, env, device, simulator, bundle_id,
                         any(PROFILES[name]["mode"] == "installed" for name in selected))
        if args.check:
            print("Preflight passed; no Appium session was opened. Account state is verified by the UI gate during execution.")
            return 0
        run_root = Path(args.results_root).expanduser().resolve() if args.results_root else (
                ROOT / "target" / ("ios-profile-runs-" + datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + str(os.getpid())))
        if run_root.exists() and any(run_root.iterdir()):
            raise PreflightError("Results root must be new or empty; existing reports are preserved")
        run_root.mkdir(parents=True, exist_ok=True)
        git = subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, capture_output=True, text=True)
        git_sha = git.stdout.strip() if git.returncode == 0 else "unknown"
        dirty = subprocess.run(["git", "status", "--porcelain", "--", "src/test", "scripts", "ci-scripts", "pom.xml"],
                               cwd=ROOT, capture_output=True, text=True)
        env["COACH_RUN_GIT_DIRTY"] = "true" if dirty.stdout.strip() else "false"
        records = {name: {"status": "planned" if name in selected else "not-requested",
                          "test": profile["test"], "mode": profile["mode"]}
                   for name, profile in PROFILES.items()}
        summary = {"profiles": records, "selected": selected, "gitSha": git_sha,
                   "gitDirty": env["COACH_RUN_GIT_DIRTY"] == "true",
                   "device": device, "startedAt": datetime.now().astimezone().isoformat(),
                   "appVersion": info.get("CFBundleShortVersionString", "unknown"),
                   "appBuild": info.get("CFBundleVersion", "unknown"), "bundleId": bundle_id}
        results = []
        exit_code = 0
        def save_summary():
            (run_root / "run-summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
        save_summary()
        for name in selected:
            folder = run_root / name
            folder.mkdir()
            gate = PROFILES[name].get("gate")
            phases = []
            if PROFILES[name].get("configuration_gate"):
                phases.append(("configuration", PROFILES[name]["configuration_gate"]))
            if gate:
                phases.append(("preflight", gate))
            phases.append(("tests", None))
            for phase, selector in phases:
                result_dir = folder / phase / "allure-results"
                result_dir.mkdir(parents=True)
                results.append(result_dir)
                command = command_for(name, props, env, info, device, result_dir, git_sha, selector)
                command.extend(option for option in maven if not option.startswith("-D"))
                print("Running " + name + "/" + phase, flush=True)
                records[name]["status"] = "running-" + phase
                save_summary()
                try:
                    code = execute(command, env, folder / (phase + ".log"), props)
                except KeyboardInterrupt:
                    records[name]["status"] = "interrupted"
                    summary["exitCode"] = 130
                    save_summary()
                    raise
                records[name][phase + "Exit"] = code
                save_summary()
                if code:
                    records[name]["status"] = "blocked-by-precondition" if phase != "tests" else "failed"
                    exit_code = 1
                    break
            else:
                records[name]["status"] = "passed"
            save_summary()
            if records[name]["status"] == "blocked-by-precondition":
                for pending in selected:
                    if records[pending]["status"] == "planned":
                        records[pending]["status"] = "blocked-before-run"
                break
        summary["exitCode"] = exit_code
        summary["finishedAt"] = datetime.now().astimezone().isoformat()
        save_summary()
        print("Run summary: " + str(run_root / "run-summary.json"))
        if args.report:
            report = run_root / "allure-report"
            code = generate_report(allure, results, report, history)
            summary["reportExit"] = code
            if history:
                summary["historySource"] = str(history.parent)
            save_summary()
            if code:
                return exit_code or 2
            print("Serve the report over HTTP: python3 -m http.server 5051 --bind 127.0.0.1 --directory " + str(report))
        return exit_code
    except (PreflightError, OSError, ValueError, plistlib.InvalidFileException) as error:
        print("Preflight failed: " + redact(str(error), os.environ), file=sys.stderr)
        return 2


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        sys.exit(130)
