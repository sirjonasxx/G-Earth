package wasm.disassembly.modules.sections.code;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.instructions.Expression;
import wasm.disassembly.modules.Module;
import wasm.disassembly.types.ValType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Func extends WASMOpCode {

    private Vector<Locals> localss; // intended double s
    private Expression expression;


    public Func(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        localss = new Vector<>(in, Locals::new, module);
        expression = new Expression(in, module);
    }

    public Func(List<Locals> localss, Expression expression) {
        this.localss = new Vector<>(localss);
        this.expression = expression;
    }

    public Func(ValType[] localVariables, Expression expression) {
        setLocalVariables(localVariables);
        this.expression = expression;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        localss.assemble(out);
        expression.assemble(out);
    }

    public List<Locals> getLocalss() {
        return localss.getElements();
    }

    public void setLocalss(List<Locals> localss) {
        this.localss = new Vector<>(localss);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public void setLocalVariables(ValType[] locals) {
        List<Locals> localss = new ArrayList<>();

        Locals current = null;
        for (ValType valType : locals) {
            if (current == null || current.getValType() != valType) {
                if (current != null) localss.add(current);
                current = new Locals(1, valType);
            }
            else {
                current.setAmount(current.getAmount() + 1);
            }
        }
        if (current != null) localss.add(current);
        this.localss = new Vector<>(localss);
    }
}
