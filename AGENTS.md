# Evoe

Android music player, fork of [Echo](https://github.com/brahmkshatriya/echo) with the Deezer extension bundled. Extension-based: the app knows no particular service, extensions supply content. Solo project, no external contributions (see `README.md`).

`applicationId` is `dev.doggo785.evoe`. Kotlin namespaces are inherited from upstream (`dev.brahmkshatriya.echo`) — do not "fix" them. The README still says "Gladix" / `dev.rschwertley.gladix` in places; the Gradle files are the source of truth.

## Project map

| Module | Path | What it is |
| --- | --- | --- |
| `:app` | `app/` | The player: Media3 playback, Android Auto, Android TV, history, downloads, runtime extension loading. Namespace `dev.brahmkshatriya.echo`. |
| `:common` | `common/` | KMP library (android + jvm targets, `commonMain` only). The extension ABI contract: `clients/`, `models/`, `settings/`, `providers/`. Dependencies are `api()`-exposed so extensions inherit them. |
| `:deezer-extension` | `deezer-extension/ext/` | Plain Kotlin/JVM + shadow. Bundled Deezer extension, ships as a `.eapk` shadowJar. |

`settings.gradle.kts` remaps `:deezer-extension` to `deezer-extension/ext/`. The outer `deezer-extension/*.gradle.kts`, `deezer-extension/app/`, and the nested `settings.gradle.kts` are the old standalone project shell: dead, not part of this build. Same for root `index.html` / `script.js` / `styles.css` (an old JetBrains inspection report). `pair.html` is the TV pairing companion page.

`rootProject.name = "Echo"` is stale too. Nothing to fix, just don't trust these names when searching.

## Tech stack

- Kotlin 2.4.10, AGP 9.3.2, JDK 17 (`jvmToolchain(17)`), Gradle configuration cache on
- compileSdk 37, minSdk 24, targetSdk 37
- UI: XML layouts + ViewBinding + Fragments. No Compose, no DataBinding.
- DI: Koin 4.2.2. Playback: Media3/ExoPlayer. DB: Room (`androidx.room3`). Lists: Paging3. Images: Coil3. HTTP: OkHttp. Serialization: kotlinx.serialization.
- Firebase Crashlytics optional: `google-services.json` is gitignored, build works without it.
- Static analysis: Detekt 1.23.8, default config (no `detekt.yml`), PSI-only, applied to every module.

## Commands

```
./gradlew :app:assembleDebug        # dev build (applicationId suffix .debug)
./gradlew :app:assembleRelease      # R8 minified APK
./gradlew :app:bundleRelease        # Play bundle
./gradlew detekt                    # all modules; or :deezer-extension:detekt for one
./gradlew test                      # unit tests (app + deezer-extension)
./gradlew :common:updateKotlinAbi   # refresh the committed ABI dump after an intended API change
```

CI (`.github/workflows/quality.yml`) runs `detekt`, `test` + kover, `:common:checkKotlinAbi`, gitleaks-with-baseline, and the osv SBOM gate on PRs and `main`. There is no pre-commit hook: run `detekt` and `test` locally before saying a change is done. Android Lint is `abortOnError = false` and `:common` / `:deezer-extension` have no Lint task at all, so Lint proves nothing here.

Read `CONSTRAINTS.md` before writing code. Do not weaken it to make a change pass.

## Code conventions

- **Architecture**: MVVM. Fragment + `ViewModel` (`viewModelScope`) + thin repository. No Presenters, no other UI framework.
- **DI**: every Koin module is declared in `app/src/main/java/dev/brahmkshatriya/echo/di/DI.kt` (`single`, `singleOf`, `viewModelOf`, `workerOf`, `includes`). Consume with `by viewModel()`, `by inject()`. Startup lives in `MainApplication.kt`.
- **Async**: `StateFlow` / `Flow`, `flowOn`, `stateIn(WhileSubscribed)`. Paged lists go through `ui/common/PagedSource.kt` + a `PagingDataAdapter`.
- **Screens**: `Fragment(R.layout.x)` or `XBinding.inflate`, then `RecyclerView` / `ViewPager2` / Material components.
- **Strings**: always `getString(R.string.*)`, never hardcoded user-facing text.
- **Logging**: `android.util.Log` with a tag. No Timber.
- **Errors**: `runCatching` + `Result`, `AppException` hierarchy in the app, `ClientException` at the extension boundary. No bare coroutine scopes.
- **`@OptIn`** at the use site only, never blanket at file or module level.
- **Tests**: JUnit 4 only — no MockK, Mockito, Robolectric, or Espresso in this repo. Locations: `app/src/test/java/...` and `deezer-extension/ext/src/test/kotlin/...`. Keep new tests pure-JVM: extract the decision logic into a plain function or class so it can be tested without Android.

Copy these before inventing a pattern: screen → `ui/history/HistoryFragment.kt` + `HistoryViewModel.kt` + `history/HistoryRepository.kt`; paged list → `ui/common/PagedSource.kt`; DI registration → `di/DI.kt`.

## Boundaries

- Never change `:common`'s public API without running `./gradlew :common:updateKotlinAbi` and committing the diff to `common/api/jvm/common.api` alongside the change. `:common:checkKotlinAbi` fails the build otherwise, and it gates every minified `:app` build.
- `app/proguard-rules.pro` keeps one `# anchor:` comment per keep rule. Keep them in parity: `verifyExtensionAbi` fails release builds when R8 renames or repackages an anchor class.
- Coil must stay at or above 3.6.0. Below that R8 drops `GenericViewTarget` default-method side effects and album art renders blank in minified builds only. The long comment in `gradle/libs.versions.toml` explains why this is hard to believe.
- Don't rewire `verifyExtensionAbi`, `verifyCleanKotlinOutput`, or `checkKotlinAbi` unless the change *is* about those guards.
- `release` is deliberately signed with the debug keystore for in-place upgrade continuity. Leave it.
- New dependencies go through `gradle/libs.versions.toml`, not hardcoded coordinates.
- Build types `nightly` and `stable` are inherited from upstream and never built here; `debug` and `release` are the live ones.

## Git workflow (GitHub flow)

- Branch off `main`, open a PR, then delete the remote and local branch after the merge. `main` stays linear.
- A PR is ready but left open until the user says to merge it: only an explicit go-ahead in the current conversation authorizes merging, never silence, a passing check, or "make a clean PR". Squash and merge, with the squash subject set explicitly.
- Commits and PR titles follow Conventional Commits. Release-please reads them, so the format is mandatory. See `docs/agents/github-writing.md`.
- Every PR is in English and passes the human-writer + Slopless gate before opening. See `docs/agents/github-writing.md`.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles, each label string equal to its name. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` at the repo root plus `docs/adr/`. See `docs/agents/domain.md`.
