#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RUNTIME_TARGET="$SCRIPT_DIR/ExtensionRuntime"
JAVA_VERSION=8u442b06
AZUL_API_BASE=https://api.azul.com/metadata/v1/zulu/packages

if [ "$(/usr/bin/uname -s)" != "Darwin" ]; then
    printf '%s\n' "This installer is for macOS only." >&2
    exit 2
fi

if [ "$(/usr/bin/id -u)" -eq 0 ]; then
    printf '%s\n' "Run this installer without sudo so the runtime remains owned by your user." >&2
    exit 2
fi

case "$(/usr/bin/uname -m)" in
    arm64|aarch64)
        AZUL_ARCH=aarch64
        ;;
    x86_64|amd64)
        AZUL_ARCH=x64
        ;;
    *)
        printf 'Unsupported macOS architecture: %s\n' "$(/usr/bin/uname -m)" >&2
        exit 2
        ;;
esac

for required_tool in /usr/bin/curl /usr/bin/plutil /usr/bin/shasum /usr/bin/tar
do
    if [ ! -x "$required_tool" ]; then
        printf 'Required macOS tool is missing: %s\n' "$required_tool" >&2
        exit 2
    fi
done

EXISTING_JAVA=$(/usr/bin/find "$RUNTIME_TARGET" -type f -path '*/bin/java' 2>/dev/null | /usr/bin/head -n 1 || true)
EXISTING_JAVAFX=$(/usr/bin/find "$RUNTIME_TARGET" -type f -name jfxrt.jar 2>/dev/null | /usr/bin/head -n 1 || true)
if [ -n "$EXISTING_JAVA" ] && [ -n "$EXISTING_JAVAFX" ] \
    && "$EXISTING_JAVA" -version 2>&1 | /usr/bin/grep -q 'version "1\.8\.0_442'
then
    printf '%s\n' "Java 8u442 with JavaFX is already installed in $RUNTIME_TARGET"
    exit 0
fi

TEMP_DIR=$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/gearth-extension-java.XXXXXX")
cleanup() {
    /bin/rm -rf "$TEMP_DIR"
}
trap cleanup EXIT HUP INT TERM

PACKAGE_LIST="$TEMP_DIR/packages.json"
PACKAGE_DETAIL="$TEMP_DIR/package.json"
ARCHIVE="$TEMP_DIR/runtime.tar.gz"
EXTRACTED="$TEMP_DIR/extracted"

QUERY_URL="$AZUL_API_BASE/?java_version=$JAVA_VERSION&os=macos&arch=$AZUL_ARCH&archive_type=tar.gz&java_package_type=jre&javafx_bundled=true&release_status=ga&availability_types=CA&page=1&page_size=1"

printf '%s\n' \
    "Downloading Azul Zulu Java 8 with JavaFX for legacy G-Earth extensions." \
    "G-Earth itself will continue to use its separate Java 21 runtime." \
    "Azul Zulu terms: https://www.azul.com/products/core/openjdk-terms-of-use/"

/usr/bin/curl --fail --silent --show-error --location \
    --proto '=https' --tlsv1.2 \
    -H 'accept: application/json' \
    "$QUERY_URL" \
    --output "$PACKAGE_LIST"

PACKAGE_UUID=$(/usr/bin/plutil -extract 0.package_uuid raw -o - "$PACKAGE_LIST" 2>/dev/null || true)
if [ -z "$PACKAGE_UUID" ]; then
    printf '%s\n' "Azul did not return a Java $JAVA_VERSION JavaFX package for this Mac." >&2
    exit 1
fi

/usr/bin/curl --fail --silent --show-error --location \
    --proto '=https' --tlsv1.2 \
    -H 'accept: application/json' \
    "$AZUL_API_BASE/$PACKAGE_UUID" \
    --output "$PACKAGE_DETAIL"

