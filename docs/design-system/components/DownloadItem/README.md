# DownloadItem

Download row: 96×68 thumbnail, up to two lines of title, a status line that fits the state, and one trailing control.

| State | Status line | Trailing control |
| --- | --- | --- |
| Downloading | progress bar, `62% · 14s · 1080p` | Pause |
| Saving | progress bar, "Saving…" | none |
| Queued | `#3 · 720p` | Pause |
| Paused | grey bar, `Paused · 40% · 720p` | Resume, Delete |
| Failed / Canceled | error (or "Canceled") in the danger colour | ⋮ menu: Retry, Delete |
| Saved | `1080p · YouTube`, play dot on the thumbnail | ⋮ menu: Share, Delete |

Tapping a saved row opens the file; tapping a paused row resumes it; tapping a failed row retries it.
