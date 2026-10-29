# Setup and development

Everything you need to build, run, test and package ClipSave from source. For how the app works inside, see
[docs/architecture.md](docs/architecture.md); for publishing a release, see [docs/releasing.md](docs/releasing.md).

## Requirements

* JDK 17+ (Android Studio's bundled JDK works)
* Android SDK with API 36 (Android Studio installs it)
* A device or emulator on Android 10+ (minSdk 29)

## Build and run

```bash
git clone git@github.com:sahedulislamrony/ClipSave.git
cd ClipSave
git switch dev                       # day-to-day work happens on dev; see CONTRIBUTING.md
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # macOS example
./gradlew :app:installDebug          # needs a device/emulator
```

Debug APKs are written per ABI to `app/build/outputs/apk/debug/` (yt-dlp's Python + ffmpeg make the universal
APK ~200 MB; each per-ABI APK is roughly a third of that).

| Command | What it does |
|---|---|
| `./gradlew :app:installDebug` | Build and install the debug app |
| `./gradlew :app:assembleDebug` | Debug APKs (one per ABI plus universal) |
| `./gradlew :app:lintDebug` | Android lint (also run by CI) |
| `./test.sh` | Unit tests (also run by CI); `--ci` runs build + lint + tests like CI |
| `./build.sh` | Signed release APKs into `outputs/` |

## Tests

```bash
./test.sh                       # unit tests, no device needed (~20 s)
./test.sh --ci                  # everything pull requests run: debug build + lint + unit tests
./test.sh --filter UrlParser    # only tests whose class or name contains the text
./gradlew :app:testDebugUnitTest   # the plain Gradle equivalent
```

* The script prints a pass/fail summary and exits non-zero on failure, so it works in hooks and scripts. The HTML
  report is at `app/build/reports/tests/testDebugUnitTest/index.html`.
* Unit tests live in `app/src/test/java/com/sahed/clipsave/` (JUnit 4) and cover the logic that doesn't need
  Android: link parsing, platform detection, quality selection, playlist numbering, file-name safety and the
  download-history format. UI, WorkManager and yt-dlp are tested by running the app (see below).
* Pull requests into `dev` and `main` run these same tests (**Unit tests** check). A fix should come with a test
  that fails without it.
* Keep new logic in plain Kotlin where possible (no `Context`), so it can be tested like this.

## Test the share flow without another app

```bash
adb shell am start -n com.sahed.clipsave/.ui.ShareReceiverActivity \
  -a android.intent.action.SEND -t text/plain \
  --es android.intent.extra.TEXT "'https://www.instagram.com/reel/XXXX/'"
```

## Signed release build

```bash
./build.sh            # or ./build.sh --clean
```

Writes signed, verified APKs to `outputs/` (gitignored), plus `SHA256SUMS.txt` and the R8
`mapping-<version>.txt`:

| File | For |
|---|---|
| `ClipSave-<version>-arm64-v8a.apk` | most phones |
| `ClipSave-<version>-armeabi-v7a.apk` | older 32-bit phones |
| `ClipSave-<version>-x86_64.apk` | emulators, Chromebooks |
| `ClipSave-<version>-universal.apk` | everywhere, ~3× larger |

* The first run generates a signing key in `keystore/` and `keystore.properties` (both gitignored, random
  password). **Back them up** — every update must be signed with the same key.
* Without `keystore.properties`, Gradle produces an *unsigned* APK rather than falling back to the debug key.
* Forks should create and use their own key.
* Test a release build for anything touching downloads, storage or dependencies: R8 can break code that works
  in debug (keep rules live in `app/proguard-rules.pro`).

## Project layout

| Path | Role |
|---|---|
| `ClipSaveApp`, `AppContainer` | creates notification channels; hand-rolled service locator (prefs, cookies, engine, extractor, downloads, scheduler) |
| `ui/MainActivity` | Welcome (first launch) → Home · Downloads · Settings tabs under a floating glass `Dock`; pushed pages (`Pages.kt`: How it works, Theme, Terms) |
| `ui/ShareReceiverActivity` | `ACTION_SEND text/plain` target; floating sheet in its own task |
| `ui/FormatSheet`, `ExtractViewModel` | link → qualities (or playlist options) → enqueue (shared by Home and Share) |
| `ui/DownloadsScreen`, `HomeScreen`, `SettingsScreen` | the three tabs |
| `core/Extractor`, `Engine` | yt-dlp via [youtubedl-android](https://github.com/junkfood02/youtubedl-android); `Engine.update()` pulls the newest yt-dlp |
| `core/UrlParser`, `Platform` | first link in shared text, YouTube playlist detection, platform labels |
| `core/Prefs` | SharedPreferences: default quality, Wi-Fi only, theme, save folder, playlist numbering, onboarding, last update |
| `core/CookieStore`, `ui/LoginActivity` | optional Instagram/Facebook sign-in (WebView → cookie file) for private/age-gated posts |
| `download/DownloadWorker` | WorkManager foreground (data-sync) task: yt-dlp + ffmpeg → MediaStore/folder; playlists run as one sequential batch |
| `download/DownloadScheduler` | enqueue single videos / playlists, cancel, retry, pause / resume (one, a playlist, or all) |
| `download/DownloadRepository` | JSON-backed history (`files/downloads.json`) |
| `download/MediaStoreSaver`, `Notifications` | scoped-storage save and progress/finished notifications |

UI rules and tokens are in the [design system](docs/design-system/README.md).

## Behaviour worth knowing

* Extractors live in yt-dlp, not in this repo. When a site breaks, use **Settings → Other → Check for updates**
  (the app also checks once a day when opened, if nothing is downloading). If it still fails, report it to
  [yt-dlp](https://github.com/yt-dlp/yt-dlp/issues).
* Many Instagram/Facebook posts need a logged-in session. Only the session cookies are stored (private app
  files), never the password.
* If the chosen save folder disappears or its permission is revoked, files fall back to `Movies/ClipSave`.
* The app's only network traffic is yt-dlp talking to the sites you ask for, thumbnails, and the yt-dlp update
  download.
