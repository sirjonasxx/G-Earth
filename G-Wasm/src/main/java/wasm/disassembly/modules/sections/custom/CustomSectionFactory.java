package wasm.disassembly.modules.sections.custom;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.custom.namesection.NameSection;
import wasm.disassembly.values.WName;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;

public class CustomSectionFactory {

    public static CustomSection get(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        long size = WUnsignedInt.read(in, 32);
        String name = WName.read(in);

        if (name.equals("name")) {
            return new NameSection(in, module, size);
        }

        return new UnImplementedCustomSection(in, module, size, name);
    }
}
