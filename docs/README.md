# G-Earth
Feature-rich habbo packet logger & manipulator for Windows, Linux and Mac.

- The packaged application includes Java 21. Java 8 is only needed as a separate runtime for legacy extensions.

Join the G-Earth [Discord server](https://discord.gg/AVkcF8y)

**Download the [chrome extension](https://chrome.google.com/webstore/detail/g-chrome/cdjgbghobmfmfcenhoahgfnfpcadddag) if you're using Unity**

# Installation and execution

* **Windows:** Double-click `G-Earth.exe` from the Windows release package.
* **Linux:** Start G-Earth from the launcher included in the Linux package for your architecture.

## macOS — Apple Silicon and Intel

The macOS build no longer requires a system Java installation, manual signing of G-Mem or disabling System Integrity Protection. Download the artifact matching your Mac:

- `mac-aarch64` for Apple Silicon (`M1`, `M2`, `M3`, `M4` and later);
- `mac-x64` for Intel Macs.

Extract the package and keep `G-Earth.app` and `G-Earth.command` together in the same directory. G-Earth includes its own Java 21 runtime; do not replace it with Java 8 and do not start it with `java -jar`.

### Nitro and normal use

Open the application from Finder or run:

```shell
open "G-Earth.app"
```

If Gatekeeper blocks the first launch, open **System Settings > Privacy & Security** and select **Open Anyway**.

### Habbo AIR/Flash memory mode

AIR memory inspection needs a locally signed development copy of Habbo. Quit Habbo and Habbo Launcher completely, then locate the current application:

```shell
find "$HOME/Library/Application Support/Habbo Launcher/downloads/air" \
    -maxdepth 3 -type d -name Habbo.app
```

Create and prepare a copy without `sudo`, replacing `/actual/path/to/Habbo.app` with the path returned above:

```shell
cd "G-Earth.app/Contents/Resources"
mkdir -p Habbo-Development
ditto "/actual/path/to/Habbo.app" "Habbo-Development/Habbo.app"
./Prepare-Habbo-for-G-Mem.command "Habbo-Development/Habbo.app"
./Prepare-Habbo-for-G-Mem.command --check "Habbo-Development/Habbo.app"
```

Back up `~/Library/Application Support/Habbo Launcher/versions.json`. In its `installations` array, change only the `path` of the entry whose `client` is `air`; it must point to the absolute `Habbo-Development` directory, not to `Habbo.app`. Restart Habbo Launcher after saving the file.

Start G-Earth from the directory that contains the application:

```shell
./G-Earth.command
```

Then click **Connect** in G-Earth before starting the AIR client from Habbo Launcher. `G-Earth.command` pins the main application to its bundled Java 21 runtime and requests administrator privileges for the required loopback configuration.

### Java 8 extensions

Legacy graphical extensions can use an isolated Java 8/JavaFX 8 runtime while G-Earth remains on Java 21:

```shell
cd "G-Earth.app/Contents/Resources"
./Install-Extension-Java.command
cd ../../..
./G-Earth.command --check-extension-java
```

The installer selects the correct architecture, verifies the download checksum and installs the secondary runtime under `G-Earth.app/Contents/Resources/ExtensionRuntime`.

For detailed verification, updates, restoration and troubleshooting, see the [current macOS installation guide](wiki/macOs-Installation-guide.md). The current setup does **not** require disabling SIP.

# Features
* Log outgoing and incoming packets
* Inject, block & replace packets on the fly
* Automatic packet expression prediction
* Auto detect hotel
* Retro support - enter game host & port manually (only the first time)
* Advanced scheduler
* Advanced extension support
* Python scripting on the fly
* SOCKS proxy
* Identify packets through [Harble API](https://api.harble.net/messages/) and [sulek.dev](https://www.sulek.dev)
* Supports both Unity (browser) and Flash


# Extensions

Interested in creating your own extension? Check one of the frameworks: 

Name | Language | Developers | Github
--- | --- | --- | --- |
G-Earth (Native) | Java | sirjonasxx | https://github.com/sirjonasxx/G-Earth
G-Python<sup>1</sup> | Python | sirjonasxx | https://github.com/sirjonasxx/G-Python
Xabbo | C# | b7 | https://github.com/b7c/Xabbo.GEarth
Xabbo scripter<sup>2</sup> | C# | b7 | https://github.com/b7c/Xabbo.Scripter
G-Node | Node.js | WiredSpast | https://github.com/WiredSpast/G-Node
G-Rust | Rust | WiredSpast | https://github.com/WiredSpast/G-Rust
GProgrammer<sup>3</sup> | Javascript | at15four2020 | https://github.com/at15four2020/GProgrammer/wiki

<sub>1: built-in in G-Earth through the [live scripting console](https://github.com/sirjonasxx/G-Earth/wiki/G-Python-qtConsole) </sub>  
<sub>2: not an implementation of the extension API, but allows for C# scripting </sub>  
<sub>3: not an implementation of the extension API, but allows for Javascript scripting, also check [G-WiredFly](https://github.com/at15four2020/G-Wiredfy) </sub>

Release your extensions in the [G-ExtensionStore](https://github.com/sirjonasxx/G-ExtensionStore)

For the memorysearcher that extracts the RC4 table, go to [G-Mem](https://github.com/sirjonasxx/G-Mem).
