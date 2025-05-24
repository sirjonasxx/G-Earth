package gearth.app.services.unity_tools.codepatcher.debug;

import gearth.app.services.unity_tools.codepatcher.IncomingPacketPatcher;
import wasm.disassembly.modules.sections.code.Func;

public class IncomingPacketPatcherDebug extends IncomingPacketPatcher {

    public IncomingPacketPatcherDebug() {
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
