# G-Earth
Habbo packet logger & manipulator for Linux distributions (for now). 

- Requires Java 8
- Byte to string & string to byte encoding is slightly different from the Habbo PacketLogger standards. (on purpose)

Execution command (temporary, verified in Ubuntu):
$ sudo -E java -jar G-Earth.jar 


FEATURES:
* Client side packet logging & injection
* Easily blocking & replacing packets (not yet in the UI)
* Packet expressions
* Responsive UI
* Encoding/decoding


WORKING ON:
* Using IPtables instead of editting the hosts file
* Auto detect hotel
* Retrieve the RC4 key from memory in order to be a third-party to the outgoing messages


TO-DO:
* Scheduler
* Block incoming packets from UI
* Extension support

