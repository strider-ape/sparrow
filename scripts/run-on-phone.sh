#!/usr/bin/env bash
# Builds the debug skin, installs it on the connected phone, starts Poweramp with Sparrow applied
# (which also forces a skin reload) and optionally saves a screenshot.
#
# Usage: scripts/run-on-phone.sh [light] [screenshot.png]
set -euo pipefail
cd "$(dirname "$0")/.."

style=Sparrow
if [[ "${1:-}" == "light" ]]; then style=SparrowLight; shift; fi
shot="${1:-}"

./gradlew :skin:assembleDebug --console=plain -q
adb install -r skin/build/outputs/apk/debug/skin-debug.apk >/dev/null

# Resource ids are 0x80xxxxxx, which am passes as a signed 32-bit int.
hex=$(grep "style/$style = " skin/stable-ids.txt | sed 's/.*= 0x//')
id=$(( 16#$hex - 16#100000000 ))

adb shell am force-stop com.maxmpz.audioplayer
adb shell am start -n com.maxmpz.audioplayer/.StartupActivity \
	--es theme_pak io.github.strider_ape.sparrow --ei theme_id "$id" >/dev/null

if [[ -n "$shot" ]]; then
	sleep 4
	adb exec-out screencap -p > "$shot"
	echo "Saved $shot"
fi
