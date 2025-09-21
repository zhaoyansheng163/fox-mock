package com.yangxj.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;

/**
 * @author yangxj
 * @date 2019/5/16-21:32
 */
@Slf4j
@DisallowConcurrentExecution
//@PersistJobDataAfterExecution

public class MyJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        log.info(" >>>>myJob 执行描述{}>>>>>时间：{}>",jobExecutionContext.getJobDetail().getDescription(),LocalDateTime.now());
    }
}
