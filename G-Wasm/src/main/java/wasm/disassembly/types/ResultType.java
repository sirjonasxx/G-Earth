package wasm.disassembly.types;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.conventions.CustomVector;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResultType extends WASMOpCode {

    private CustomVector<ValType> vector;

    public ResultType(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        vector = new CustomVector<>(
                in,
                (in1, m) -> ValType.from_val(in1.read()),
                (valType, out) -> out.write(valType.val),
                module
        );
    }

    public ResultType(List<ValType> valTypes) {
        vector = new CustomVector<>(
                valTypes,
                (valType, out) -> out.write(valType.val)
        );
    }

    public ResultType(CustomVector<ValType> vector) {
        this.vector = vector;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        vector.assemble(out);
    }

    public List<ValType> typeList() {
        return vector.getElements();
    }

    public CustomVector<ValType> getVector() {
        return vector;
    }

    public void setVector(CustomVector<ValType> vector) {
        this.vector = vector;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResultType)) {
            return false;
        }

        ResultType other = (ResultType) obj;
        return vector.getElements().equals(other.vector.getElements());
    }

    @Override
    public String toString() {
         return "(" + vector.getElements().stream().map(Enum::name).collect(Collectors.joining(" ")) + ")";
    }
}
