# ClipSave

One-tap video saver. **White first, iris accent, liquid glass, almost no words.**

## Principles

1. **White first.** Light is the default theme. Dark is a choice in Settings › Theme (Light · Dark · System).
2. **One colour.** Iris (`primary`, #5B3BF0 light / #9C88FF dark) marks the main action, selection, progress and the active tab — nothing else.
3. **One material.** Everything that floats is liquid glass: the dock, sheets, every secondary button, segmented controls, pills on thumbnails.
4. **Fewer words.** A screen has a title and labels — no helper paragraphs, no captions unless they show data (size, version, %). Icons and thumbnails carry the rest.

## Content

- Sentence case, 1–3 words per label: *Download*, *Paste*, *Sign in*, *Check*.
- Numbers instead of sentences: `62%`, `1080p`, `v2.4.0`.
- The one reassurance line, on Welcome and How it works: "Save only what you have the right to." Error messages are the only other full sentences.
- Never show internals — no engine names or versions. Updates = **Check for updates**.

## Liquid glass

Recipe (class `.glass`): `glass` fill + `backdrop-filter: blur(24px) saturate(180%)` + 1px `glass-edge` rim + inset top `glass-highlight` + `shadow-glass` + a soft-light diagonal sheen. The active item inside a glass control is a `glass-lens` capsule. Glass needs something behind it: thumbnails scroll under the dock, sheets float over a scrim.

The **dock** floats 16dp from the sides and 26dp from the bottom: a single pill tab bar (Home · Downloads · Settings) with a dot on Downloads while something is running. It is hidden on pushed pages (How it works, Theme).

## Visual foundations

- **Type:** Plus Jakarta Sans in the specs; the Android app does not bundle it and falls back to the system sans. Titles 30/36 ExtraBold at −0.03em; welcome 40/42. Body 15 semibold for rows.
- **Shape:** everything interactive is a pill; cards and rows 24dp (`Radius.Lg`); the quality sheet is a detached 34dp sheet (`Radius.Xl`).
- **Buttons:** only *primary* (solid iris, inner highlight, iris glow) and *glass*. No outlines, no text links.
- **Contrast:** text ≥ 4.5:1 in both themes; `ink-faint` only for placeholders and disabled.

## Iconography

In-house line icons, 24 grid, 1.9 stroke, round caps, `currentColor`. 22px in the dock, 18px in rows and buttons. The mark: iris rounded square with a down arrow into a tray.

## Screens

The Android app is the source of truth. `screens.html` and the `preview.html` files are visual prototypes; the
Home and Downloads previews predate the implemented layouts described below.

| Screen | |
| --- | --- |
| WelcomeScreen | Art, 2-line headline, Get started |
| HomeScreen | Centered wordmark, link field with Paste/Clear, Download, How it works |
| QualitySheet | Glass sheet: name field, Video/Audio, up to 4 options, Download. Playlist variant adds folder + numbering |
| DownloadsScreen | Pause/Resume all, filters (All · Active · Saved · Playlists), sections of download rows |
| SettingsScreen | System (Theme, Notifications), Downloads (Quality, Save to, Wi-Fi only), Accounts, Other |
