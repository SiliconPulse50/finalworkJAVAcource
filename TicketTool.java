import java.util.HashMap;
import java.util.Map;

public class TicketTool {

    private static final Map<String, Integer> PRICE = new HashMap<>();
    static {
        PRICE.put("儿童票", 100);
        PRICE.put("成人票", 220);
        PRICE.put("老人票", 120);
    }

    /** args 是一个年龄，例如 "8" */
    public String query(String args) {
        int age;
        try {
            age = Integer.parseInt(args.trim());
        } catch (NumberFormatException e) {
            return "年龄格式不对：" + args + "，应为整数，例如 8";
        }
        if (age < 0 || age > 150) {
            return "年龄不合法：" + age;
        }

        String type;
        if (age < 12)      type = "儿童票";
        else if (age < 60) type = "成人票";
        else               type = "老人票";

        int price = PRICE.get(type);
        return "年龄 " + age + " 岁 → " + type + " " + price + " 元";
    }
}