DOWNLOAD_URL=$(/usr/bin/plutil -extract download_url raw -o - "$PACKAGE_DETAIL" 2>/dev/null || true)
EXPECTED_SHA256=$(/usr/bin/plutil -extract sha256_hash raw -o - "$PACKAGE_DETAIL" 2>/dev/null || true)
PACKAGE_NAME=$(/usr/bin/plutil -extract name raw -o - "$PACKAGE_DETAIL" 2>/dev/null || true)

case "$DOWNLOAD_URL" in
    https://cdn.azul.com/*) ;;
    *)
        printf 'Refusing unexpected download URL: %s\n' "$DOWNLOAD_URL" >&2
        exit 1
        ;;
esac

if [ "${#EXPECTED_SHA256}" -ne 64 ]; then
    printf '%s\n' "Azul package metadata did not contain a valid SHA-256 checksum." >&2
    exit 1
fi
case "$EXPECTED_SHA256" in
    *[!0-9a-fA-F]*)
        printf '%s\n' "Azul package metadata did not contain a valid SHA-256 checksum." >&2
        exit 1
        ;;
esac

printf 'Downloading %s...\n' "$PACKAGE_NAME"
/usr/bin/curl --fail --show-error --location \
    --proto '=https' --tlsv1.2 \
    "$DOWNLOAD_URL" \
    --output "$ARCHIVE"

ACTUAL_SHA256=$(/usr/bin/shasum -a 256 "$ARCHIVE" | /usr/bin/awk '{print $1}')
if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
    printf '%s\n' \
        "The downloaded Java archive failed SHA-256 verification." \
        "Expected: $EXPECTED_SHA256" \
        "Actual:   $ACTUAL_SHA256" >&2
    exit 1
fi

if /usr/bin/tar -tzf "$ARCHIVE" \
    | /usr/bin/awk 'BEGIN { bad=0 } /(^\/|(^|\/)\.\.($|\/))/ { bad=1 } END { exit bad ? 0 : 1 }'
then
    printf '%s\n' "The Java archive contains an unsafe path and will not be extracted." >&2
    exit 1
fi

/bin/mkdir -p "$EXTRACTED"
/usr/bin/tar -xzf "$ARCHIVE" -C "$EXTRACTED"

EXTRACTED_ROOT=$(/usr/bin/find "$EXTRACTED" -mindepth 1 -maxdepth 1 -type d | /usr/bin/head -n 1)
if [ -z "$EXTRACTED_ROOT" ]; then
    printf '%s\n' "The Java archive did not contain a runtime directory." >&2
    exit 1
fi

JAVA_BIN=$(/usr/bin/find "$EXTRACTED_ROOT" -type f -path '*/bin/java' | /usr/bin/head -n 1)
JAVAFX_JAR=$(/usr/bin/find "$EXTRACTED_ROOT" -type f -name jfxrt.jar | /usr/bin/head -n 1)
if [ -z "$JAVA_BIN" ] || [ -z "$JAVAFX_JAR" ]; then
    printf '%s\n' "The downloaded runtime is incomplete or does not include JavaFX 8." >&2
    exit 1
fi
/bin/chmod 755 "$JAVA_BIN"

JAVA_VERSION_OUTPUT=$("$JAVA_BIN" -version 2>&1)
if ! printf '%s\n' "$JAVA_VERSION_OUTPUT" | /usr/bin/grep -q 'version "1\.8\.0_442'; then
    printf '%s\n' "The downloaded runtime is not the expected Java 8u442 build." >&2
    exit 1
fi

if [ -e "$RUNTIME_TARGET" ]; then
    BACKUP_TARGET="$SCRIPT_DIR/ExtensionRuntime.previous-$(/bin/date +%Y%m%d-%H%M%S)"
    printf 'Preserving the previous extension runtime at %s\n' "$BACKUP_TARGET"
    /bin/mv "$RUNTIME_TARGET" "$BACKUP_TARGET"
fi

/bin/mv "$EXTRACTED_ROOT" "$RUNTIME_TARGET"

printf '%s\n' \
    "Java 8 with JavaFX was installed successfully in:" \
    "$RUNTIME_TARGET"
