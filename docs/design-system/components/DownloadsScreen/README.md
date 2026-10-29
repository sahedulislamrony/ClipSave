# DownloadsScreen

A filtered, sectioned list of downloads.

- Header: title plus one toggle — **Pause all** while anything is queued or running, otherwise **Resume all** when something is paused.
- Filters are a glass segmented control: All · Active (with a count) · Saved · Playlists.
- Sections, in order: Downloading · Paused · Queued · Failed · Saved. Queued and Paused read in run order (`#1`, `#2`…); the others newest first. Queued has **Cancel all**; Failed and Saved have **Clear**.
- Rows are [DownloadItem](../DownloadItem/README.md) cards.
- **Playlists** shows one card per playlist folder (cover, video count, saved progress). Opening it lists the videos in playlist order with a back button and a per-playlist Pause / Resume.
- Empty state: a soft icon circle (plus "Playlists you download show up here" on the Playlists filter).
