# Changelog

All notable changes to ClipSave are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Automated unit tests (`./test.sh`) for link parsing, quality selection, playlist numbering, file names and
  download history; they run on every pull request.

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

[Unreleased]: https://github.com/sahedulislamrony/ClipSave/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/sahedulislamrony/ClipSave/releases/tag/v1.0.0
