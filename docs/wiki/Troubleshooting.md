**HABBO AIR CURRENTLY DOESN'T WORK FOR MAC**

There are some known issues with the execution G-Earth, please read this through carefully.

First of all, make sure you have extracted the .rar file into its own folder.
If you're using a mac; make sure you have completed the mac-installation wiki page.

## Problem 1: G-Earth doesn't open
* It's recommended to use Java 8, some problems have occurred with people using Java 11 or other versions. It shouldn't be hard to fix this with a simple google search.
* On Linux, if you get a "Invalid MIT-MAGIC-COOKIE-1" exception, execute "xhost +local:" in a terminal
* On Linux, if you get a "could not find or load main class gearth.Main", javafx might not be installed. Execute "apt install openjfx" to install javafx
* It will almost always be an issue with Java, so try reinstalling

## Problem 2: Stuck at 76% or 57%
* On windows; navigate to https://visualstudio.microsoft.com/downloads/ and download the c++ redistributable 2017 ( https://imgur.com/a/bgvL8fN ), make sure to select the right version.
* Try another browser. (on Mac, use Firefox. On Windows, use Chrome, Firefox or Opera, others might work as well, but IE and Edge do not)
* MAKE SURE your downloaded the right .rar file, for Windows, check if you're running on a 32bit or 64bit device first.
* If you got a message redirecting you to the troubleshooting page, the issue likely has to do with something G-Mem related.
  - Try double clicking G-Mem.exe and see if any errors appear, this may help you in troubleshooting
  - In rare cases, I found people that couldn't use G-Mem.exe if it was executed with admin permissions. If you can verify you have the same issue, you could make your hosts file editable by non-admin users and run G-Earth in a non-admin fashion. Beware: this is a security risk but I found no other solution

## Problem 3: Habbo loads, but G-Earth isn't connected
* Your hosts file might have some permissions issues. Delete it and place a new one: https://github.com/sirjonasxx/G-Earth/issues/17
* The port of the hotel you're trying to connect with (30000 or 38101 on most hotels) must not be in use by any other process than G-Earth. There are platform-specific ways to find out what ports are in-use by googling.
* Make sure you don't have a VPN(/browser extension) enabled
* Your antivirus might be the problem, try to disable it
* Your firewall might be the problem, try to disable it

## Creating an Issue
If the solutions did NOT help you, navigate to https://github.com/sirjonasxx/G-Earth/issues and, if it hasn't been solved before, create a new issue.
Include the following things in your issue:
* Java version
* Your operating system
* For windows: open CMD as administrator and navigate to the G-Earth folder, execute "java -jar G-Earth.jar" and wait for your specific problem to occur. Afterwards, take a screenshot and include it in the issue.
* For mac&linux: the same thing, but open a terminal instead and use the command "sudo java -jar G-Earth.jar"


If you had an issue and solved it with a different solution and feel like it belongs on this page, feel free to create an issue.