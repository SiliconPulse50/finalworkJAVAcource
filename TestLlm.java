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
 * 第 3 课 · 第一步：先确认"能不能连上真模型"
 *
 * 这个文件不用你改，跑通就行。它只做三件事：
 *   1) 从环境变量读 API Key（没有就立刻退出，并告诉你怎么设）
 *   2) 用 HttpURLConnection 发一个最简单的请求
 *   3) 把原始响应打印出来，再用你的 JsonParser 把 content 取出来
 *
 * 跑通它，第 3 课的主体就成了一半 —— 剩下的只是把它塞进 AgentLoop。
 */
public class TestLlm {

    static final String ENDPOINT = "https://api.deepseek.com/chat/completions";

    public static void main(String[] args) throws Exception {

        // ---------- 1. 读 Key ----------
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("[启动失败] 环境变量 DEEPSEEK_API_KEY 没有设置");
            System.err.println("  Git Bash : export DEEPSEEK_API_KEY=sk-xxxxxxxx");
            System.err.println("  CMD      : set DEEPSEEK_API_KEY=sk-xxxxxxxx");
            System.err.println("  IDEA     : Run -> Edit Configurations -> Environment variables");
            System.exit(1);
        }
        key = key.trim();
        System.out.println("Key 已读到，长度 " + key.length() + "，前 6 位 " + key.substring(0, 6) + "...");

        // ---------- 2. 手拼请求体 ----------
        // 注意这是 Java 字符串，里面的双引号都写成 \"
        String body = "{"
                + "\"model\":\"deepseek-chat\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"你是一个简洁的助手。\"},"
                + "{\"role\":\"user\",\"content\":\"用一句话介绍深圳。\"}"
                + "],"
                + "\"temperature\":0.3,"
                + "\"stream\":false"
                + "}";
        System.out.println("请求体：" + body);

        // ---------- 3. 发请求 ----------
        long t0 = System.currentTimeMillis();
        String resp = post(ENDPOINT, key, body);
        System.out.println("耗时：" + (System.currentTimeMillis() - t0) + " ms");

        // ---------- 4. 看原始响应 ----------
        System.out.println("===== 原始响应 =====");
        System.out.println(resp);

        // ---------- 5. 用自己写的解析器取值 ----------
        System.out.println("===== 解析后 =====");
        Map<String, Object> root = JsonParser.parseObject(resp);

        List<Object> choices = JsonUtil.getList(root.get("choices"));
        if (choices == null || choices.isEmpty()) {
            System.out.println("没有 choices，说明这不是一个正常响应（多半是错误信息）");
            return;
        }

        Map<String, Object> first = JsonUtil.getMap(choices.get(0));
        Map<String, Object> message = JsonUtil.getMap(first.get("message"));

        System.out.println("role    = " + message.get("role"));
        System.out.println("content = " + message.get("content"));
        System.out.println("finish  = " + first.get("finish_reason"));
    }

    /** 一个最小的 POST：把 body 发出去，把响应（或错误体）原样读回来 */
    static String post(String endpoint, String key, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("HTTP 状态码：" + code);

            // 关键：非 200 必须读 errorStream，否则只能看到状态码，看不到原因
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
            return readAll(is);
        } finally {
            conn.disconnect();
        }
    }

    static String readAll(InputStream is) throws IOException {
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
