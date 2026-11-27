import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Cache {

    /** key = 任务原文，value = 上次算出来的答案 */
    private static final Map<String, String> MAP = new ConcurrentHashMap<>();

    private static final AtomicLong HIT  = new AtomicLong();
    private static final AtomicLong MISS = new AtomicLong();

    /** 命中返回答案，未命中返回 null */
    public static String get(String task) {
        String v = MAP.get(task);
        if (v != null) HIT.incrementAndGet();
        else           MISS.incrementAndGet();
        return v;
    }

    public static void put(String task, String answer) {
        MAP.put(task, answer);
    }

    public static void stats() {
        System.out.println(">>> 缓存 命中 " + HIT.get() + " / 未命中 " + MISS.get()
                + " / 已存 " + MAP.size() + " 条");
    }
}

