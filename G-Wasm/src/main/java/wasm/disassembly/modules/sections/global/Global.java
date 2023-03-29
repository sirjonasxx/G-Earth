package wasm.disassembly.modules.sections.global;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.instructions.Expression;
import wasm.disassembly.modules.Module;
import wasm.disassembly.types.GlobalType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Global extends WASMOpCode {

    private GlobalType globalType;
    private Expression expression;

    public Global(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        globalType = new GlobalType(in, module);
        expression = new Expression(in, module);
    }

    public Global(GlobalType globalType, Expression expression) {
        this.globalType = globalType;
        this.expression = expression;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        globalType.assemble(out);
        expression.assemble(out);
    }

    public GlobalType getGlobalType() {
        return globalType;
    }

    public void setGlobalType(GlobalType globalType) {
        this.globalType = globalType;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
