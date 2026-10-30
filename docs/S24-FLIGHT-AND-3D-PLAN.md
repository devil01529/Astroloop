# S24 flight and 3D presentation plan — 1.5-s24.5

## Player problems

The textured ship still has a legacy triangular shield underlay. Its silhouette is
small on a tall phone display. The relative joystick moves its centre during a
long drag, making steering difficult to repeat. Asteroid artwork rotates in the
screen plane, so rocks look like spinning cut-outs rather than tumbling objects.

## This release

1. Replace the angular player shield with a restrained rounded glow. Enlarge the
   player hull by 25% for readability, retaining the established collision radius
   and progression balance. Align the exhaust and damage feedback with the larger hull.
2. Add a fixed, always-visible flight joystick anchored inside the screen's safe
   area, with a left/right preference, larger thumb knob, radial dead zone, smooth
   proportional response, and full speed at the ring edge at every sensitivity.
   Touches outside the pad do not steer. A second finger cannot steal steering;
   pad releases cannot accidentally select menus or trigger double-tap pause.
3. Improve touch-flight acceleration, reversal, and braking without increasing
   top speed. Keep keyboard/gamepad control, menu taps, cutscenes, and interruption
   pausing compatible. Reset held input when switching scenes, resizing, or pausing.
4. Render textured, lit 3D asteroid meshes that tumble around multiple axes.
   Their rotation advances with simulation time and pauses with the game. Reuse
   mesh/material data, batch each object's visible faces, and use simpler geometry
   for small rocks. Offer a lower-cost graphics option in settings.
5. Preserve offline, ad-free gameplay, all weapons/hazards, saves, and the existing
   signing key. Release version code 10 through the working GitHub update channel.

## 3D scope

The asteroid presentation uses projected 3D geometry and lighting in the current
native renderer. The game remains a top-down shooter: this is a 2.5D improvement,
not a claim of a full free-flight 3D engine or a replacement of every story scene.

## Verification before publication

Exercise fixed-stick anchoring and screen insets, handedness, centre dead zone,
edge speed at all sensitivities, pointer ownership, interruption resets, and menu
and pause transitions. Check touch handling at different time steps. Render native
Android previews to inspect the larger ship, shield, joystick, and multi-axis rock
poses. Verify visible meshes stay inside their collision bounds and do not change
simulation state. Run the complete game suite, release build, and lint; verify the
same APK signer and increasing version; publish and download back the APK/source
and update manifest. Measure only environment-supported rendering, without claiming
physical Galaxy S24 FPS, thermals, or touch validation.

## Implemented and checked

All five release steps above are implemented in version 1.5-s24.5 (version code
10). The complete Android suite passed: 2,477 passed, one skipped, no failures or
errors. The three release-tooling tests passed. The release build and Android
lint completed successfully with no lint errors.

[Native renderer preview](images/s24-flight-3d-preview.png): an illustrative scene
drawn by the actual Android renderer, showing multiple roll poses, the larger
ship, rounded shield/exhaust, and idle fixed joystick. This is not a phone capture.

The signed artifact's checksum, signer, lint totals, and detailed validation scope
are recorded in [S24-VALIDATION.json](S24-VALIDATION.json). Publication checks
download the APK and matching complete source back from the public release.

Physical S24 touch feel, sustained frame rate, thermals, and Samsung installer
behavior remain device checks. A full free-flight 3D engine is outside this
release; the completed 3D work is the multi-axis asteroid presentation described
above.
