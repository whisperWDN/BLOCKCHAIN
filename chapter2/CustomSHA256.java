package chapter2;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CustomSHA256 {

    private static final int[] K = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    // 初始哈希值（H0~H7）
    private int[] H = {
        0x6a09e667, 0xbb67ae85,
        0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c,
        0x1f83d9ab, 0x5be0cd19
    };

    public String computeSHA256(String message) {
        byte[] msgBytes = padMessage(message.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < msgBytes.length; i += 64) {
            byte[] block = Arrays.copyOfRange(msgBytes, i, i + 64);
            processBlock(block);
        }

        ByteBuffer buffer = ByteBuffer.allocate(32);
        for (int h : H) {
            buffer.putInt(h);
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : buffer.array()) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    // Step 1 & 2: 填充消息并追加长度
    private byte[] padMessage(byte[] message) {
        int originalLength = message.length;
        long bitLength = (long) originalLength * 8;

        int paddingLength = (56 - (originalLength + 1) % 64 + 64) % 64;
        byte[] padding = new byte[paddingLength + 1];
        padding[0] = (byte) 0x80;

        ByteBuffer buffer = ByteBuffer.allocate(originalLength + padding.length + 8);
        buffer.put(message);
        buffer.put(padding);
        buffer.putLong(bitLength); // big-endian

        return buffer.array();
    }

    // Step 4: 处理每个块
    private void processBlock(byte[] block) {
        int[] W = new int[64];

        for (int i = 0; i < 16; i++) {
            W[i] = ByteBuffer.wrap(block, i * 4, 4).getInt();
        }

        for (int i = 16; i < 64; i++) {
            int s0 = Integer.rotateRight(W[i - 15], 7) ^ Integer.rotateRight(W[i - 15], 18) ^ (W[i - 15] >>> 3);
            int s1 = Integer.rotateRight(W[i - 2], 17) ^ Integer.rotateRight(W[i - 2], 19) ^ (W[i - 2] >>> 10);
            W[i] = W[i - 16] + s0 + W[i - 7] + s1;
        }

        int a = H[0], b = H[1], c = H[2], d = H[3];
        int e = H[4], f = H[5], g = H[6], h = H[7];

        for (int i = 0; i < 64; i++) {
            int S1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
            int ch = (e & f) ^ (~e & g);
            int temp1 = h + S1 + ch + K[i] + W[i];
            int S0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
            int maj = (a & b) ^ (a & c) ^ (b & c);
            int temp2 = S0 + maj;

            h = g; g = f; f = e;
            e = d + temp1;
            d = c; c = b; b = a;
            a = temp1 + temp2;
        }

        H[0] += a;
        H[1] += b;
        H[2] += c;
        H[3] += d;
        H[4] += e;
        H[5] += f;
        H[6] += g;
        H[7] += h;
    }

    public static void testDoubleSHA256LeadingZeros() {
        CustomSHA256 sha256 = new CustomSHA256();
        long nonce = 0;
        int targetZeros = 10;
        long startTime = System.currentTimeMillis();

        while (true) {
            String input = String.valueOf(nonce);
            String firstHash = sha256.computeSHA256(input);
            String secondHash = sha256.computeSHA256(firstHash);

            if (secondHash.startsWith("0".repeat(targetZeros))) {
                long endTime = System.currentTimeMillis();
                System.out.println("Found nonce: " + nonce);
                System.out.println("Double SHA-256: " + secondHash);
                System.out.println("Time used: " + (endTime - startTime) + "ms");
                break;
            }
            nonce++;
            if (nonce % 100000 == 0) {
                System.out.println("Checked nonce: " + nonce);
            }
        }
    }

    public static void main(String[] args) {
        // CustomSHA256 sha256 = new CustomSHA256();
        // String input = "hello world";
        // String hash = sha256.computeSHA256(input);
        // System.out.println("SHA-256(\"" + input + "\") = " + hash);
        testDoubleSHA256LeadingZeros();
    }
}
