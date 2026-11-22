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

        String answer = run(task);

        System.out.println();
        System.out.println("===== 最终答案 =====");
        System.out.println(answer);
    }

    /** Agent 的全部编排逻辑，都在这个 for 里 */
    static String run(String task) {

        // 记着"已经知道了什么"，每轮的工具结果都追加进来
        StringBuilder known = new StringBuilder();

        for (int round = 1; round <= 3; round++) {
            System.out.println("---------- 第 " + round + " 轮 ----------");

            String prompt = buildPrompt(task, known.toString());
            System.out.println("[发给模型] " + prompt.replace("\n", " / "));
            String reply = fakeLlm(prompt);
            System.out.println("[模型说] " + reply);

            // =================================================================
            // TODO 2（出口）：先写这个，它是整个循环唯一能返回的地方
            //
            // 如果 reply 以 "FINAL:" 开头，就把 "FINAL:" 后面那段作为答案返回。
            // 判定标准：跑起来时，第 2 轮应该直接结束，不会走到第 3 轮。
            //
            // 可用方法：reply.startsWith("FINAL:")、reply.substring(6)
            // =================================================================
            if(reply.startsWith("FINAL:")){
                return reply.substring("Final:".length()).trim();
            }
            // =================================================================
            // TODO 3（解析）：从 reply 里拆出工具名和参数
            //
            // reply 的格式固定是：TOOL:calc|2*200
            // 你要得到 tool = "calc"，args = "2*200"。
            //
            // 建议的三步：
            //   a. 去掉前面的 "TOOL:"（长度是多少？数一数）
            //   b. 找 '|' 的位置
            //   c. 按 '|' 切成两半
            //
            // 提示：可以先 String body = reply.substring(...)，再 indexOf('|')
            // =================================================================



            //修改---------模型回复采用解析Json 的方式
            String json = JsonUtil.extractJson(reply);
            if (json == null) {
                return "模型未返回有效 JSON：" + reply;
            }

            Map<String, Object> cmd = JsonParser.parseObject(json);
            String action = JsonUtil.getString(cmd, "action", "");

            if ("final".equals(action)) {
                return JsonUtil.getString(cmd, "answer", "").trim();
            }

            String tool = JsonUtil.getString(cmd, "tool", "");
            String args = JsonUtil.getString(cmd, "args", "");



            // =================================================================
            // TODO 4（执行 + 回填）：
            //
            // a. 用 switch (tool) 判断，是 "calc" 就调用 new CalcTool().eval(args)
            // b. 把结果拼成一行 "calc(2*200)=400" 追加进 known，末尾要加换行 '\n'
            //    （不加换行的话，第 2 轮的提示词里两行会粘在一起）
            //
            // 拼字符串可以用：known.append(tool).append('(').append(args)...
            // 把 long 转成字符串可以用：String.valueOf(结果)
            String result;
            switch (tool) {
                case "calc":
                    result = String.valueOf(new CalcTool().eval(args));
                    break;
                default:
                    result = "未知工具：" + tool;
            }
            known.append(tool).append('(').append(args).append(")=").append(result).append('\n');
            System.out.println("[工具] " + tool + "(" + args + ") = " + result);
            // =================================================================

            System.out.println();   // 轮次之间空一行，好看
        }

        return "3 轮用完，模型还没给出最终答案";
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
