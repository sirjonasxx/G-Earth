## Requirements
* Understand the G-Python extension interface (https://github.com/sirjonasxx/G-Python)
* Make sure your python version (`python --version` on commandline) is >= 3.2
* Execute the following line on commandline: `python -m pip install qtconsole pyqt5 jupyter-console g-python` to install the required packages

(IMPORTANT: On Linux devices, the installation command might require a `sudo`, since G-Earth runs in `sudo` as well.)

## Usage
1. Go to the "Extra" tab on G-Earth and check the "Enable G-Python Scripting" checkbox
2. Go to the "Extensions" tab and open a G-Python shell

The shell will automatically do the following things for you:
* Import all G-Python classes & modules (`Extension`, `Direction`, `HMessage`, `HPacket`, `hparsers`, `htools`)
* Import the `sleep()` function from the `time` module
* Initialize a G-Python extension (named `ext`) and connect it to G-Earth
* Create an interface for all methods of the Extension object (for example: `ext.send_to_server(packet)` can also be executed with just `send_to_server(packet)`)

![G-Python shell extension](https://i.imgur.com/ekOPLYu.png)

### QtConsole shortcuts

Shortcuts:
* TAB -> Autocomplete
* SHIFT+TAB -> Show function arguments + docs
* ENTER -> Execute the entered python script or continue it on the next line, depending on context
* CTRL+ENTER -> Same as "ENTER", but always continue the current script
* SHIFT+ENTER -> Same as "ENTER", but always execute

Commands:
* %quickref -> list of all qtConsole commands
* %clear -> clears the window
* %save -> saves specific lines to a file (example: `%save test 4-7`, saves lines 4 to 7 to `test.py`)
* %load -> loads script from a file (example: `load test`, loads `test.py`)

## Example
#### Example 1: send all user signs

![example 1](https://i.imgur.com/4kjnPlo.png)

_(hint: to save this script: `%save all_signs 23-24`)_

#### Example 2: wave when typing "wave"

![example 2](https://i.imgur.com/xo6GhOi.png)

_(hint: if you're going to `sleep()` during an `intercept()` callback, do an asynchronous intercept (check g-python repo), or all incoming/outgoing packet flow will be paused)_

_(hint: in the method signature `def on_speech(message: HMessage)`, `: HMessage` isn't required but makes auto-completion easier)_

#### Example 3: get all room furniture

![example 3](https://i.imgur.com/CJCErDh.png)

#### Example 4: get all room users

![example 4](https://i.imgur.com/b2czJUw.png)

_(prints the list of all HEntities in the room, in which all of them are mapped to their corresponding string representation)_