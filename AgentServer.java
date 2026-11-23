import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第 4 课 · 服务端：作业里「多线程网络通信 20 分」的主体
 *
 * 整个服务端只做两件事：
 *   1) 在 9000 端口等人连进来（accept 会一直阻塞，直到有人来）
 *   2) 连进来一个，就开一个线程去服务他，然后立刻回去等下一个
 *
 * 注意：这个 main 里没有任何业务逻辑 —— 不读任务、不调模型、不算数。
 * 所有业务都在 SessionTask 里。主线程越"闲"，并发能力越强。
 */
public class AgentServer {

    static final int PORT = 9000;

    public static void main(String[] args) throws IOException {

        // 整个程序只有一个 Llm 对象，所有会话共用。
        // 为什么可以共用？因为它只有 key 和 endpoint 两个不可变字段，
        // 每次 chat() 内部新建自己的 HttpURLConnection，用完 disconnect。
        // 无状态 => 天然线程安全。
        Llm llm = new Llm();

        ServerSocket server = new ServerSocket(PORT);
        Log.server("启动成功，监听 " + PORT + " 端口，等待连接...");

        // 会话编号。用 AtomicInteger 而不是 ++count：
        // 严格说这里没有竞争（accept 只在一个线程里执行），
        // 用原子类属于防御性编程 —— 答辩时如果被问"这里根本没竞争吧"，
        // 就照这句回答，比硬说"需要原子性"更可信。
        AtomicInteger seq = new AtomicInteger(0);

        while (true) {
            // 阻塞点：没人连接时，程序就停在这一行
            Socket socket = server.accept();

            int id = seq.incrementAndGet();
            Log.server("会话 " + id + " 已连接，来自 " + socket.getRemoteSocketAddress()
                    + "，当前线程 " + Thread.currentThread().getName());

            // =============================================================
            // TODO 1：给这个会话开一个新线程，然后立刻回到 while 开头
            //
            // 目标：主线程不能在这里等着处理业务，否则第二个用户就得排队。
            //
            // 提示（把下面这行的注释去掉就能用）：
            // new Thread(new SessionTask(socket, id, llm), "agent-" + id).start();
            new Thread(new SessionTask(socket ,id,llm),"agent-"+id).start();
            // 三个参数分别是：干什么（Runnable）、线程叫什么名字。
            // 名字一定要带 id，否则日志里分不清是谁在打印。
            // =============================================================

        }
    }
}
