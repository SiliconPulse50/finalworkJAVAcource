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

            while (true) {
                System.out.print("任务> ");
                String task = scanner.nextLine();

                if (task == null || task.isBlank()) continue;
                if ("exit".equalsIgnoreCase(task.trim())) break;

                out.println(task);          // 发一行（println 自带换行，服务端才能 readLine 到）

                System.out.println("  等待服务端处理（真模型要 3~10 秒）...");
                String answer = in.readLine();   // 阻塞等结果

                if (answer == null) {
                    System.out.println("  服务端关闭了连接");
                    break;
                }
                System.out.println("答案：" + answer + "\n");
            }
        }

        System.out.println("已断开。");
    }
}
