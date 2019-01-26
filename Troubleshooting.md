There are some known issues while using G-Earth, please read this through carefully.

First of all, make sure you have extracted the .rar file to its own folder.
If you're using a mac; make sure you have completed the mac-installation wiki page.


If the following solutions do NOT help you, navigate to https://github.com/sirjonasxx/G-Earth/issues and, if it hasn't been solved before, create a new issue.
Include the following things in your issue:
* Java version
* Your operating system
* For windows: open CMD as administrator and navigate to the G-Earth folder, execute "java -jar G-Earth.jar" and wait for your specific problem to occur. Afterwards, take a screenshot and include it in the issue.
* For mac&linux: the same thing, but open a terminal instead and use the command "sudo java -jar G-Earth.jar"


## Problem 1: G-Earth doesn't open
* It's recommended to use Java 8, some problems have occurred with people using Java 11 or other versions. It shouldn't be hard to fix this with a simple google search.
* On Linux, if you get a "Invalid MIT-MAGIC-COOKIE-1" exception, execute "xhost +local:" in a terminal
* It will almost always be an issue with Java, so try reinstalling

## Problem 2: Stuck at 76%
* On windows; navigate to https://visualstudio.microsoft.com/downloads/ and download the c++ redistributable 2017 ( https://imgur.com/a/bgvL8fN ), make sure to select the right version.
* Try another browser. (on Mac, use Firefox. On Windows, use Chrome, Firefox or Opera, others might work as well, but IE and Edge do not)
* Your hosts file might have some permissions issues. Delete it and place a new one: https://github.com/sirjonasxx/G-Earth/issues/16
* MAKE SURE your downloaded the right .rar file, for Windows, check if you're running on a 32bit or 64bit device first.

If you had an issue and solved with a different solution and feel like it belongs on this page, feel free to create an issue.