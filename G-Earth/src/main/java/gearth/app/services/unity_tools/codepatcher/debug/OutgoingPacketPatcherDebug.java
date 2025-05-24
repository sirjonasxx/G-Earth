package gearth.app.services.unity_tools.codepatcher.debug;

import gearth.app.services.unity_tools.codepatcher.OutgoingPacketPatcher;
import wasm.disassembly.modules.sections.code.Func;

public class OutgoingPacketPatcherDebug extends OutgoingPacketPatcher {

    public OutgoingPacketPatcherDebug() {
        patchOnlyOnce = false;
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOK_DEBUG;
    }

    @Override
    public String getImportName() {
        return super.getImportName() + "_debug";
    }

    @Override
    public String getExportName() {
        return null;
    }

    @Override
    public boolean codeMatches(int id, Func code) {
        return true;
    }
}
