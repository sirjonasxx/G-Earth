package gearth.services.unity_tools;

import com.sun.org.apache.xpath.internal.operations.Mod;
import gearth.services.unity_tools.codepatcher.IncomingPacketPatcher;
import gearth.services.unity_tools.codepatcher.OutgoingPacketPatcher;
import gearth.services.unity_tools.codepatcher.ReturnBytePatcher;
import gearth.services.unity_tools.codepatcher.SetKeyPatcher;
import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.modules.sections.code.Locals;
import wasm.disassembly.modules.sections.imprt.Import;
import wasm.disassembly.modules.sections.imprt.ImportDesc;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.CodeCompare;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
