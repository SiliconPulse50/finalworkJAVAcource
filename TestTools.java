public class TestTools {
    public static void main(String[] args) {
        CalcTool c = new CalcTool();
        System.out.println("calc 2*200        -> " + c.eval("2*200"));
        System.out.println("calc 400+1200+440 -> " + c.eval("400+1200+440"));
        System.out.println("avg  80,90,75     -> " + c.avg("80,90,75"));

        HotelTool h = new HotelTool();
        System.out.println("hotel 深圳,2,豪华  -> " + h.query("深圳,2,豪华"));
        System.out.println("hotel 错误参数     -> " + h.query("深圳,2"));

        TicketTool t = new TicketTool();
        System.out.println("ticket 8          -> " + t.query("8"));
        System.out.println("ticket 65         -> " + t.query("65"));
        System.out.println("ticket 八         -> " + t.query("八"));

        FileTool f = new FileTool();
        System.out.println("readfile          -> " + f.read("sales.txt"));
        System.out.println("writefile         -> " + f.write("test.txt,你好,世界"));
    }
}

