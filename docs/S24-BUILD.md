# Astro Loop S24 custom build

Based on PubDeer/astro-loop commit de9720d0dfa3d92f9002d90327602b16e7dbcb68.
This is an unofficial modification, distributed under the original GPL-3.0-only
license. Original game, art, music, and authorship remain credited to PubDeer.

## Improvements

- Requests the highest available display mode up to 120 Hz at the current
  resolution; requests up to 60 Hz when Android battery saver is active at resume.
  Android and Samsung Game Booster may override the request.
- Limits the previously uncapped hangar/menu render loop to the display rate.
- Uses nanosecond frame budgets matching the display, capped at 120 FPS, instead
  of rounding every frame down to 8 milliseconds. This is a frame-rate target,
  not a guarantee of measured performance.
- Canceled touch gestures no longer select upgrades or menu actions.
- Lifting the steering finger transfers control to a remaining finger without
  jumping; multi-finger gestures do not become menu taps.
- Uses the final release position when distinguishing a tap from a drag.
- Clears held movement and queued touches when pausing. Losing window focus
  pauses active play, including the cabinet game.
- Separate application ID `com.astroloop.game.s24` and label `Astro Loop S24`.
  Installs alongside the original; save progress is separate.
- Documents and narrowly suppresses an inherited AndroidX lint restriction on
  the public Activity keyboard callback; dispatch behavior stays intact.
- No ads, analytics, or in-app purchases. Gameplay works offline. Version
  1.5-s24.4 adds internet access solely for player-requested GitHub update checks
  and downloads, plus permission to open Android's package installer.

## Build

Requires full JDK 17 or newer, Android SDK platform 36, build-tools 35.0.0.
Set `sdk.dir` in local.properties to your SDK directory, then run:

```sh
./gradlew :app:assembleGameRelease :app:testGameDebugUnitTest :app:lintGameRelease
```

The release APK is unsigned unless you provide your own keystore.properties
using the repository's template. A distributed custom APK is signed separately
with a local custom key; it does not use PubDeer's signing identity. Preserve
that key for compatible updates. Never include signing secrets in source bundles.

## Install on Galaxy S24

Download the APK, open it in My Files, and allow installation from that source
if Android asks. If Samsung Auto Blocker prevents installing downloaded APKs,
review its setting under Settings > Security and privacy > Auto Blocker.
Restore any installation settings you change after installation.

Open Astro Loop S24. Steer with one finger; weapons fire automatically.
Double-tap or use Back to pause. Use Samsung's Adaptive motion smoothness setting
if you want the display to allow 120 Hz. The game works without a connection.

## Validation limits

Automated tests and static checks are run in the build environment. No physical
Galaxy S24 is attached; actual frame rate, thermals, audio latency, and Samsung
firmware behavior require an on-device play test. No claim of physical-device
certification is made.

Initial build validation: 2433 tests passed, 1 skipped, no failures. Android lint completed with no errors and 151 warnings. See S24-VALIDATION.json for artifact details.

## Realistic artwork update — 1.5-s24.2

- Bundled realistic metal ship hulls with glass cockpits and detailed engines.
- Visible single-barrel, twin-barrel, and rail-cannon mounts; metallic cannon rounds.
- Cratered rock, iron-rich, and fractured asteroid textures with natural rocky
  edges sized to the unchanged collision radius. Mineral tinting and existing hazard
  indicators preserve the six asteroid types' visual cues.
- A single 1280 × 1280 atlas is decoded once, with per-thread reusable drawing
  state. No image downloads or new game permissions are needed.
- Version code 7 and the same custom signing key permit an in-place update of
  the previous S24 build. Keep the existing app installed to retain progress.
- New native Android graphics tests check transparent fades, collision bounds,
  unchanged asteroid state, and all three ship/gun families. A render preview
  is exported during the tests for visual inspection.

Latest validation: 2437 tests passed, one skipped, no failures. Android lint completed with no errors. The update uses the same signer as 1.5-s24.1. See S24-VALIDATION.json for details.

## Flight polish update — 1.5-s24.3

See [the improvement plan](IMPROVEMENT-PLAN.md) for the implemented changes, test scope, and on-device validation limits. Tap the right-edge pause/menu button for steering response, Balanced/Smooth frame rate, nebula detail, and starfield motion options. Version code 8 updates the existing S24 app without requiring an uninstall.

Latest validation: 2450 tests passed, one skipped, no failures. Android lint completed with no errors. The update uses the same signer as 1.5-s24.1. See S24-VALIDATION.json for details.

## GitHub updates — 1.5-s24.4

The right-edge pause/settings panel now includes Updates. It checks public releases
from [devil01529/Astroloop](https://github.com/devil01529/Astroloop), downloads only
after you choose Download, and verifies the APK before asking Android to install it.
The game remains paused while the update dialogs are open. Cancellation and failed
downloads leave the current installation and saves intact.

Install this APK over an earlier custom S24 version once; earlier versions do not
have an updater. Version code 9 and the original custom signing identity preserve
in-place update compatibility. Future releases must keep that identity and increase
the version code. See [in-app updates](IN-APP-UPDATES.md) for release instructions
and the separate status of optional GitHub Actions signing.
