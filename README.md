# Sparrow

A minimal, sleek, modern skin for [Poweramp](https://powerampapp.com/), the Android music player.

Sparrow keeps the screen calm: large rounded cover art, clean typography, quiet controls and lots of breathing room. It follows your system's light or dark mode.

> **Status:** early development. The main player screen, nav bar and dark/light themes are styled; list screens are next.

## Options

Sparrow's options appear in Poweramp's theme settings once the skin is verified. Defaults are marked in bold.

| Option | Choices |
|---|---|
| Layout | Classic, **Clean** (cover on top, track info below), Full cover |
| Track info alignment | **Left**, Centered |
| Track info background | Shown, **Hidden** |
| Button backgrounds | Shown, **Hidden** |
| Seekbar | Wave, Static, **Plain** |
| Dot-matrix font | Off, **Times and format**, Plus artist line |
| Buttons on cover (rating, lyrics, menu) | Shown, **Hidden** |
| Extra skip buttons | Shown, **Hidden** |
| Seekbar position (Plain seekbar) | Bottom, **Under track info** |
| Control size (Plain seekbar) | Normal, **Large** |
| Time backgrounds (Plain seekbar) | Shown, **Hidden** |
| Volume slider | Hidden, **Shown** |
| More button (Clean layout) | Hidden, **By the title** |

## Requirements

- Poweramp v3, build 1031 or newer
- Android 8.0 or newer

## Install

Official signed releases will be published on the [Releases](https://github.com/strider-ape/sparrow/releases) page.

1. Install the APK.
2. Open **Sparrow** and tap **Apply Sparrow**, or pick it in Poweramp's theme settings.

Official releases are signed with this certificate (SHA-256):

```
1f4929856f114cd009e21cb80aaaae3e0908fbb5842286a38fafc80b7c5875a7
```

### About verification

Poweramp only fully trusts skins whose signing certificate is on its allowlist. An unverified skin still works, but it shows an `[unverified]` label, doesn't save its options, and is switched off 48 hours after you select it. The official Sparrow releases will be submitted for verification. Builds you make yourself are signed with your own key, so they stay unverified.

## Build from source

You need JDK 17 or newer and the Android SDK (platform 36).

```bash
./gradlew :skin:assembleDebug
```

The APK is written to `skin/build/outputs/apk/debug/`. The first build downloads Poweramp's public resources from the [official SDK repository](https://github.com/maxmpz/powerampapi) at a pinned commit and checks them by SHA-256. They are used only at build time and aren't part of this repository.

To install on a connected phone:

```bash
adb install -r skin/build/outputs/apk/debug/skin-debug.apk
```

Then open Sparrow and tap **Apply Sparrow**. This also forces Poweramp to reload the skin, so use it after every new build.

### Release signing

Create `keystore.properties` in the project root (it's ignored by git):

```properties
storeFile=path/to/sparrow-release.jks
storePassword=...
keyAlias=sparrow
keyPassword=...
```

When this file exists, both debug and release builds use the release key, so test builds are verified too.

## How it works

A Poweramp skin is an APK of Android resources. Poweramp reads `skin/src/main/res/xml/skins.xml` to find the skin's themes, then applies styles that override its own (`com.maxmpz.audioplayer:style/...`). Skins can't replace Poweramp's layouts, so everything is done through styles, colors, shapes and fonts.

| Path | Contents |
|---|---|
| `skin/src/main/res/xml/skins.xml` | Skin entries and, later, their options |
| `skin/src/main/res/values/sparrow_styles.xml` | The Sparrow themes |
| `skin/src/main/kotlin/.../SkinInfoActivity.kt` | The small launcher screen |

## License

Sparrow is free software, licensed under the [GNU General Public License v3.0](LICENSE).

The [Doto](https://github.com/oliverlalan/Doto) dot-matrix font is bundled under the SIL Open Font License 1.1 ([licenses/OFL-Doto.txt](licenses/OFL-Doto.txt)).

The build setup follows the official Poweramp skin sample (Apache License 2.0). Poweramp and its resources belong to their owners; Sparrow is an independent project and isn't affiliated with Poweramp.
