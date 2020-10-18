package com.atguigu.gmall.scheduling.job;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

@Component
public class MyJobHandler {

    @XxlJob(value = "myJobHandler")
    public ReturnT<String> test(String arg) {
        System.out.println("这个是xxl-job的第一个定时任务" + System.currentTimeMillis());
        XxlJobLogger.log("this is my first jobHandler!!");
        return ReturnT.SUCCESS;
    }
}
