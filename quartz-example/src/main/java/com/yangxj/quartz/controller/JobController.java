package com.yangxj.quartz.controller;

import com.yangxj.quartz.job.MyJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;


/**
 * @author yangxj
 * @date 2019/5/16-21:45
 * 注意：一个任务JOB可以添加多个Trigger 但是一个Trigger只能绑定一个JOB 这点需要注意
 */
@Slf4j
@RestController
public class JobController {
    @Autowired
    Scheduler scheduler;
    @RequestMapping("pause")
    public void pauseJob(String jobName,String jobGroup) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        log.info(">>暂停作业 {}>>",scheduler.getJobDetail(jobKey).getDescription());
        scheduler.pauseJob(jobKey);
    }
    @RequestMapping("resume")
    public void resumeJob(String jobName,String jobGroup) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        log.info(">>重启作业 {}>>",scheduler.getJobDetail(jobKey).getDescription());
        scheduler.resumeJob(jobKey);
    }
    @RequestMapping("add")
    public void addJob(String jobName,String jobGroup,String jobDescription) throws SchedulerException, ParseException {
        log.info(">>开启一个作业,jobName:{},jobGroup: {},jobDescription: {}>>",jobName,jobGroup,jobDescription);
        HashMap<String,String> execParams = new HashMap<>();
        execParams.put("exec_user","yangxj");
        JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withDescription(jobDescription).withIdentity(jobName, jobGroup).setJobData(new JobDataMap(execParams)).build();
        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule("0/3 * * * * ?")).endAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-05-16 23:49:00")).build();
        scheduler.scheduleJob(jobDetail, cronTrigger);
    }
    @RequestMapping("remove")
    public String removeJob(String jobName,String jobGroup) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        log.info(">>移除作业 {}>>",scheduler.getJobDetail(jobKey).getDescription());
        boolean b = scheduler.deleteJob(jobKey);
        return b==true?"移除成功": "移除失败";
    }
}
