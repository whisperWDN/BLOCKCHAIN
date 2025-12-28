package chapter2;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CustomMD5 {

    // 四个缓冲区的初始值（A, B, C, D）
    private int A = 0x67452301;
    private int B = 0xefcdab89;
    private int C = 0x98badcfe;
    private int D = 0x10325476;

    // 常量T[i]，为 sin(i) 的绝对值乘以 2^32 向下取整
    private static final int[] T = new int[64];

    static {
        for (int i = 0; i < 64; i++) {
            T[i] = (int) (long) ((1L << 32) * Math.abs(Math.sin(i + 1)));
        }
    }

    // 四个非线性函数
    private int F(int x, int y, int z) {
        return (x & y) | (~x & z);
    }

    private int G(int x, int y, int z) {
        return (x & z) | (y & ~z);
    }

    private int H(int x, int y, int z) {
        return x ^ y ^ z;
    }

    private int I(int x, int y, int z) {
        return y ^ (x | ~z);
    }

    // 左循环移位
    private int rotateLeft(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    // 每轮的移位值
    private static final int[] S = {
            7, 12, 17, 22, 7, 12, 17, 22,
            7, 12, 17, 22, 7, 12, 17, 22,

            5, 9, 14, 20, 5, 9, 14, 20,
            5, 9, 14, 20, 5, 9, 14, 20,

            4, 11, 16, 23, 4, 11, 16, 23,
            4, 11, 16, 23, 4, 11, 16, 23,

            6, 10, 15, 21, 6, 10, 15, 21,
            6, 10, 15, 21, 6, 10, 15, 21
    };

    // 主处理逻辑
    public String computeMD5(String message) {
        byte[] msgBytes = padMessage(message.getBytes(StandardCharsets.UTF_8));
        int[] buffer = {A, B, C, D};

        for (int i = 0; i < msgBytes.length / 64; i++) {
            byte[] block = Arrays.copyOfRange(msgBytes, i * 64, (i + 1) * 64);
            processBlock(block, buffer);
        }

        ByteBuffer resultBuffer = ByteBuffer.allocate(16);
        for (int val : buffer) {
            resultBuffer.putInt(Integer.reverseBytes(val)); // Little-endian
        }

        byte[] result = resultBuffer.array();
        StringBuilder hexString = new StringBuilder();
        for (byte b : result) {
            hexString.append(String.format("%02x", b & 0xFF));
        }

        return hexString.toString();
    }

    // Step 1: 填充消息 + Step 2: 添加长度
    private byte[] padMessage(byte[] input) {
        int originalLength = input.length;
        long bitLength = (long) originalLength * 8;

        int paddingLength = (56 - (originalLength + 1) % 64 + 64) % 64;
        byte[] padding = new byte[paddingLength + 1];
        padding[0] = (byte) 0x80;

        ByteBuffer buffer = ByteBuffer.allocate(originalLength + padding.length + 8);
        buffer.put(input);
        buffer.put(padding);
        buffer.putLong(Long.reverseBytes(bitLength)); // Little-endian 64-bit length
        return buffer.array();
    }

    // Step 4: 处理每个512位块
    private void processBlock(byte[] block, int[] buffer) {
        int[] M = new int[16];
        for (int i = 0; i < 16; i++) {
            M[i] = ByteBuffer.wrap(block, i * 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        }

        int a = buffer[0], b = buffer[1], c = buffer[2], d = buffer[3];

        for (int i = 0; i < 64; i++) {
            int f = 0, g = 0;
            if (i < 16) {
                f = F(b, c, d);
                g = i;
            } else if (i < 32) {
                f = G(b, c, d);
                g = (5 * i + 1) % 16;
            } else if (i < 48) {
                f = H(b, c, d);
                g = (3 * i + 5) % 16;
            } else {
                f = I(b, c, d);
                g = (7 * i) % 16;
            }

            int temp = d;
            d = c;
            c = b;
            b = b + rotateLeft(a + f + M[g] + T[i], S[i]);
            a = temp;
        }

        buffer[0] += a;
        buffer[1] += b;
        buffer[2] += c;
        buffer[3] += d;
    }

    // 测试
    public static void main(String[] args) {
        CustomMD5 md5 = new CustomMD5();
        String input = "hello world";
        String result = md5.computeMD5(input);
        System.out.println("MD5(\"" + input + "\") = " + result);
    }
}
