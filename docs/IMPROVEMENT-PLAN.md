# S24 flight polish — version 1.5-s24.3

The focus of this release is readable combat, controllable steering, and a clear
way to pause and tune the game on a phone. This is a focused improvement pass,
not a claim that every game function has been redesigned or physically tested.

## Implemented

1. A 48 dp pause/settings button at the screen edge, positioned away from the HUD
   with cutout clearance. A native scrollable panel explains the controls and
   pauses the current scene while it is open. Apply & resume saves preferences.
2. Persistent Balanced (60 FPS cap) and Smooth (120 FPS cap) modes. Both game and
   hangar pacing respect the choice; battery saver still takes priority for the
   requested display mode. Actual FPS depends on device and workload.
3. Steering response from 0.7× to 1.6×, full speed available at every setting, and
   a floating joystick origin that follows long drags. Returning a drag to its
   origin and long holds no longer count as menu or double-tap-pause taps.
4. A restrained layered nebula backdrop, cached shader objects, and soft bright
   star halos. Backdrop detail and starfield motion can be disabled separately.
5. A steady low-hull edge warning at 25% health, leaving the center transparent.
   This warning does not flash. Story flashes and other existing game effects
   are not changed by the starfield-motion preference.
6. Robust star wrapping after large camera movements and safe zero-size surface
   handling. Textured ship rendering restores shared drawing state consistently.

## Validation

Test the actual settings dialog controls, saved preferences, joystick reversal,
full-speed movement at low sensitivity, tap rejection, frame caps, native Canvas
background/warning rendering, surface recovery, and the existing regression suite.
Inspect the native renderer preview. Verify release signing, application ID,
version code, and permissions. Publish the signed APK and matching complete source.

## Device checks still needed

No physical Galaxy S24 is attached. Sustained frame rate, thermals, battery drain,
Samsung firmware behavior, and subjective control feel need on-device playtesting.
The new settings let the player choose comfort and performance on their phone.
