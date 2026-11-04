# Sparrow roadmap

Where Sparrow is going: a calm, modern player that feels light, uses the space well and keeps every choice switchable.
Items are grouped by priority. Each one says what it is, why it matters and how to build it, with pointers into the
skin SDK (`poweramp_skin_sdk/reference_resources` in [maxmpz/powerampapi](https://github.com/maxmpz/powerampapi)).

Effort: **S** = an hour or two, **M** = a session, **L** = several sessions.

## Design principles

1. **Content first.** The cover and the track are the hero. Everything else is quiet until touched.
2. **Big, easy targets.** Controls you use every day are large and spaced apart. Rare controls live in menus.
3. **One accent, used sparingly.** Color comes from the cover or the system, never from decoration.
4. **Even rhythm.** Consistent spacing between groups; no dead gaps, no crowding.
5. **Everything is an option.** Opinionated defaults, but each change can be switched off in the skin settings.

## Now playing

| Item | Why | How | Effort |
|---|---|---|---|
| ~~**Modern font for titles and lists**~~ | Done: Inter is the default through the Font option (`values/sparrow_font_inter.xml`, 61 text styles) | Remaining: Poweramp's tutorial help bubbles still use their handwritten font; find the style behind them | S |
| **Format line as an outlined pill** | Matches the reference; groups the technical details | `TopMetaInfoLayout` ignores backgrounds. Try a background on each `TopMetaInfoLabel` instead, or draw the pill as a separate view injected through `sparrow_extralayout_includes.xml` and attached to `meta_info_layout` | M |
| **Screen-size aware spacing** | The seekbar/controls layout uses fixed dp padding tuned for a ~915dp tall phone; short phones may crowd | Add `values-h700dp`, `values-h800dp` dimens for the padding and button offset in `Sparrow_Option_SeekbarTop`, and test with `adb shell wm size` | M |
| **Larger title without clipping** | Scaling the title (`Alt_ItemTrackTitle_scene_aa_scale`) clips long titles, so it was reverted | Override the title `TextAppearance` size for the main scene only (needs a scene-specific text style), or use autosize if Poweramp's FastTextView supports it | M |
| **Cover shadow and size option** | A slightly smaller cover with a soft shadow gives more air | `corners_aa_scene_aa` plus AA bounds styles (`TopAABounds`, `Alt_TopAABounds`); shadow via elevation if supported | M |
| **Edge-to-edge "blended" cover** | Very immersive; the cover melts into the background | Poweramp build 1035+ only. Expose `overlap_blended_layout` as an option with a `minSdk`-style build note; see `styles-blended.xml` | S |
| **Lyrics view polish** | Lyrics should read like a page, not a list | `styles-item-lyrics.xml` (17 styles): larger line spacing, dim non-current lines, Inter font | M |

## Color

| Item | Why | How | Effort |
|---|---|---|---|
| **Wallpaper accent (Android 12+)** | Personal color without extra setup | `values-v31` colors from `@android:color/system_accent1_*`, applied to seekbar progress, thumb, volume thumb and nav active icon. Option: Off / Wallpaper | S |
| **Cover-tinted background color** | Calm, flat color instead of blur when preferred | `aaSolidColorTarget` (lightVibrant, vibrant, darkVibrant, muted...) on the main `TopMilk` style. Option: Blur / Solid color | M |
| **Accent color picker** | Users who want a fixed brand color | A `radio` option with 6-8 swatches (labels can use HTML color spans, see the SDK readme) that overlays seekbar/thumb colors | S |
| **True black (AMOLED) dark mode** | Battery and contrast on OLED phones | Overlay setting `colorBgPrimary`, `colorBgNavbar`, list backgrounds to `#000` | S |
| **Better Sparrow Light** | Light mode currently uses Poweramp's grey `#d9d9d9` background | Set the light theme backgrounds to near-white (`#f7f7f8`) and recheck contrast of every element | M |

## Lists and library

| Item | Why | How | Effort |
|---|---|---|---|
| **Quieter list headers** | Header buttons (shuffle, play, search, SELECT, more) still feel heavy | `ItemTextHeader*Button` and `ItemHeader*Button` styles; consider icon-only buttons with no ring, or one pill holding several buttons | M |
| **Spacious song rows** | More breathing room, clearer hierarchy | `ItemTrack*` paddings and `ItemTrackTitle_Text` / `ItemTrackLine2_Text` / `ItemTrackMeta_Text` sizes; option Compact / Comfortable | M |
| **Album and artist grids** | Big covers make browsing a pleasure | Grid substyles (`*_ListSubstyle_grid*`), `corners_aa_*_grid`, label placement under covers without backgrounds | L |
| **Library category icons** | The colored round icons don't match the minimal look | Monochrome line icons replacing Poweramp's `*_colored` drawables (`all_songs_colored`, `albums_colored`...). First check whether they're exposed as theme attributes; if not, look for the item style that sets them | M |
| **Corner radius sliders** | Users love tuning roundness | `seekbar` options bound to `corners_aa_scene_aa`, `corners_aa_tracks`, `corners_aa_albums` (the sample's "Dynamic Corners" shows how) | S |
| **Text size slider** | Accessibility and taste | `seekbar` option with `textSizeMultiplierPref` on the main text styles | S |

## Other screens

| Item | Why | How | Effort |
|---|---|---|---|
| **Equalizer** | The EQ screen is fully stock today | `styles-eq.xml` (160 styles): knob colors, slider thumbs, preset chips. Match the seekbar and volume styling | L |
| **Search** | First screen many users open | `styles-settings-search.xml`, search bar corners (`corners_searchbar`), result rows | M |
| **Popups and dialogs** | Menus should feel like part of Sparrow | `styles-popup.xml`, `styles-dialogs.xml`: corners, background, divider, Inter font | M |
| **Mini player** | Lives on every list screen | `ItemMiniplayer*` styles, cover corners, button sizes | S |
| **Landscape** | Tablets and landscape phones | Test every option in landscape; add `values-land` overrides where layouts break | M |

## Beyond the main app

| Item | Why | How | Effort |
|---|---|---|---|
| **Poweramp Equalizer app support** | The same skin can theme the standalone Equalizer app | Equalizer-specific styles carry a `peq` prefix in the reference resources; test with the Equalizer app installed | M |
| **Bundled visualizer presets** | A matching, minimal visualizer | `.milk` files in `src/main/assets/milk_presets` named "name - author.milk", plus the `com.maxmpz.PowerampVisPresets` meta-data (see `poweramp_vis_presets_example`) | M |
| **Info screen redesign** | The launcher screen is the first impression on install | Show a live preview, a short options tour and links to GitHub and verification status | M |

## Release and quality

| Item | Why | How | Effort |
|---|---|---|---|
| **v0.2.0 release** | GitHub still serves v0.1.0 | Bump `versionCode`/`versionName`, tag, build release, publish with screenshots | S |
| **README screenshots** | People decide from images | Capture with `scripts/run-on-phone.sh <file>`, dark and light, store in `docs/screenshots/` | S |
| **Option combination checks** | Several options assume the Clean layout + Plain seekbar | Script that builds variants with different defaults and captures screenshots of each | M |
| **Shorter-screen test** | Catch crowding before users do | `adb shell wm size 1080x2000` (reset with `wm size reset`), screenshot, compare | S |
| **Changelog** | Users and reviewers see what changed | `CHANGELOG.md`, one section per release | S |
