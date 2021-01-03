let gearth_isconnected = false;

let revision = "{{RevisionName}}";
let g_ws;


let chachas = [];
let chachaClass = -1;

let out_send_param1 = -1;
let out_send_param3 = -1;
let out_packet_objid = new Uint8Array(4);

let in_recv_param1 = -1;
let in_packet_prefix = new Uint8Array(16);

let _gearth_returnbyte_copy;
let _gearth_outgoing_copy;
let _gearth_incoming_copy;

let _malloc;
let _free;


var packetBuff = {"out": [], "in": []};

function readLittleEndian(arr) {
    return arr[0] + arr[1] * 256 + arr[2] * 65536 + arr[3] * 16777216;
}

function readBigEndian(arr) {
    return arr[3] + arr[2] * 256 + arr[1] * 65536 + arr[0] * 16777216;
}

function writeLittleEndian(n) {
    let arr = new Uint8Array(4);
    for (let i = 0; i < 4; i++) {
        let rest = n % 256;
        arr[i] = rest;
        n -= rest;
        n /= 256;
    }
    return arr;
}

function writeBigEndian(n) {
    let arr = new Uint8Array(4);
    for (let i = 0; i < 4; i++) {
        let rest = n % 256;
        arr[3 - i] = rest;
        n -= rest;
        n /= 256;
    }
    return arr;
}



function packetToString(packet) {
    let chars = [];
    let textDecode = new TextDecoder('latin1');
    for (let i = 0; i < packet.length; i++) {
        let byte = packet[i];
        if (byte < 32 || (byte > 127 && byte < 160) || byte === 93 || byte === 91 || byte === 125 || byte === 127) {
            chars.push("["+byte+"]");
        }
        else {
            chars.push(textDecode.decode(packet.slice(i, i+1)));
        }
    }

    return chars.join("");
}

function printPacket(type, packet) {
    packet = new Uint8Array(packet);
    if (packet.length > 2000) {
        console.log("[" + type + " capture]" + " -> skipped");
    }
    else {
        console.log("[" + type + " capture]" + " -> " + packetToString(packet));
    }
}


let _g_packet_split = 600;

function inject_out(packet) {

    if (chachas.length > 1) {
        packet[5] =  _gearth_returnbyte_copy(chachas[0], packet[5], chachaClass);
        packet[4] =  _gearth_returnbyte_copy(chachas[0], packet[4], chachaClass);
    }

    let i = 0;
    while (i < packet.length) {
        let inject_amount = Math.min(_g_packet_split, packet.length - i);

        let packet_location = _malloc(inject_amount + 16);
        unityInstance.Module.HEAPU8.set(out_packet_objid, packet_location);
        unityInstance.Module.HEAPU8.fill(0, packet_location + 4, packet_location + 12);
        unityInstance.Module.HEAPU8.set(writeLittleEndian(inject_amount), packet_location + 12);
        unityInstance.Module.HEAPU8.set(packet.slice(i, i + inject_amount), packet_location + 16);

        _gearth_outgoing_copy(out_send_param1, packet_location, out_send_param3);
        _free(packet_location);

        i += inject_amount;
    }

}

function handle_out(packet) {
    if (gearth_isconnected) {
        let g_message = new Uint8Array(packet.length + 1);
        g_message[0] = 1; // toserver
        g_message.set(new Uint8Array(packet), 1);
        g_ws.send(g_message);
    }
    else {
        inject_out(packet);
    }
}

function inject_in(packet) {
    let i = 0;
    while (i < packet.length) {
        let inject_amount = Math.min(_g_packet_split, packet.length - i);

        let packet_location = _malloc(inject_amount + 16);
        unityInstance.Module.HEAPU8.set(in_packet_prefix, packet_location);
        unityInstance.Module.HEAPU8.set(packet.slice(i, i + inject_amount), packet_location + 16);

        _gearth_incoming_copy(in_recv_param1, packet_location, 0, inject_amount, 0);
        _free(packet_location);

        i += inject_amount;
    }
}

function handle_in(packet) {
    if (gearth_isconnected) {
        let g_message = new Uint8Array(packet.length + 1);
        g_message[0] = 0; // toclient
        g_message.set(new Uint8Array(packet), 1);
        g_ws.send(g_message);
    }
    else {
        inject_in(packet);
    }
}


function collect_packets(type) {
    let finishedPackets = [];

    while (packetBuff[type].length >= 6 && readBigEndian(packetBuff[type].slice(0, 4)) + 4 <= packetBuff[type].length) {
        let packetLength = readBigEndian(packetBuff[type].slice(0, 4)) + 4;
        let packet = packetBuff[type].slice(0, packetLength);
        finishedPackets.push(packet);
        packetBuff[type] = packetBuff[type].slice(packetLength);
    }

    return finishedPackets;
}





let onOpen = function() {
    g_ws.send(new TextEncoder('latin1').encode(revision));
    gearth_isconnected = true;
};

let onClose = function() {
    gearth_isconnected = false;
};

let onMessage = function(message) {
    let buffer = new Uint8Array(message.data);

    if (buffer[0] === 0) {
        inject_in([].slice.call(buffer.slice(1)));
    }
    else if (buffer[0] === 1) {
        inject_out([].slice.call(buffer.slice(1)));
    }
    else {
        gearth_isconnected = false;
        g_ws.close();
    }
};

let onError = function(event) {
    gearth_isconnected = false;
};

let portRequester = new WebSocket("ws://localhost:9039/ws/portrequest");
portRequester.onmessage = function(message) {
    let port = message.data.split(" ")[1];

    let _g_packet_url = "ws://localhost:" + port + "/ws/packethandler";
    g_ws = new WebSocket(_g_packet_url);
    g_ws.binaryType = "arraybuffer";

    g_ws.onopen = onOpen;
    g_ws.onclose = onClose;
    g_ws.onmessage = onMessage;
    g_ws.onerror = onError;

    portRequester.close();
};

