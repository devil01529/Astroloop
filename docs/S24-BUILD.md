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
- The flight joystick defaults to the bottom centre inside the safe display area.
  A second thumb already inside the pad can take over when the first lifts;
  unrelated touches cannot steal steering or turn the gesture into a tap.
  Releasing both thumbs brakes. Left/right placement remains available.
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

Open Astro Loop S24. Drag the fixed centre pad with either thumb to steer; release
both to brake. Put your other thumb on the pad before lifting to switch hands.
Weapons fire automatically. Use the right-edge pause button or Back to pause;
double-tapping outside the steering pad also pauses. Use flight settings to move
the joystick to the right, change steering response, or turn 3D asteroids off.
Use Samsung's Adaptive motion smoothness setting
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

Validation for 1.5-s24.2: 2437 tests passed, one skipped, no failures. Android lint completed with no errors. The update uses the same signer as 1.5-s24.1.

## Flight polish update — 1.5-s24.3

See [the improvement plan](IMPROVEMENT-PLAN.md) for the implemented changes, test scope, and on-device validation limits. Tap the right-edge pause/menu button for steering response, Balanced/Smooth frame rate, nebula detail, and starfield motion options. Version code 8 updates the existing S24 app without requiring an uninstall.

Validation for 1.5-s24.3: 2450 tests passed, one skipped, no failures. Android lint completed with no errors. The update uses the same signer as 1.5-s24.1.

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

## Fixed flight and 3D asteroid update — 1.5-s24.5

The visible joystick now has a stable centre, a radial dead zone, proportional
speed, and a larger knob. Its ring stays the same size at every sensitivity;
reaching the edge always requests full speed. Touch flight accelerates and turns
more promptly, brakes on release, and keeps the original maximum speed.

The player hull is 25% larger. A rounded shield glow and oval engine exhaust
replace the old triangular overlays. The collision radius remains unchanged.
Asteroids use textured, lit 3D meshes with independent roll axes, so different
faces turn into view. Small rocks use fewer triangles; meshes, materials, and
drawing buffers are reused. Settings retain a simpler graphics option.

This is a 2.5D presentation improvement within the existing top-down game. It is
not a full free-flight 3D conversion. No physical S24 has been tested.

Version code 10 uses the same signing identity as the previous S24 releases.
On 1.5-s24.4, open the pause/settings panel and choose Updates. On earlier
versions, download and install the APK over your existing app without uninstalling.
See [the implemented plan](S24-FLIGHT-AND-3D-PLAN.md) and
[the release validation report](S24-VALIDATION.json).

## Centre controls and rock impacts — 1.5-s24.6

This release defaults existing installations to the centred joystick. Its new
position preference also offers left/right placement. A waiting thumb must have
started inside the pad and still be inside when the steering thumb lifts.

The hull is now 1.75 times its original drawing size, 40% larger than 1.5-s24.5.
Asteroid contact follows its rotated nose and wings using a capsule bound.
Twelve asymmetric asteroid meshes replace the smooth shells. Blended surface
materials are baked into padded face tiles once, then lit and depth-sorted during
rendering. Small rocks use fewer faces.

Projectile impacts transfer momentum according to rock mass and hit direction;
off-centre hits add spin. Ramming destroys the contacted rock after resolving
momentum and player damage, respecting evasion and invulnerability. Weapon kills
still require enough damage for the rock's health. Destruction produces separate
slabs, wedges, and splinters which fade within 1.5 seconds; these are cosmetic,
not smaller live asteroids, and cannot collide, split, or drop extra loot.

Version code 11 uses the existing signing key and public update feed. See the
[implemented plan](S24-CENTRE-AND-IMPACT-PLAN.md). Physics is an arcade impulse
model; no physical S24 measurements or full rigid-body simulation are claimed.
