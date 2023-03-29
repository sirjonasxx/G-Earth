package wasm.disassembly.modules.sections.element;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.instructions.Expression;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.TableIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Elem extends WASMOpCode {

    private TableIdx tableIdx;
    private Expression offset;
    private Vector<FuncIdx> init;

    public Elem(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        tableIdx = new TableIdx(in, module);
        offset = new Expression(in, module);
        init = new Vector<>(in, FuncIdx::new, module);
    }

    public Elem(TableIdx tableIdx, Expression offset, Vector<FuncIdx> init) {
        this.tableIdx = tableIdx;
        this.offset = offset;
        this.init = init;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        tableIdx.assemble(out);
        offset.assemble(out);
        init.assemble(out);
    }

    public TableIdx getTableIdx() {
        return tableIdx;
    }

    public void setTableIdx(TableIdx tableIdx) {
        this.tableIdx = tableIdx;
    }

    public Expression getOffset() {
        return offset;
    }

    public void setOffset(Expression offset) {
        this.offset = offset;
    }

    public Vector<FuncIdx> getInit() {
        return init;
    }

    public void setInit(Vector<FuncIdx> init) {
        this.init = init;
    }
}
