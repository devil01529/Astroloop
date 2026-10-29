> **Astro Loop S24:** [Download the signed APK](https://github.com/devil01529/Astroloop/releases/latest/download/astro-loop-s24.apk) · [Complete release source](https://github.com/devil01529/Astroloop/releases/latest/download/astro-loop-s24-source.zip). Install this update-enabled version once, then open the right-edge pause/settings panel and tap **Updates**. Everyone you share it with uses the same public releases, without a GitHub account. See [how releases and updates work](docs/IN-APP-UPDATES.md).

> **Unofficial Galaxy S24 build:** This source includes realistic ship, asteroid, and cannon artwork, a pause/settings panel, adjustable steering and frame rate, layered backgrounds, low-hull feedback, and interruption pausing. See [build and installation notes](docs/S24-BUILD.md). The upstream links below refer to the original PubDeer release.

<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="120" alt="Astro Loop icon">

*PubDeer presents*

# Astro Loop

**A roguelike shooter caught in a loop you were never meant to escape.**

[**pubdeer.com**](https://pubdeer.com) · [watch the trailer](https://www.youtube.com/shorts/G5ii5txIdFI)

Get it on **[F-Droid](https://f-droid.org/packages/com.astroloop.game/)**, or grab the APK from [Releases](https://github.com/PubDeer/astro-loop/releases).

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="220" alt="Combat">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="220" alt="Choose an upgrade">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="220" alt="Black market">

</div>

> `DECRYPTING SIGNAL // ASTRO LOOP`
>
> Signal acquired. One ship, one pilot, a crew waiting to be recruited.
> The rest is out there in the dark.

## System readout

- **Simple to play.** Steer with a single finger. Your guns fire on their own. Easy to pick up, hard to put down.
- **Open source.** No black boxes. The whole game is open here on GitHub, GPLv3.
- **Plays offline.** Works with no signal at all. The whole game lives on your phone.
- **No ads, no tracking.** Gameplay works offline. The custom updater contacts GitHub only when you choose to check or download an update.
- **No camera, mic, location or contacts.** The custom updater uses internet access and asks Android to install a verified APK only with your approval.
- **Free forever.** No price, no in-app purchases, no pay-to-win. The whole game, free.

## Gameplay

- Fly out and survive endless waves of asteroids and enemies.
- Recruit a crew of pilots and unlock new ships to fly.
- Pick fresh weapons and upgrades every run.
- The difficulty never stops climbing. See how far you get.

## Building

Requires Android SDK 36, build-tools 35.0.0, and a full JDK 17 or newer.

```bash
./gradlew :app:assembleGameDebug
# output: app/build/outputs/apk/game/debug/app-game-debug.apk
./gradlew :app:assembleGameRelease :app:testGameDebugUnitTest :app:lintGameRelease
# unsigned release: app/build/outputs/apk/game/release/app-game-release-unsigned.apk
```

No game engine: rendering uses Android Canvas in Kotlin. This custom build adds bundled realistic sprite artwork to the original vector effects.

## Contributing

Report custom S24 build issues in [this repository](https://github.com/devil01529/Astroloop/issues).
The original game is by [PubDeer](https://github.com/PubDeer/astro-loop).
You can read, build, and modify the complete source under the GPL.

## License

Astro Loop is free software under the GNU General Public License v3.0
(`GPL-3.0-only`). See [LICENSE](LICENSE).

The bundled fonts (Exo 2, Orbitron) are under the SIL Open Font License 1.1.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Copyright (C) 2026 PubDeer

## Support

If you enjoy Astro Loop, you can support development on [GitHub Sponsors](https://github.com/sponsors/PubDeer) or 
[Ko-fi](https://ko-fi.com/pubdeer).

No cookies. No trackers. No nonsense.
