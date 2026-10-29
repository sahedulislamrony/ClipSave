# Contributing to ClipSave

Thanks for helping! ClipSave is a small, privacy-first app, so a few principles guide what gets merged:

* **No ads, analytics, trackers, crash reporters or self-updating APKs.** Pull requests that add any of these
  will be declined.
* **Fewer permissions is better.** New permissions need a strong reason.
* **Extractor problems belong upstream.** If a site stopped working, first try **Settings → Other → Check for
  updates**; if it still fails, report it to [yt-dlp](https://github.com/yt-dlp/yt-dlp/issues). ClipSave only
  wraps yt-dlp.
* **Few words in the UI.** See the [design system](docs/design-system/README.md).

## Reporting bugs and ideas

Use the [issue templates](../../issues/new/choose). For a bug, include your Android version, device, app
version (Settings → Other), the link type (not the private link itself) and what you saw. Search existing issues
first. Security problems go through [SECURITY.md](SECURITY.md), not public issues.

## Development setup

Build, run and test instructions are in [docs/setup.md](docs/setup.md); read
[docs/architecture.md](docs/architecture.md) for how the pieces fit together. Quick start:

```bash
git clone git@github.com:sahedulislamrony/ClipSave.git
cd ClipSave
./gradlew :app:installDebug
```

## Making a change

1. Fork the repo and branch from `main` (`fix/short-name`, `feat/short-name`).
2. Keep the change focused; unrelated cleanups go in their own pull request.
3. Match the surrounding code: Kotlin official style (see `.editorconfig`), the existing comment density and
   naming. Comments explain *why*, not *what*.
4. If behaviour or the user flow changes, update the docs ([README](README.md),
   [architecture](docs/architecture.md), design-system READMEs) in the same pull request, and add a line under
   **Unreleased** in [CHANGELOG.md](CHANGELOG.md).
5. Test on a device or emulator, including a **release build** (`./build.sh`) for anything touching downloads,
   storage or dependencies — R8 can break code that works in debug.
6. Open a pull request using the template. CI must pass.

Commit messages: a short imperative summary line (`Fix resume after process death`), then a body explaining the
why when it isn't obvious.

## Licensing of contributions

ClipSave is licensed under the [GPL-3.0-or-later](LICENSE) and used under the [Terms of Use](TERMS.md). By submitting a contribution you agree it is
licensed under the same terms. New dependencies must be GPL-3.0 compatible (Apache-2.0, MIT, BSD, LGPL, GPL-3.0
are fine); add them to [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Code of conduct

Participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).
