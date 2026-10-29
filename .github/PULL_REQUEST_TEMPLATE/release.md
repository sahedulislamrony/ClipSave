<!-- Release pull request: dev → main. Open it with ?template=release.md on the compare URL. -->

## Release

Version: **x.y.z** (versionCode **n**)

## Before merging

- [ ] `dev` has been tested on a device, including a **release build** (`./build.sh`) and a real download
- [ ] `versionName` and `versionCode` are bumped in `app/build.gradle.kts`
- [ ] `CHANGELOG.md`: **Unreleased** moved under `## [x.y.z] - YYYY-MM-DD`
- [ ] All checks pass: Build and lint, Branch rules, Release readiness, CodeQL

## After merging

The **Release** workflow builds the signed APKs and publishes `vx.y.z` automatically. Check the release page and
install the arm64 APK on a device. Merge with a **merge commit** (not squash), so `dev` and `main` stay in step.
