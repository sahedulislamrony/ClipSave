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

Build, run and test instructions are in [SETUP.md](SETUP.md); read
[docs/architecture.md](docs/architecture.md) for how the pieces fit together. Quick start:

```bash
git clone git@github.com:sahedulislamrony/ClipSave.git
cd ClipSave
./gradlew :app:installDebug
```

## Branches and pull requests

```
<username>/<topic>  ──PR──►  dev  ──PR (maintainer, after testing)──►  main  ──►  automatic release
   your branch            checks run             all checks run again
```

* **`main` is locked.** Nobody pushes to it, including the maintainer; it only changes through a release pull
  request from `dev`, and every merge there publishes a release.
* **`dev` is where work is integrated and tested.** Only the maintainer can push to it directly; everyone else
  opens a pull request.
* **Your branch** is `<username>/<topic>`, created from `dev`, for example `sahed/fix-resume` or
  `sahed/feature-folder-picker`. Pull requests from branches named any other way are rejected by the
  **Branch rules** check.

Releases (version bump, changelog, the `dev` → `main` pull request) are described in
[docs/releasing.md](docs/releasing.md).

## Making a change

1. Fork the repo (or, if you have write access, clone it) and create your branch from `dev`:

   ```bash
   git switch dev && git pull
   git switch -c <username>/<topic>
   ```
2. Keep the change focused; unrelated cleanups go in their own pull request.
3. Match the surrounding code: Kotlin official style (see `.editorconfig`), the existing comment density and
   naming. Comments explain *why*, not *what*.
4. If behaviour or the user flow changes, update the docs ([README](README.md),
   [architecture](docs/architecture.md), design-system READMEs) in the same pull request, and add a line under
   **Unreleased** in [CHANGELOG.md](CHANGELOG.md). Don't change `versionName`/`versionCode`; the maintainer does
   that when releasing.
5. Add or update **unit tests** for logic you change, and run them with `./test.sh` before pushing (see
   [SETUP.md](SETUP.md#tests)).
6. Test on a device or emulator, including a **release build** (`./build.sh`) for anything touching downloads,
   storage or dependencies — R8 can break code that works in debug.
7. Open a pull request **into `dev`** using the template. These checks must pass before it can be merged:

   | Check | What it verifies |
   |---|---|
   | **Build and lint** | debug build and Android lint |
   | **Unit tests** | the automated tests in `app/src/test` |
   | **CodeQL** | code scanning for the app and the workflow files |
   | **Branch rules** | your branch is `<username>/<topic>` and targets `dev` |

   Pull requests into `dev` are squash-merged.

Commit messages: a short imperative summary line (`Fix resume after process death`), then a body explaining the
why when it isn't obvious.

## Licensing of contributions

ClipSave is licensed under the [GPL-3.0-or-later](LICENSE) and used under the [Terms of Use](TERMS.md). By submitting a contribution you agree it is
licensed under the same terms. New dependencies must be GPL-3.0 compatible (Apache-2.0, MIT, BSD, LGPL, GPL-3.0
are fine); add them to [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Code of conduct

Participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).
