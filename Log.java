import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志：先把格式统一，第 7 课再改成「队列 + 单写线程」的异步版本。
 *
 * 现在的实现是 synchronized 锁住 System.out —— 多个线程同时打印时，
 * 保证每一行是完整的，不会出现两行粘在一起。
 *
 * 时间戳精确到毫秒，是为了第 4 课演示「多个会话交错执行」：
 * 只有毫秒级的时间戳，才能看出 agent-1 还在等的时候，agent-3 已经干完别的事了。
 */
public class Log {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Object LOCK = new Object();

    /** 服务端自己的日志（不在任何会话里） */
    public static void server(String msg) {
        print("[server]  ", msg);
    }

    /** 某个会话的日志，带会话编号 */
    public static void info(int sessionId, String msg) {
        print(String.format("[agent-%-2d]", sessionId), msg);
    }

    private static void print(String who, String msg) {
        String line = LocalTime.now().format(FMT) + " " + who + " " + msg;
        synchronized (LOCK) {
            System.out.println(line);
        }
    }
}
