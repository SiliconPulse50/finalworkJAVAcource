import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 第 4 课 · 一个会话的一生
 *
 * 一个 SessionTask 实例 = 一个 TCP 连接 = 一个线程 = 一个 agent-N。
 *
 * 关键修正（第一版写错了）：
 *   一个连接要能【连续处理多个任务】，直到客户端主动断开。
 *   第一版只 readLine 一次就退出 try 块，导致 try-with-resources
 *   立刻关掉 socket —— 客户端问第二个问题时就会收到
 *   "你的主机中的软件中止了一个已建立的连接"。
 *
 * 所以 readLine 必须放在 while (true) 里，只有返回 null（对方关了）才退出。
 */
//这是“会话”这个概念没落实——一个会话应该能连续处理多个任务，直到客户端主动断开。
public class SessionTask implements Runnable {

    private final Socket socket;
    private final int id;
    private final Llm llm;



    public SessionTask(Socket socket, int id, Llm llm) {
        this.socket = socket;
        this.id = id;
        this.llm = llm;
    }

    @Override
    public void run() {


        long t0 = System.currentTimeMillis();
        Log.info(id, "线程启动 " + Thread.currentThread().getName());

        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // 防止客户端连上却一直不发数据，把这个线程永远占住
            s.setSoTimeout(120_000);

            int round = 0;      // 这个会话里处理到第几个任务

            // 只要客户端不关连接，就一直等下一行
            while (true) {
                String task = in.readLine();

                if (task == null) {
                    // 对方调用 close() 或者进程退出，readLine 会返回 null
                    Log.info(id, "客户端断开，会话结束");
                    break;
                }
                if (task.isBlank()) continue;      // 空行忽略

                round++;
                Log.info(id, "收到第 " + round + " 个任务：" + task);

                long t0Task = System.currentTimeMillis();
                try {
                    Progress progress = line -> out.println("[进度] " + line);
                    String answer = Mine.run(task, progress);
                    out.println("[结果] " + answer);
                    out.println("[结束]");

//                    String answer = Mine.run(task);
//                    out.println(answer);
                    Log.info(id, "第 " + round + " 个任务完成，耗时 "
                            + (System.currentTimeMillis() - t0Task) + " ms");

                } catch (Exception e) {
                    // 单次任务失败：回一句错误，但【不关闭连接】，用户可以接着问
                    out.println("[错误] 本次任务失败：" + e.getClass().getSimpleName()
                            + " " + e.getMessage());
                    Log.info(id, "第 " + round + " 个任务失败：" + e.getMessage());
                }
            }

            Log.info(id, "会话结束，存活 " + (System.currentTimeMillis() - t0) + " ms");

        } catch (IOException e) {
            // 这里兜的是网络层的问题：SoTimeout 到期、连接被复位等
            Log.info(id, "网络异常：" + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            // 第 6 课会在 finally 里把会话从在线表里移除
            Log.info(id, "连接已关闭");
        }
    }
}
