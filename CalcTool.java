/**
 * 第 1 课 · 文件一：本地工具（你来写方法体）
 *
 * 现状：方法体是 throw，所以一跑就会崩。这是故意的。
 * 你的任务：把 eval 方法体写出来，让它能算 "2*200" 得到 400。
 *
 * 要求：
 *   1) 只支持 "a*b" 这一种写法就够了，不需要括号、不需要优先级
 *   2) 参数不合法时抛 IllegalArgumentException，不要返回 0（返回 0 会让错误悄悄传下去）
 *
 * 会用到的 String 方法：
 *   expr.indexOf('*')        找 * 的位置，没有返回 -1
 *   expr.substring(0, i)     取 [0, i) 这一段
 *   expr.trim()              去掉首尾空格
 *   Long.parseLong(str)      把字符串变成数字
 */
public class CalcTool {
    /** args 格式："80,90,75"，返回平均值（保留一位小数） */
    public String avg(String args) {
        String[] parts = args.split(",");
        double sum = 0;
        for (String p : parts) {
            sum += Double.parseDouble(p.trim());
        }
        return String.format("%.1f", sum / parts.length);
    }

    public long eval(String expr) {
        // TODO 1：把这里换成你自己的实现
        // 步骤提示：
        //   a. expr = expr.trim();
        //   b. 找到 '*' 的位置，存在 int star 里
        //   c. 如果 star < 0，说明没有乘号 → 抛异常
        //   d. 否则把左右两段各 parse 成 long，返回它们的乘积
        expr=expr.trim();
        int plus = expr.indexOf('+');
        if (plus > 0) {
            long sum = 0;
            for (String part : expr.split("\\+")) {
                sum += Long.parseLong(part.trim());
            }
            return sum;
        }

        int star=expr.indexOf('*');
        if(star>0){
           long a=Long.parseLong(expr.substring(0,star).trim());
           long b=Long.parseLong(expr.substring(star+1).trim());
           return a*b;
        }
        throw new UnsupportedOperationException("CalcTool.eval 还没写");
    }
//    public static void main(String[] args) {
//        System.out.println(new CalcTool().eval("2*200"));
//    }
// 在 CalcTool 里临时加，测完注释掉
public static void main(String[] args) {
    CalcTool c = new CalcTool();
    System.out.println(c.avg("80,90,75"));      // 81.7
    System.out.println(c.avg("80, 90, 75"));    // 81.7  ← 带空格也得行
    System.out.println(c.avg("100"));           // 100.0
}

}