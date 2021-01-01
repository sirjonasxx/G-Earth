package gearth.services.unity_tools;

import gearth.services.unity_tools.codepatcher.IncomingPacketPatcher;
import gearth.services.unity_tools.codepatcher.OutgoingPacketPatcher;
import gearth.services.unity_tools.codepatcher.ReturnBytePatcher;
import gearth.services.unity_tools.codepatcher.SetKeyPatcher;
import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;

import java.io.IOException;
import java.util.Arrays;

public class WasmCodePatcher {

    private String file;

    public WasmCodePatcher(String file) {
        this.file = file;
    }

    public void patch() throws IOException, InvalidOpCodeException {
        Module module = new Module(file, Arrays.asList(
                new SetKeyPatcher(),
                new ReturnBytePatcher(),
                new OutgoingPacketPatcher(),
                new IncomingPacketPatcher()
        ));
        module.assembleToFile(file);
    }
}
