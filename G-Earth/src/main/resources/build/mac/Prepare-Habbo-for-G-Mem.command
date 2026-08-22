#!/bin/sh

set -eu

MODE=prepare
if [ "${1:-}" = "--check" ]; then
    MODE=check
    shift
fi

if [ "$#" -gt 1 ]; then
    printf 'Usage: %s [--check] [/path/to/Habbo.app]\n' "$0" >&2
    exit 2
fi

caller_home() {
    if [ -n "${SUDO_USER:-}" ] && [ "$SUDO_USER" != "root" ]; then
        /usr/bin/dscacheutil -q user -a name "$SUDO_USER" 2>/dev/null \
            | /usr/bin/awk '/^dir: / { sub(/^dir: /, ""); print; exit }'
    else
        printf '%s\n' "${HOME:-}"
    fi
}

find_habbo_app() {
    downloads_dir="$(caller_home)/Library/Application Support/Habbo Launcher/downloads/air"
    latest_app=""
    for candidate in "$downloads_dir"/*/Habbo.app
    do
        [ -d "$candidate" ] || continue
        if [ -z "$latest_app" ] || [ "$candidate" -nt "$latest_app" ]; then
            latest_app=$candidate
        fi
    done
    printf '%s\n' "$latest_app"
}

APP_PATH=${1:-}
if [ -z "$APP_PATH" ]; then
    APP_PATH=$(find_habbo_app)
fi

if [ -z "$APP_PATH" ] || [ ! -d "$APP_PATH" ]; then
    printf '%s\n' "Habbo.app was not found. Pass its full path as the first argument." >&2
    exit 2
fi

INFO_PLIST="$APP_PATH/Contents/Info.plist"
HABBO_EXECUTABLE="$APP_PATH/Contents/MacOS/Habbo"
if [ ! -f "$INFO_PLIST" ] || [ ! -f "$HABBO_EXECUTABLE" ]; then
    printf 'Not a supported Habbo AIR application: %s\n' "$APP_PATH" >&2
    exit 2
fi

BUNDLE_ID=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$INFO_PLIST" 2>/dev/null || true)
if [ "$BUNDLE_ID" != "com.sulake.habboair" ]; then
    printf 'Unexpected application identifier %s at %s\n' "$BUNDLE_ID" "$APP_PATH" >&2
    exit 2
fi

ARCH=$(/usr/bin/uname -m)
CURRENT_ENTITLEMENTS=$(/usr/bin/codesign -d --arch "$ARCH" --entitlements :- "$APP_PATH" 2>&1 || true)
if printf '%s\n' "$CURRENT_ENTITLEMENTS" | /usr/bin/grep -q 'com.apple.security.get-task-allow'; then
    printf 'Habbo is already prepared for G-Mem: %s\n' "$APP_PATH"
    exit 0
fi

if [ "$MODE" = check ]; then
    printf '%s\n' \
        "Habbo does not allow memory inspection (com.apple.security.get-task-allow is absent)." \
        "Run this command without --check, while Habbo is closed, to prepare it."
    exit 1
fi

if [ "$(/usr/bin/id -u)" -eq 0 ]; then
    printf '%s\n' "Run this preparation command without sudo so Habbo keeps your user ownership." >&2
    exit 2
fi

if /usr/bin/pgrep -x Habbo >/dev/null 2>&1; then
    printf '%s\n' "Close Habbo before changing its local development signature." >&2
    exit 2
fi

BACKUP_PATH="$APP_PATH.gearth-original"
if [ ! -e "$BACKUP_PATH" ]; then
    printf 'Creating backup at %s\n' "$BACKUP_PATH"
    /usr/bin/ditto "$APP_PATH" "$BACKUP_PATH"
else
    printf 'Keeping existing backup at %s\n' "$BACKUP_PATH"
fi

TEMP_DIR=$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/gearth-habbo-sign.XXXXXX")
ENTITLEMENTS="$TEMP_DIR/Habbo-debug.entitlements"
cleanup() {
    /bin/rm -rf "$TEMP_DIR"
}
trap cleanup EXIT HUP INT TERM

/bin/cat > "$ENTITLEMENTS" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key>
    <true/>
    <key>com.apple.security.cs.allow-jit</key>
    <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <true/>
    <key>com.apple.security.cs.disable-library-validation</key>
    <true/>
    <key>com.apple.security.files.user-selected.read-write</key>
    <true/>
    <key>com.apple.security.get-task-allow</key>
    <true/>
    <key>com.apple.security.network.client</key>
    <true/>
    <key>com.apple.security.network.server</key>
    <true/>
</dict>
</plist>
PLIST

/usr/bin/plutil -lint "$ENTITLEMENTS" >/dev/null

AIR_FRAMEWORK="$APP_PATH/Contents/Frameworks/Adobe AIR.framework"
DISCORD_FRAMEWORK="$APP_PATH/Contents/Frameworks/DiscordRichPresence.framework"

for executable in \
    "$AIR_FRAMEWORK/Versions/1.0/Adobe AIR" \
    "$DISCORD_FRAMEWORK/Versions/A/DiscordRichPresence"
do
    if [ -f "$executable" ]; then
        /usr/bin/codesign --force --sign - --timestamp=none "$executable"
    fi
done

for framework in "$AIR_FRAMEWORK" "$DISCORD_FRAMEWORK"
do
    if [ -d "$framework" ]; then
        /usr/bin/codesign --force --sign - --timestamp=none "$framework"
    fi
done

/usr/bin/codesign --force --sign - --timestamp=none --options runtime \
    --entitlements "$ENTITLEMENTS" "$APP_PATH"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$APP_PATH"

SIGNED_ENTITLEMENTS="$TEMP_DIR/signed-entitlements.plist"
/usr/bin/codesign -d --arch "$ARCH" --entitlements - --xml "$APP_PATH" \
    > "$SIGNED_ENTITLEMENTS" 2>/dev/null
if [ "$(/usr/libexec/PlistBuddy -c 'Print :com.apple.security.get-task-allow' "$SIGNED_ENTITLEMENTS")" != "true" ]; then
    printf '%s\n' "The development entitlement could not be applied." >&2
    exit 1
fi

printf '%s\n' \
    "Habbo is prepared for local G-Mem debugging." \
    "If Habbo Launcher replaces the application during an update, run this command again." \
    "Original backup: $BACKUP_PATH"
