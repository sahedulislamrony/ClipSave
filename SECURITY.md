# Security policy

## Supported versions

Only the latest release receives security fixes. Please update before reporting.

## Reporting a vulnerability

**Do not open a public issue for security problems.**

* Preferred: GitHub's private reporting — **Security → Report a vulnerability** on the
  [repository](https://github.com/sahedulislamrony/ClipSave/security/advisories/new).
* Or email **sahedul.dev@gmail.com** with the subject `ClipSave security`.

Include what you found, steps to reproduce, the affected version, and the impact you see. You can expect an
acknowledgement within a few days and a fix or a clear answer as soon as it can be assessed. Please give us
reasonable time to release a fix before disclosing publicly; we are happy to credit you.

## Scope

In scope: the ClipSave app and its build/release tooling (for example how sign-in cookies are stored, file
handling, exported components, the release workflow).

Out of scope: vulnerabilities in [yt-dlp](https://github.com/yt-dlp/yt-dlp), FFmpeg or the websites ClipSave
talks to (report those upstream), and issues that need a rooted device or physical access to an unlocked phone.

## Verifying a download

Release APKs are signed with the project key. Compare checksums against `SHA256SUMS.txt` on the release page, and
check the signing certificate matches:

```
9e7ac24db911b702528987fc9d181ef06947123de56d8b26cb8b88b48f63aa59
```

Check yours with `apksigner verify --print-certs ClipSave-<version>-<abi>.apk` (look for "certificate SHA-256
digest"). If it differs, do not install the file.

## Data handling

ClipSave has no servers and collects nothing. The only sensitive data it holds is the optional Instagram/Facebook
session cookies, kept in the app's private storage and removed with **Settings → Accounts → Sign out**. Your
password is never stored.
