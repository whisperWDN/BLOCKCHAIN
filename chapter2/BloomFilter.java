package chapter2;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class BloomFilter {

    private final BitSet bitSet;
    private final int bitSize;
    private final int hashFunctionCount;

    public BloomFilter(int bitSize, int hashFunctionCount) {
        this.bitSize = bitSize;
        this.hashFunctionCount = hashFunctionCount;
        this.bitSet = new BitSet(bitSize);
    }

    // 添加元素
    public void add(String value) {
        for (int i = 0; i < hashFunctionCount; i++) {
            int hash = hash(value, i);
            bitSet.set(hash);
        }
    }

    // 查询元素是否可能存在
    public boolean mightContain(String value) {
        for (int i = 0; i < hashFunctionCount; i++) {
            int hash = hash(value, i);
            if (!bitSet.get(hash)) {
                return false; // 一个位没被设置，肯定不存在
            }
        }
        return true; // 所有位都被设置，可能存在
    }

    // 多哈希函数模拟：使用字符串 + 种子方式
    private int hash(String value, int seed) {
        int result = 0;
        byte[] bytes = (value + seed).getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            result = result * 31 + b;
        }
        return (result & Integer.MAX_VALUE) % bitSize;
    }

    // 测试方法
    public static void main(String[] args) {
        BloomFilter bloomFilter = new BloomFilter(1 << 20, 7); // 1M bits, 7 hash funcs

        // 添加元素
        bloomFilter.add("apple");
        bloomFilter.add("banana");
        bloomFilter.add("cherry");

        // 查询
        System.out.println("apple: " + bloomFilter.mightContain("apple"));     // true
        System.out.println("banana: " + bloomFilter.mightContain("banana"));   // true
        System.out.println("grape: " + bloomFilter.mightContain("grape"));     // false (very likely)
        System.out.println("watermelon: " + bloomFilter.mightContain("watermelon")); // false
    }
}
