package wasm.disassembly.types;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.code.Func;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FuncType extends WASMOpCode {

    private ResultType parameterType;
    private ResultType resultType;

    public FuncType(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        if (in.read() != 0x60) throw new InvalidOpCodeException("Function types must be encoded with 0x60");

        parameterType = new ResultType(in, module);
        resultType = new ResultType(in, module);
    }

    public FuncType(ResultType parameterType, ResultType resultType) {
        this.parameterType = parameterType;
        this.resultType = resultType;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(0x60);
        parameterType.assemble(out);
        resultType.assemble(out);
    }

    public ResultType getParameterType() {
        return parameterType;
    }

    public void setParameterType(ResultType parameterType) {
        this.parameterType = parameterType;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FuncType)) {
            return false;
        }

        FuncType other = (FuncType) obj;

        return parameterType.equals(other.parameterType) &&
                resultType.equals(other.resultType);
    }

    @Override
    public String toString() {
        return parameterType + " -> " + resultType;
    }
}
