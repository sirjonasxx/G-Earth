# G-Earth
Feature-rich habbo packet logger & manipulator for Windows, Linux and Mac.

- Requires Java 8

[CLICK HERE FOR TROUBLESHOOTING](https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting)

Join the G-Earth [Discord server](https://discord.gg/AVkcF8y)

**Download the [chrome extension](https://chrome.google.com/webstore/detail/g-chrome/cdjgbghobmfmfcenhoahgfnfpcadddag) if you're using Unity**

# Execution
* **Windows:** Double click G-Earth.exe, which will be delivered in the release. Double clicking the .jar file might work as well.
* **Linux** Execute `sudo java -jar G-Earth.jar` on the command line. You can include the `t` flag to log packets in your terminal
* **Mac** Follow the [MacOs Installation guide](https://github.com/sirjonasxx/G-Earth/wiki/macOs-Installation-guide) (flash only)

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
Geode | C# & Visual Basic | ArachisH, LilithRainbows | https://github.com/LilithRainbows/Geode
Xabbo | C# | b7 | https://github.com/b7c/Xabbo.Scripter
G-Node | Node.js | WiredSpast | https://github.com/WiredSpast/G-Node
GProgrammer<sup>2</sup> | Javascript | at15four2020 | https://github.com/at15four2020/GProgrammer/wiki

<sub>1: built-in in G-Earth through the [live scripting console](https://github.com/sirjonasxx/G-Earth/wiki/G-Python-qtConsole) </sub>  
<sub>2: not an implementation of the extension API, but allows for Javascript scripting, also check [G-WiredFly](https://github.com/at15four2020/G-Wiredfy) </sub>

Release your extensions in the [G-ExtensionStore](https://github.com/sirjonasxx/G-ExtensionStore)

For the memorysearcher that extracts the RC4 table, go to [G-Mem](https://github.com/sirjonasxx/G-Mem).
