# Private owner menu — 1.5-s24.8

## Implemented plan

1. Add Owner access to Pilot settings, reachable from the right-edge pause panel.
2. Generate an installation key in Android Keystore only when activation is
   requested. Show its SHA-256 public-key fingerprint as the installation code.
3. Accept only an ECDSA activation signature from the private owner issuer key,
   over the application-specific prefix and that installation code. Ship only
   the public verification key. Restore authorization on app startup only when
   the signature and locally retained Keystore identity match.
4. Require Android's screen-lock confirmation each time the activated menu opens.
   Denial or cancellation leaves it locked. Close the menu on backgrounding.
5. Add `/money unlimited` and `/money normal`. Unlimited mode presents a
   replenishing 1,000,000,000-yen wallet and ignores spending/earning writes while
   enabled. Disable to restore the saved normal balance. Purchases and unlocks
   made while enabled remain. Normal earnings saturate safely at the integer
   limit. Refresh the hangar balance when closing settings.
6. Test issuer/device mismatch, tampering, invalid input, fail-closed authorization,
   repeated purchases, balance restoration, integer overflow, and menu commands.
   Build and sign version code 13 with the existing app identity and update feed.

## Activation on your S24

Install the APK over the previous S24 build. Open Pilot settings > Owner access,
tap Copy installation code, and send that code in the original build chat.
The code identifies the app installation, not your phone number or password.
The builder issues an activation signed with the retained private issuer key.
Paste it into Owner access and tap Activate. Then open Owner access again,
confirm your phone's screen lock, and run `/money unlimited`.

No activation has been pre-issued: the actual phone's installation code is
required. Anyone with the APK can see the activation screen, but an ordinary
recipient cannot sign a grant or reuse your grant on their own installation.
Do not send your phone PIN, password, or biometric information to the builder.

Gameplay, grant verification, and the activated menu work offline. Updates keep
the installation key; uninstalling or clearing app data can require activation
again. Copying preferences or restoring a backup to another phone does not copy
the Keystore private key, so owner access remains locked there.

## Security and ownership

The issuer private key is separate from the APK signing key. Neither belongs in
GitHub, APK assets, source archives, or a publicly downloadable release. The
repository includes `scripts/issue_owner_activation.py`, which requires the
privately held issuer PEM and a specific installation code. Preserve both private
keys outside the repository for future updates and owner reactivation.

The screen lock protects menu access on the activated phone; unlimited money,
when enabled, remains active for anyone you let play on that unlocked phone.
This protects the original signed APK on an ordinary Android installation. An
open-source offline app cannot prevent somebody from modifying their own copy,
or guarantee protection against a rooted/compromised device.

No physical S24 is attached. Android Keystore and the real Samsung credential
screen still require the owner's installation/activation test. Automated tests
cover signature verification, currency behavior, and dialog commands.

## Automated release checks

2,511 Android tests passed, one skipped, zero failures/errors. Three release-tooling tests passed. Release build and lint passed with no errors (169 warnings). The signed APK uses the existing S24 certificate. Actual owner activation remains pending the phone's installation code.
