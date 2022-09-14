package cn.wust.yq.crawler.compoent;

import cn.wust.yq.crawler.pojo.YQInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class YqmsProcessor implements PageProcessor {


    private static final Logger log = LoggerFactory.getLogger(YQSpider.class);

    @Override
    public void process(Page page) {
        String level = page.getRequest().getExtra("level").toString();
        switch (level){
            case "startDownload":
                startDownload(page);
                break;
            case "processList":
                processList(page);
                break;
            case "processDetails":
                processDetails(page);
                break;
        }
    }

    /**
     * 解析详情页数据并保存到数据库
     * @param page
     */
    private void processDetails(Page page) {
        Html html = page.getHtml();
        // id
        String url = page.getUrl().toString();
        int beginIndex = url.indexOf("id=") + 3;
        int endIndex = url.indexOf('&', beginIndex);
        String id = url.substring(beginIndex, endIndex);
        // title
        String title = html.$("span[title].ellipsis", "title").toString();
        // source
        String source = html.$(".info-source", "title").toString();
        // release_time
        List<String> timeAndAddress = html.$(".info-time", "text").all();
        String timeStr = timeAndAddress.get(0);
        LocalDateTime releaseTime = LocalDateTime
                .parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        // author
        String author = html.$(".info-nakename", "title").toString();
        if (author == null || author.equals("")) {
            author = html.$(".user-nakename", "title").toString();
        }
        // release_address
        String releaseAddress = null;
        if (timeAndAddress.size() > 1)
            releaseAddress = timeAndAddress.get(1).substring(4);
        // attribute
        String attributeElement = html.$(".detail-tendency").toString();
        String attribute = "";
        String[] attributeArray = {"negative", "neutral", "positive"};
        for (String attr : attributeArray) {
            if (attributeElement.contains(attr)) attribute = attr; break;
        }
        // key_word
        List<String> keywordList = html.$(".keyword-box", "text").all();
        String keyWord = String.join(",", keywordList);
        //source_link
        String sourceLink = html.$(".detail-link", "href").toString();
        // body_text
        String[] bodyClassArray = {
                ".detail-article-box",
                ".blog-txt-box",
                ".detail-txt-box",
                ".detail-blog-box"
        };
        String bodyText = "";
        for (String bodyClass : bodyClassArray) {
            bodyText = html.$(bodyClass, "text").toString();
            if (bodyText != null && !bodyText.equals("")) break;
        }
        if (bodyText.length() > 4500) {
            bodyText = bodyText.substring(0, 4500);
            bodyText += " 此处省略"+ (bodyText.length()-4500) + "字";
        }

        String releaseSite = source;
        String region = releaseAddress;

        YQInfo yqInfo = new YQInfo(id, title,
                source, releaseTime,
                author, releaseSite,
                attribute, region,
                keyWord, sourceLink,
                bodyText, releaseAddress);
        page.putField("yqInfo", yqInfo);
    }

    /**
     * 处理列表页，发出下载详情页的请求
     * @param page
     */
    private void processList(Page page) {
        Html html = page.getHtml();
        Selectable selectable = html.$("body>div");
        HashSet<String> idSet = new HashSet<>(selectable
                .$("span[id].list-title.selection-tag", "id").all());
        for (String id : idSet) {
            String url = "https://yqms.istarshine.com/v4/detail?from=1&id=" + id
                    + "&isSearch=false&index=undefined&keywords=undefined";
            Request request = new Request(url);
            request.putExtra("level", "downloadDetails");
            page.addTargetRequest(request);
        }
    }

    /**
     * 发出下载列表页的请求
     * @param page
     */
    private void startDownload(Page page) {
        for (int i = 1; i <= 5; i++) {
            String url = "download page " + i;
            Request request = new Request(url);
            request.putExtra("pageNum", Integer.toString(i));
            request.putExtra("level", "downloadListPage");
            page.addTargetRequest(request);
            log.info("请求list page " + i);
        }
    }

    private Site site = Site.me()
            .setCharset("gbk")
            .setTimeOut(60 * 1000)
            .setRetrySleepTime(10*1000)
            .setRetryTimes(3);

    @Override
    public Site getSite() {
        return site;
    }
}
