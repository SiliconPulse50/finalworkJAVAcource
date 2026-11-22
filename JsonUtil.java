import java.util.List;
import java.util.Map;

/**
 * JSON 相关的辅助工具：截取、转义、安全取值。
 *
 * 这是作业里最实用的一个类：
 *   extractJson()  从大模型一堆废话里把 JSON 抠出来
 *   escape()       把用户输入安全地拼进 JSON 请求体
 *   getString()    取值时不用到处强转和判空
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /**
     * 从混杂文本中截出第一个完整的 JSON 对象/数组。
     *
     * 为什么不能用 indexOf 找最后一个 }？
     *   因为模型可能返回多段 JSON，或者 JSON 的字符串里本身就带 } —— 比如
     *   {"tool":"echo","args":"}{"}  ← 这里的 } 在字符串里，不该被当成结束符。
     *
     * 做法：从第一个 { 开始，数括号层数（depth）：
     *   遇到 { 就 +1，遇到 } 就 -1，减到 0 就是配对的结尾。
     *   但必须跳过字符串内部的括号，所以要维护 inString 和 escape 两个状态。
     *
     * 这就是"括号配对"的全部逻辑，20 行。
     */
    public static String extractJson(String text) {
        if (text == null || text.isEmpty()) return null;

        // 找到第一个 { 或 [ 作为起点
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) return null;

        char open = text.charAt(start);
        char close = (open == '{') ? '}' : ']';

        int depth = 0;
        boolean inString = false;    // 当前是否在 "..." 里面
        boolean escaping = false;    // 上一个字符是不是反斜杠

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inString) {
                // 字符串内部：只关心转义和结束引号，括号一律不算数
                if (escaping) {
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
        }
        return null;   // 括号没配平，说明模型输出被截断了
    }

    /**
     * 把任意用户输入转义成可以安全放进 JSON 字符串里的形式。
     *
     * 不转义的后果：用户输入一个双引号，整个请求体就非法了，
     * 服务器直接返回 400，你还以为是 API Key 有问题。
     */
    public static String escape(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    // 其余控制字符改成 unicode 转义形式，保证输出一定是合法 JSON
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);      // 中文直接保留，不需要转义
                    }
            }
        }
        return sb.toString();
    }

    // ==================== 安全取值 ====================

    public static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }

    /** 取字符串，取不到就用默认值 —— 拼提示词时最常用，避免 null 污染 */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        String v = getString(map, key);
        return v == null ? defaultValue : v;
    }

    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return v == null ? defaultValue : Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Object v) {
        return (v instanceof Map) ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Object v) {
        return (v instanceof List) ? (List<Object>) v : null;
    }
}
