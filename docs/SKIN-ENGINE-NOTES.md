# Poweramp skin engine notes

Practical lessons from building Sparrow, for whoever works on it next. The official reference is the
[skin sample readme](https://github.com/maxmpz/powerampapi/blob/master/poweramp_skin_sample/readme.md) and the
`poweramp_skin_sdk/reference_resources` folder. Clone the SDK repo next to this one to search it:

```bash
git clone --depth 1 https://github.com/maxmpz/powerampapi.git ../powerampapi
```

## Build and deploy

- `scripts/run-on-phone.sh [light] [screenshot.png]` builds the debug APK, installs it, applies the skin and optionally
  saves a screenshot. It applies through Sparrow's info screen (`--ez apply true`) because sending Poweramp's theme
  extras to an already open Poweramp is ignored, and force-stopping Poweramp interrupts playback.
- Both debug and release builds are signed with the release key when `keystore.properties` exists, so test builds count
  as verified once Poweramp allowlists the key.
- `skin/stable-ids.txt` is regenerated on every build (sorted) and must be committed: it keeps option ids stable so
  users' saved options survive updates. AGP passes its own `--emit-ids`, so the build copies AGP's output instead of
  emitting directly.
- Poweramp's public resources are downloaded at build time (pinned commit + SHA-256), never committed.

## How styling works

- A skin can't replace layouts. Everything is done by pointing Poweramp's theme attributes
  (`com.maxmpz.audioplayer:TopPlay`, `...:ItemTrackTitle_scene_aa`, ...) at styles that derive from Poweramp's own.
- **Always derive from the matching Poweramp style** (`parent="com.maxmpz.audioplayer:style/..."`). Copied styles break
  animations and drift when Poweramp updates.
- **Each skin variant needs its own `<skin>` entry**, with `lightMode`/`darkMode` pointing at the other, or Poweramp
  logs `no otherSkinInfo` and fails to load.

## Options and overlay order

- Options apply overlay styles on top of the base theme, **in the order they appear in `xml/sparrow_options.xml`**.
- Poweramp's layout overlays (alt layout, plain seekbar...) set many attributes. A base-theme override of those
  attributes is silently replaced, so **overrides of layout-specific attributes must live in an option declared after
  the option that sets them**. Example: `Sparrow_Option_SeekbarTop` comes after the seekbar option.
- Such overrides depend on the chosen layout and seekbar: `Sparrow_Option_LargeControls` derives from
  `PlainSeekbar_Alt_*` styles, which squeeze the wave seekbar and hide its skip buttons. Sparrow solves this by
  **routing**: the Plain-only options (seekbar position, control size, time backgrounds) set Poweramp's attributes to
  `?sparrowTop*` theme attributes. The base themes point those at Sparrow's styles, and the Wave and Static seekbar
  choices (`Sparrow_Seekbar_Wave`, `Sparrow_Seekbar_Static`) point them back at Poweramp's stock styles. Attribute
  references resolve when views inflate, so the declaration order of the options doesn't matter for them.
- `dependency="<key>"` did **not** switch an option off when the referenced option had its empty choice (tested with
  defaults on an unverified skin), so don't rely on it.
- The Classic layout was dropped: it overlays the title on the cover and runs the seekbar into the volume slider.
- Poweramp's `overlap_plain_seekbar` resolves to `ActivityTheme_Whitepro_PlainSeekbar` or, inside the Clean layout
  overlay, `..._PlainSeekbar_Alt`, and it sets the button styles directly, so Sparrow can't extend it with one style.
- `defaultValue` holds the overlap value of the default choice (`@style/...` or `@com.maxmpz.audioplayer:attr/...`).
- Options only work for **verified** skins. For an unverified skin the options page still appears and picking a choice
  reloads the skin, but Poweramp keeps drawing the defaults and the page shows the default again when reopened. So
  **the defaults are what everyone sees until verification**; test other choices by changing `defaultValue` and
  redeploying.

## Layout engine (FastLayout)

- Views are positioned with attach rules: `layout_attachTop/Bottom/Left/Right` take a sibling id, `parent`, `inset`,
  `@null` or a percentage like `50%`. `layout_attachBottomToBottom` aligns edges.
- `android:layout_gravity` often doesn't move a view that also has attach rules or scene params.
- **Scenes** (`scene_aa`, `scene_playing`, `scene_lib`, ...) restyle views per screen state. A view that looks right
  while paused can change while playing. Always test both. `*_scene_playing` attributes are separate overrides.
- The plain seekbar sits at the bottom of `waveseek_layout`, and the play buttons are centered in it. Sparrow moves the
  seekbar up with bottom padding on that container and moves the buttons down with a negative
  `PlainSeekbar_buttons_offset`. Both are fixed dp values, so they depend on screen height.
- Play buttons fill the container's height (minus padding) up to `layout_maxWidth/Height`, so padding the container
  also shrinks them. Sparrow's large buttons set an explicit `layout_height`.
- **Play/pause swap.** Poweramp crossfades the two buttons in place (the "more buttons" pause appears at once with
  `jumpVisibility`, the play fades out while shrinking to 0.75). With bare glyphs that drew a triangle and bars on top
  of each other. Sparrow keeps both at 88dp (so the skip buttons don't shift) and scales the hidden one to 0.5 with
  `android:scaleX/scaleY`, which scenes animate; FastLayout's own `scale` attribute didn't animate here.
- **Title size.** `android:textSize` in a `*_scene_aa` style is ignored. The player-screen title and artist sizes come
  from `Alt_ItemTrackTitle_scene_aa_scale` and `Alt_ItemTrackLine2_scene_aa_scale` (theme floats, 1 on most phones,
  applied to 16.5dp text); Sparrow uses 1.4 and 1.12. The larger title needs 3dp less left margin to line up with
  the artist line. Long titles still marquee within their bounds.

## Icons

- Poweramp's play, pause and skip icons are designed inside a circle, so the glyph only fills part of the button.
  Sparrow replaces the drawables (`main_play`, `main_rw`, `main_ff`, `pause_small_narrow`, `main_pro_pause`) with its own
  vectors that fill the button.
- With the Plain seekbar ("more buttons" mode), the playing-state pause uses `?main_pro_pause`, not
  `?pause_small_narrow`, and its size is capped separately.
- `drawableWidth/Height` didn't resize these icons; changing the drawable did.
- Nav bar icons are theme attributes (`nav_lists`, `nav_equ`, `nav_search`, `nav_hamburger`); a selector with
  `state_activated`, `state_pressed` and Poweramp's `w:state_toolbar_in_header` swaps outline and filled versions.
- Library category icons are theme attributes too (`all_songs`, `folders`, `albums`, ... 21 in total, see
  `default-styles.xml`). The list menu ("more") icon is set directly as `android:drawable` in its style.
- The quick buttons row (visualizer, sleep timer, repeat, shuffle) is built in code from the user's button
  configuration since build 1003; its icons are **not** exposed as theme attributes.
- Vector `fillColor` can reference Poweramp theme colors (`?com.maxmpz.audioplayer:attr/colorPlayButtonsTint`), so one
  drawable works for dark and light themes.

## Progress bar and volume styles

- The plain seekbar is an Android `SeekBar` (`PlainSeekbar` style), so its progress drawable, thumb and `splitTrack`
  are all skinnable. Theme colors (`colorSeekbarBg`, `colorSeekbarProgress`, `colorSeekbarThumb`) keep the Accent
  option working in every style.
  - A `<scale>` layer keeps a rounded fill rounded; a `<clip>` layer cuts a drawing (the Wave line's vector wave).
  - A selector around whole layer lists works as a progress drawable: the level passes through to the active list,
    so the bar can get thicker while pressed (Pill).
  - `splitTrack` cuts the track by the thumb's full bounds, so a slim handle inside a wider transparent box leaves a gap
    (Expressive). Turn it off when the thumb is hidden, or the fill gets a hole at its end.
  - Poweramp's `PlainSeekbar` has a ripple background that draws a grey band while touched; Sparrow sets it to `@null`.
- The volume slider (`DSPLinearKnob`) draws a value fill with `holeStartColor`/`Mid`/`End`, `holeColorWidth` and
  `holeColorRadius`, a halo with `holeGlow*`, starting at `holeColorStartPoint` (Poweramp's preamp style uses 0.5, the
  middle). The fill and thumb travel only when a `hole` drawable is set, and then **the view's side padding counts
  twice**: Sparrow's fill styles use 18dp padding for a track inset by 36dp.
- Option `icon`s show in a small square slot (about 30dp, cropped, not scaled), tinted with the text color, so preview
  icons are square and one color with alpha.

## Lyrics

- Synced lyrics use `ItemLyricsTitle` (other lines) and `ItemLyricsTitle_active` (the current line), with
  `_scene_small` and `_scene_zoomed` variants; unsynced lyrics use `_scene_unsync*` (already left aligned).
  `ItemLyricsTitle_Text` is the text appearance (22sp); Sparrow's Inter option doesn't cover it, so lyrics styles set
  the font themselves.
- Poweramp draws the other lines in a grey (`colorIconDisabled`) and dims them again with `lyricsPrevAlpha` and
  `lyricsNextAlpha` (the next lines fade gradually). Sparrow uses the full text color and lets those two set the fade.
- The current line zooms with `scale`/`layout_compensateScale` (pivot at the left edge); Spotlight uses a slight 1.06.
  Rows add 16dp of padding, so an 8sp margin lines lyrics up with the player's track info.
- **Spacing between lines vs inside a line.** The vertical margins of the line styles set the gap between lyric lines;
  `lineSpacingMultiplier` in the text appearance sets the leading inside a wrapped line. Keeping the first clearly
  larger (16sp + 16sp vs 0.95) makes a wrapped line read as one unit.
- **The header** (the current track above the lyrics) is a list header `lyricsHeaderSize` tall (theme dimension,
  Poweramp: 100dp; Sparrow: 124dp). Its cover (`ItemTrackAAImage_scene_lyrics`) is pinned top and bottom, so its
  margins position it inside that height: margins alone shrink the cover instead of moving the header.
- Before the first timed line (e.g. at 0:00) no line is current, so all lines show as upcoming.
- **Where the current line sits is fixed in Poweramp's code**, about a fifth of the way down the screen. A 300dp
  `lyricsHeaderSize` didn't move it (the list scrolled the header away to keep the line in the same spot), and no
  theme attribute controls it, so a skin can't center the current line vertically.
- The lyrics background is `aaEffectReadable` (a `MilkViewEffect`): a positive `aaBlurIntensity` overrides the user's
  brightness, and `aaSolidColorTarget` works there too.
- Word-by-word highlighting and blurring the other lines would need code.

## Fonts

- The Font option's base overlay (`values/sparrow_fonts.xml`) points about 60 Poweramp text appearance attributes at
  Sparrow styles that set `android:fontFamily="?sparrowFont"`. Each choice is a child style that only sets
  `sparrowFont`, so adding a font is one style and one `res/font` family. The lyrics styles read `?sparrowFont` too.
- `sparrowFont` accepts a family name as well: the System choice sets `sans-serif` and skips the overlay.
- Instrument Sans, Plus Jakarta Sans and Manrope are single variable font files from google/fonts; each weight is a
  `<font>` entry with `android:fontVariationSettings="'wght' N"` (API 26+, Sparrow's minimum). Inter is four static
  files.

## Injected views

- `TopContainer_extraLayout` is a slot for extra views on the main screen. Poweramp's play counter uses it when enabled
  in Poweramp's settings, so Sparrow's `sparrow_extralayout_includes.xml` includes both the counter (via
  `Sparrow_OptionCounter`) and the optional volume slider, using `<include android:layout="?attr" w:optional="true"/>`.
- The volume slider is `com.maxmpz.widget.player.DSPLinearKnob` with `orientation="horizontal"`, bound to
  `dsp_master_volume`, styled from `EqPreampLinearKnob`. `SceneParams` children control visibility per scene.
  Bind it like Poweramp's own knobs (`volume_panel.xml`): add `dspParamDef` (the real 0-100% range) and
  `enablingParam="dsp_master_volume_enabled"` (disabled state when Poweramp's volume control is off).
- `dsp_master_volume` mirrors Android's music volume (4/30 showed as 13% in Poweramp's own Volume knob and on the
  slider). The equalizer status line shows `NO DVC` when Direct Volume Control is off; the slider still works then.
- To test volume without changing it permanently, use `adb shell input keyevent KEYCODE_VOLUME_UP` / `_DOWN` while
  paused. `cmd media_session volume --set` does nothing while the stream is muted.

## Looks (separate skins)

- Unverified skins ignore option choices, but picking a **skin entry** always works. So each look is its own `<skin>` in
  `xml/skins.xml` with its own theme (`values/sparrow_looks.xml`), applied from Poweramp's skin list or from the app
  (`StartupActivity` with `theme_pak` and `theme_id` = the look's style id; ids that aren't skin entries fall back to the
  default skin).
- A look only sets Sparrow theme attributes; the options' default overlays read them. The Background option's default
  points `TopMilk` at `?sparrowBgColor`, so a look's `sparrowBgColor` decides its background.
- Looks have no light variant, so they don't follow the system's light mode.

## Background

- The main screen background is the `TopMilk` view (`style="?TopMilk"` in Poweramp's layout), only set by Poweramp's
  base theme, so an option can replace it in any order.
- `aaSolidColorTarget` (`darkVibrant`, `vibrant`, `muted`, `lightVibrant`, ...) fills it with one color from the
  cover's Android palette instead of the blur. When the palette has no such color it falls back to `colorAABgColor`
  (black in dark themes). On 10 consecutive library tracks, `darkVibrant` was missing on 5 (pastel and
  black-and-white covers), `vibrant` on 3 (covers with no strong color) and `muted` on none, but `muted` was grey on 5.
  `darkMuted` is often missing on colorful covers. There is no fallback chain between targets.
- `aaBlurIntensityMult` and `aaBlurSaturationMult` still apply to the solid color: intensity scales brightness and
  keeps the hue, saturation makes it bolder. `aaBlurDarken` clamps each channel (#609cb4 became #608080 at 0.5), which
  turns colors grey.
- Poweramp darkens the bottom of the main screen toward black in both themes, which suits the dark skin but not dark
  controls in the light skin.
- Options can point an attribute at `?attr/...`, so one option resolves to different styles in the dark and light
  skins (`sparrowBgColor` and friends).
- **Overlays above the background.** `TopContainer_extraLayout` is included last, so its views draw over the cover and
  controls. The background is a SurfaceView, which clears its area in the window when drawn. Sparrow's overlay
  (`layout/merge_sparrow_bg_overlay.xml`) uses `translationZ -4dp` and every Sparrow background style
  `translationZ -8dp` (`Sparrow_Bg`): lower Z draws first, so the order is background, overlay, then everything else.
  Without the background's Z, the SurfaceView erased the overlay. The Gradient and Aurora looks and Sparrow Light's
  white fade use it.
- Pure black (Onyx): any solid target with `aaBlurIntensityMult` 0; lists with `aaEffectList` darkened to 0.

## Nav bar

- `Navbar` is the bar's style on the player screen; the list screens use `Navbar_scene_navbar_*` scene styles. All of
  them take their side margins from `navbar_marginLR`, so set that attribute to change the width everywhere.
  The mini player lives inside the bar on list screens.
- **Keep one width on every screen.** When the width differs per screen, Poweramp animates only the bar's outline: the
  buttons jump straight to their final positions, so the last button pops in at the end of the animation. With the
  same width, only the height changes (to fit the mini player), and that animates smoothly.
- To check an animation, record it (`adb shell screenrecord --time-limit 7 /sdcard/x.mp4`, with `MSYS_NO_PATHCONV=1`
  in Git Bash) and tile the frames with `ffmpeg -vf "fps=20,crop=...,tile=3x5"`.
- There is no backdrop blur: a translucent bar shows list text behind it unblurred, so keep the fill around 92%.
- The glass recipe follows the Iconify app (`core/ui/components/bottomnavbar` in Mahmud0808/Iconify): surface variant
  at 92% (`system_neutral2_800` / `_100` on Android 12+), idle tabs at 35%, a 60% accent glow behind the current tab.
  Iconify's finger-following glow, elastic drag and haptics are Compose code; a skin can't reproduce them.
- `NavBarButton` backgrounds get `state_activated` for the current tab. A ripple without a mask is unbounded, but on
  `FastButton` it was not visible while pressing in screenshots; the pressed icon state does show.

## Debugging tricks

- **Tint to see bounds.** Temporarily give a container or button `android:background="#5500ff00"` to see its real size
  and position. This found both the controls-container layout and the icon-padding problem.
- **Check pixels, not impressions.** Subtle changes (a stroke color) are hard to see in screenshots; read pixel values
  (e.g. PowerShell `System.Drawing.Bitmap.GetPixel`) before and after.
- `adb logcat | grep SkinnableThemeManager` shows skin loading and failures.
- Some views ignore backgrounds (`TopMetaInfoLayout`); adding padding there hid the format line entirely.

## Known limitations

- Fixed dp spacing in the seekbar-position and control-size options (see the roadmap: screen-size dimens).
- Title scaling clips long titles; don't use `Alt_ItemTrackTitle_scene_aa_scale` for a bigger title.
- Full cover + Wave uses the Clean layout's wave container (`Alt_TopWaveseekLayout` instead of `Alt2_...`).
- `ItemTrackLine2_Text` is shared by the main screen and every list, so a font change to the artist line applies to both.
