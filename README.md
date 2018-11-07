# G-Earth
Habbo packet logger & manipulator for Windows and Linux devices.

- Requires Java 8

# Windows execution
Double click G-Earth.exe, which will be delivered in the release. Note that executing G-Earth requires admin privileges.

# Linux execution
Execution command (temporary, verified in Ubuntu):
> $ sudo java -jar G-Earth.jar 

Additionally, you can add the -t flag to log the packets in your terminal instead of opening a new window for it.

Execute this command if you get the following error:
> Invalid MIT-MAGIC-COOKIE-1 keyInvalid MIT-MAGIC-COOKIE-1 keyException

> $ xhost +local:

# Features
* Log outgoing and incoming packets
* Injection, both sides
* Blocking & replacing packets functionality
* Packet expressions
* Encoding/decoding
* Auto detect hotel
* Retro support - enter game host & port manually (only the first time)
* Advanced scheduler
* Advanced extension support
* Block/replace packets, replace Strings & ints & .. in all packets, included extension
* Always admin, included extension
* Packetlogger

