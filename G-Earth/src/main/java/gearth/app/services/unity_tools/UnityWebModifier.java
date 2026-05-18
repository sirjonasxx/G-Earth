package gearth.app.services.unity_tools;

import gearth.app.misc.Cacher;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UnityWebModifier {

    /**
     * Debug flag to enable debug code in the unity patches.
     */
    private static final boolean DEBUG = false;

    public byte[] modifyCodeFile(final String revision, byte[] content) throws UnityWebModifierException {
        final File cacheDir = new File(Cacher.getCacheDir(), String.format("UNITY-%s", revision));
        final File cacheFile = new File(cacheDir, "code.wasm");

        if (cacheDir.isDirectory() && cacheFile.exists()) {
            try {
                return Files.readAllBytes(cacheFile.toPath());
            } catch (IOException e) {
                throw new UnityWebModifierException("Failed to read code file from cache", e);
            }
        }

        try {
            final WasmCodePatcher wasmCodePatcher = new WasmCodePatcher(content);
            final byte[] wasmCodePatched = wasmCodePatcher.patch(DEBUG);

            if (!cacheDir.isDirectory()) {
                if (!cacheDir.mkdirs()) {;
                    throw new UnityWebModifierException("Failed to create cache directory");
                }
            }

            Files.write(cacheFile.toPath(), wasmCodePatched);

            return wasmCodePatched;
        } catch (UnityWebModifierException e) {
            throw e;
        } catch (Exception e) {
            throw new UnityWebModifierException("Could not patch code file", e);
        }
    }

    public String modifyFrameworkFile(final String revision, final int websocketPort, String contents) throws UnityWebModifierException {
        contents = insertFrameworkCode(contents, 0, "js_code/unity_code.js");
        contents = insertFrameworkCodeAfter(contents, "var asm=createWasm();", "js_code/unity_exports.js");
        contents = insertFrameworkCodeAfter(contents, "function createWasm(){", "js_code/unity_imports.js");

        if (DEBUG) {
            contents = insertFrameworkCodeAfter(contents, "function createWasm(){", "js_code/unity_imports_debug.js");
        }

        // Make these variables global.
        contents = replaceOrThrow(contents, "var _free", "_free");
        contents = replaceOrThrow(contents, "var _malloc", "_malloc");
        contents = replaceOrThrow(contents, "var Module=typeof unityFramework!=\"undefined\"?unityFramework:{};", "var Module=typeof unityFramework!=\"undefined\"?unityFramework:{}; _module = Module;");
        contents = replaceOrThrow(contents, "{{RevisionName}}", revision);
        contents = replaceOrThrow(contents, "{{WebsocketPort}}", Integer.toString(websocketPort));

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

        try (final InputStream inputStream = UnityWebModifier.class.getResourceAsStream("/gearth/services/unity_tools/" + codeName)) {
            if (inputStream == null) {
                throw new UnityWebModifierException("Could not find " + codeName);
            }

            final String code = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return firstPart + code + lastPart;
        } catch (IOException e) {
            throw new UnityWebModifierException("Could not read " + codeName, e);
        }
    }

    private static String replaceOrThrow(String contents, final String search, final String replace) throws UnityWebModifierException {
        if (contents.contains(search)) {
            return contents.replace(search, replace);
        }

        throw new UnityWebModifierException("Could not find " + search);
    }
}
