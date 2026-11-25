import java.util.HashMap;
import java.util.Map;

public class HotelTool {

    // 模拟数据库：查表代替真实查询
    private static final Map<String, Integer> PRICE = new HashMap<>();
    static {
        PRICE.put("深圳-标准", 300);
        PRICE.put("深圳-豪华", 600);
        PRICE.put("广州-标准", 250);
        PRICE.put("广州-豪华", 500);
    }

    /** args 格式："城市,晚数,房型"，例如 "深圳,2,豪华" */
    public String query(String args) {
        String[] p = args.split(",");
        if (p.length != 3) {
            return "参数错误：应为 城市,晚数,房型，收到 " + args;
        }
        String city = p[0].trim();
        String nights = p[1].trim();
        String type = p[2].trim();

        String key = city + "-" + type;
        Integer price = PRICE.get(key);
        if (price == null) {
            return "没有查到 " + key + " 的价格，可选：" + PRICE.keySet();
        }

        int n = Integer.parseInt(nights);
        return key + " 每晚 " + price + " 元 × " + n + " 晚 = " + (price * n) + " 元";
    }
}

