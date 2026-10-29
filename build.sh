#!/usr/bin/env bash
#
# Builds signed ClipSave release APKs into ./outputs.
#
#   ./build.sh            build (creates the signing key on first run)
#   ./build.sh --clean    clean first
#   ./build.sh --help
#
# Output (./outputs, gitignored):
#   ClipSave-<version>-arm64-v8a.apk      most phones — this is the one to share
#   ClipSave-<version>-armeabi-v7a.apk    older 32-bit phones
#   ClipSave-<version>-x86_64.apk         emulators / Chromebooks
#   ClipSave-<version>-universal.apk      works everywhere, ~3x the size
#   SHA256SUMS.txt                        checksums for the files above
#   mapping-<version>.txt                 R8 mapping, to de-obfuscate crash stack traces
#
# Signing: reads keystore.properties. If it is missing, a new keystore is generated in ./keystore
# with random passwords. BACK UP ./keystore AND keystore.properties — the same key must sign every
# future update, or users have to uninstall before updating.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
ROOT="$PWD"
OUT="$ROOT/outputs"
PROPS="$ROOT/keystore.properties"
KEYSTORE_DIR="$ROOT/keystore"

say()  { printf '\033[1;35m▸ %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m! %s\033[0m\n' "$*" >&2; }
die()  { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

CLEAN=0
for arg in "$@"; do
  case "$arg" in
    --clean) CLEAN=1 ;;
    -h|--help) sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "Unknown option: $arg (try --help)" ;;
  esac
done

# --- Toolchain ----------------------------------------------------------------------------------
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home" \
    "$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr" \
    "/opt/android-studio/jbr"; do
    if [[ -x "$candidate/bin/java" ]]; then JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  command -v java >/dev/null || die "No JDK found. Install Android Studio or set JAVA_HOME (JDK 17+)."
else
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -z "${ANDROID_HOME:-}" && -f local.properties ]]; then
  ANDROID_HOME="$(sed -n 's/^sdk\.dir=//p' local.properties | head -1)"
fi
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
[[ -d "$ANDROID_HOME" ]] || die "Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties."
export ANDROID_HOME

APKSIGNER="$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
[[ -n "$APKSIGNER" ]] || die "apksigner not found in $ANDROID_HOME/build-tools. Install Android SDK Build-Tools."

sha256() { if command -v sha256sum >/dev/null; then sha256sum "$@"; else shasum -a 256 "$@"; fi; }

# --- Signing key ----------------------------------------------------------------------------------
if [[ ! -f "$PROPS" ]]; then
  say "No keystore.properties — generating a release signing key"
  command -v keytool >/dev/null || die "keytool not found (it ships with the JDK)."
  command -v openssl >/dev/null || die "openssl is required to generate a random password."
  mkdir -p "$KEYSTORE_DIR"
  chmod 700 "$KEYSTORE_DIR"
  [[ ! -e "$KEYSTORE_DIR/clipsave-release.jks" ]] || die "keystore/clipsave-release.jks exists but keystore.properties is missing. Restore keystore.properties (or move the old keystore away)."
  PASS="$(openssl rand -base64 33 | tr -dc 'A-Za-z0-9' | head -c 32)"
  keytool -genkeypair -noprompt \
    -keystore "$KEYSTORE_DIR/clipsave-release.jks" -storetype PKCS12 \
    -alias clipsave -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=ClipSave, O=ClipSave, C=BD" >/dev/null
  umask 077
  cat > "$PROPS" <<EOF
storeFile=keystore/clipsave-release.jks
storePassword=$PASS
keyAlias=clipsave
keyPassword=$PASS
EOF
  chmod 600 "$PROPS" "$KEYSTORE_DIR/clipsave-release.jks"
  warn "New signing key created in ./keystore (+ keystore.properties). BACK THESE UP somewhere safe."
  warn "Lose them and you can never ship an update that installs over existing copies."
else
  say "Using signing key from keystore.properties"
fi
[[ -f "$ROOT/$(sed -n 's/^storeFile=//p' "$PROPS")" ]] || die "keystore.properties points at a keystore file that does not exist."

# --- Build ----------------------------------------------------------------------------------------
VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)"
[[ -n "$VERSION" ]] || die "Could not read versionName from app/build.gradle.kts."

TASKS=(:app:assembleRelease)
(( CLEAN )) && TASKS=(:app:clean "${TASKS[@]}")
say "Building ClipSave $VERSION (${TASKS[*]})"
./gradlew "${TASKS[@]}" --console=plain

# --- Collect + verify -----------------------------------------------------------------------------
APK_DIR="app/build/outputs/apk/release"
shopt -s nullglob
built=("$APK_DIR"/*.apk)
(( ${#built[@]} > 0 )) || die "Build finished but no APKs found in $APK_DIR."

say "Collecting APKs into outputs/"
mkdir -p "$OUT"
rm -f "$OUT"/ClipSave-*.apk "$OUT"/SHA256SUMS.txt "$OUT"/mapping-*.txt

for apk in "${built[@]}"; do
  # app-arm64-v8a-release.apk -> arm64-v8a ; app-universal-release.apk -> universal
  abi="$(basename "$apk" .apk)"; abi="${abi#app-}"; abi="${abi%-release}"
  [[ "$abi" != *unsigned* ]] || die "$(basename "$apk") is unsigned — check keystore.properties."
  dest="$OUT/ClipSave-$VERSION-$abi.apk"
  cp "$apk" "$dest"
  # apksigner prints JDK native-access warnings on stderr; only show its output if verification fails.
  if ! check="$("$APKSIGNER" verify "$dest" 2>&1)"; then echo "$check" >&2; die "Signature verification failed for $(basename "$dest")."; fi
done

[[ -f app/build/outputs/mapping/release/mapping.txt ]] && cp app/build/outputs/mapping/release/mapping.txt "$OUT/mapping-$VERSION.txt"
(cd "$OUT" && sha256 ClipSave-*.apk > SHA256SUMS.txt)

CERT="$("$APKSIGNER" verify --print-certs "$(ls "$OUT"/ClipSave-*-universal.apk | head -1)" 2>/dev/null | sed -n 's/.*certificate SHA-256 digest: //p' | head -1)"

say "Done — signed APKs verified"
ls -lh "$OUT" | awk 'NR>1 {printf "   %-8s %s\n", $5, $9}'
echo
echo "   Signing certificate SHA-256: $CERT"
echo "   Output folder:               $OUT"
