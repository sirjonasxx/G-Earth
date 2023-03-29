package wasm.disassembly.modules.sections.export;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.GlobalIdx;
import wasm.disassembly.modules.indices.MemIdx;
import wasm.disassembly.modules.indices.TableIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExportDesc extends WASMOpCode {

    private WASMOpCode exportValue;
    private int exportType;


    public ExportDesc(BufferedInputStream in, Module module) throws InvalidOpCodeException, IOException {
        exportType = in.read();
        if (exportType < 0x00 || exportType > 0x03) {
            throw new InvalidOpCodeException("invalid exportdesc type");
        }

        exportValue = exportType == 0x00 ? new FuncIdx(in, module) :
                (exportType == 0x01 ? new TableIdx(in, module) :
                        (exportType == 0x02 ? new MemIdx(in, module) :
                                new GlobalIdx(in, module)));
    }

    public ExportDesc(WASMOpCode exportValue, int exportType) {
        this.exportValue = exportValue;
        this.exportType = exportType;
    }

    public ExportDesc(FuncIdx funcIdx) {
        this(funcIdx, 0x00);
    }

    public ExportDesc(TableIdx tableIdx) {
        this(tableIdx, 0x01);
    }

    public ExportDesc(MemIdx memIdx) {
        this(memIdx, 0x02);
    }

    public ExportDesc(GlobalIdx globalIdx) {
        this(globalIdx, 0x03);
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(exportType);
        exportValue.assemble(out);
    }


    public WASMOpCode getExportValue() {
        return exportValue;
    }

    public void setExportValue(WASMOpCode exportValue) {
        this.exportValue = exportValue;
    }

    public int getExportType() {
        return exportType;
    }

    public void setExportType(int exportType) {
        this.exportType = exportType;
    }
}
