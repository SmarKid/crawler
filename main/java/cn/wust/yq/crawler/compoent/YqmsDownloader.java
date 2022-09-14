package cn.wust.yq.crawler.compoent;

import com.microsoft.playwright.*;
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
import java.nio.file.Paths;
import java.util.Random;

@Component
public class YqmsDownloader implements Downloader {

    @Value("${yqms.username}")
    private String yqmsUsername;

    @Value("${yqms.password}")
    private String yqmsPassword;


    private static final Logger log = LoggerFactory.getLogger(Downloader.class);

    Playwright playwright = Playwright.create();
    Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false).setSlowMo(50).setTimeout(60 * 1000)
    );
    BrowserContext context;

    @Override
    public Page download(Request request, Task task) {
        String level = request.getExtra("level");
        Page page = null;
        switch (level) {
            case "start" :
                page = startDownload();
                break;
            case "downloadListPage" :
                page = downloadListPage(request);
                break;
            case "downloadDetails" :
                page = downloadDetails(request);
                break;
        }
        return page;
    }

    /**
     * 下载详情页数据
     * @param request
     * @return
     */
    private Page downloadDetails(Request request) {
        log.info("yqms开始下载详情页，url：" + request.getUrl());
        com.microsoft.playwright.Page page = context.newPage();
        page.navigate(request.getUrl());
        Random random = new Random();
        page.waitForTimeout((10 + random.nextInt(20)) * 1000);
        Page wpage = createPage(page.content(), request.getUrl()
                , "processDetails", "");
        page.close();
        return wpage;
    }

    private Page downloadListPage(Request request) {
        String pageNum = request.getExtra("pageNum");
        log.info("开始下载list page " + pageNum);
        com.microsoft.playwright.Page page = context.pages().get(0);
        if (!page.locator(".number.active").textContent().equals(pageNum)) {
            page.locator(".number"
                    , new com.microsoft.playwright.Page.LocatorOptions()
                            .setHasText(pageNum)).click();
        }
        String htmlStr = page.waitForSelector(".clearfix").innerHTML();
        Page wPage = createPage(htmlStr, "", "processList", pageNum);
        return wPage;
    }

    /**
     * 处理登录爬取页面
     * 1、登录
     * 2、打开列表页
     * 3、返回处理信号到Processor
     */
    private Page startDownload() {
        if (loadContext() != 0) {
            return null;
        }
        Page page = createPage("", "", "startDownload", "");
        return page;
    }

    /**
     * 1、点击一个项目返回其渲染后的页面
     */
    private void downloadItem() {

    }

    /**
     * 1、查找state文件，不存在跳转2，存在跳转3
     * 2、重新登录 return
     * 3、检查state失效否，失效跳转2，没有失效return
     * @return
     */
    private int loadContext() {
        int status = 1;
        File file = new File("json/yqmsState.json");
        if (!file.exists()){
            // 不存在，登录无效
            if(updateLoginContext() != 0) {
                status = 1;
            }else{
                status = 0;
            }
        }
        else {
            // 存在，加载context并检查
            context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setStorageStatePath(file.toPath()));
            context.setDefaultNavigationTimeout(120 * 1000);
            if (0 == vertifyLogin()) {
                log.info("登录文件在有效期内");
                status = 0;
            }
            else {
                log.info("登录过期");
                status = updateLoginContext();
            }
        }
        return status;

    }

    private int vertifyLogin() {
        int status = 1;
        String url = "https://yqms.istarshine.com/v4/subject";
        context.pages().forEach(page -> page.close());
        com.microsoft.playwright.Page page = context.newPage();
        page.setDefaultNavigationTimeout(120 * 1000);
        int tryTimes = 2;
        do {
            page.navigate(url);
            status = findUsername(page);
        } while (status == 1 && tryTimes-- > 0);
        return status;
    }

    /**
     * 1、打开登录页。
     * 2、输入用户名密码，点击登录。
     * 3、检查登录状态
     * @return
     */
    private int updateLoginContext() {
        int status = 1;
        if (context == null) {
            context = browser.newContext();
        }
        context.pages().forEach(com.microsoft.playwright.Page::close);
        com.microsoft.playwright.Page loginPage = context.newPage();
        int i = 0;
        do {
            log.info("尝试第"+ (i+1) + "次登录");
            loginPage.navigate("https://uc.istarshine.com/usercenter/login.html?service=https%3A%2F%2Fyqms.istarshine.com%2Fv4%2Floginloading");
            loginPage.fill(".login-content .uname", yqmsUsername,
                    new com.microsoft.playwright.Page.FillOptions().setTimeout(2 * 60 * 1000));
            loginPage.fill(".login-content .pwd", yqmsPassword);
            loginPage.click("#loginBtn");
            ++i;
        } while (findUsername(loginPage)!=0 && i<3);
        if (findUsername(loginPage)==0) {
            context.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get("json/yqmsState.json")));
            status = 0;
            log.info("更新登录成功");
        }
        else {
            log.warn("更新登录异常");
        }
        return status;
    }

    private int findUsername(com.microsoft.playwright.Page page) {
        String phone = null;
        try {
            phone = page.locator(".phone").textContent();
        } catch (Exception e) {
            log.error(e.toString());
        }
        int status = 1;
        if (phone != null && phone.contains(yqmsUsername)) {
            status = 0;
        }
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

    @Override
    public void setThread(int i) {

    }
}
