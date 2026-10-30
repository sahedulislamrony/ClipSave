# Changelog

All notable changes to ClipSave are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed
- App updates are optional: the update screen now has **Update later** next to **Update now**, and Back does the
  same. A version you put off isn't offered again when the app opens (a newer release is), while **Settings →
  Check for updates** always offers it.
- Sharing a single video shows its thumbnail, title and channel within moments (YouTube, Vimeo, TikTok,
  Dailymotion) while the qualities are still being read, instead of a bare spinner.
- Reading a YouTube video's qualities skips the HLS manifest, one fewer request and about 20% faster.

## [1.2.0] - 2026-10-29

### Changed
- Project housekeeping: centered README badges, the CI badge now tracks `main`, and Dependabot is turned off.
  No change to the app itself.

## [1.1.0] - 2026-10-29

### Added
- Automated unit tests (`./test.sh`) for link parsing, quality selection, playlist numbering, file names and
  download history; they run on every pull request.
- In-app updates: ClipSave checks GitHub Releases each time it opens (and from **Settings → Check for
  updates**), downloads the same APK variant that's installed (arm64-v8a, armeabi-v7a, x86_64 or universal) and
  hands it to the system installer. A newer release must be installed before the app can be used.
- Playlists are read 100 videos at a time (**Load next 100** / **Load remaining N**), and you tick which videos
  to download, up to 100 per download across pages. Qualities and the Download button show an estimated total
  size for the ticked videos.
- Screenshots in `docs/screens`, shown in the README.
- **Settings → Other → Developer**: who makes ClipSave, with GitHub, LinkedIn and email links.

### Changed
- The app ID and code package are now `com.sahedul.clipsave` (was `com.sahed.clipsave`). Android sees this as a
  new app, so uninstall 1.0.0 before installing 1.1.0.
- Fetching a YouTube video's qualities is about 4× faster (≈25 s → 5–7 s on a phone): the picker no longer
  solves YouTube's player challenge, which only the download needs. yt-dlp also keeps a cache between runs.
- Playlist files are numbered by their place in the whole playlist, padded to its full length.
- Download notifications show in the normal notification list and status bar instead of under "Silent", carry
  the white ClipSave mark on the brand colour, drop the Cancel button, and open Downloads when tapped.
- A download shows "Preparing…" with a moving bar until data starts arriving, instead of a frozen 0%.

### Fixed
- Instagram and Facebook sign-in showed a blank white screen.
- Long playlists (and endless YouTube Mixes) took minutes to open, or never did, because every video was
  listed before the sheet appeared.
- Sharing a link without ever opening ClipSave never asked for notification permission, so on Android 13+ no
  download notifications appeared.

## [1.0.0] - 2026-10-29

First public release.

### Added
- Share-sheet download flow: `Share → ClipSave` from any app, or paste a link on Home (a copied link is picked
  up when the app opens).
- Quality sheet with Video/Audio switch, sizes, and an editable file name; optional one-tap mode with a saved
  default quality (Best, 1080p, 720p, 480p, MP3).
- YouTube playlist downloads into a named folder with 1→N, N→1 or no numbering; a Playlists view in Downloads.
- Background downloads in a foreground service with progress notifications and Cancel.
- Pause / resume (single, per playlist, or all) that continues from the partial file; retry for failures.
- Save to a custom folder (Storage Access Framework) or `Movies/ClipSave`, `Music/ClipSave`, `Pictures/ClipSave`;
  automatic fallback if the chosen folder becomes unavailable.
- Optional Instagram and Facebook sign-in for private or age-gated posts (session cookies only).
- Wi-Fi only mode, light/dark/system theme, in-app yt-dlp updates (manual and daily).
- ClipSave logo: adaptive and themed launcher icon, status-bar icon, in-app mark and docs artwork.
- Terms of Use and disclaimer (`TERMS.md`), agreed on first launch and readable under Settings → Other → Terms.
- Signed per-ABI release APKs, `build.sh`, and a GitHub Actions release workflow.

[Unreleased]: https://github.com/sahedulislamrony/ClipSave/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/sahedulislamrony/ClipSave/releases/tag/v1.2.0
[1.1.0]: https://github.com/sahedulislamrony/ClipSave/releases/tag/v1.1.0
[1.0.0]: https://github.com/sahedulislamrony/ClipSave/releases/tag/v1.0.0
