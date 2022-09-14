package cn.wust.yq.crawler.compoent;

import cn.wust.yq.crawler.pojo.YQInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;
import us.codecraft.webmagic.utils.HttpConstant;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class YQProcessor implements PageProcessor {


    private static final Logger log = LoggerFactory.getLogger(YQSpider.class);

    @Override
    public void process(Page page) {
        String level = page.getRequest().getExtra("level").toString();
        switch (level){
            case "infoList":
                parseinfoList(page);
                break;
            case "infoDetail":
                parseDetail(page);
                break;
            case "start":
                parseStartTask(page);
                break;
        }

    }

    private void parseStartTask(Page page) {
        for (int i = 1; i <= 5; ++i) {
            // 时间戳避免去重
            Date date = new Date();
            Request request = new Request(date.toString() + i);
            request.putExtra("level", "infoList");
            request.putExtra("page", Integer.toString(i));
            page.addTargetRequest(request);
        }
    }

    private void parseinfoList(Page page) {
        String content = page.getRawText();
        JsonObject returnData = new JsonParser().parse(content).getAsJsonObject();
        if (!"0000".equals(returnData.get("code").getAsString())) {
            // 返回错误
            log.info("infoList返回错误，message: " +
                    returnData.get("message").getAsString());
        } else {
            // 解析详情页地址
            String pageNum = returnData.get("page").getAsString();
            log.info("解析infoList，page="+pageNum);
            JsonArray list = returnData.get("list").getAsJsonArray();
            for (JsonElement item : list) {
                JsonObject itemObject = item.getAsJsonObject();
                String id = itemObject.get("hbaseId").getAsString();
                String funId = itemObject.get("keywordId").getAsString();
                String keyName = itemObject.get("keywordName").getAsString();
                String url = "http://yq.jmnews.cn/staticweb/#/common/detail" +
                        "?id=" + id +
                        "&funId=" + funId +
                        "&keyName=" + keyName +
                        "&from=warning";
                Request request = new Request(url);
                request.putExtra("level", "infoDetail");
                page.addTargetRequest(request);
            }
        }
    }


    private void parseDetail(Page page) {
        Html html = page.getHtml();
        String hbaseid = page.getRequest().getUrl().split("=")[1].split("&")[0];
        String title = html.$("span[nz-tooltip].ng-star-inserted>div>div", "text").toString();

        List<Selectable> nodes = html.$("div.ant-descriptions-view tbody tr td[colspan]").nodes();
        String source = nodes.get(0).$("td", "text").toString();
        String dateString = nodes.get(1).$("td", "text").toString();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm");
        Date releaseTime = null;
        try {
            releaseTime = df.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Document doc = Jsoup.parse(html.toString());
        String author = nodes.get(2).$("td", "text").toString();
        String releaseSite = nodes.get(3).$("td", "text").toString();
        String attribute = nodes.get(4).$("td", "text").toString();
        String region = nodes.get(5).$("td", "text").toString();
        String keyWord = doc.select("nz-anchor").text();
        String releaseAddress = nodes.get(8).$("td", "text").toString().trim();
        String sourceLink = nodes.get(9).$("td a", "href").toString();
        String bodyText = doc.select("nz-card#mycard").text();
        if (bodyText.length() > 4500) {
            int len = bodyText.length();
            bodyText = bodyText.substring(0, 4500);
            bodyText += " 此处省略"+ (len-4500) + "字";
        }
        YQInfo yqInfo = new YQInfo(hbaseid, title,
                source, releaseTime,
                author, releaseSite,
                attribute, region,
                keyWord, sourceLink,
                bodyText, releaseAddress);
        page.putField("yqInfo", yqInfo);
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
