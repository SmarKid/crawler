package cn.wust.yq.crawler.compoent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover;
import us.codecraft.webmagic.scheduler.QueueScheduler;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class YqmsSpider {

    private static final Logger log = LoggerFactory.getLogger(YqmsSpider.class);

    @Autowired
    YQPipeline pipeline;

    @Autowired
    YqmsDownloader downloader;

    @Scheduled(initialDelay = 1000, fixedDelay = 24 * 60 * 60 * 1000)
    public void doCrawler() {
        // 随机等待0-15分钟
        Random r=new Random();
        try {
            int wait = r.nextInt(15);
            log.info("随机等待 " + wait + " 分钟");
            TimeUnit.SECONDS.sleep(wait);
        } catch (InterruptedException e) {
            log.info(e.toString());
        }
        String indexUrl = "https://yqms.istarshine.com/v4/subject";
        log.info("开始爬取" + indexUrl);
        Request request = new Request(indexUrl);
        request.putExtra("level", "start");
        Spider.create(new YqmsProcessor())
                .addPipeline(pipeline)
                .addRequest(request)
                .setDownloader(downloader)
                .setScheduler(new QueueScheduler()
                        .setDuplicateRemover(new BloomFilterDuplicateRemover(100000)))
                .thread(1)
                .run();
    }
}
