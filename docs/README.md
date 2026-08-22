# G-Earth
Feature-rich habbo packet logger & manipulator for Windows, Linux and Mac.

- The packaged application includes Java 21. Java 8 is only needed as a separate runtime for legacy extensions.

[CLICK HERE FOR TROUBLESHOOTING](https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting)

Join the G-Earth [Discord server](https://discord.gg/AVkcF8y)

**Download the [chrome extension](https://chrome.google.com/webstore/detail/g-chrome/cdjgbghobmfmfcenhoahgfnfpcadddag) if you're using Unity**

# Execution
* **Windows:** Double click G-Earth.exe, which will be delivered in the release. Double clicking the .jar file might work as well.
* **Linux** Execute `sudo java -jar G-Earth.jar` on the command line. You can include the `t` flag to log packets in your terminal
* **Mac:** Open `G-Earth.app`. For Flash/AIR memory inspection and legacy Java 8 extensions, follow the [macOS installation guide](wiki/macOs-Installation-guide.md).

## macOS and Apple Silicon

The macOS package supports both Apple Silicon (`arm64`) and Intel (`x86_64`) without Rosetta. It includes:

- a bundled Java 21 runtime for G-Earth;
- a universal, signed `G-MemZ` helper built from source;
- `Prepare-Habbo-for-G-Mem.command`, which prepares a user-owned Habbo AIR development copy without disabling SIP; and
- `Install-Extension-Java.command`, which installs a separate Java 8/JavaFX 8 runtime for legacy extensions.

G-Earth must remain on Java 21. Legacy extension processes can use Java 8 independently through `ExtensionRuntime`, `GEARTH_EXTENSION_JAVA`, `JAVA8_HOME` or the `gearth.extension.java` system property.

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
