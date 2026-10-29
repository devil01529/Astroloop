# GitHub releases and in-app updates

Repository: https://github.com/devil01529/Astroloop

## Players

Install the first update-enabled APK, version 1.5-s24.4, manually over your existing
Astro Loop S24 installation. Keep the existing app installed to retain progress.
Choose **Updates** in the right-edge pause/settings panel. If a newer version is
available, choose **Download**, then **Install**. Android asks for installation
approval; on Android 8+ it first asks you to allow installs from Astro Loop S24.
Samsung Auto Blocker may independently prevent installation.

Everyone you share the APK with uses the same public release channel. No GitHub
account, token, payment, or subscription is required. Updating never grants players
write access to this repository. A canceled or failed download leaves the current
installation and saves intact.

Gameplay remains offline and ad-free. Internet access is used only when the player
checks for or downloads an update. There is no background polling, silent install,
advertising SDK, or analytics. The game stays paused while update dialogs are open.

The updater reads:
https://github.com/devil01529/Astroloop/releases/latest/download/update.json

Before installation, it checks the APK's declared length, SHA-256, application ID,
higher version code, and signing certificate against the installed app. It rejects
foreign repositories and non-HTTPS asset URLs. The file is shared with Android's
installer through a restricted FileProvider, without storage permissions.

## Publishing future updates

Changing the source does not update installed games. A signed GitHub release with
the four assets below is required. Players then choose Updates inside the app.

1. Increase the game flavor's `versionCode` and `versionNameSuffix` in
   `app/build.gradle.kts`. Keep application ID `com.astroloop.game.s24`.
2. Build, test, and lint the game. Sign the release APK using the **existing S24
   signing key**, alias `astro-loop-s24`; a new key cannot update existing copies.
3. Commit the complete source and tag it with `v` followed by the app version name,
   such as `v1.5-s24.4`. Never reuse or overwrite a published version tag.
4. Use `scripts/release_manifest.py` to generate `update.json` from the signed APK
   and Gradle's `output-metadata.json`. Run `scripts/check_release_version.py`
   before publication; it rejects versions that existing installations would ignore.
5. Publish a normal (not draft or prerelease) GitHub release marked Latest, containing
   `astro-loop-s24.apk`, `update.json`, `astro-loop-s24-source.zip`, and `SHA256SUMS.txt`.
   Confirm both the latest-manifest URL and the APK download work.

The source ZIP must correspond to the release tag and retain LICENSE and attribution.
Never commit signing keys, passwords, or tokens, or attach them to a public release.
Keep a private backup of the existing signing key: losing it breaks update continuity.

## Optional GitHub Actions signing

The supplied `Signed Android release` workflow can build, test, sign, and publish
future tagged releases. **Cloud signing is not configured:** the current ChatGPT
GitHub integration rejects Actions secret access with HTTP 403. The initial release
is built and signed in the build environment and published directly.

To enable cloud signing, a repository owner must securely configure these encrypted
Actions repository secrets using the existing private signing material:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of the existing S24 JKS.
- `ANDROID_KEYSTORE_PASSWORD`: its store password, also used for the key password.

Then set repository Actions variable `S24_SIGNING_CONFIGURED` to `true`. Until then,
the workflow's release job is deliberately disabled. The certificate's public SHA-256
fingerprint is in `scripts/signing-certificate.sha256`; the workflow checks it before
publishing. No private signing material is included in this repository or source ZIP.

## Validation limits

Automated checks cover updater dialogs, permission and installer handoff, checksum,
package and signing identity rejection, cancellation, offline retry, and the existing
game regression suite. Release validation checks the actual signed APK and public
download endpoint. These do not establish physical Galaxy S24 installation,
Samsung-specific settings behavior, measured FPS, or thermals. No phone is attached.
