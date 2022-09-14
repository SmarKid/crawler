package cn.wust.yq.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CrawlerUtils {
    public static boolean ping(String target_name, int out_time)
            throws IOException {

        Runtime runtime = Runtime.getRuntime();

        String ping_command = "ping " + target_name + " -w " + out_time;

        System.out.println("命令格式：" + ping_command);

        Process process = runtime.exec(ping_command);

        if (null == process)
            return false;

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"));// windows下编码默认是GBK，Linux是UTF-8

        String line = null;

        while (null != (line = bufferedReader.readLine())) {

            System.out.println(line);

            if (line.startsWith("bytes from",3))
                return true;
            if (line.startsWith("from"))
                return true;
        }

        bufferedReader.close();

        return false;
    }
}
