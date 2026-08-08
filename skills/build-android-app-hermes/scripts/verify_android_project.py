#!/usr/bin/env python3
"""Verify an Android application project and optionally run its build gates."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


TASKS = ("testDebugUnitTest", "lintDebug", "assembleDebug", "--no-daemon")
ANDROID_NAME = "{http://schemas.android.com/apk/res/android}name"


def fail(message: str) -> None:
    print(f"[FAIL] {message}")


def ok(message: str) -> None:
    print(f"[OK] {message}")


def find_manifest(root: Path) -> Path | None:
    candidates = sorted(root.glob("*/src/main/AndroidManifest.xml"))
    app_manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
    if app_manifest.is_file():
        return app_manifest
    return candidates[0] if candidates else None


def find_wrapper(root: Path) -> Path | None:
    names = ("gradlew.bat", "gradlew") if os.name == "nt" else ("gradlew", "gradlew.bat")
    for name in names:
        candidate = root / name
        if candidate.is_file():
            return candidate
    return None


def run_build(root: Path, wrapper: Path) -> bool:
    command = [str(wrapper), *TASKS]
    if os.name != "nt" and wrapper.name == "gradlew":
        command[0] = f"./{wrapper.name}"
    print("[RUN] " + " ".join(command))
    completed = subprocess.run(command, cwd=root, check=False)
    return completed.returncode == 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project_root", type=Path)
    parser.add_argument("--build", action="store_true", help="Run unit tests, lint, and debug assembly.")
    parser.add_argument(
        "--require-no-internet",
        action="store_true",
        help="Fail if the source manifest declares android.permission.INTERNET.",
    )
    args = parser.parse_args()

    root = args.project_root.expanduser().resolve()
    failures = 0
    if not root.is_dir():
        fail(f"Project root does not exist: {root}")
        return 2

    settings = [root / "settings.gradle.kts", root / "settings.gradle"]
    if any(path.is_file() for path in settings):
        ok("Gradle settings found")
    else:
        fail("No settings.gradle.kts or settings.gradle found")
        failures += 1

    manifest = find_manifest(root)
    if manifest:
        ok(f"Manifest found: {manifest.relative_to(root)}")
        if args.require_no_internet:
            try:
                tree = ET.parse(manifest)
            except ET.ParseError as error:
                fail(f"Manifest XML is invalid: {error}")
                failures += 1
            else:
                declared = {
                    node.attrib.get(ANDROID_NAME)
                    for tag in ("uses-permission", "uses-permission-sdk-23")
                    for node in tree.getroot().findall(tag)
                }
                if "android.permission.INTERNET" in declared:
                    fail("Source manifest declares android.permission.INTERNET")
                    failures += 1
                else:
                    ok("Source manifest does not declare android.permission.INTERNET")
    else:
        fail("No application AndroidManifest.xml found")
        failures += 1

    wrapper = find_wrapper(root)
    if wrapper:
        ok(f"Gradle wrapper found: {wrapper.name}")
    else:
        fail("No Gradle wrapper found")
        failures += 1

    if args.build and wrapper and not failures:
        if run_build(root, wrapper):
            ok("Gradle verification tasks passed")
        else:
            fail("Gradle verification tasks failed")
            failures += 1

    apks = sorted(root.glob("*/build/outputs/apk/**/*.apk"))
    if apks:
        for apk in apks:
            ok(f"APK found: {apk.relative_to(root)}")
    elif args.build and not failures:
        fail("Build passed but no APK was discovered")
        failures += 1
    else:
        print("[INFO] No built APK discovered; run with --build to create one")

    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
