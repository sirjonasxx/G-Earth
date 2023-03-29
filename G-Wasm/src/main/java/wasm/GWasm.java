package wasm;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;

import java.io.*;
import java.util.ArrayList;

public class GWasm {

    public static void main(String[] args) throws IOException, InvalidOpCodeException {

        long start = System.currentTimeMillis();
        Module module = new Module("C:\\Users\\jonas\\Desktop\\Projects\\Jznnp\\S\\habbo2020\\rawfiles\\0.23.0_(534)\\habbo2020-global-prod.wasm.gz", true, new ArrayList<>());
        long end = System.currentTimeMillis();

        System.out.println("Disassemble time: " + (end - start));

        start = System.currentTimeMillis();
        module.assembleToFile("C:\\Users\\jonas\\Desktop\\Projects\\Jznnp\\S\\habbo2020\\rawfiles\\0.23.0_(534)\\out\\habbo2020-global-prod.wasm.gz", true);
        end = System.currentTimeMillis();

        System.out.println("Assemble time: " + (end - start));

        System.out.println("hi");
    }
}
