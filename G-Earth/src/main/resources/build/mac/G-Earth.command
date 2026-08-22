#!/bin/sh

set -eu

SCRIPT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
APP="$SCRIPT_DIRECTORY/G-Earth.app"
LAUNCHER="$APP/Contents/MacOS/nativeJavaApplicationStub"
MAIN_JAVA_HOME="$APP/Contents/PlugIns/jre.jre/Contents/Home"

if [ ! -x "$LAUNCHER" ]; then
    echo "G-Earth launcher not found at: $LAUNCHER" >&2
    exit 1
fi

if [ ! -x "$MAIN_JAVA_HOME/bin/java" ]; then
    echo "The bundled Java 21 runtime is missing or incomplete: $MAIN_JAVA_HOME" >&2
    exit 1
fi

# Keep application data under the invoking user's account when the launcher is
# called through sudo. On macOS sudo normally preserves HOME, but resolve it
# explicitly for configurations that reset it to /var/root.
CALLER_HOME=${HOME:-}
if [ -n "${SUDO_USER:-}" ] && [ "$SUDO_USER" != "root" ]; then
    RESOLVED_HOME=$(/usr/bin/dscl . -read "/Users/$SUDO_USER" NFSHomeDirectory 2>/dev/null \
        | /usr/bin/awk '{print $2}' || true)
    if [ -n "$RESOLVED_HOME" ]; then
        CALLER_HOME=$RESOLVED_HOME
    fi
fi

run_launcher() {
    HOME=$CALLER_HOME JAVA_HOME=$MAIN_JAVA_HOME exec "$LAUNCHER" "$@"
}

# Runtime diagnostics do not modify network interfaces and do not need root.
if [ "${1:-}" = "--check-extension-java" ]; then
    run_launcher "$@"
fi

if [ "$(/usr/bin/id -u)" -eq 0 ]; then
    run_launcher "$@"
fi

exec /usr/bin/sudo /usr/bin/env \
    "HOME=$CALLER_HOME" \
    "JAVA_HOME=$MAIN_JAVA_HOME" \
    "$LAUNCHER" "$@"
