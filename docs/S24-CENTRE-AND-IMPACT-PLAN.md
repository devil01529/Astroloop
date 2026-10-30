# Centre controls, irregular asteroids, and impacts — 1.5-s24.6

## Requested changes and implementation

1. Put the joystick at the bottom centre by default, including on upgrades from
   the old left/right preference. Keep optional left/right positions. A second
   thumb already inside the pad can take over when the first lifts; unrelated
   touches cannot take over. The centre never follows the drag.
2. Increase the player hull from 1.25 to 1.75 times its original drawing size:
   40% larger than 1.5-s24.5. Keep the rounded shield and exhaust aligned with it.
   Match asteroid contact to the larger, rotated hull with a capsule bound.
3. Replace smooth ellipsoid asteroids with 12 asymmetric, dented, ridged meshes.
   Derive lighting from the actual faces, remove sphere-pole texture pinching,
   and sort visible faces by depth so dents do not show rear faces through them.
   Bake blended rock materials with padded face tiles once to avoid texture seams.
   Use fewer triangles for small rocks and reuse geometry/materials/buffers.
4. Transfer momentum on weapon hits according to direction, damage, and rock
   mass; off-centre impacts add spin. Ramming transfers momentum and destroys
   the asteroid. Keep contact damage, shields, evasion, and weapon health rules.
5. Destroyed asteroids disappear permanently. Replace the two live smaller
   asteroids with brief cosmetic shards: wedges, slabs, and splinters with their
   own meshes, broken faces, sizes, and spin. No fragment collisions or loot.
   Opposite ejection pairs inherit the parent's velocity; a fixed-capacity pool
   prevents an unbounded debris field during chain kills.
6. Preserve offline/ad-free play, saves, the signing identity, and GitHub updates.
   Build and publish version code 11 with the complete matching source.

## Validation

Test centre placement and safe insets, old-preference migration, deliberate
two-thumb handover, release/cancel/pause, projectile impulse direction and mass,
ram momentum and energy, real-game destruction, and shard lifetime/capacity.
Inspect native Android renderer previews of varied rock silhouettes and fracture
pieces at large and gameplay sizes. Run the full regression suite, release build,
and lint, then verify signing and download the public APK/source back.

This remains top-down 2.5D combat. Physics is an arcade impulse model, not a full
rigid-body simulator. Physical S24 touch feel, sustained FPS, and thermals cannot
be measured in this environment.

## Completed validation

All six implementation steps are complete. The full Android suite passed with
2,491 tests passed, one skipped, and no failures or errors. The release build and
lint passed; lint reported 166 warnings and no errors. Three release-tooling
tests also passed.

The [native renderer preview](images/s24-centre-fracture-preview.png) shows the
12 asymmetric rocks, independent fracture meshes, ship at gameplay drawing size,
and centre pad. It is a test scene, not a physical-phone screenshot. A separate
test exercised the actual GameSurface renderer at 1080 × 2400.

See [S24-VALIDATION.json](S24-VALIDATION.json) for the signed APK fingerprint,
checksum, version, and verification scope. Public release checks download back
the APK, corresponding source ZIP, update manifest, and checksums.
