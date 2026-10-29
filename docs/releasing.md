# Releasing

Releases are built by [`.github/workflows/release.yml`](../.github/workflows/release.yml) when a `v*` tag is
pushed, using the same [`build.sh`](../build.sh) you can run locally.

## One-time setup

1. Run `./build.sh` once locally. It creates the signing key in `keystore/` and `keystore.properties` (both
   gitignored). **Back them up** — every release must be signed with this key, or users have to uninstall
   before updating.
2. Add these repository secrets (Settings → Secrets and variables → Actions):

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | `base64 -i keystore/clipsave-release.jks` |
   | `KEYSTORE_PASSWORD` | `storePassword` from `keystore.properties` |
   | `KEY_ALIAS` | `keyAlias` from `keystore.properties` (`clipsave`) |

## Each release

1. Bump `versionCode` (always increase) and `versionName` in `app/build.gradle.kts`.
2. In `CHANGELOG.md`, rename **Unreleased** to the new version with today's date and start a fresh
   **Unreleased** section; update the compare links at the bottom.
3. Commit, and open a pull request or push to `main`.
4. Tag and push:

   ```bash
   git tag v1.0.1 && git push origin v1.0.1
   ```
5. The workflow publishes per-ABI APKs and `SHA256SUMS.txt` to the GitHub Release. Check the release page,
   install the arm64 APK on a device, and try a download.

## Checking a release

```bash
sha256sum -c SHA256SUMS.txt
apksigner verify --print-certs ClipSave-<version>-arm64-v8a.apk   # must match SECURITY.md
```
