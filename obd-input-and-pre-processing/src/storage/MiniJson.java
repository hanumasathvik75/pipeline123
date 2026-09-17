package com.ignitionai.obdinput.storage;

import java.util.*;

public class MiniJson {
    public static Object parse(String json) {
        return parseValue(new StringIterator(json.trim()));
    }

    private static Object parseValue(StringIterator iter) {
        iter.skipWhitespace();
        char c = iter.peek();
        if (c == '{') return parseObject(iter);
        if (c == '[') return parseArray(iter);
        if (c == '"') return parseString(iter);
        if (c == 't') { iter.expect("true"); return true; }
        if (c == 'f') { iter.expect("false"); return false; }
        if (c == 'n') { iter.expect("null"); return null; }
        return parseNumber(iter);
    }

    private static Map<String, Object> parseObject(StringIterator iter) {
        Map<String, Object> map = new HashMap<>();
        iter.next(); // skip '{'
        iter.skipWhitespace();
        if (iter.peek() == '}') {
            iter.next();
            return map;
        }
        while (true) {
            iter.skipWhitespace();
            String key = parseString(iter);
            iter.skipWhitespace();
            iter.expect(":");
            Object value = parseValue(iter);
            map.put(key, value);
            iter.skipWhitespace();
            char next = iter.next();
            if (next == '}') return map;
            if (next != ',') throw new RuntimeException("Expected ',' or '}' but got " + next);
        }
    }

    private static List<Object> parseArray(StringIterator iter) {
        List<Object> list = new ArrayList<>();
        iter.next(); // skip '['
        iter.skipWhitespace();
        if (iter.peek() == ']') {
            iter.next();
            return list;
        }
        while (true) {
            list.add(parseValue(iter));
            iter.skipWhitespace();
            char next = iter.next();
            if (next == ']') return list;
            if (next != ',') throw new RuntimeException("Expected ',' or ']' but got " + next);
        }
    }

    private static String parseString(StringIterator iter) {
        iter.next(); // skip '"'
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = iter.next();
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char esc = iter.next();
                if (esc == '"' || esc == '\\' || esc == '/') sb.append(esc);
                else if (esc == 'n') sb.append('\n');
                else if (esc == 'r') sb.append('\r');
                else if (esc == 't') sb.append('\t');
                else if (esc == 'b') sb.append('\b');
                else if (esc == 'f') sb.append('\f');
                else throw new RuntimeException("Unsupported escape: \\" + esc);
            } else {
                sb.append(c);
            }
        }
    }

    private static Number parseNumber(StringIterator iter) {
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            char c = iter.peek();
            if (Character.isDigit(c) || c == '-' || c == '.' || c == 'e' || c == 'E' || c == '+') {
                sb.append(iter.next());
            } else {
                break;
            }
        }
        String s = sb.toString();
        if (s.contains(".") || s.contains("e") || s.contains("E")) return Double.parseDouble(s);
        return Long.parseLong(s);
    }

    private static class StringIterator {
        private final String str;
        private int pos = 0;

        public StringIterator(String str) { this.str = str; }
        
        public boolean hasNext() { return pos < str.length(); }
        
        public char peek() {
            if (!hasNext()) throw new RuntimeException("Unexpected EOF");
            return str.charAt(pos);
        }
        
        public char next() {
            if (!hasNext()) throw new RuntimeException("Unexpected EOF");
            return str.charAt(pos++);
        }
        
        public void skipWhitespace() {
            while (hasNext() && Character.isWhitespace(str.charAt(pos))) pos++;
        }
        
        public void expect(String expected) {
            for (int i = 0; i < expected.length(); i++) {
                if (next() != expected.charAt(i)) {
                    throw new RuntimeException("Expected '" + expected + "'");
                }
            }
        }
    }
}
