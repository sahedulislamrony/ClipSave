# ClipSave architecture

```
Share sheet / Home "paste link"
        │  (text containing a URL → UrlParser.firstUrl)
        ▼
ExtractViewModel ──► Extractor ──► yt-dlp (youtubedl-android) ──► MediaInfo (title, thumb, qualities)
        │                                                              │
        │  user picks a FormatOption — or, with a saved default       │
        │  quality, the sheet is skipped and the closest one is used  │
        ▼                                                              ▼
DownloadScheduler ──► WorkManager ──► DownloadWorker (foreground, dataSync)
                                          │ yt-dlp + ffmpeg → work dir
                                          ▼
                                   MediaStoreSaver → chosen folder, or Movies/Music/Pictures/ClipSave
                                          │
                                   DownloadRepository (JSON) ──► Downloads tab / notifications
```

## App flow

1. **First launch** — `WelcomeScreen`; "Get started" sets `Prefs.onboarded`, then the app asks for the
   notification permission (API 33+) and runs a daily-throttled yt-dlp update (`Engine.updateIfStale`,
   skipped while a download is active).
2. **Entry** — either `ShareReceiverActivity` (floating sheet over the source app, finishes as soon as the
   download is queued) or the Home tab (paste field; a copied link is picked up once when the window gains
   focus). Both drive the same `ExtractViewModel` and `ExtractSheet`.
3. **Resolve** — `ExtractViewModel.load` → `Extractor.fetch` (single video) or `fetchPlaylist` (YouTube link with
   `list=`). States: `Loading → Ready | PlaylistReady | Error → Started`.
   * `Ready` shows the quality sheet. If `Prefs.defaultQuality` is not `ask`, the sheet is skipped and
     `FormatOption.pick` chooses the saved quality (or the closest lower one).
   * `PlaylistReady` never auto-starts; the saved default only preselects a row. "Only this video" appears when
     the link was a video opened inside a playlist.
   * `Error` shows a readable message; login-related failures offer "Sign in" (opens Settings).
4. **Enqueue** — `DownloadScheduler` writes `DownloadItem` rows (status `QUEUED`) and starts a WorkManager job
   (network constraint: connected, or unmetered with **Wi-Fi only**). Home then switches to the Downloads tab.
5. **Download** — `DownloadWorker` runs yt-dlp in a work dir, then copies the result out with `MediaStoreSaver`
   (`RUNNING → PROCESSING → DONE`), and posts a "Download complete" notification that opens the file.
6. **Manage** — Downloads tab: pause / resume / retry / delete / share / open; section headers offer
   Cancel all (queued) and Clear (failed, saved).

## Components

| Piece | Notes |
|---|---|
| `ShareReceiverActivity` | `ACTION_SEND text/plain`; separate task (`.ShareSheet`) so the quality sheet floats over the source app; toasts and closes if no link is found |
| `MainActivity` | `singleTask`; hosts Welcome, the three tabs, and pushed pages (How it works, Theme). Accepts an `EXTRA_TAB` to open on a given tab (used for "Sign in" from the share sheet) |
| `FormatSheet` / `ExtractViewModel` | shared by the share flow and the Home screen; editable file name, Video/Audio switch, at most four options per type |
| `Engine` | initialises yt-dlp + ffmpeg (`Starting → Ready | Failed`); `update()` pulls the newest yt-dlp (Settings → Check for updates, and daily) |
| `Extractor` | builds the yt-dlp request (`--no-playlist` for single videos, session cookies if signed in), maps failures to user-readable messages |
| `CookieStore` / `LoginActivity` | optional Instagram/Facebook sign-in; WebView cookies are copied to Netscape cookie files in private app storage, merged for `--cookies`; password never stored |
| `DownloadWorker` | one yt-dlp process per download; cancelling the work kills the process. A playlist is one work request (`KEY_BATCH`) that downloads its videos one after another |
| Playlists | YouTube links with `list=` open the playlist sheet (`Extractor.fetchPlaylist`, `--flat-playlist -J`; private/deleted entries are dropped). Files go to `<download folder>/<playlist name>/`, numbered `1→N`, `N→1` or not at all (`Numbering`; last choice remembered) |
| Downloads → Playlists | one card per playlist folder (`PlaylistGroup`), opening to its videos in playlist order with a per-playlist pause/resume |
| Dock glass | backdrop blur via [Haze](https://github.com/chrisbanes/haze) (`glass(haze = …)`); keep Haze on a release built against the project's Compose BOM |
| `MediaStoreSaver` | scoped-storage save (API 29+), no storage permission. Default targets: `Movies/ClipSave` (video), `Music/ClipSave` (audio), `Pictures/ClipSave` (images). With **Save to** set, files go to that tree via the Storage Access Framework; if that fails the default folder is used |
| `Notifications` | channels `downloads` (low, ongoing progress with a Cancel action) and `done` (finished / failed; one summary per playlist) |

## Download states and recovery

`QUEUED → RUNNING → PROCESSING → DONE`, plus `PAUSED`, `FAILED`, `CANCELED`.

| Action | Effect |
|---|---|
| Pause (one / playlist / all) | Marks `PAUSED` first (the worker reads the status to tell a pause from a failure), then cancels the work or, for a playlist video, kills only that yt-dlp process. The work dir `<external files>/tmp/<id>` (cache dir fallback) is kept so yt-dlp continues its `.part` file on resume. Items already saving (`PROCESSING`) are left to finish |
| Resume | Back to `QUEUED`; singles get a new work request, playlist videos go back through their playlist's worker so they still run one at a time |
| Cancel | Notification **Cancel** stops the download (the whole playlist for a batch); **Cancel all** cancels queued items. Work dirs are deleted. Running/queued rows in the list only offer Pause — delete a paused item to discard it |
| Retry | Failed/canceled items restart from `QUEUED` |
| Delete | Saved: removes the file as well as the row. Otherwise: removes the partial work dir and the row |
| Process death | On launch, rows left `QUEUED/RUNNING/PROCESSING` become `FAILED("Interrupted")`; if WorkManager reruns a playlist batch, its interrupted rows are re-queued automatically |

## Settings

Theme (Light default / Dark / System), Notifications (system settings), Quality (Ask · Best · 1080p · 720p ·
480p · MP3), Save to (folder picker; "Use Movies/ClipSave" resets), Wi-Fi only, Accounts (Instagram, Facebook
sign in/out), How it works, Report a bug / Request a feature (opens the mail app, no network code), Check for
updates (updates the embedded yt-dlp, shows the app version).

## Privacy / footprint

No ads, analytics, crash reporters or self-updater. The only network traffic is yt-dlp talking to the sites
the user asks for, thumbnails (Coil), and the yt-dlp update download. Permissions: internet, network state,
notifications, wake lock, and a data-sync foreground service.
