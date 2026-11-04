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
- That makes such overrides depend on the chosen layout: `Sparrow_Option_LargeControls` derives from
  `PlainSeekbar_Alt_*` styles, so it's correct for Clean + Plain and only approximate for other combinations.
  The option summaries say so.
- `defaultValue` holds the overlap value of the default choice (`@style/...` or `@com.maxmpz.audioplayer:attr/...`).
- Options only persist, and the options page only appears, for **verified** skins. Unverified skins use the defaults.

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
- Several options assume the Clean layout with the Plain seekbar.
- `ItemTrackLine2_Text` is shared by the main screen and every list, so a font change to the artist line applies to both.
