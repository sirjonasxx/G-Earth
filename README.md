# G-Earth
Habbo packet logger & manipulator, currently only for Linux distributions. 

- Requires Java 8
- Byte to string & string to byte encoding is slightly different from the Habbo PacketLogger standards. (on purpose)

Execution command (temporary, verified in Ubuntu):
> $ sudo -E java -jar G-Earth.jar 

Execute this command if u get the following error:
> Invalid MIT-MAGIC-COOKIE-1 keyInvalid MIT-MAGIC-COOKIE-1 keyException

> $ xhost +local:


FEATURES:
* Client & server side packet logging & injection
* Easily blocking & replacing packets (not yet in the UI)
* Packet expressions
* Responsive UI
* Encoding/decoding
* Auto detect hotel

DOING:
* Scheduler (packetsending not consistent yet)

TO-DO:
* Block specific packets from displaying in the logger (in Logger tab)
* Extension support
* In settings -> enable CS admin on connect (wow fun)
* In settings -> block/replace packets, replace Strings & ints & .. in all packets
* Info tab
* Cross-platform support
