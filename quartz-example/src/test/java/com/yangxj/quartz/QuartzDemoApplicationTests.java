//package com.yangxj.quartz;
//
//import com.yangxj.quartz.job.MyJob;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.quartz.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class QuartzDemoApplicationTests {
//    @Autowired
//    Scheduler scheduler;
//
//    @Test
//    public void contextLoads() throws SchedulerException {
//        JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withDescription("with jobA in groupA job init....").withIdentity("jobA", "groupA").build();
//        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule("0 0/2 * * * ?")).build();
//        scheduler.scheduleJob(jobDetail,cronTrigger);
//        scheduler.start();
//    }
//
//
//}
