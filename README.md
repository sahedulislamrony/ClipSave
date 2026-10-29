# ClipSave

Android video downloader with a share-sheet workflow — **no ads, no analytics,
no self-updater**. Share a link from Instagram, Facebook, YouTube, TikTok, X, Vimeo, Reddit… to
ClipSave, pick a quality, and it downloads in the background to `Movies/ClipSave`
(or `Music/ClipSave` for audio).

Design overview: [docs/architecture.md](docs/architecture.md).

## Build & run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:installDebug          # needs a device/emulator, minSdk 29
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (~218 MB: yt-dlp's Python + ffmpeg for 3 ABIs).
To shrink it, restrict `abiFilters` in `app/build.gradle.kts` to `arm64-v8a`.

Test the share flow without another app:

```bash
adb shell am start -n com.sahed.clipsave/.ui.ShareReceiverActivity \
  -a android.intent.action.SEND -t text/plain \
  --es android.intent.extra.TEXT "'https://www.instagram.com/reel/XXXX/'"
```

## Layout

| Path | Role |
|---|---|
| `ui/ShareReceiverActivity` | `ACTION_SEND text/plain` target; floating sheet in its own task |
| `ui/FormatSheet`, `ExtractViewModel` | link → qualities → enqueue (shared by Home and Share) |
| `core/Extractor`, `Engine` | yt-dlp via [youtubedl-android](https://github.com/junkfood02/youtubedl-android); `Engine.update()` pulls the newest yt-dlp |
| `core/CookieStore`, `ui/LoginActivity` | optional Instagram/Facebook sign-in (WebView → cookie file) for private/age-gated posts |
| `download/DownloadWorker` | WorkManager foreground (data-sync) task: yt-dlp + ffmpeg → MediaStore; playlists run as one sequential batch |
| `download/DownloadScheduler` | enqueue single videos / playlists, cancel, retry, pause all / resume all |
| `download/DownloadRepository` | JSON-backed history |

## Notes

* Extractors live in yt-dlp, not here. When Instagram/Facebook change, use **Settings → Update now**
  (it also auto-updates once a day).
* Many Instagram/Facebook posts need a logged-in session; sign in under **Settings → Accounts**.
  Only the session cookies are stored (private app files), never the password.
* Release builds are signed with the debug key — add a real keystore before distributing.
* Only download content you own or have permission to save; platform terms may restrict downloading.
