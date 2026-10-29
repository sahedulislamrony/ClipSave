# ClipSave

Android video downloader with a share-sheet workflow — **no ads, no analytics,
no self-updater**. Share a link from Instagram, Facebook, YouTube, TikTok, X, Vimeo, Reddit… to
ClipSave (or paste it on the Home screen), pick a quality, and it downloads in the background to
`Movies/ClipSave` (`Music/ClipSave` for audio, `Pictures/ClipSave` for images), or to a folder you
choose under **Settings → Save to**.

Design overview: [docs/architecture.md](docs/architecture.md) · UI spec: [docs/design-system](docs/design-system/README.md).

## Features

* **Share or paste** — `Share → ClipSave` from any app, or paste a link on Home (a link copied
  elsewhere is picked up automatically when the app opens).
* **Quality sheet** — Video/Audio switch, up to four qualities with sizes, editable file name.
  Set **Settings → Quality** to a fixed default (Best, 1080p, 720p, 480p, MP3) for one-tap
  downloads that skip the sheet; **Ask** (default) always shows it.
* **YouTube playlists** — a link with `list=` opens a playlist sheet: folder name, numbering
  (1→N, N→1, none) and one quality cap. Videos download one after another into
  `<download folder>/<playlist name>/`; **Downloads → Playlists** groups them per playlist.
* **Background downloads** — foreground service with a progress notification (Cancel action);
  survives leaving the app.
* **Pause / resume** — per download, per playlist, or **Pause all / Resume all**; partial files are
  kept, so resuming continues where it stopped. Failed or canceled items can be retried.
* **Wi-Fi only** switch, light/dark/system theme, optional Instagram/Facebook sign-in.

## Build & run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:installDebug          # needs a device/emulator, minSdk 29
```

Debug APKs are written per ABI to `app/build/outputs/apk/debug/` (yt-dlp's Python + ffmpeg make the
universal APK ~218 MB; each per-ABI APK is roughly a third of that).

## Release build (signed APKs)

```bash
./build.sh            # or ./build.sh --clean
```

Writes signed, verified APKs to `outputs/` (gitignored): `ClipSave-<version>-arm64-v8a.apk` (most phones),
`-armeabi-v7a`, `-x86_64`, `-universal`, plus `SHA256SUMS.txt` and the R8 `mapping-<version>.txt`.
Bump `versionCode`/`versionName` in `app/build.gradle.kts` before each release.

* **First run** generates a signing key in `keystore/` and `keystore.properties` (both gitignored,
  random password). **Back them up** — every update must be signed with the same key.
* Needs JDK 17+ and the Android SDK; the script finds Android Studio's JDK and `local.properties` itself.
* **GitHub Releases:** `.github/workflows/release.yml` runs `build.sh` on every `v*` tag and publishes the
  APKs. Add the secrets `KEYSTORE_BASE64` (`base64 -i keystore/clipsave-release.jks`), `KEYSTORE_PASSWORD`
  and `KEY_ALIAS` first, then:

  ```bash
  git tag v1.0.0 && git push origin v1.0.0
  ```

Google Play won't accept this app (its policies prohibit downloaders for sites like YouTube and apps that
fetch executable code outside Play), so distribute through GitHub Releases, [Obtainium](https://obtainium.imranr.dev/)
(auto-updates from those releases), or [IzzyOnDroid](https://apt.izzysoft.de/fdroid/).

Test the share flow without another app:

```bash
adb shell am start -n com.sahed.clipsave/.ui.ShareReceiverActivity \
  -a android.intent.action.SEND -t text/plain \
  --es android.intent.extra.TEXT "'https://www.instagram.com/reel/XXXX/'"
```

## Layout

| Path | Role |
|---|---|
| `ClipSaveApp`, `AppContainer` | creates notification channels; hand-rolled service locator (prefs, cookies, engine, extractor, downloads, scheduler) |
| `ui/MainActivity` | Welcome (first launch) → Home · Downloads · Settings tabs under a floating glass `Dock`; pushed pages (`Pages.kt`: How it works, Theme) |
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

## Notes

* Extractors live in yt-dlp, not here. When Instagram/Facebook change, use
  **Settings → Other → Check for updates** (the app also checks once a day when opened, if nothing
  is downloading).
* Many Instagram/Facebook posts need a logged-in session; sign in under **Settings → Accounts**.
  Only the session cookies are stored (private app files), never the password.
* If the chosen save folder disappears or its permission is revoked, files fall back to
  `Movies/ClipSave` rather than being lost.
* Release builds are signed with your own key (see *Release build*); without `keystore.properties`, Gradle
  produces an unsigned APK rather than falling back to the debug key.
* Only download content you own or have permission to save; platform terms may restrict downloading.
