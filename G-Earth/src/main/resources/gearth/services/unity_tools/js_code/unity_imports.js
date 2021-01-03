function g_outgoing_packet(param1, param2, param3) {
    out_send_param1 = param1;
    out_send_param3 = param3;
    out_packet_objid = unityInstance.Module.HEAPU8.slice(param2, param2 + 4);

    let length = readLittleEndian(unityInstance.Module.HEAPU8.subarray(param2 + 12, param2 + 12 + 4));
    let array = [].slice.call(unityInstance.Module.HEAPU8.subarray(param2 + 12 + 4, param2 + 12 + 4 + length));

    packetBuff["out"] = packetBuff["out"].concat(array);

    for (let packet of collect_packets("out")) {
        handle_out(packet);
    }
}

function g_incoming_packet(param1, param2, param3, param4, param5) {
    in_recv_param1 = param1;
    in_packet_prefix = unityInstance.Module.HEAPU8.slice(param2, param2 + 16);

    let buffer = unityInstance.Module.HEAPU8.slice(param2 + 16, param2 + 16 + param4);
    packetBuff["in"] = packetBuff["in"].concat([].slice.call(buffer));

    let packets = collect_packets("in");
    for (let packet of packets) {
        if (chachas.length > 1) {
            packet[5] = _gearth_returnbyte_copy(chachas[1], packet[5], chachaClass);
            packet[4] = _gearth_returnbyte_copy(chachas[1], packet[4], chachaClass);
        }
        handle_in(packet);
    }
}

function g_chacha_setkey(param1, param2, param3, param4) {
    if (chachas.length === 2) {
        chachas = [];
    }

    chachas.push(param1);
}

function g_chacha_returnbyte(param1, param2, param3) {
    chachaClass = param3;
    return param2;
}

env["g_outgoing_packet"] = g_outgoing_packet;
env["g_incoming_packet"] = g_incoming_packet;
env["g_chacha_setkey"] = g_chacha_setkey;
env["g_chacha_returnbyte"] = g_chacha_returnbyte;