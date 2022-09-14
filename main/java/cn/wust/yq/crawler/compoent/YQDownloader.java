package cn.wust.yq.crawler.compoent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.*;

@Component
public class YQDownloader implements Downloader {

    @Value("${indexUrl}")
    private String indexUrl;

    @Value("${yq.username}")
    private String yqUsername;

    @Value("${yq.password}")
    private String yqPassword;

    Browser browser;

    Playwright playwright;

    BrowserContext context;

    APIRequestContext requestContext;

    int loginStatus;

    private static final Logger log = LoggerFactory.getLogger(Downloader.class);

    public YQDownloader() {
        /**
         * 初始化工作
         * 新建窗口
         */
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
//                new BrowserType.LaunchOptions()
//                        .setHeadless(false).setSlowMo(50)
        );
    }

    @Override
    public Page download(Request request, Task task) {
        /*
            跳转到相应操作
         */
        if(loginStatus != 0){
            return null;
        }
        Page page = null;
        if ("start".equals(request.getExtra("level"))) {
            page = startTask();
        }
        else if ("infoList".equals(request.getExtra("level"))) {
            page = infoListDownload(request, task);
        }
        else if ("infoDetail".equals(request.getExtra("level"))) {
            page = infoDetailDownload(request.getUrl());
        }

        return page;
    }

    private Page startTask() {
        // 登录
        if(loadContext() != 0) {
            log.info("登录失败");
            this.loginStatus = 1;
        } else {
            this.loginStatus = 0;
        }
        // 标记开始
        String htmlStr = "start";
        String currentUrl = "start";
        String level = "start";
        Page page = createPage(htmlStr, currentUrl,level, null);
        return page;
    }

    @Override
    public void setThread(int i) {

    }

    private String verifyCodeServer(byte[] vertifyCodeByte) {
        /**
         * 由验证码byte信息返回验证码结果
         */
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://api.dididati.com/v3/upload/base64");
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> list = new ArrayList<>();
        list.add(new BasicNameValuePair("image", new String(Base64.getEncoder().encode(vertifyCodeByte))));
        // zzk: 9a653c885ed2db4dde269c0809bfa8ab311244478ebb774b
        list.add(new BasicNameValuePair("userkey", "ffce95e5c573089d3f65c6b1407c2968a60ee839a0a57a96"));
        HttpEntity httpEntity = null;// 注意：尽量指定编码，否则会出现请求失败，获取不到数据
        try {
            httpEntity = new UrlEncodedFormEntity(list, "utf-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e.toString());
        }
        httpPost.setEntity(httpEntity);
        setPostHeaders(httpPost);
        // 执行得到结果
        CloseableHttpResponse postResponse = null;
        String vertifyCodeJson = null;
        try {
            postResponse = httpClient.execute(httpPost);
            vertifyCodeJson = EntityUtils.toString(postResponse.getEntity(), "utf-8");
            httpClient.close();
        } catch (Exception e) {
            log.error(e.toString());
        }
        JSONObject jsonObject = JSON.parseObject(vertifyCodeJson);
        String vertifyCode = null;
        if (jsonObject.get("err").equals("0")) {
            //组装填写验证码请求
            vertifyCode = JSONPath.eval(jsonObject, "result.code").toString();
        }else {
            log.warn("验证码返回错误，可能已欠费");
        }
        return vertifyCode;
    }


    public static void setPostHeaders(HttpPost httpPost) {
        httpPost.addHeader("Accept", "*/*");
        httpPost.addHeader("Accept-Encoding", "gzip, deflate");
        httpPost.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        //httpPost.addHeader("Content-Type","multipart/form-data; boundary=48940923NODERESLTER3890457293");
        httpPost.addHeader("Connection", "close");
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:24.0) Gecko/20100101 Firefox/24.0");
        httpPost.addHeader("Host", "api.dididati.com:80");
    }


    /**
     * 登录打码平台 http://dididati.com/ 获取用户key
     */
    private String loginDiditati(CloseableHttpClient httpClient) {
        String username = "";
        String password = "";
        URIBuilder uriBuilder = null;
        String respose_json = null;
        try {
            uriBuilder = new URIBuilder("http://yun.itheima.com/search");
            uriBuilder.setParameter("username", username);
            uriBuilder.setParameter("password", password);
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            CloseableHttpResponse response = httpClient.execute(httpGet);
            respose_json = EntityUtils.toString(response.getEntity(), "utf-8");
        } catch (Exception e) {
            log.error(e.toString());
        }
        JSONObject json = JSONObject.parseObject(respose_json);
        if ("0".equals(json.getJSONArray("err"))){
            // String userKey = json.getJSONArray("result");
        }
        json.getJSONArray("");
        return null;
    }


    /**
     * 下载列表页数据
     * @param request
     * @param task
     * @return
     */
    private Page infoListDownload(Request request, Task task) {
        String url = "http://yq.jmnews.cn/gateway/monitor/api/warning/log/auth/warningSendRecord/search";
        Map<String, String> data = new HashMap<>();
        Date endTime = new Date();
        Date startTime = DateUtils.addDays(endTime, -3);
        String endTimeString = DateFormatUtils.format(endTime, "yyyy-MM-dd");
        String startTimeString = DateFormatUtils.format(startTime, "yyyy-MM-dd");
        data.put("endTime", endTimeString + " 00:00:00"); // endTime 2022-09-14 00:00:00
        data.put("startTime", startTimeString + " 23:59:00"); // 2022-09-14 00:00:00
        data.put("keywordId", "3115709,1287447");
        data.put("page", request.getExtra("page"));
        data.put("pageSize", "50");
        APIResponse response = requestContext.post(
                "http://yq.jmnews.cn/gateway/monitor/api/warning/log/auth/warningSendRecord/search",
                RequestOptions.create().setData(data).setTimeout(2 * 60 * 1000));
        String text = response.text();
        response.dispose();
        if (text == null || text.trim().equals("")) {
            log.info("infoListDownload 详情页空" + request.getExtra("page"));
            return null;
        }
        Page page = createPage(text, url, "infoList", request.getExtra("page"));
        return page;
    }

    /**
     * 下载详情页数据
     */
    private Page infoDetailDownload(String url) {
        String hbaseid = url.split("=")[1].split("&")[0];
        log.info("yq开始下载详情页，hbaseid=" + hbaseid);
        com.microsoft.playwright.Page page = context.newPage();
        page.navigate(url);
        Random random=new Random();
        int ran_int = random.nextInt(40);
        page.waitForTimeout((10 + ran_int) * 1000);
        String content = page.content();
        String currentUrl = page.url();
        String level = "infoDetail";
        String pageNum = "";
        Page pageReturn = createPage(content, currentUrl, level, pageNum);
        page.close();

        return pageReturn;
    }

    /**
     * 加载context，确保context处于登录状态
     * @param
     * @return
     */
    private int loadContext() {
        int status = 1;
        File file = new File("json/yqState.json");
        if (!file.exists()){
            if(updateLoginContext() != 0) {
                status = 1;
            }else{
                status = 0;
            }
        }
        else {
            // 加载context并检查
            context = browser.newContext(
                    new Browser.NewContextOptions()
                        .setStorageStatePath(Paths.get("json/yqState.json")));
            requestContext = context.request();
            if (0 == vertifyLogin()) {
                log.info("登录文件在有效期内");
                status = 0;
            }
            else {
                log.info("登录过期");
                if (context!=null) {
                    context.close();
                }
                status = updateLoginContext();
            }
        }
        return status;
    }

    /**
     * 检测context的登录状态
     * @return int 0:登陆成功, 1:登陆失效, 2: 超时
     */
    private int vertifyLogin() {
        int status = 1;
        String url = "http://yq.jmnews.cn/gateway/monitor/api/user/auth/get/head/userInfo";
        com.microsoft.playwright.Page page = context.newPage();
        Response response = page.navigate(url);
        JsonObject jsonObject = new JsonParser().parse(response.text()).getAsJsonObject();
        page.close();
        if (jsonObject.get("code").getAsString().equals("20000")) {
            status = 0;
        }
        loginStatus = status;
        return status;
    }

    /**
     * 在页面中寻找是否存在用户名
     * @param page
     * @return 0: 存在, others: 不存在
     */
    private int findUsername(com.microsoft.playwright.Page page) {
        List<String> nzplacement = page.locator("span[nzplacement]").allTextContents();
        int status = 1;
        if (nzplacement == null || nzplacement.size()<=0) {
            status = 1;
        }
        else if (nzplacement.get(1).contains(yqUsername)) {
            status = 0;
        }
        return status;
    }

    /**
     * 更新并加载context
     * @return status 成功为0，否则为1
     */
    private int updateLoginContext() {
        int status = 1;
        int i = 0;
        if (context!=null) {
            context.close();
            context = null;
        }
        if (requestContext != null) {
            requestContext.dispose();
            requestContext = null;
        }
        if (browser != null) {
            browser.close();
            browser = null;
        }
        browser = playwright.chromium().launch();
        context = browser.newContext();
        com.microsoft.playwright.Page loginPage = context.newPage();
        do {
            log.info("尝试第"+ (i+1) + "次登录");
            loginPage.navigate(indexUrl);
            log.info("标题：" + loginPage.title());
            loginPage.fill("[formcontrolname=\"userName\"]", yqUsername);
            loginPage.fill("[formcontrolname=\"password\"]", yqPassword);

            byte[] vertifyCodeByte = loginPage.locator("div.login-box img.ng-star-inserted").first()
                    .screenshot(new Locator.ScreenshotOptions()
                            .setPath(Paths.get("screenshot"+ (i+1) + ".png"))
                    );
            String vertifyCode = verifyCodeServer(vertifyCodeByte);
            log.info("验证码为" + vertifyCode + ". 保存为" + "screenshot"+ (i+1) + ".png");
            loginPage.fill("[formcontrolname=yqzcode]", vertifyCode);
            loginPage.waitForTimeout(2 * 1000);
            loginPage.click("[nz-button]>span");
            loginPage.waitForTimeout(20 * 1000);
            ++i;
        } while (findUsername(loginPage)!=0 && i<3);
        if (findUsername(loginPage)==0) {
            context.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get("json/yqState.json")));
            requestContext = context.request();
            status = 0;
            log.info("更新登录成功");
        }
        else {
            log.warn("更新登录异常");
        }
        loginPage.close();
        return status;
    }

    /**
     * 创建 us.codecraft.webmagic.Page
     *
     * @param htmlStr
     * @param currentUrl
     * @param level
     * @param pageNum
     * @return us.codecraft.webmagic.Page
     */
    private Page createPage(String htmlStr, String currentUrl,String level,String pageNum) {
        Page page = new Page();
        // 设置网页源码 + url
        page.setRawText(htmlStr);
        page.setUrl(new PlainText(currentUrl));
        page.isDownloadSuccess();

        // 设置request对象
        Request request = new Request(currentUrl);
        request.putExtra("level",level);
        if (pageNum != null) {
            request.putExtra("pageNum",pageNum);
        }
        page.setRequest(request);

        return page;
    }
}
