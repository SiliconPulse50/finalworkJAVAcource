
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Online {

    /** key = 会话编号，value = 这个会话正在处理的任务 */
    private static final Map<Integer, String> MAP = new ConcurrentHashMap<>();

    public static void add(int id, String task) {
        MAP.put(id, task);
        System.out.println(">>> 当前在线 " + MAP.size() + " 个会话：" + MAP.keySet());
    }

    public static void remove(int id) {
        MAP.remove(id);
        System.out.println(">>> 当前在线 " + MAP.size() + " 个会话：" + MAP.keySet());
    }
}

