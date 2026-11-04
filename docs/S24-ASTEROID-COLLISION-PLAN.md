# Asteroid collisions — 1.5-s24.7

## Implementation plan

1. Give active asteroids solid contact with each other. Reuse the mass model
   from weapon impacts: larger rocks carry more momentum, metal is denser, and
   ice is lighter. Exchange equal and opposite impulses along the contact normal
   with a subdued rebound. Glancing contact transfers some sideways motion into
   spin through surface friction.
2. Separate overlapping rocks in proportion to inverse mass, without adding
   artificial velocity. Do not bounce an already separating pair again. Handle
   coincident centres and quiet, crowded contacts without invalid velocities.
3. Advance movement and magnetic attraction in short simulation steps. Shorten
   steps further for fast, small rocks so typical fast impacts cannot skip
   through one another. Keep a bounded step budget for extreme debug settings.
4. Use a reusable spatial grid to find nearby pairs, with each pair visited once
   per solver pass. Rebuild it between passes as overlapping rocks move. Keep
   collisions in both normal gameplay and the brief post-death simulation.
5. Preserve the centre joystick, larger ship, irregular meshes, ship ramming,
   weapon damage, cosmetic shards, pause behavior, offline play, and ad-free
   distribution. Rock-to-rock contact changes motion; it does not award kills,
   split rocks, or damage their health.
6. Test head-on and glancing impacts, mass differences, conservation/dissipation,
   overlap recovery, multi-rock chains, fast motion, frame-rate consistency,
   inactive entities, pause, and the actual game update. Build, sign, publish,
   and download back version code 12 with the matching source and update feed.

## Scope and validation

Collision uses circular bounds in the top-down simulation. The textured,
irregular 3D meshes remain the visual presentation; this is not a full 3D
polyhedron rigid-body solver. No physical Galaxy S24 is attached, so actual
device frame rate, heat, and touch feel still require an on-phone play test.

## Implemented design

`MovementSystem` runs the contact solver after each short movement step. Both
normal play and post-death play-out use that path. A zero, negative, or non-finite
time step is ignored, and the existing game pause returns before simulation.
Magnetic attraction is integrated per step, preserving its steering behavior.

`AsteroidContactSystem` uses a restitution of 0.55, suppressed below a relative
approach speed of 4 world units/second to settle quiet contacts. Friction is capped
at 0.18 times the normal impulse, with disc inertias for spin. Three solver passes
correct overlap by inverse mass, leaving 0.02 world units of tolerance. Position
correction does not alter velocity. Already separating pairs receive no impulse.

The spatial grid uses the largest active diameter as its cell width. Each centre
is inserted once; only its cell and eight neighbours are searched. Primitive
arrays are reused, including across pooled-entity reuse. The grid is rebuilt
between passes, and each pair is checked once per pass. Dense groups still need
more work than sparse fields; this is not a measured S24 performance guarantee.

Normal steps are at most 1/120 second. The fastest speed and smallest radius
shorten them further to limit travel to 35% of that radius per step. A 32-step
budget bounds cost for extreme debug values. This is discrete, substepped contact,
not exact swept collision for every grazing trajectory or arbitrary debug speed.

## Release validation

All six implementation steps are complete. The full Android suite passed with
2,505 tests passed, one skipped, and no failures or errors. Fourteen new tests
cover the contact physics and actual game integration. Three release-tooling
tests also passed. The release build and lint passed with 166 warnings and no
errors, unchanged from the previous release.

The APK is signed with the existing S24 certificate and aligned for installation.
See [S24-VALIDATION.json](S24-VALIDATION.json) for its version, SHA-256, certificate,
and validation scope. Publication checks download back the signed APK, matching
source archive, update manifest, and checksums from the public GitHub release.
The latest public-download report is tracked on the repository's main branch in
[S24-LIVE-RELEASE-VALIDATION.json](S24-LIVE-RELEASE-VALIDATION.json).
