# Branching and releasing

```
<username>/<topic> ──PR──► dev ──PR (maintainer)──► main ──► Release workflow ──► GitHub Release
```

| Branch | Who can change it | How |
|---|---|---|
| `main` | nobody directly | pull request from `dev` only, merged with a **merge commit**; every merge can publish a release |
| `dev` | maintainer directly; everyone else by pull request | contributors' pull requests from `<username>/<topic>`, squash-merged |
| `<username>/<topic>` | its author | day-to-day work |

**Checks run on pull requests, not on the maintainer's direct pushes to `dev`:** a push you make straight to
`dev` skips every check (and the ruleset lets repository admins push there), which keeps day-to-day work fast.
Everyone else goes through a pull request, where the checks are required, and the release pull request into
`main` runs them all again.

The rules are enforced by two GitHub rulesets ([`.github/rulesets`](../.github/rulesets)) and these workflows:

| Workflow | When | Check name(s) |
|---|---|---|
| `ci.yml` | pull requests into `dev`/`main`, pushes to `main` (feeds the README badge) | **Build and lint**, **Unit tests** |
| `codeql.yml` | pull requests into `dev`/`main`, pushes to `main`, and weekly | **Analyze (java-kotlin)**, **Analyze (actions)** (code scanning) |
| `branch-rules.yml` | pull requests into `dev`/`main` | **Branch rules**: `<username>/<topic>` → `dev`, only `dev` → `main` |
| `release-check.yml` | pull requests into `main` | **Release readiness**: new version, higher `versionCode`, changelog entry, release (R8) build |
| `release.yml` | push to `main` | builds signed APKs and publishes the GitHub Release |

## Releasing a version

1. On `dev`, with everything for the release merged and tested on a device (including `./build.sh` and a real
   download):
   * bump `versionCode` (always increase) and `versionName` in `app/build.gradle.kts`;
   * in `CHANGELOG.md`, move **Unreleased** under `## [x.y.z] - YYYY-MM-DD` and start a fresh **Unreleased**;
   * commit and push to `dev`.
2. Open the release pull request `dev` → `main` with the release template:

   ```bash
   gh pr create --base main --head dev --title "Release x.y.z" --body-file .github/PULL_REQUEST_TEMPLATE/release.md
   ```
3. Wait for all checks: Build and lint, Unit tests, Branch rules, **Release readiness**, CodeQL. They run again on this
   pull request even though they passed on `dev`.
4. Merge with a **merge commit**.
5. The **Release** workflow starts on `main`: it builds the signed APKs and publishes `vx.y.z` with the
   `## [x.y.z]` changelog section as the notes. If that version is already released (for example a docs-only
   merge), it does nothing.
6. Check the release page, install the arm64 APK on a device and try a download. Users on Obtainium get the
   update automatically; others download it from the release.

If the Release run fails after the merge, fix the cause and re-run it from **Actions → Release → Run workflow**
(choose `main`). No new tag exists until it succeeds.

## One-time repository setup

1. **Create the branch** `dev` (push it from your clone) and keep `main` as the default branch.
2. **Signing secrets** (Settings → Secrets and variables → Actions), taken from your local `keystore.properties`:

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | `base64 -i keystore/clipsave-release.jks` |
   | `KEYSTORE_PASSWORD` | `storePassword` |
   | `KEY_ALIAS` | `keyAlias` (`clipsave`) |

   **Back up `keystore/` and `keystore.properties`.** Every release must be signed with the same key.
3. **Rulesets.** Import [`main.json`](../.github/rulesets/main.json) and [`dev.json`](../.github/rulesets/dev.json)
   under Settings → Rules → Rulesets → New ruleset → Import a ruleset, or apply them with the GitHub CLI:

   ```bash
   # new rulesets
   gh api repos/OWNER/REPO/rulesets --method POST --input .github/rulesets/dev.json
   gh api repos/OWNER/REPO/rulesets --method POST --input .github/rulesets/main.json
   # or update an existing one: gh api repos/OWNER/REPO/rulesets/ID --method PUT --input .github/rulesets/main.json
   ```

   * `main`: no bypass; pull request required; signed commits; the checks above; code scanning; merge commits only.
   * `dev`: pull request required for everyone except repository **admins** (the maintainer), who may push
     directly; the checks above (except Release readiness) and code scanning also apply to pull requests.
4. **Merge settings** (Settings → General → Pull Requests): allow merge commits and squash merging;
   optionally enable "Automatically delete head branches".
5. **Code scanning:** CodeQL results appear after the first run of `codeql.yml`; the rulesets wait for them.
   If CodeQL cannot analyse the Kotlin sources, check the **Analyze (java-kotlin)** log; the matrix in
   `codeql.yml` is where to adjust it.
6. Dependency updates are done by hand; Dependabot version updates are off (no `.github/dependabot.yml`).

## Checking a release

```bash
sha256sum -c SHA256SUMS.txt
apksigner verify --print-certs ClipSave-<version>-arm64-v8a.apk   # must match SECURITY.md
```
