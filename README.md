<a href="https://discord.gg/AVkcF8y">
    <img src="https://img.shields.io/discord/744927320871010404?color=%237289da&label=Join%20us!&logo=discord&logoColor=white" alt="Discord bagde" title="Join us on Discord!" align="right" />
</a>

# G-Earth

<a href="https://discord.gg/AVkcF8y">
    <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/themes/G-Earth/logo.png?raw=true" alt="G-Earth logo" title="G-Earth" align="right" height="100" />
</a>

Feature-rich habbo packet logger & manipulator for Windows, Linux and Mac.

- Requires Java 8

[CLICK HERE FOR TROUBLESHOOTING](https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting)

## Getting Started

### Requirements

This project runs with Java + JavaFX. If you are only planning to run the G-Earth and its native extensions, you only need the JRE version, but if you are insterested in develop extensions, you might need the JDK version for native extensions, but there are [other frameworks](#frameworks).

You can download the JRE and the JDK that comes with JavaFX from [Oracle downloads page](https://www.oracle.com/java/technologies/downloads/#java8).

> Note that the Java 8 is the most stable and compatible with G-Earth, later versions of Java doesn't come with JavaFX and we will not help with installations of newer versions.

### Instalation

1. Go to the latest release page [here](https://github.com/sirjonasxx/G-Earth/releases/latest);
1. Look for your OS specifc version in the assets section and download it;
1. Extract it to a new folder;

> If you are playing in the browser-based Unity client, you must install the [G-Chrome extension](https://chrome.google.com/webstore/detail/g-chrome/cdjgbghobmfmfcenhoahgfnfpcadddag) to make it work with G-Earth.

> **For Mac users**: follow the [MacOs Installation guide](https://github.com/sirjonasxx/G-Earth/wiki/macOs-Installation-guide) (flash only).

### Running

Run the `G-Earth` **executable** and it should ask for admin permissions, otherwise you need to right click on it and select to run as admin. Double clicking the `.jar` file might work as well.

You can run the `G-Earth.jar` file with `java -jar G-Earth.jar` on a admin-enabled terminal. If you include the `t` flag, the packets will be printed in your terminal.

## Features

* Auto detect hotel
* Retro support - enter game host & port manually (only the first time)
* Log outgoing and incoming packets
  * There are three packets representations: legacy, structure/expression (prediction) and hex;
* Inject, block & replace packets on the fly
* Advanced scheduler
* Advanced extension support
* Python scripting on the fly
* SOCKS proxy
* Identify packets through [Harble API](https://api.harble.net/messages/) and [sulek.dev](https://www.sulek.dev)
* Supports both Unity (browser) and Flash

### Connection

TODO

### Injection

TODO

### Tools

TODO

### Scheduler

TODO

### Extensions

The most commom extensions uses the native framework, as it doesn't required further installations from who will use it. These extensions are a single `.jar` file, and you can add it to G-Earth fllowing these steps:

1. Go to the "Extensions" tab;
1. Click in the "Install" button;
1. An file picker will shown, select the extension `.jar` file;
1. In a couple of seconds the extension should shown in the list.

You can close any non-internal extension by clicking in the "red arrow" icon. It won't uninstall it, you can restart the extension by clicking in the "refresh" icon but if you want to uninstall click on the "red cross" icon. The "green play" icon is an extension-specific feature, it allows the extension to do some task only after you click on it (eg open external window).

Icon|Function
-|-
![exit icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonExit.png?raw=true)|Close/exit the extension (the extension will stop working).
![exit icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonReload.png?raw=true)|Restart/reload the extension.
![exit icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonDelete.png?raw=true)|Uninstall/delete the extension.
![exit icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonResume.png?raw=true)|Extension-specific action.

#### Frameworks

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
<sub>2: not an implementation of the extension API, but allows for Javascript scripting, also check [G-Wiredfy](https://github.com/at15four2020/G-Wiredfy) </sub>

Release your extensions in the [G-ExtensionStore](https://github.com/sirjonasxx/G-ExtensionStore)

### Extra

TODO

## Others

For the memorysearcher that extracts the RC4 table, go to [G-Mem](https://github.com/sirjonasxx/G-Mem).

# How to Contribute

TODO (fork)

1. Clone repo and create a new branch: $ git checkout https://github.com/alichtman/stronghold -b name_for_new_branch.
1. Make changes and test
1. Submit Pull Request with comprehensive description of changes

# Donations

This is free, open-source software. If you'd like to support the development of future projects, or say thanks for this one, you can donate BTC at `1GEarthEV9Ua3RcixsKTcuc1PPZd9hqri3`.