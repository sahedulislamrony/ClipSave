# ClipSave architecture

```
Share sheet / Home "paste link"
        │  (text containing a URL)
        ▼
ExtractViewModel ──► Extractor ──► yt-dlp (youtubedl-android) ──► MediaInfo (title, thumb, qualities)
        │                                                              │
        │  user picks a FormatOption (or a saved default is applied)   │
        ▼                                                              ▼
DownloadScheduler ──► WorkManager ──► DownloadWorker (foreground, dataSync)
                                          │ yt-dlp + ffmpeg → temp dir
                                          ▼
                                   MediaStoreSaver → Movies/ClipSave, Music/ClipSave
                                          │
                                   DownloadRepository (JSON) ──► Downloads tab / notifications
```

| Piece | Notes |
|---|---|
| `ShareReceiverActivity` | `ACTION_SEND text/plain`; separate task (`.ShareSheet`) so the quality sheet floats over the source app |
| `FormatSheet` / `ExtractViewModel` | shared by the share flow and the Home screen |
| `Engine` | initialises yt-dlp + ffmpeg; `update()` pulls the newest yt-dlp (also daily) |
| `Extractor` | builds the yt-dlp request, maps failures to user-readable messages |
| `CookieStore` / `LoginActivity` | optional Instagram/Facebook sign-in; session cookies only, stored in private app files |
| `DownloadWorker` | one yt-dlp process per download; cancelling the work kills the process. A playlist is one work request (`KEY_BATCH`) that downloads its videos one after another |
| Playlists | YouTube links with `list=` open the playlist sheet (`Extractor.fetchPlaylist`, `--flat-playlist -J`). Files go to `<download folder>/<playlist name>/`, numbered `1→N`, `N→1` or not at all (`Numbering`) |
| Downloads → Playlists | one card per playlist folder (`PlaylistGroup`), opening to its videos in playlist order with a per-playlist pause/resume |
| Dock glass | backdrop blur via [Haze](https://github.com/chrisbanes/haze) (`glass(haze = …)`); keep Haze on a release built against the project's Compose BOM |
| Pause / resume | `DownloadScheduler.pauseAll/resumeAll`: PAUSED keeps the work dir (`files/tmp/<id>`) so yt-dlp continues its `.part` file on resume |
| `MediaStoreSaver` | scoped-storage save (API 29+), no storage permission |

## Privacy / footprint

No ads, analytics, crash reporters or self-updater. Permissions: internet, network state,
notifications, and a data-sync foreground service.
