package cn.wust.yq.crawler.compoent;

import cn.wust.yq.crawler.dao.YQInfoDao;
import cn.wust.yq.crawler.pojo.YQInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

@Component
public class YQPipeline implements Pipeline {

    @Autowired
    private YQInfoDao yqInfoDao;

    private static final Logger log = LoggerFactory.getLogger(YQPipeline.class);

    @Override
    public void process(ResultItems resultItems, Task task) {
        YQInfo info = resultItems.get("yqInfo");
        if (info != null){
            try{
                yqInfoDao.save(info);
                log.info("下载成功，hbaseid:"+info.getHbaseid());
            }
            catch (Exception e) {
                log.info("下载失败，hbaseid:"+info.getHbaseid());
                log.error(e.toString());
            }
        }
    }
}
