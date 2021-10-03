<h1 align="center" id="logo"><img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/themes/G-Earth/logo.png?raw=true" alt="G-Earth logo" title="G-Earth" height="100" /><br/>G-Earth</h1>

<p align="center">
    <b>Feature-rich Habbo packet logger & manipulator for Windows, Linux and Mac.</b>
</p>

<p align="center">
    <a href="https://discord.gg/AVkcF8y">
        <img src="https://img.shields.io/discord/744927320871010404?color=%237289da&label=Join%20us!&logo=discord&logoColor=white&style=flat-square" alt="Discord bagde" title="Join us on Discord!" />
    </a>
    <a href="https://github.com/sirjonasxx/G-Earth/commits/master">
        <img src="https://img.shields.io/github/release-date/sirjonasxx/G-Earth?logo=github&label=Last%20release&style=flat-square" title="Release date bagde" />
    </a>
    <a href="https://github.com/sirjonasxx/G-Earth/commits/master">
        <img src="https://img.shields.io/github/downloads/sirjonasxx/G-Earth/total?label=Total %20downloads&logo=github&style=flat-square" title="Total downloads badge" />
    </a>
    <a href="https://github.com/sirjonasxx/G-Earth/commits/master">
        <img src="https://img.shields.io/github/contributors/sirjonasxx/G-Earth?logo=github&style=flat-square&label=Contributors" title="Last commit bagde" />
    </a>
    <a href="https://github.com/sirjonasxx/G-Earth/commits/master">
        <img src="https://img.shields.io/github/issues/sirjonasxx/G-Earth?logo=github&style=flat-square&label=Issues" title="Issues counter badge" />
    </a>
</p>

<p align="center" id="logo">
    <a href="#about">About</a> •
    <a href="#getting-started">Getting Started</a> •
    <a href="#features">Features</a> •
    <a href="#wiki">Wiki</a> •
    <a href="#contributing">Contributing</a> •
    <a href="#credits">Credits</a> •
    <a href="#troubleshooting">Troubleshooting</a> •
    <a href="#donations">Donations</a> •
    <a href="#license">License</a>
</p>

## 📖 About

[(Back to top)](#logo)

TODO

## ⚡️ Getting Started

[(Back to top)](#logo)

### ☝️ Requirements

This project runs with Java + JavaFX. If you are only planning to run the G-Earth and its native extensions, you only need the JRE version, but if you are insterested in develop extensions, you might need the JDK version for native extensions, but there are [other frameworks](#frameworks).

You can download the JRE and the JDK that comes with JavaFX from [Oracle downloads page](https://www.oracle.com/java/technologies/downloads/#java8).

> 🔔 **Please note**: the Java 8 is the most stable and compatible with G-Earth, later versions of Java doesn't come with JavaFX and we will not help with installations of newer versions.

### ✅ Instalation

1. Go to the latest release page [here](https://github.com/sirjonasxx/G-Earth/releases/latest);
1. Look for your OS specifc version in the assets section and download it;
1. Extract it to a new folder;

> 🔔 If you are playing in the browser-based Unity client, you must install the [G-Chrome extension](https://chrome.google.com/webstore/detail/g-chrome/cdjgbghobmfmfcenhoahgfnfpcadddag) to make it work with G-Earth.

> 🔔 **For Mac users**: follow the [MacOs Installation guide](https://github.com/sirjonasxx/G-Earth/wiki/macOs-Installation-guide) (flash only).

### 🏃 Running

Run the `G-Earth` **executable** and it should ask for admin permissions, otherwise you need to right click on it and select to run as admin. Double clicking the `.jar` file might work as well.

You can run the `G-Earth.jar` file with `java -jar G-Earth.jar` on a admin-enabled terminal. If you include the `t` flag, the packets will be printed in your terminal.

## ⚙️ Features

[(Back to top)](#logo)

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

### 🌐 Connection

TODO

### 💉 Injection

TODO

### 🛠️ Tools

TODO

### ⏲️ Scheduler

TODO

### 🧩 Extensions

The most commom extensions uses the native framework, as it doesn't required further installations from who will use it. These extensions are a single `.jar` file, and you can add it to G-Earth fllowing these steps:

1. Go to the "Extensions" tab;
1. Click in the "Install" button;
1. An file picker will shown, select the extension `.jar` file;
1. In a couple of seconds the extension should shown in the list.

You can close any non-internal extension by clicking in the <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonExit.png?raw=true" title="Red arrow icon" /> icon. It won't uninstall it, you can restart the extension by clicking in the <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonReload.png?raw=true" title="Refresh icon" /> icon but if you want to uninstall click on the <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonDelete.png?raw=true" title="Red cross icon" /> icon. The <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonResume.png?raw=true" title="Green play icon" /> icon is an extension-specific feature, it allows the extension to do some task only after you click on it (eg open external window).

Icon|Function
-|-
![red arrow icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonExit.png?raw=true)|Close/exit the extension (the extension will stop working).
![refresh icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonReload.png?raw=true)|Restart/reload the extension.
![red cross icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonDelete.png?raw=true)|Uninstall/delete the extension.
![green play icon](https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonResume.png?raw=true)|Extension-specific action.

> 🔔 **Please note**: if the <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonReload.png?raw=true" title="Refresh icon" /> and <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonDelete.png?raw=true" title="Red cross icon" /> icons appear without you click on the <img src="https://github.com/sirjonasxx/G-Earth/blob/master/G-Earth/src/main/resources/gearth/ui/buttons/files/ButtonExit.png?raw=true" title="Red arrow icon" /> icon, it means that the extension crashed. It might be an ocasional error, so you can try again reloading the extension, but if it keeps happening you have the contact the author of the extension.

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

### ➕ Extra

TODO

## 📒 Wiki

[(Back to top)](#logo)

TODO

## Others

[(Back to top)](#logo)

For the memorysearcher that extracts the RC4 table, go to [G-Mem](https://github.com/sirjonasxx/G-Mem).

# ✍️ Contributing

[(Back to top)](#logo)

Please take a look at our [contributing](https://github.com/sirjonasxx/G-Earth/blob/master/CONTRIBUTING.md) guidelines if you're interested in helping!

## To Do List

- Wiki update

# 👨‍💻 Credits

[(Back to top)](#logo)

TODO (https://allcontributors.org/)

# 🐛 Troubleshooting

[(Back to top)](#logo)

[CLICK HERE FOR TROUBLESHOOTING](https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting)

# 🌟 Donations

[(Back to top)](#logo)

This is free, open-source software. If you'd like to support the development of future projects, or say thanks for this one, you can donate BTC at `1GEarthEV9Ua3RcixsKTcuc1PPZd9hqri3`.

# ⚠️ License

[(Back to top)](#logo)

The MIT License (MIT) 2018 - [sirjonasxx](https://github.com/sirjonasxx). Please have a look at the [LICENSE.md](LICENSE.md) for more details.
