package chapter2;

import java.util.HashMap;
import java.util.Map;

public class Trie {

    // Trie节点类
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private final TrieNode root;

    // 构造函数，初始化根节点
    public Trie() {
        root = new TrieNode();
    }

    // 插入一个单词
    public void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.isEndOfWord = true;
    }

    // 搜索一个完整的单词
    public boolean search(String word) {
        TrieNode node = searchNode(word);
        return node != null && node.isEndOfWord;
    }

    // 判断是否存在以prefix为前缀的单词
    public boolean startsWith(String prefix) {
        return searchNode(prefix) != null;
    }

    // 辅助函数，返回走完字符串路径的最后一个节点
    private TrieNode searchNode(String str) {
        TrieNode node = root;
        for (char ch : str.toCharArray()) {
            node = node.children.get(ch);
            if (node == null) return null;
        }
        return node;
    }

    // 简单测试
    public static void main(String[] args) {
        Trie trie = new Trie();
        trie.insert("apple");
        System.out.println(trie.search("apple"));   // true
        System.out.println(trie.search("app"));     // false
        System.out.println(trie.startsWith("app")); // true
        trie.insert("app");
        System.out.println(trie.search("app"));     // true
    }
}
