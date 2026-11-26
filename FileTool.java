
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTool {

    // 固定在一个子目录里，防止模型传 "../" 之类跑到别处去
    private static final Path DIR = Path.of("data");

    /** args 是文件名，例如 "sales.txt" */
    public String read(String args) {
        try {
            Path p = DIR.resolve(args.trim());
            if (!Files.exists(p)) {
                return "文件不存在：" + p.toAbsolutePath();
            }
            return Files.readString(p, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "读取失败：" + e.getMessage();
        }
    }

    /** args 格式："文件名,内容"。只按【第一个】逗号切，内容里的逗号不受影响 */
    public String write(String args) {
       // indexOf(',') + 两次 substring	切出 name 和 content
        int comma = args.indexOf(',');
        if (comma < 0) {
            return "参数错误：应为 文件名,内容";
        }
        String name = args.substring(0, comma).trim();
        String content = args.substring(comma + 1);

        try {
            //Files.createDirectories(DIR)	保证 data 目录存在
            Files.createDirectories(DIR);
            //DIR.resolve(name)	名字 + 目录 → 完整路径
            Path p = DIR.resolve(name);
            //Files.writeString(p, content, UTF_8)	把内容写进那个路径
            Files.writeString(p, content, StandardCharsets.UTF_8);
            return "已写入 " + p.toAbsolutePath() + "（" + content.length() + " 字符）";
        } catch (IOException e) {
            return "写入失败：" + e.getMessage();
        }
    }
}
