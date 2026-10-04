# Shared debug keystore

`debug.keystore` is this machine's SDK-generated debug key, committed on
purpose. Every APK shipped so far (debug and release builds alike) carries
its signature, so Android treats a rebuild as an in-place upgrade.

Why it is versioned: CI runners generate a fresh debug keystore on every
run. Without this file, each CI-built APK would carry a new signature and
no GitHub release could ever upgrade the previous one (nor any locally
installed build). Pointing the `debug` signing config at this file gives
local builds and CI builds the same identity.

Not a secret: alias `androiddebugkey`, passwords `android` — the SDK's
published defaults. Anyone can sign with it; that has been true of every
shipped APK. Do NOT replace it with a real key as a side change: that is a
one-way door forcing every user through an uninstall with total data loss
(history, downloads, extension logins). See the signing note on the
`release` block in `app/build.gradle.kts`.
