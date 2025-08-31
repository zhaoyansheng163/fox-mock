package com.cxytiandi.foxmock.agent.utils;


import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdScheduler;

import java.lang.reflect.Method;

public class ScheduledUtils {
    private static Object beanFactory;
    public static StdScheduler getStdScheduler() {
        System.out.println("getStdScheduler");
        Object target = null;
        try{
            beanFactory = SpringUtils.getBeanFactory();
            try {
                Method getBeanByName = beanFactory.getClass().getMethod("getBean", String.class);
                target = getBeanByName.invoke(beanFactory, "schedulerFactory");
            } catch (Throwable nameGetEx) {
                // 名称获取失败，尝试按类型
                ClassLoader appCl = beanFactory.getClass().getClassLoader();
                Class<?> targetClass;
                try {
                    targetClass = appCl.loadClass("schedulerFactory");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                Method getBeanByType = beanFactory.getClass().getMethod("getBean", Class.class);
                target = getBeanByType.invoke(beanFactory, targetClass);
            }


        }catch (Exception e){
            System.out.println("getStdScheduler errr");
        }
        return (StdScheduler)target;
    }
    public static void triggerQuartzJob(String className){
        System.out.println("trigger:" + className);
        String[] parts=className.split("\\.");
        String simpleClassName = parts[parts.length-1];
        String jobName=simpleClassName+"Detail";
        try{
            StdScheduler stdScheduler = getStdScheduler();
            if(null != stdScheduler){
                JobKey jobKey = new JobKey(jobName);
                stdScheduler.triggerJob(jobKey);
                stdScheduler.start();
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
