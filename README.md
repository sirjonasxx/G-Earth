# G-Earth
Habbo packet logger & manipulator for Linux distributions (for now). 

- Requires Java 8
- Byte to string & string to byte encoding is slightly different from the Habbo PacketLogger standards. (on purpose)

Execution command (temporary, verified in Ubuntu):
> $ sudo -E java -jar G-Earth.jar 



FEATURES:
* Client & server side packet logging & injection
* Easily blocking & replacing packets (not yet in the UI)
* Packet expressions
* Responsive UI
* Encoding/decoding
* Auto detect hotel


TO-DO:
* Block packets from displaying in the logger (in Logger tab)
* Scheduler
* Extension support
* In settings -> enable CS admin on connect
* In settings -> block/replace packets, replace Strings & ints & .. in all packets
* Info tab
* Cross-platform support
