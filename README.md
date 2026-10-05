# Evoe

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3d37c0bc97394647978e15fe7cb30070)](https://app.codacy.com/gh/Doggo785/evoe/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade) [![GitHub release](https://img.shields.io/github/v/release/Doggo785/evoe)](https://github.com/Doggo785/evoe/releases) [![Quality](https://github.com/Doggo785/evoe/actions/workflows/quality.yml/badge.svg)](https://github.com/Doggo785/evoe/actions/workflows/quality.yml) [![License: UPL](https://img.shields.io/badge/License-UPL-blue.svg)](LICENSE.md)

Evoe is an Android music player for daily listening. Install it, sign in to Deezer, and press play. It runs on your phone, in your car with Android Auto, and on your TV. Your queue survives restarts. Your history stays searchable.

> Evoe is a hobby project maintained by one person. It is not tied to Deezer or any other service.

---

## What it is

Evoe is a fork of [Gladix](https://github.com/rschwertley/gladix) by rschwertley, which is itself a fork of [Echo](https://github.com/brahmkshatriya/echo) by brahmkshatriya. It keeps Echo's extension model: the app hosts no content, extensions provide it. Deezer ships inside the app, so there is nothing extra to install. More extensions load at runtime as separate APKs.

Package: `dev.doggo785.evoe`

---

## What Evoe adds over Echo

### Android Auto

Browse tabs, artist pages, real search with voice input, a queue view, shuffle and repeat controls, and auto-pause on disconnect. Unavailable and region-locked tracks are skipped automatically, with a circuit breaker after three straight failures so a bad queue can't spin. Errors show as plain messages instead of silent failures.

### Android TV

A full-screen D-pad player, a mini-player bar, and focus routing that holds up across the nav rail and mixed-span grids. Deezer login on TV works through a pairing code and a companion web page, since the usual login flow is hard to use with a remote.

### Playback and queue

The queue is stored on disk and restored as-is - the same track, the same spot - across cold starts, process death and app updates. A buffering watchdog retries and then skips a stuck track rather than leaving the player wedged, with a grace window so a slow stream start is not read as a stall.

### History

Listening history with date sections, sort by date, title or artist, filtering by extension, and text or voice search. Tapping an entry plays that track and rebuilds its context - the playlist, album or radio station it came from.

### Interface

Full-screen album art with a slow pan, a structured Info tab with credits and technical detail, and compact context menus.

---

## Installing

Get the latest APK from [Releases](https://github.com/Doggo785/evoe/releases). Sideloaded copies check GitHub for new versions and offer the update in the app. Copies installed from a store update through that store.

Privacy policy: [privacy-policy.md](privacy-policy.md).

---

## Building

Standard Android Studio project. JDK 17.

```
./gradlew :app:assembleDebug        # dev build (applicationId suffix .debug)
./gradlew :app:assembleRelease      # APK
./gradlew :app:bundleRelease        # Play bundle
```

Firebase Crashlytics is wired in; `google-services.json` is gitignored, and the build works without it.

---

## Extensions

Extensions are separate APKs loaded at runtime. Deezer is bundled; others are installed by the user.

Two build checks guard the extension boundary, and both run on every shipped build:

- `verifyExtensionAbi` confirms R8 has not renamed or repackaged the classes extensions link against. An R8 change once broke every third-party extension at once, and this catches that at build time rather than in the field.
- `verifyCleanKotlinOutput` guards against stale inlined code after a public inline function changes.

---

## Contributing

Everyone can contribute. Open an issue to report a bug or suggest an idea, or open a pull request with a change. Use Conventional Commits in pull request titles. Say what changed and how you tested it.

---

## Credits

Built on [Echo](https://github.com/brahmkshatriya/echo) by [brahmkshatriya](https://github.com/brahmkshatriya). Extension architecture, core playback and much of the interface are theirs. This repo continues work started in Gladix by rschwertley.
