import java.util.Map;

/**
 * 第 1 课 · 文件二：主程序（你来填三个 TODO）
 *
 * 这个文件已经能编译，但跑起来结果是错的 / 会崩。
 * 三个 TODO 填完，输出才会变成完整的两轮 Agent 流程。
 *
 * 不要重写整个文件，只改标了 TODO 的地方。
 */
public class Mine {

    public static void main(String[] args) {
        String task = "帮我算深圳两日游的往返交通费：2 个人，每人 200 元";
        System.out.println("任务：" + task);
        System.out.println();

        //String answer = run(task);
        String answer = run(task, line -> System.out.println("  [进度] " + line));

        System.out.println();
        System.out.println("===== 最终答案 =====");
        System.out.println(answer);
    }

    /** Agent 的全部编排逻辑，都在这个 for 里 */
    static String run(String task, Progress progress) {

        // 记着"已经知道了什么"。每轮的工具结果都追加进来，下一轮拼进提示词。
        StringBuilder known = new StringBuilder();
        Llm llm = new Llm();

        for (int round = 1; round <= 3; round++) {

            // ==========================================================
            // 第 1 步：拼提示词，问模型
            // ----------------------------------------------------------
            // 提示词 = 角色说明 + 输出格式 + 任务 + 已知结果。
            // 模型只能看到提示词里的东西，所以"已经算过什么"必须每轮都带上，
            // 否则它每一轮都会重新要求调用同一个工具。
            // ==========================================================
            progress.emit("第 " + round + " 轮：正在请求模型...");

            String prompt = buildPrompt(task, known.toString());

            // 调试用：想看完整提示词就打开下面这行（会打在服务端控制台，不影响客户端）
            // System.out.println("[调试] 提示词 = " + prompt.replace("\n", " / "));

            String reply = llm.chat(prompt);

            progress.emit("第 " + round + " 轮：模型返回 " + reply);

            // ==========================================================
            // 第 2 步：判断它是不是要给答案了（循环的唯一出口）
            // ----------------------------------------------------------
            // 模型的回复有两种：带 JSON（要调工具）、不带 JSON（在说人话，即答案）。
            // extractJson 返回 null 就是后者，这时把原文整个当答案返回。
            //
            // 作业里那句"直到大模型生成最终结果"，实现上就是这个 if。
            // ==========================================================
            String json = JsonUtil.extractJson(reply);
            if (json == null) {
                return reply.trim();
            }

            Map<String, Object> cmd = JsonParser.parseObject(json);
            String action = JsonUtil.getString(cmd, "action", "");

            // 带 JSON 也分两种：action=final（给答案）或 action=tool（要工具）
            if ("final".equals(action)) {
                return JsonUtil.getString(cmd, "answer", "").trim();
            }
            if (!"tool".equals(action)) {
                return "协议异常，action=" + action;
            }

            // ==========================================================
            // 第 3 步：解析工具指令
            // ----------------------------------------------------------
            // 解析后的 Map 长这样：{action=tool, tool=calc, args=2*200}
            //
            // getString 的第三个参数是"字段不存在时的默认值"，
            // 防止模型漏字段时拿到 null、后面空指针。
            //
            // 为什么不用 substring 切了？
            //   因为 JSON 里有花括号、引号、转义，而且模型还可能在
            //   外面包一层 ```json 围栏或加上"好的，我来算"这种解释文字。
            // ==========================================================
            String tool = JsonUtil.getString(cmd, "tool", "");
            String args = JsonUtil.getString(cmd, "args", "");

            // ==========================================================
            // 第 4 步：执行工具，把结果回填
            // ----------------------------------------------------------
            // a. dispatch 用 switch 分发，没有框架、没有反射
            // b. 把结果拼成 "calc(2*200)=400" 追加到 known，末尾必须带 \n
            //
            // 回填是整件事的关键：模型自己不会算术。
            // 第 1 轮它只会说"我要算 2*200"，你把这行结果告诉它，
            // 第 2 轮它才写得出"共 400 元"。
            // ==========================================================
            progress.emit("第 " + round + " 轮：调用 " + tool + "(" + args + ")");

            String result = dispatch(tool, args);

            progress.emit("第 " + round + " 轮：工具返回 " + result);

            known.append(tool).append('(').append(args).append(")=").append(result).append('\n');
        }

        // 3 轮用完还没答案：兜底返回，绝不能让调用方拿到 null
        return "3 轮用完，模型还没给出最终答案";
    }

    /**
     * 工具分发：一个 switch。
     *
     * 这就是作业里「用 if-else 或 switch 依次调用本地工具类」那一条。
     * 加一个新工具只改两处：
     *   1) 写一个工具类，比如 HotelTool
     *   2) 在这里加一个 case
     */
    static String dispatch(String tool, String args) {
        switch (tool) {
            case "calc":
                return String.valueOf(new CalcTool().eval(args));
            default:
                return "未知工具：" + tool;
        }
    }



    /** 拼提示词（已经写好了，不用改，但要看懂它拼出了什么） */
    static String buildPrompt(String task, String known) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个任务助手。\n");
        //假模型和提示词也要一起改
        sb.append("需要计算时，只输出JSON：");
        sb.append("{\"action\":\"tool\",\"tool\":\"calc\",\"args\":\"算式\"}\n");

        sb.append("已经有答案时，只输出JSON：");
        sb.append("{\"action\":\"final\",\"answer\":\"答案正文\"}\n");
        sb.append("任务：").append(task).append('\n');
        if (!known.isEmpty()) {
            sb.append("已知结果：\n").append(known);
        }
        return sb.toString();
    }

    /**
     * 假模型（不用改）。
     * 它的行为：看到提示词里有"已知结果"就给答案，否则要求调工具。
     * 第 3 课会把它换成真的 DeepSeek。
     */
    static String fakeLlm(String prompt) {
        if (prompt.contains("已知结果")) {
            return "{\"action\":\"final\",\"answer\":\"2 人往返交通费共 400 元\"}";
        }
        return "{\"action\":\"tool\",\"tool\":\"calc\",\"args\":\"2*200\"}";
    }
}
