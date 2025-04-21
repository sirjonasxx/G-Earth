package gearth.services.unity_tools;

import gearth.services.unity_tools.codepatcher.*;
import gearth.services.unity_tools.codepatcher.debug.IncomingPacketPatcherDebug;
import gearth.services.unity_tools.codepatcher.debug.OutgoingPacketPatcherDebug;
import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.misc.StreamReplacement;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class WasmCodePatcher {

    private final byte[] content;

    public WasmCodePatcher(final byte[] content) {
        this.content = content;
    }

    public byte[] patch(final boolean injectDebug) throws IOException, InvalidOpCodeException, UnityWebModifierException {
        final ByteArrayInputStream baseStream = new ByteArrayInputStream(content);
        final BufferedInputStream inputStream = new BufferedInputStream(baseStream);

        // Patch.
        final List<StreamReplacement> patches = Arrays.asList(
                new SalsaSetKeyPatcher(),
                new SalsaReturnBytePatcher(),
                new OutgoingPacketPatcher(),
                new IncomingPacketPatcher()
        );

        if (injectDebug) {
            patches.add(new IncomingPacketPatcherDebug());
            patches.add(new OutgoingPacketPatcherDebug());
        }

        final Module module = new Module(inputStream, patches);

        // Check if any patch failed.
        for (StreamReplacement patch : patches) {
            if (!patch.isPatched()) {
                throw new UnityWebModifierException("Failed to patch " + patch.getClass().getSimpleName());
            }
        }

        // Assemble.
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        module.assemble(outputStream);

        return outputStream.toByteArray();
    }
}
