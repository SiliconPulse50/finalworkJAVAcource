import java.util.Map;

/**
 * 第 2 课 · 先验证解析器能用（这个文件不用你改，跑一遍看懂就行）
 *
 * 它干的事：拿一段"模型风格"的回复，先截出 JSON，再解析成 Map。
 */
public class TestJson {

    public static void main(String[] args) {

        // 故意模仿真实模型的输出：前后有解释文字、外面包着 markdown 围栏
        String reply = "好的，我来帮你算。首先需要计算交通费：\n"
                + "```json\n"
                + "{\"tool\":\"calc\",\"args\":\"2*200\"}\n"
                + "```\n"
                + "请稍等。";

        System.out.println("===== 模型原始回复 =====");
        System.out.println(reply);

        // 第 1 步：从这堆文字里把 {...} 截出来
        String json = JsonUtil.extractJson(reply);
        System.out.println("===== 截取到的 JSON =====");
        System.out.println(json);

        // 第 2 步：解析成 Map
        Map<String, Object> cmd = JsonParser.parseObject(json);

        System.out.println("===== 解析结果 =====");
        System.out.println("整个 Map   = " + cmd);
        System.out.println("tool       = " + JsonUtil.getString(cmd, "tool", "没这个字段"));
        System.out.println("args       = " + JsonUtil.getString(cmd, "args", "没这个字段"));

        // 第 3 步：验证一个坑 —— get 返回的是 Object，不是 String
        Object raw = cmd.get("args");
        System.out.println("args 的 Java 类型 = " + raw.getClass().getSimpleName());
    }
}
