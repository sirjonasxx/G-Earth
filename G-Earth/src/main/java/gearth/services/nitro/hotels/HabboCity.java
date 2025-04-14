package gearth.services.nitro.hotels;

import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.services.nitro.NitroAsset;
import gearth.services.nitro.NitroHotel;
import gearth.services.nitro.NitroPacketModifier;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.instructions.numeric.NumericI32ConstInstr;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.sections.code.Code;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.modules.sections.export.Export;
import wasm.misc.ExpressionBlockVisitor;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mikee on 2025-03-27.
 */
public class HabboCity extends NitroHotel {

    private static final Logger log = LoggerFactory.getLogger(HabboCity.class);
    private byte[] aesKey;
    private byte[] aesIv;
    private byte[] saltKey;
    private byte[] saltIv;

    public HabboCity() {
        super("habbocity.me",
                Collections.singletonList("wss://websocket.habbocity.me/websocket/"),
                Collections.singletonList(new NitroAsset("nitro.habbocity.me", "/crypto/file/city_crypto_bg.wasm")));
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return new HabboCityPacketModifier(this);
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
        if ("/crypto/file/city_crypto_bg.wasm".equals(uri)) {
            // Extract keys.
            final InputStream stream = new ByteArrayInputStream(data);
            final BufferedInputStream bis = new BufferedInputStream(stream);

            try {
                parseWebassemblyModule(new Module(bis, new ArrayList<>()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse webassembly module", e);
            }
        }
    }

    private void parseWebassemblyModule(Module module) {
        final long startTime = System.currentTimeMillis();

        log.debug("Parsing HabboCity webassembly module");

        for (Export export : module.getExportSection().getExports()) {
            if (!export.getName().equals("encrypt")) {
                continue;
            }

            if (export.getExportDesc().getExportValue() instanceof FuncIdx) {
                // Find offset to export.
                final FuncIdx funcIdx = (FuncIdx) export.getExportDesc().getExportValue();
                final Code code = module.getCodeSection().getCodeByIdx(funcIdx);
                final Func func = code.getCode();

                // Visit all blocks in the function.
                final HabboCityVisitor visitor = new HabboCityVisitor(module);

                visitor.visit(func.getExpression());

                // Extract result.
                this.aesKey = visitor.getAesKey();
                this.aesIv = visitor.getAesIv();
                this.saltKey = visitor.getSaltKey();
                this.saltIv = visitor.getSaltIv();

                if (this.aesKey == null) {
                    throw new RuntimeException("Failed to extract aes key from module");
                }

                if (this.aesIv == null) {
                    throw new RuntimeException("Failed to extract aes iv from module");
                }

                if (this.saltKey == null) {
                    throw new RuntimeException("Failed to extract salt key from module");
                }

                if (this.saltIv == null) {
                    throw new RuntimeException("Failed to extract salt iv from module");
                }

                // Print result.
                log.debug("HabboCity aesKey  {}", Hex.toHexString(this.aesKey));
                log.debug("HabboCity aesIv   {}", Hex.toHexString(this.aesIv));
                log.debug("HabboCity saltKey {}", Hex.toHexString(this.saltKey));
                log.debug("HabboCity saltIv  {}", Hex.toHexString(this.saltIv));

                // Print time taken.
                final long endTime = System.currentTimeMillis();
                log.debug("Found keys in {}ms", endTime - startTime);
            }
        }
    }

    public static class HabboCityPacketModifier implements NitroPacketModifier {

        private final HabboCity parent;

        private final Cipher staticCipherEnc;
        private final Cipher staticCipherDec;

        private Cipher clientCipherEnc;
        private Cipher clientCipherDec;

        private Cipher serverCipherEnc;
        private Cipher serverCipherDec;

        private boolean firstClient = true;
        private boolean firstServer = true;

        public HabboCityPacketModifier(HabboCity parent) {
            this.parent = parent;

            try {
                final SecretKeySpec staticKeySpec = new SecretKeySpec(parent.aesKey, "AES");
                final GCMParameterSpec staticParameterSpec = new GCMParameterSpec(128, parent.aesIv);

                this.staticCipherEnc = Cipher.getInstance("AES/GCM/NoPadding", "BC");
                this.staticCipherEnc.init(Cipher.ENCRYPT_MODE, staticKeySpec, staticParameterSpec);

                this.staticCipherDec = Cipher.getInstance("AES/GCM/NoPadding", "BC");
                this.staticCipherDec.init(Cipher.DECRYPT_MODE, staticKeySpec, staticParameterSpec);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void initCiphers(final byte[] handshakePacket) throws Exception {
            // Read SSO from packet.
            final HPacket packet = HPacketFormat.EVA_WIRE.createPacket(handshakePacket);
            final String sso = packet.readString();

            // Hash for keys.
            final byte[] aesKey = this.sha256(sso, parent.saltKey);
            final byte[] aesIv = this.sha256(sso, parent.saltIv);

            // Setup keys.
            final SecretKeySpec aesKeySpec = new SecretKeySpec(aesKey, 0, 16, "AES");
            final IvParameterSpec aesIvSpec = new IvParameterSpec(aesIv, 0, 16);

            // Setup ciphers.
            this.clientCipherEnc = Cipher.getInstance("AES/CTR/NoPadding");
            this.clientCipherEnc.init(Cipher.ENCRYPT_MODE, aesKeySpec, aesIvSpec);

            this.clientCipherDec = Cipher.getInstance("AES/CTR/NoPadding");
            this.clientCipherDec.init(Cipher.DECRYPT_MODE, aesKeySpec, aesIvSpec);

            this.serverCipherEnc = Cipher.getInstance("AES/CTR/NoPadding");
            this.serverCipherEnc.init(Cipher.ENCRYPT_MODE, aesKeySpec, aesIvSpec);

            this.serverCipherDec = Cipher.getInstance("AES/CTR/NoPadding");
            this.serverCipherDec.init(Cipher.DECRYPT_MODE, aesKeySpec, aesIvSpec);
        }

        private byte[] sha256(final String data, final byte[] salt) throws Exception {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
            final byte[] inputSso = data.getBytes(StandardCharsets.UTF_8);
            final byte[] input = new byte[inputSso.length + salt.length];

            System.arraycopy(inputSso, 0, input, 0, inputSso.length);
            System.arraycopy(salt, 0, input, inputSso.length, salt.length);

            return digest.digest(input);
        }

        private byte[] doCipher(Cipher cipher, byte[] data) {
            if (cipher == null) {
                throw new IllegalStateException("Cipher not initialized.");
            }

            final byte[] modified = cipher.update(data);

            if (modified == null) {
                throw new IllegalStateException("Failed to use cipher on packet. " +
                        "This can happen if the BouncyCastyle security provider is used for AES/CTR. " +
                        "Please use the default security provider instead of " + cipher.getProvider().getName() + ".");
            }

            return modified;
        }

        @Override
        public byte[] clientToGearth(byte[] data) throws Exception {
            if (this.firstClient) {
                this.firstClient = false;

                data = this.staticCipherDec.doFinal(data);

                this.initCiphers(data);
            } else {
                data = doCipher(this.clientCipherDec, data);
            }

            return data;
        }

        @Override
        public byte[] gearthToClient(byte[] data) {
            return doCipher(this.clientCipherEnc, data);
        }

        @Override
        public byte[] serverToGearth(byte[] data) {
            return doCipher(this.serverCipherDec, data);
        }

        @Override
        public byte[] gearthToServer(byte[] data) throws Exception {
            if (this.firstServer) {
                this.firstServer = false;

                data = this.staticCipherEnc.doFinal(data);
            } else {
                data = doCipher(this.serverCipherEnc, data);
            }

            return data;
        }
    }

    protected static class HabboCityVisitor extends ExpressionBlockVisitor {

        private final Module module;

        private byte[] aesKey;
        private byte[] aesIv;
        private byte[] saltKey;
        private byte[] saltIv;

        public HabboCityVisitor(final Module module) {
            this.module = module;
        }

        public byte[] getAesKey() {
            return aesKey;
        }

        public byte[] getAesIv() {
            return aesIv;
        }

        public byte[] getSaltKey() {
            return saltKey;
        }

        public byte[] getSaltIv() {
            return saltIv;
        }

        private void findAesKey(final List<Instr> block) {
            final int pattern = findMatch(Arrays.asList(
                            InstrType.I32_CONST,
                            InstrType.CALL,
                            InstrType.LOCAL_GET,
                            InstrType.I32_CONST,
                            InstrType.I32_ADD,
                            InstrType.I64_CONST,
                            InstrType.I64_STORE,
                            InstrType.LOCAL_GET),
                    block);

            if (pattern != -1) {
                if (aesKey != null) {
                    throw new RuntimeException("Found multiple aes key");
                }

                aesKey = module.getDataSection().getData(((NumericI32ConstInstr) block.get(pattern)).getConstValue(), 16);
            }
        }

        private void findAesIv(final List<Instr> block) {
            final int pattern = findMatch(Arrays.asList(
                            InstrType.I32_CONST,
                            InstrType.I64_LOAD,
                            InstrType.LOCAL_TEE,
                            InstrType.I64_STORE,
                            InstrType.LOCAL_GET,
                            InstrType.I32_CONST,
                            InstrType.I32_STORE16,
                            InstrType.LOCAL_GET,
                            InstrType.LOCAL_GET),
                    block);

            if (pattern != -1) {
                if (aesIv != null) {
                    throw new RuntimeException("Found multiple aes iv");
                }

                aesIv = module.getDataSection().getData(((NumericI32ConstInstr) block.get(pattern)).getConstValue(), 12);
            }
        }

        private void findSaltKey(final List<Instr> block) {
            final int pattern = findMatch(Arrays.asList(
                            InstrType.LOCAL_TEE,
                            InstrType.CALL,
                            InstrType.LOCAL_GET,
                            InstrType.I32_CONST,
                            InstrType.I32_CONST,
                            InstrType.CALL),
                    block);

            if (pattern != -1) {
                if (saltKey != null) {
                    throw new RuntimeException("Found multiple salt key");
                }

                saltKey = module.getDataSection().getDataBetween(
                        ((NumericI32ConstInstr) block.get(pattern + 3)).getConstValue(),
                        ((NumericI32ConstInstr) block.get(pattern + 4)).getConstValue());
            }
        }

        private void findSaltIv(final List<Instr> block) {
            final int pattern = findMatch(Arrays.asList(
                            InstrType.LOCAL_GET,
                            InstrType.CALL,
                            InstrType.LOCAL_GET,
                            InstrType.I32_CONST,
                            InstrType.I32_CONST,
                            InstrType.CALL),
                    block);

            if (pattern != -1) {
                if (saltIv != null) {
                    throw new RuntimeException("Found multiple salt iv");
                }

                saltIv = module.getDataSection().getDataBetween(
                        ((NumericI32ConstInstr) block.get(pattern + 3)).getConstValue(),
                        ((NumericI32ConstInstr) block.get(pattern + 4)).getConstValue());
            }
        }

        @Override
        public void onBlock(List<Instr> block) {
            findAesKey(block);
            findAesIv(block);
            findSaltKey(block);
            findSaltIv(block);
        }

        /**
         * Finds a match in the block.
         *
         * @param pattern The pattern to search for, null is a wildcard
         * @param block The block to search in
         * @return the index of the first match, or -1 if no match is found
         */
        private int findMatch(List<InstrType> pattern, List<Instr> block) {
            if (pattern.size() > block.size()) {
                return -1;
            }

            for (int i = 0; i <= block.size() - pattern.size(); i++) {
                boolean match = true;
                for (int j = 0; j < pattern.size(); j++) {
                    if (pattern.get(j) != null && !block.get(i + j).getInstrType().equals(pattern.get(j))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return i;
                }
            }
            return -1;
        }

    }
}
