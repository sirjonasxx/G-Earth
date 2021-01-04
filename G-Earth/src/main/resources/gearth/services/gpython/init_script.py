from time import sleep

from g_python.gextension import Extension
from g_python.hmessage import Direction, HMessage
from g_python.hpacket import HPacket
from g_python import hparsers, hunityparsers
from g_python import htools, hunitytools

extension_info = {"title": "$G_PYTHON_SHELL_TITLE$", "description": "G-Python scripting console", "version": "1.0", "author": ""}

ext = Extension(extension_info, ["--port", "$G_EARTH_PORT$", "--auth-token", "$COOKIE$"], {"can_leave": False})
ext.start()


def is_closed(): return ext.is_closed()


def send_to_client(packet): return ext.send_to_client(packet)


def send_to_server(packet): return ext.send_to_server(packet)


def on_event(event_name: str, func): return ext.on_event(event_name, func)


def intercept(direction: Direction, callback, id=-1, mode='default'): return ext.intercept(direction, callback, id, mode)


def start(): return ext.start()


def stop(): return ext.stop()


def write_to_console(text, color='black', mention_title=True): return ext.write_to_console(text, color, mention_title)


def request_flags(): return ext.request_flags()


HPacket.default_extension = ext
