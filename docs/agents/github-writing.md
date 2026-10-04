# GitHub writing conventions

All commits, PRs, and releases in this repo follow the same contract. Release-please reads commit subjects to cut releases, so the format below is required, not a suggestion.

## Commits

Format:

```
type(scope): short description
```

Rules:

- `type` is one of `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
- `scope` is lowercase and names the area (`feed`, `queue`, `deezer`, `player`, `quality`, `build`). Omit it only when no area fits.
- Description is lowercase, imperative mood, no trailing period. Keep it under 72 chars.
- One logical change per commit. Split refactors from features.
- Breaking changes add a `BREAKING CHANGE:` footer. That footer triggers a major bump.

Examples:

```
feat(queue): pin session user block on shuffle
fix(feed): swiped rows animate back instead of sticking right
refactor(feed): share and harden the undo paths
test(deezer): cover favorites card routing and next chain
docs(build): note personal evoe-f4fa4 Firebase project in E2
chore(quality): scope cyclonedx SBOM to shipped runtime
```

What release-please does with each type:

- `feat` triggers a minor bump.
- `fix` triggers a patch bump.
- `BREAKING CHANGE` footer triggers a major bump.
- `refactor`, `test`, `docs`, `chore` do not trigger a release on their own.

Write the changelog entry with the change, while the impact is fresh. Release-please collects subjects since the last tag. A vague subject produces a vague changelog line.

## Pull requests

PRs are in English. Title and body stay in English even when the surrounding discussion is in French.

Title rules:

- The title follows the commit format. It becomes the squash subject on `main`, so it must parse as a Conventional Commit.
- The `pr-title-check` workflow blocks merge when the title does not parse.

Body rules:

- Fill in `.github/pull_request_template.md`. Keep each section short.
- Link the issue (`Fixes #123`) when one exists.
- List manual test steps. Name the variant you built (`debug` or `release`).
- Confirm `detekt`, `test`, and `gitleaks protect` ran. Paste failures instead of describing them.
- Never reply to Codacy review comments. It is a bot. Reply only to the owner.

Public prose gate:

- Draft the title and body with the `human-writer` subagent.
- Run Slopless on the draft until it passes with no findings.
- Save the raw JSON under `.slopless/findings/`. Keep the filename timestamped.

Checks before requesting review:

- `./gradlew detekt` passes.
- `./gradlew test` passes.
- `gitleaks protect --redact --no-banner --staged` reports nothing new.
- No secrets, no `@Ignore`, no new `@Suppress`, no `TODO(` in the diff unless the issue tracks it.

Merge rules:

- Squash and merge only. Set the squash subject explicitly to the PR title.
- A PR stays open until the owner says to merge it. A green check is never consent.
- Delete the remote and local branch after the merge. Keep `main` linear.

## Releases

Release-please owns GitHub releases. It runs on every push to `main`.

Files:

- `release-please-config.json` holds the config.
- `.release-please-manifest.json` holds the last released version.
- `.github/workflows/release-please.yml` runs the action and opens the release PR.

APK version split:

- `versionCode` and `versionName` in `app/build.gradle.kts` derive from the git commit count (`3.1.NNNNN`). That stays as is. It keeps Play and sideload upgrades monotonic.
- The GitHub release version (semver in the tag, `vX.Y.Z`) is consumer-facing. It comes from release-please tags. The two numbers do not need to match.

Changelog:

- Release-please builds `CHANGELOG.md` from squash subjects since the last release.
- Group entries by impact for the reader. Subjects written per the commit rules above need no rewrite.
- Breaking changes ship with a migration note in the release body.

## Workflows

- `release-please.yml`: on push to `main`, opens or updates a release PR. Merging it creates the tag and the GitHub release.
- `release-apk.yml`: on release published, builds `:app:assembleRelease` and attaches the single `...-release.apk` to the release. Fails when the tag disagrees with `version.txt` or the APK lacks the `-release` marker.
- `pr-title-check.yml`: on PR open, edit, and sync, fails when the title does not parse as a Conventional Commit.
