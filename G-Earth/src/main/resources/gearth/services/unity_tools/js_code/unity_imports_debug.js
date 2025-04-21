const debugOutgoingFuncs = [
    // Enter function ids
];

const debugIncomingFuncs = [
    // Enter function ids
];

function g_outgoing_packet_debug(funcIdx, param1, param2, param3) {
    try {
        if (debugOutgoingFuncs.includes(funcIdx)) {
            let length = readLittleEndian(Module.HEAPU8.subarray(param2 + 12, param2 + 12 + 4));
            let array = [].slice.call(Module.HEAPU8.subarray(param2 + 12 + 4, param2 + 12 + 4 + length));

            console.log(`g_outgoing_packet_debug: ${funcIdx}, ${param1}, ${param2}, ${param3}`);
            console.log('\n', hexdump(array, 16));
            return;
        }

        let length = readLittleEndian(Module.HEAPU8.subarray(param2 + 12, param2 + 12 + 4));
        if (length  < 6 || length > 100) {
            return;
        }

        let array = [].slice.call(Module.HEAPU8.subarray(param2 + 12 + 4, param2 + 12 + 4 + length));
        let str = data_to_string(array);

        if (str.includes('avatar.move')) {
            console.log(`g_outgoing_packet_debug: ${funcIdx}, ${param1}, ${param2}, ${param3} | length: ${length}, array: ${array}, hex: ${data_to_hex(array)}`);
            console.log(str);
        }
    } catch (e) {
        // ignore
    }
}

function g_incoming_packet_debug(funcIdx, param1, param2, param3, param4, param5) {
    try {
        if (debugIncomingFuncs.includes(funcIdx)) {
            let array = Module.HEAPU8.slice(param2 + 16, param2 + 16 + param4);
            let parsedLen = readBigEndian(array.subarray(0, 4)) + 4;

            console.log(`g_incoming_packet_debug: ${funcIdx}, ${param1}, ${param2}, ${param3}, ${param4}, ${param5}`);
            console.log('matches length', parsedLen, param4, parsedLen === param4);
            console.log('\n', hexdump(array, 16));
            return;
        }

        if (param4 < 6 || param4 > 3000) {
            return;
        }

        let array = Module.HEAPU8.slice(param2 + 16, param2 + 16 + param4);
        let str = data_to_string(array);

        if (str.includes('flatctrl')) {
            console.log(`g_incoming_packet_debug: ${funcIdx}, ${param1}, ${param2}, ${param3}, ${param4}, ${param5} | length: ${param4}, array: ${array}, hex: ${data_to_hex(array)}`);
            console.log(str);
        }
    } catch (e) {
        // ignore
    }
}

function data_to_string(data) {
    let str = "";

    for (let i = 0; i < data.length; i++) {
        str += String.fromCharCode(data[i]);
    }

    return str;
}

function data_to_hex(data) {
    let hex = "";

    for (let i = 0; i < data.length; i++) {
        let h = data[i].toString(16);
        if (h.length < 2) {
            h = "0" + h;
        }
        hex += h;
    }

    return hex;
}

function hexdump(buffer, blockSize) {
    if (buffer instanceof ArrayBuffer && buffer.byteLength !== undefined) {
        buffer = String.fromCharCode.apply(String, [].slice.call(new Uint8Array(buffer)));
    } else if (Array.isArray(buffer)) {
        buffer = String.fromCharCode.apply(String, buffer);
    } else if (buffer.constructor === Uint8Array) {
        buffer = String.fromCharCode.apply(String, [].slice.call(buffer));
    } else if (typeof buffer !== 'string') {
        return false;
    }
    blockSize = blockSize || 16;
    var lines = [];
    var hex = "0123456789ABCDEF";
    for (var b = 0; b < buffer.length; b += blockSize) {
        var block = buffer.slice(b, Math.min(b + blockSize, buffer.length));
        var addr = ("0000" + b.toString(16)).slice(-4);
        var codes = block.split('').map(function (ch) {
            var code = ch.charCodeAt(0);
            return " " + hex[(0xF0 & code) >> 4] + hex[0x0F & code];
        }).join("");
        codes += "   ".repeat(blockSize - block.length);
        var chars = block.replace(/[\x00-\x1F\x20]/g, '.');
        chars +=  " ".repeat(blockSize - block.length);
        lines.push(addr + " " + codes + "  " + chars);
    }
    return lines.join("\n");
}

asmLibraryArg["g_outgoing_packet_debug"] = g_outgoing_packet_debug;
asmLibraryArg["g_incoming_packet_debug"] = g_incoming_packet_debug;