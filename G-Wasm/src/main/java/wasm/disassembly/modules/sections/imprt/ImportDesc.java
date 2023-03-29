package wasm.disassembly.modules.sections.imprt;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.TypeIdx;
import wasm.disassembly.types.GlobalType;
import wasm.disassembly.types.MemType;
import wasm.disassembly.types.TableType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImportDesc extends WASMOpCode {

    private WASMOpCode importValue;
    private int importType;

    public ImportDesc(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        importType = in.read();
        if (importType < 0x00 || importType > 0x03) {
            throw new InvalidOpCodeException("invalid importdesc type");
        }

        importValue = importType == 0x00 ? new TypeIdx(in, module) :
                (importType == 0x01 ? new TableType(in, module) :
                        (importType == 0x02 ? new MemType(in, module) :
                                new GlobalType(in, module)));
    }

    public ImportDesc(WASMOpCode importValue, int importType) {
        this.importValue = importValue;
        this.importType = importType;
    }

    public ImportDesc(TypeIdx typeIdx) {
        this(typeIdx, 0x00);
    }

    public ImportDesc(TableType tableType) {
        this(tableType, 0x01);
    }

    public ImportDesc(MemType memType) {
        this(memType, 0x02);
    }

    public ImportDesc(GlobalType globalType) {
        this(globalType, 0x03);
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(importType);
        importValue.assemble(out);
    }

    public WASMOpCode getImportValue() {
        return importValue;
    }

    public void setImportValue(WASMOpCode importValue) {
        this.importValue = importValue;
    }

    public int getImportType() {
        return importType;
    }

    public void setImportType(int importType) {
        this.importType = importType;
    }
}
