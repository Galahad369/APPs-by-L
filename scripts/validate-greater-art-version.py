#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def expect(errors: list[str], text: str, pattern: str, expected: str, label: str) -> None:
    match = re.search(pattern, text, re.MULTILINE)
    if not match:
        fail(errors, f"{label}: expected field not found")
        return
    actual = match.group(1)
    if actual != expected:
        fail(errors, f"{label}: expected {expected!r}, found {actual!r}")


def main() -> int:
    errors: list[str] = []

    gradle = read("greater-art/app/build.gradle.kts")
    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
    version_code_match = re.search(r"versionCode\s*=\s*(\d+)", gradle)

    if not version_name_match:
        fail(errors, "Gradle: versionName not found")
    if not version_code_match:
        fail(errors, "Gradle: versionCode not found")
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    version = version_name_match.group(1)
    code = version_code_match.group(1)

    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        fail(errors, f"Gradle: versionName {version!r} is not major.minor.patch")
    if int(code) <= 0:
        fail(errors, f"Gradle: versionCode must be positive, found {code}")

    apk_rel = f"greater-art/releases/GreaterArt-{version}.apk"
    apk = ROOT / apk_rel
    if not apk.is_file():
        fail(errors, f"Artifact missing: {apk_rel}")
        apk_hash = None
    else:
        digest = hashlib.sha256()
        with apk.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
        apk_hash = digest.hexdigest()

    root_readme = read("README.md")
    expect(
        errors,
        root_readme,
        r"Current repository version:\s*\*\*([^*]+)\*\*",
        f"{version} (code {code})",
        "Root README version",
    )
    expected_root_link = f"[greater-art/releases/GreaterArt-{version}.apk](greater-art/releases/GreaterArt-{version}.apk)"
    if expected_root_link not in root_readme:
        fail(errors, f"Root README APK link must point to {apk_rel}")

    app_readme = read("greater-art/README.md")
    expect(errors, app_readme, r"^- Version:\s*\*\*([^*]+)\*\*", version, "Greater Art README version")
    expect(errors, app_readme, r"^- Version code:\s*\*\*([^*]+)\*\*", code, "Greater Art README version code")
    expect(errors, app_readme, r"^- APK:\s*\x60([^\x60]+)\x60", f"releases/GreaterArt-{version}.apk", "Greater Art README APK")
    if apk_hash:
        expect(errors, app_readme, r"^- APK SHA-256:\s*\x60([^\x60]+)\x60", apk_hash, "Greater Art README APK SHA-256")

    handoff = read("greater-art/HANDOFF.md")
    expect(
        errors,
        handoff,
        r"^\*\*Current version:\*\*\s*\x60([^\x60]+)\x60",
        f"{version} (code {code})",
        "HANDOFF current version",
    )
    expect(
        errors,
        handoff,
        r"^\*\*Latest APK:\*\*\s*\x60([^\x60]+)\x60",
        f"releases/GreaterArt-{version}.apk",
        "HANDOFF latest APK",
    )

    state_match = re.search(r"## Repository state\s*(.*?)(?=\n## |\n### )", handoff, re.DOTALL)
    if not state_match:
        fail(errors, "HANDOFF: Repository state section not found")
    else:
        state = state_match.group(1)
        expect(errors, state, r"^- Version:\s*\*\*([^*]+)\*\*", version, "HANDOFF repository-state version")
        expect(errors, state, r"^- Version code:\s*\*\*([^*]+)\*\*", code, "HANDOFF repository-state version code")
        expect(errors, state, r"^- APK:\s*\x60([^\x60]+)\x60", f"releases/GreaterArt-{version}.apk", "HANDOFF repository-state APK")
        if apk_hash:
            expect(errors, state, r"^- APK SHA-256:\s*\x60([^\x60]+)\x60", apk_hash, "HANDOFF repository-state APK SHA-256")

    landing = read("index.html")
    if f"GREATER ART · {version}" not in landing:
        fail(errors, f"index.html hero version must be {version}")
    if f'greater-art/releases/GreaterArt-{version}.apk' not in landing:
        fail(errors, f"index.html APK link must point to {apk_rel}")

    if errors:
        print(f"Version consistency check FAILED with {len(errors)} problem(s):", file=sys.stderr)
        for error in errors:
            print(f" - {error}", file=sys.stderr)
        return 1

    print(f"Version consistency OK: Greater Art {version} (code {code})")
    print(f"Artifact: {apk_rel}")
    if apk_hash:
        print(f"SHA-256: {apk_hash}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
