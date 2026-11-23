import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 第 3 课：把假模型换成真 DeepSeek。
 *
 * 设计上只有一条铁律：**方法签名必须和 fakeLlm 一样** ——
 *     String chat(String prompt)
 * 这样 AgentLoop 里只需要换一行，其余逻辑一个字符都不动。
 *
 * 内部就四步：
 *   1) 拼请求体（JSON，用户输入必须转义）
 *   2) 发 POST（HttpURLConnection）
 *   3) 读响应（非 200 要读 errorStream）
 *   4) 从 choices[0].message.content 里取出文本
 */
public class Llm {

    private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";

    private final String key;

    public Llm() {
        // 构造时就检查 Key，缺了立刻炸，不要等到第一个会话里才炸
        String k = System.getenv("DEEPSEEK_API_KEY");
        if (k == null || k.isBlank()) {
            throw new IllegalStateException(
                    "环境变量  DEEPSEEK_API_KEY 未设置。\n"
                    + "IDEA：Run -> Edit Configurations -> Environment variables 里加一条\n"
                    + "终端：export DEEPSEEK_API_KEY=sk-xxxxxxxx");
        }
        this.key = k.trim();      // trim 很重要：复制 Key 时常带首尾空格，导致 401
    }

    /**
     * 给一段提示词，返回模型的回复文本。
     * 签名与 fakeLlm 完全一致，所以可以直接替换。
     */
    public String chat(String prompt) {
        // 这两行打印有两个用处：
        //   1) 让你一眼看出"这次真的走了网络"，而不是假模型瞬间返回
        //   2) 带上线程名，第 5 课多会话并发时，日志里能看出是谁在等
        String who = Thread.currentThread().getName();
        System.out.println("[" + who + "] [LLM] -> 请求 DeepSeek，提示词 " + prompt.length() + " 字符");
        long t0 = System.currentTimeMillis();

        try {
            //拼接请求体，调用deepseek,提取回复，去掉首尾空白
            String body = buildBody(prompt);
            String resp = post(body);
            String content = extractContent(resp);
            if (content == null) {
                throw new IllegalStateException("响应里没有 content，原始响应：" + resp);
            }
            System.out.println("[" + who + "] [LLM] <- 返回 " + content.length() + " 字符，耗时 "
                    + (System.currentTimeMillis() - t0) + " ms");
            return content.trim();
        } catch (IOException e) {
            System.out.println("[" + who + "] [LLM] <- 失败，耗时 " + (System.currentTimeMillis() - t0) + " ms");
            // 转成非受检异常，这样 AgentLoop 不用改方法签名
            throw new RuntimeException("调用 DeepSeek 失败：" + e.getMessage(), e);
        }
    }

    /** 拼请求体。注意：prompt 里有中文、换行、引号，必须转义，否则整个 JSON 会坏 */
    private String buildBody(String prompt) {
        return "{"
                + "\"model\":\"deepseek-chat\","
                + "\"messages\":["
                + "{\"role\":\"user\",\"content\":\"" + JsonUtil.escape(prompt) + "\"}"
                + "],"
                + "\"temperature\":0.3,"
                + "\"stream\":false"
                + "}";
    }

    /** 发 POST，返回响应文本；非 200 时把错误体一起抛出来 */
    private String post(String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        try {
            // 顺序：先 setRequestMethod，再 setDoOutput
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + key);
            // 两个超时：不设的话，一个卡住的请求会永久占住一个会话线程
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            // 关键：非 200 必须读 errorStream，否则只看得到状态码，看不到原因
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
            String text = readAll(is);

            if (code != 200) {
                throw new IOException("HTTP " + code + "：" + text);
            }
            return text;
        } finally {
            conn.disconnect();     // 必须：归还底层 socket
        }
    }

    /** 从 {"choices":[{"message":{"content":"..."}}]} 里取出 content */
    private String extractContent(String resp) {
        Map<String, Object> root = JsonParser.parseObject(resp);
        List<Object> choices = JsonUtil.getList(root.get("choices"));
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Map<String, Object> first = JsonUtil.getMap(choices.get(0));
        Map<String, Object> message = JsonUtil.getMap(first.get("message"));
        Object content = message.get("content");
        return content == null ? null : content.toString();
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toString("UTF-8");
    }
}
