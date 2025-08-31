package com.cxytiandi.foxmock.agent.utils;


import org.quartz.JobKey;
import org.quartz.impl.StdScheduler;

import java.lang.reflect.Method;

public class ScheduledUtils {
    private static Object beanFactory;
    public static StdScheduler getStdScheduler() {
        System.out.println("getStdScheduler");
        Object target = null;
        try{
            beanFactory = SpringUtils.getBeanFactory();
            System.out.println("beanFactory:\r\n " + beanFactory);
            try {
                Method getBeanByName = beanFactory.getClass().getMethod("getBean", String.class);
                System.out.println("getBeanByName:\r\n " + getBeanByName);
                target = getBeanByName.invoke(beanFactory, "quartzScheduler");
                System.out.println("target1111:\r\n " + target);
            } catch (Throwable nameGetEx) {
                // 名称获取失败，尝试按类型
                System.out.println("nameGetEx:\r\n " + nameGetEx);
                ClassLoader appCl = beanFactory.getClass().getClassLoader();
                System.out.println("appCl:\r\n ");
                Class<?> targetClass;
                try {
                    targetClass = appCl.loadClass("quartzScheduler");
                    System.out.println("targetClass:\r\n ");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                Method getBeanByType = beanFactory.getClass().getMethod("getBean", Class.class);
                System.out.println("getBeanByType:\r\n " + getBeanByType);
                target = getBeanByType.invoke(beanFactory, targetClass);
                System.out.println("last:\r\n " + target);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
