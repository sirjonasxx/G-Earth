# G-Earth
Habbo packet logger & manipulator, currently only for Linux distributions. 

- Requires Java 8
- Byte to string & string to byte encoding is slightly different from the Habbo PacketLogger standards. (on purpose)

Execution command (temporary, verified in Ubuntu):
> $ sudo -E java -jar G-Earth.jar 

Execute this command if you get the following error:
> Invalid MIT-MAGIC-COOKIE-1 keyInvalid MIT-MAGIC-COOKIE-1 keyException

> $ xhost +local:


FEATURES:
* Log outgoing and incoming packets
* Injection, both sides
* Blocking & replacing packets functionality
* Packet expressions
* Encoding/decoding
* Auto detect hotel
* Scheduler

DOING:
* Extension support
* Cross-platform support

TO-DO:
* Extension -> block/replace packets, replace Strings & ints & .. in all packets
* Info tab

EXTRA TO-DO's:
* Block specific packets from displaying in the logger (in Logger tab)
* Extension -> enable CS admin on connect
