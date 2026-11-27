import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 第 4 课 · 控制台客户端（这个文件不用改，直接用）
 *
 * 它就是一个"发一句、收一句"的程序：
 *   你输入任务 → 发一行给服务端 → 阻塞等回答 → 打印出来 → 再等你输入
 *
 * 输入 exit 退出。
 *
 * 注意它和 AgentServer 的关系：
 *   相对于你，它是客户端（主动连 9000 端口）
 *   而你输入任务后，服务端内部又会去调 DeepSeek —— 那时候它是客户端
 *   同一个程序里，两种角色同时存在
 */
public class AgentClient {

    static final String HOST = "localhost";
    static final int PORT = 9000;

    public static void main(String[] args) throws IOException {

        System.out.println("正在连接 " + HOST + ":" + PORT + " ...");
        Socket socket = new Socket(HOST, PORT);
        System.out.println("已连接。输入任务后回车，输入 exit 退出。\n");

        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner scanner = new Scanner(System.in)) {


            System.out.println(" 等待服务端处理（真模型要 3~10 秒）...");
            //break 只会跳出它所在的那一层，不会跳两层
            // 外层：每问一个问题，跑一次
            //使用一行一消息的方法和客户端通信，客户端发一行任务，服务端读到后处理，再把进度和结果按行发回去
            while (true) {
                System.out.print("任务> ");
                String task = scanner.nextLine();

                if (task.isBlank()) continue;
                if ("exit".equalsIgnoreCase(task.trim())) break;

                out.println(task);
                System.out.println(" 等待服务端处理（真模型要 3~10 秒）...");

                // 内层：把这一次应答的所有帧读完

                while (true) {
                    String line = in.readLine();

                    if (line == null) {
                        System.out.println("  服务端已断开");
                        return;
                    }
                    if ("[结束]".equals(line)) {
                        break;                       // ← 只跳出内层，回到外层继续问
                    }

                    if (line.startsWith("[进度]")) {
                        System.out.println("  " + line);
                    } else if (line.startsWith("[结果]")) {
                        System.out.println("答案：" + line.substring("[结果]".length()).trim());
                    } else {
                        System.out.println("  " + line);
                    }
                }

                System.out.println();
            }

            System.out.println();

        }

        System.out.println("已断开。");
    }
}
