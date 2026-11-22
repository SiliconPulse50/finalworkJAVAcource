import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 递归下降 JSON 解析器（纯 JDK 实现，无任何第三方库）
 *
 * 【核心思想】一条语法规则 = 一个函数；函数互相调用 = 递归。
 *
 *   value  →  object | array | string | number | true | false | null
 *   object →  { "key" : value , "key" : value }
 *   array  →  [ value , value ]
 *
 * 因为 value 里可以包含 object，object 里又可以有 value，规则自己包含自己，
 * 所以用"函数互相调用"来实现最自然：遇到 { 就调 parseObject，
 * parseObject 读到值又回头调 parseValue，一层括号对应一层函数调用栈。
 *
 * 【为什么快】只有一个游标 pos，从头走到尾，从不回退，时间复杂度 O(n)。
 *
 * 【返回类型】JSON 的类型映射到 Java：
 *   object -> LinkedHashMap<String,Object>（保持字段顺序）
 *   array  -> ArrayList<Object>
 *   string -> String
 *   整数    -> Long       （不是 Integer！大数字不会溢出）
 *   小数    -> Double
 *   布尔    -> Boolean
 *   null   -> null
 */
public final class JsonParser {

    private final String src;
    private int pos;                    // 当前读到第几个字符

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    // ==================== 对外入口 ====================

    /** 解析任意 JSON 文本，返回 Map / List / String / Long / Double / Boolean / null */
    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("待解析文本为 null");
        JsonParser p = new JsonParser(text);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (p.pos < text.length()) {
            throw p.error("JSON 已经结束，后面还有多余字符");
        }
        return value;
    }

    /** 解析成 JSON 对象（Map），最常用 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("这不是一个 JSON 对象，而是一个 " + typeName(v));
        }
        return (Map<String, Object>) v;
    }

    /** 解析成 JSON 数组（List） */
    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String text) {
        Object v = parse(text);
        if (!(v instanceof List)) {
            throw new IllegalArgumentException("这不是一个 JSON 数组，而是一个 " + typeName(v));
        }
        return (List<Object>) v;
    }

    // ==================== 解析规则（每条规则一个函数）====================

    /** 规则 1：value 可以是对象、数组、字符串、数字、true/false/null */
    private Object parseValue() {
        char c = peek();
        switch (c) {
            case '{': return parseObject();      // 对象
            case '[': return parseArray();       // 数组
            case '"': return parseString();       // 字符串
            case 't': expectWord("true");  return Boolean.TRUE;
            case 'f': expectWord("false"); return Boolean.FALSE;
            case 'n': expectWord("null");  return null;
            default:  return parseNumber();       // 数字
        }
    }

    /** 规则 2：object -> { "key" : value , "key" : value } */
    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();   // 保持字段顺序，日志里好读

        skipWhitespace();
        if (peek() == '}') {          // 空对象 {}
            pos++;
            return map;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();      // 键必须是字符串
            skipWhitespace();
            expect(':');                     // 键和值之间必须有冒号
            skipWhitespace();
            map.put(key, parseValue());      // 值可以是任何类型 -> 递归回调

            skipWhitespace();
            char c = next();
            if (c == '}') return map;        // 对象结束
            if (c != ',') throw error("对象里期望 , 或 }，实际是 '" + c + "'");
        }
    }

    /** 规则 3：array -> [ value , value ] */
    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();

        skipWhitespace();
        if (peek() == ']') {          // 空数组 []
            pos++;
            return list;
        }

        while (true) {
            skipWhitespace();
            list.add(parseValue());          // 元素可以是任何类型 -> 递归回调

            skipWhitespace();
            char c = next();
            if (c == ']') return list;
            if (c != ',') throw error("数组里期望 , 或 ]，实际是 '" + c + "'");
        }
    }

    /** 规则 4：字符串，必须处理转义 */
    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();

        while (true) {
            char c = next();
            if (c == '"') return sb.toString();      // 收尾
            if (c != '\\') {                          // 普通字符
                sb.append(c);
                continue;
            }
            // 遇到反斜杠 -> 看下一个字符决定转义成什么
            char e = next();
            switch (e) {
                case '"':  sb.append('"');  break;
                case '\\': sb.append('\\'); break;
                case '/':  sb.append('/');  break;
                case 'b':  sb.append('\b'); break;
                case 'f':  sb.append('\f'); break;
                case 'n':  sb.append('\n'); break;
                case 'r':  sb.append('\r'); break;
                case 't':  sb.append('\t'); break;
                case 'u':                          // \u4e2d 这种 Unicode 转义
                    if (pos + 4 > src.length()) throw error("\\u 后面不足 4 位十六进制");
                    String hex = src.substring(pos, pos + 4);
                    sb.append((char) Integer.parseInt(hex, 16));
                    pos += 4;
                    break;
                default:
                    throw error("不认识的转义字符 \\" + e);
            }
        }
    }

    /** 规则 5：数字。整数用 Long，带小数点或科学计数法用 Double */
    private Object parseNumber() {
        int start = pos;
        while (pos < src.length() && "+-0123456789.eE".indexOf(src.charAt(pos)) >= 0) {
            pos++;
        }
        String num = src.substring(start, pos);
        if (num.isEmpty()) {
            String got = pos < src.length() ? String.valueOf(src.charAt(pos)) : "文本结尾";
            throw error("这里应该是一个值，却读到了 " + got);
        }
        if (num.contains(".") || num.contains("e") || num.contains("E")) {
            return Double.parseDouble(num);
        }
        return Long.parseLong(num);
    }

    // ==================== 底层工具方法 ====================

    /** 跳过空格、换行、制表符。JSON 允许任意空白 */
    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    /** 看一眼当前字符，但不消费它 */
    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    /** 取当前字符并前进一格 */
    private char next() {
        if (pos >= src.length()) throw error("文本意外结束");
        return src.charAt(pos++);
    }

    /** 要求当前字符必须是 c，否则报错 */
    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            String got = pos < src.length() ? "'" + src.charAt(pos) + "'" : "文本结尾";
            throw error("期望 '" + c + "'，实际是 " + got);
        }
        pos++;
    }

    /** 要求接下来必须是指定单词（true / false / null） */
    private void expectWord(String word) {
        if (!src.startsWith(word, pos)) throw error("期望 " + word);
        pos += word.length();
    }

    /** 报错时带上位置，方便定位是模型返回的哪一段出了问题 */
    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + "（位置 " + pos + "）");
    }

    private static String typeName(Object v) {
        if (v == null) return "null";
        if (v instanceof Map) return "对象";
        if (v instanceof List) return "数组";
        if (v instanceof String) return "字符串";
        return "数字/布尔";
    }
}
