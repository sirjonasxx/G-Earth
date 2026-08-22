# macOS Apple Silicon installation guide

This repository packages G-Earth specifically for Apple Silicon (`arm64`) with its own Java 21 runtime. The `G-MemZ` helper remains a universal `arm64`/`x86_64` binary, but Intel users should download G-Earth from the [official releases](https://github.com/G-Realm/G-Earth/releases).

This guide covers the additional steps required for Habbo AIR memory inspection and legacy Java 8 extensions.

## Package components

Inside `G-Earth.app/Contents/Resources` you will find:

- `G-MemZ`: the universal helper that locates Habbo and reads the RC4 state.
- `Install-Extension-Java.command`: installs an independent Java 8/JavaFX 8 runtime.
- `Prepare-Habbo-for-G-Mem.command`: prepares a local Habbo AIR development copy.
- `ExtensionRuntime`: the optional Java 8 runtime directory used only by extensions.

The application itself always uses the Java 21 runtime bundled in `G-Earth.app`. Do not replace it with Java 8.

The package also places `G-Earth.command` next to `G-Earth.app`. This Terminal launcher pins the main process to the bundled Java 21 runtime and requests administrator privileges when Flash/AIR mode needs them.

## Starting G-Earth

For Nitro or normal use, open `G-Earth.app` from Finder:

```shell
open "G-Earth.app"
```

Flash/AIR mode modifies loopback aliases and may need administrator privileges. Run the packaged launcher from Terminal:

```shell
./G-Earth.command
```

Calling `sudo ./G-Earth.command` explicitly is also supported. Do not run the internal native launcher or `java -jar G-Earth.jar` directly, because doing so can select an incompatible system JVM.

If Gatekeeper blocks the application after the first launch, open **System Settings > Privacy & Security** and choose **Open Anyway**.

## Java 8 for legacy extensions

Legacy graphical extensions may require Java 8 and JavaFX 8 even though G-Earth itself requires Java 21. Both runtimes can coexist because extensions run in separate processes.

Install the secondary runtime without `sudo`:

```shell
cd "G-Earth.app/Contents/Resources"
./Install-Extension-Java.command
```

The installer:

1. detects Apple Silicon or Intel;
2. requests the matching Azul Zulu Java 8u442 JRE with JavaFX 8;
3. accepts only an HTTPS download returned by Azul's metadata API;
4. verifies the SHA-256 checksum from the package metadata;
5. checks the archive paths before extraction;
6. validates Java 8 and `jfxrt.jar`; and
7. preserves any existing runtime in a timestamped backup.

Java 8u442 is suitable for extensions compiled for Java 8, including extensions whose manifest reports Oracle Java 8u441. The bytecode target and JavaFX 8 availability matter; the exact vendor build number does not.

Verify the selected extension runtime:

```shell
./G-Earth.command --check-extension-java
```

The resolver checks these locations in order:

1. `-Dgearth.extension.java=/path/to/java8`;
2. `GEARTH_EXTENSION_JAVA`;
3. `JAVA8_HOME`;
4. the adjacent `ExtensionRuntime` directory;
5. known Java installation directories; and
6. `PATH`, only if the executable is actually Java 8.

An explicit absolute Java command in an extension is respected. Unqualified `java` and `javaw` commands for legacy JavaFX extensions are redirected to the validated Java 8 runtime, with `JAVA_HOME` and `PATH` updated for the child process.

## G-MemZ on Apple Silicon

The packaged `G-MemZ` is built as a universal `arm64`/`x86_64` executable. It is ad-hoc signed with Hardened Runtime and the debugger-side entitlement `com.apple.security.cs.debugger`.

Verify it without changing its signature:

```shell
cd "G-Earth.app/Contents/Resources"
lipo -archs G-MemZ
codesign --verify --strict G-MemZ
codesign -d --entitlements :- G-MemZ
./G-MemZ --self-test
```

The architecture list must contain both `arm64` and `x86_64`, and the entitlement output must contain `com.apple.security.cs.debugger`.

A custom helper can be selected before starting G-Earth:

```shell
export GEARTH_GMEM_PATH="/absolute/path/to/G-MemZ"
```

## Why Habbo AIR must be prepared

macOS requires permission on both sides of a debugging connection:

- `G-MemZ` needs `com.apple.security.cs.debugger`.
- The Habbo application being inspected needs `com.apple.security.get-task-allow`.

Running G-Earth with `sudo` does not add the target-side entitlement. Disabling System Integrity Protection is not required and is not part of this setup.

The recommended method uses a user-owned development copy. It does not modify or delete Habbo Launcher's managed download.

## Create a Habbo development copy

Quit Habbo and Habbo Launcher with **Command-Q**. Closing only their windows is not enough.

Locate the current AIR application:

```shell
find "$HOME/Library/Application Support/Habbo Launcher/downloads/air" \
    -maxdepth 3 -type d -name Habbo.app
```

The AIR version directory can change after updates. Use the current path returned by the command.

Create the development copy next to the packaged resources:

```shell
cd "G-Earth.app/Contents/Resources"
mkdir -p Habbo-Development
ditto "$HOME/Library/Application Support/Habbo Launcher/downloads/air/<version>/Habbo.app" \
    "Habbo-Development/Habbo.app"
./Prepare-Habbo-for-G-Mem.command "Habbo-Development/Habbo.app"
./Prepare-Habbo-for-G-Mem.command --check "Habbo-Development/Habbo.app"
```

Run the preparation command without `sudo`. It:

1. validates the `com.sulake.habboair` bundle identifier;
2. refuses to modify a running Habbo process;
3. preserves a `.gearth-original` backup when preparing an application directly;
4. keeps the Adobe AIR permissions required for JIT and executable memory;
5. adds `com.apple.security.get-task-allow`;
6. signs nested Adobe AIR and Discord frameworks from the inside out; and
7. performs a deep signature verification.

The final check must report that Habbo is prepared for G-Mem.

You can also verify it manually:

```shell
codesign --verify --deep --strict --verbose=2 "Habbo-Development/Habbo.app"
codesign -d --entitlements :- "Habbo-Development/Habbo.app"
lipo -archs "Habbo-Development/Habbo.app/Contents/MacOS/Habbo"
```

Do not redistribute the prepared Habbo application. The development entitlement weakens process isolation only for that local copy.

## Point Habbo Launcher to the copy

Habbo Launcher reads the AIR installation from:

```text
~/Library/Application Support/Habbo Launcher/versions.json
```

Back it up before editing:

```shell
cp -n "$HOME/Library/Application Support/Habbo Launcher/versions.json" \
    "$HOME/Library/Application Support/Habbo Launcher/versions.json.gearth-original"
```

In the `installations` array, find the entry whose `client` is `air`. Change only its `path` to the absolute directory containing the prepared `Habbo.app`.

The value must end with `Habbo-Development`, not `Habbo-Development/Habbo.app`:

```json
{
  "version": "12",
  "path": "/Users/your-name/path/to/G-Earth.app/Contents/Resources/Habbo-Development",
  "client": "air",
  "lastModified": 1787327986088
}
```

Keep the existing version and timestamp values. Quit and reopen Habbo Launcher so it reloads the file.

## Connection order

1. Start G-Earth from Terminal with administrator privileges for Flash/AIR mode.
2. Click **Connect** in G-Earth.
3. Start the AIR client from Habbo Launcher.
4. Wait for `G-MemZ` to identify the connection and extract the RC4 state.

Starting Habbo before clicking **Connect** can prevent the connection from being intercepted correctly.

## Updates and restoration

After a Habbo AIR update:

1. quit Habbo and Habbo Launcher;
2. locate the new managed `Habbo.app`;
3. replace the development copy with a fresh copy;
4. run `Prepare-Habbo-for-G-Mem.command` again; and
5. confirm that `versions.json` still points to `Habbo-Development`.

Restore Habbo Launcher's original configuration with:

```shell
cp "$HOME/Library/Application Support/Habbo Launcher/versions.json.gearth-original" \
   "$HOME/Library/Application Support/Habbo Launcher/versions.json"
```

Restart Habbo Launcher afterward.

## Troubleshooting

- **Unable to locate a Java Runtime:** use the packaged `G-Earth.app` or `G-Earth.command`; both select the included Java 21 runtime and do not require a system JVM.
- **JVM crash with Java 26:** do not run the JAR or internal launcher directly. Use `G-Earth.command` for Terminal/administrator mode.
- **No Java 8 runtime was found for extensions:** run `Install-Extension-Java.command` from `G-Earth.app/Contents/Resources`.
- **JavaFX classes are missing in an extension:** the selected Java 8 installation lacks JavaFX 8. Use the provided installer.
- **Permission denied for G-MemZ:** restore its executable bit with `chmod 755 G-MemZ`, then verify its signature.
- **G-MemZ exit code 4, `task_for_pid` failure or `(os/kern) failure`:** the running Habbo copy does not contain `com.apple.security.get-task-allow`, or Habbo Launcher started its managed copy instead of `Habbo-Development`.
- **Operation not permitted while signing Habbo:** create the user-owned development copy. `sudo` does not bypass macOS App Management protection.
- **Connection is detected but packets cannot be decrypted:** verify the running Habbo path, restart Habbo Launcher after editing `versions.json`, and click **Connect** before launching Habbo.
- **Bad CPU type in executable:** verify that `lipo -archs G-MemZ` reports the current architecture.
- **Nitro proxy does not start:** ensure at least one network service is enabled and approve the macOS administrator dialog.
- **A proxy remains enabled after an abnormal exit:** disable **Web proxy (HTTP)** and **Secure web proxy (HTTPS)** under **System Settings > Network > Details > Proxies**.

When reporting a problem, include the log lines around `G-MemZ`, `RC4`, `task_for_pid`, `(os/kern)` and `exit code`.
