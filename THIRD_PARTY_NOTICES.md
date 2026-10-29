# Third-party software

ClipSave is licensed under the [GNU GPL v3 or later](LICENSE). It is built on, and ships, the following
software. Licenses are as declared by each project's package metadata; see the linked projects for the
authoritative text.

## Bundled at runtime

| Component | Role | License |
|---|---|---|
| [yt-dlp](https://github.com/yt-dlp/yt-dlp) | Extracts and downloads media; updated in place by **Check for updates** | Unlicense |
| [youtubedl-android](https://github.com/junkfood02/youtubedl-android) (`library`, `common`, `ffmpeg`) | Runs yt-dlp and ffmpeg on Android; bundles a Python runtime and an ffmpeg build | GPL-3.0 |
| [FFmpeg](https://ffmpeg.org/) | Merges video and audio, converts to MP3/M4A | LGPL-2.1+ / GPL (build bundled by youtubedl-android) |
| [Python](https://www.python.org/) | Runtime for yt-dlp (bundled by youtubedl-android) | PSF License |

youtubedl-android is GPL-3.0, which is why ClipSave as a whole is distributed under GPL-3.0-or-later.

## Libraries

| Library | License |
|---|---|
| [AndroidX](https://developer.android.com/jetpack/androidx) (Core, Activity, Lifecycle, WorkManager) | Apache-2.0 |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) and Material 3 | Apache-2.0 |
| [Kotlin](https://kotlinlang.org/), kotlinx.coroutines, kotlinx.serialization | Apache-2.0 |
| [Coil](https://github.com/coil-kt/coil) | Apache-2.0 |
| [Haze](https://github.com/chrisbanes/haze) | Apache-2.0 |

Apache-2.0 is compatible with GPL-3.0. Versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Trademarks

Instagram, Facebook, YouTube, TikTok, X, Vimeo, Reddit, Pinterest, Dailymotion and other names mentioned in
ClipSave are trademarks of their respective owners. ClipSave is an independent project and is not affiliated
with, endorsed by, or sponsored by any of them.
