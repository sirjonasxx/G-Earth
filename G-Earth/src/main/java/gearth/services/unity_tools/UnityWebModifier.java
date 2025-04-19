package gearth.services.unity_tools;

import org.apache.commons.io.IOUtils;
import wasm.disassembly.InvalidOpCodeException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class UnityWebModifier {

    private void modifyCodeFile() throws IOException, InvalidOpCodeException {
        // new WasmCodePatcher(codeFile.getAbsolutePath()).patch();
    }

    public String modifyFrameworkFile(final String revision, String contents) throws UnityWebModifierException {
        contents = insertFrameworkCode(contents, 0, "js_code/unity_code.js");
        contents = insertFrameworkCodeAfter(contents, "var asm=createWasm();", "js_code/unity_exports.js");
        contents = insertFrameworkCodeAfter(contents, "function createWasm(){", "js_code/unity_imports.js");

        // Make these variables global.
        contents = replaceOrThrow(contents, "var _free", "_free");
        contents = replaceOrThrow(contents, "var _malloc", "_malloc");
        contents = replaceOrThrow(contents, "var Module=typeof unityFramework!=\"undefined\"?unityFramework:{};", "var Module=typeof unityFramework!=\"undefined\"?unityFramework:{}; _module = Module;");
        contents = replaceOrThrow(contents, "{{RevisionName}}", revision);

        return contents;
    }

    public String modifyLoaderFile(String contents) throws UnityWebModifierException {
        // Cache bust by checking for unused headers.
        contents = replaceOrThrow(contents, "\"Last-Modified\"", "\"G-Last-Modified\"");
        contents = replaceOrThrow(contents, "\"ETag\"", "\"G-ETag\"");

        return contents;
    }

    private static String insertFrameworkCodeAfter(final String contents, final String search, final String codeName) throws UnityWebModifierException {
        final int index = contents.indexOf(search);

        if (index == -1) {
            throw new UnityWebModifierException("Could not find " + search);
        }

        final int replaceIndex = index + search.length();

        return insertFrameworkCode(contents, replaceIndex, codeName);
    }

    private static String insertFrameworkCode(final String contents, final int index, final String codeName) throws UnityWebModifierException {
        final String firstPart = contents.substring(0, index);
        final String lastPart = contents.substring(index);

        try (final InputStream inputStream = UnityWebModifier.class.getResourceAsStream(codeName)) {
            if (inputStream == null) {
                throw new UnityWebModifierException("Could not find " + codeName);
            }

            final String code = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return firstPart + code + lastPart;
        } catch (IOException e) {
            throw new UnityWebModifierException("Could not read " + codeName);
        }
    }

    private static String replaceOrThrow(String contents, final String search, final String replace) throws UnityWebModifierException {
        if (contents.contains(search)) {
            return contents.replace(search, replace);
        }

        throw new UnityWebModifierException("Could not find " + search);
    }
}
