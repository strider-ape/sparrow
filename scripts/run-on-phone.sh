#!/usr/bin/env bash
# Builds the debug skin, installs it on the connected phone, applies Sparrow in Poweramp
# (which also reloads the skin) and optionally saves a screenshot.
#
# Usage: scripts/run-on-phone.sh [light] [screenshot.png]
set -euo pipefail
cd "$(dirname "$0")/.."

light=false
if [[ "${1:-}" == "light" ]]; then light=true; shift; fi
shot="${1:-}"

./gradlew :skin:assembleDebug --console=plain -q
adb install -r skin/build/outputs/apk/debug/skin-debug.apk >/dev/null

# Apply through Sparrow's info screen: Poweramp then starts inside Sparrow's task and reloads the skin,
# without force-stopping it (which would interrupt playback).
adb shell am start -n io.github.strider_ape.sparrow/.SkinInfoActivity \
	--ez apply true --ez light "$light" >/dev/null

if [[ -n "$shot" ]]; then
	sleep 4
	adb exec-out screencap -p > "$shot"
	echo "Saved $shot"
fi
