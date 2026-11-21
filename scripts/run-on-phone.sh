#!/usr/bin/env bash
# Builds the debug skin, installs it on the connected phone, applies Sparrow in Poweramp
# (which also reloads the skin) and optionally saves a screenshot.
#
# Usage: scripts/run-on-phone.sh [light | <look style, e.g. SparrowGradient>] [screenshot.png]
# Set NO_BUILD=1 to reapply without building.
set -euo pipefail
cd "$(dirname "$0")/.."

light=false
style=""
if [[ "${1:-}" == "light" ]]; then light=true; shift
elif [[ "${1:-}" == Sparrow* ]]; then style="$1"; shift; fi
shot="${1:-}"

if [[ -z "${NO_BUILD:-}" ]]; then
	./gradlew :skin:assembleDebug --console=plain -q
	adb install -r skin/build/outputs/apk/debug/skin-debug.apk >/dev/null
fi

# Apply through Sparrow's info screen: Poweramp then starts inside Sparrow's task and reloads the skin,
# without force-stopping it (which would interrupt playback).
args=(--ez apply true --ez light "$light")
if [[ -n "$style" ]]; then args+=(--es style "$style"); fi
# NEW_TASK | CLEAR_TASK: start the info screen fresh even if it is already open, so it always applies
MSYS_NO_PATHCONV=1 adb shell am start -f 0x10008000 -n io.github.strider_ape.sparrow/.SkinInfoActivity "${args[@]}" >/dev/null

if [[ -n "$shot" ]]; then
	sleep 4
	adb exec-out screencap -p > "$shot"
	echo "Saved $shot"
fi
