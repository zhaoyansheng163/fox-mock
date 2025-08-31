package com.cxytiandi.foxmock.agent.utils;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScheduledUtils {
    private static Object beanFactory;
    public static Object getStdScheduler() {
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
        return target;
    }
//    public static void triggerQuartzJob(String className){
//        System.out.println("trigger:" + className);
//        String[] parts=className.split("\\.");
//        String simpleClassName = parts[parts.length-1];
//        String jobName=simpleClassName+"Detail";
//        try{
//            Object stdScheduler = getStdScheduler();
//            if(null != stdScheduler){
//                JobKey jobKey = new JobKey(jobName);
//                stdScheduler.triggerJob(jobKey);
//                stdScheduler.start();
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static List<String> getAllJobsFromScheduler(Object stdScheduler) {
        List<String> jobList = new ArrayList<>();

        if (stdScheduler == null) {
            System.err.println("Scheduler is null, cannot get jobs");
            return jobList;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 使用调度器的类加载器
            ClassLoader schedulerClassLoader = stdScheduler.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(schedulerClassLoader);

            // 加载必要的 Quartz 类
            Class<?> schedulerClass = schedulerClassLoader.loadClass("org.quartz.Scheduler");
            Class<?> jobKeyClass = schedulerClassLoader.loadClass("org.quartz.JobKey");
            Class<?> groupMatcherClass = schedulerClassLoader.loadClass("org.quartz.impl.matchers.GroupMatcher");

            // 获取 getJobGroupNames 方法
            Method getJobGroupNamesMethod = schedulerClass.getMethod("getJobGroupNames");

            // 调用 getJobGroupNames 方法
            @SuppressWarnings("unchecked")
            java.util.LinkedList<String> groupNames = (java.util.LinkedList)getJobGroupNamesMethod.invoke(stdScheduler);

            // 获取 jobGroupMatches 方法 (用于创建 GroupMatcher)
            Method jobGroupMatchesMethod = groupMatcherClass.getMethod("jobGroupEquals", String.class);

            // 获取 getJobKeys 方法
            Method getJobKeysMethod = schedulerClass.getMethod("getJobKeys", groupMatcherClass);

            // 遍历所有组
            for (String groupName : groupNames) {
                // 创建 GroupMatcher
                Object groupMatcher = jobGroupMatchesMethod.invoke(null, groupName);

                // 获取该组中的所有 JobKey
                @SuppressWarnings("unchecked")
                Set<Object> jobKeys = (Set<Object>) getJobKeysMethod.invoke(stdScheduler, groupMatcher);

                // 获取 JobKey 的 getName 和 getGroup 方法
                Method getNameMethod = jobKeyClass.getMethod("getName");
                Method getGroupMethod = jobKeyClass.getMethod("getGroup");

                // 遍历该组中的所有 Job
                for (Object jobKey : jobKeys) {
                    String jobName = (String) getNameMethod.invoke(jobKey);
                    String jobGroup = (String) getGroupMethod.invoke(jobKey);

                    // 添加到结果列表
                    jobList.add(jobGroup + "." + jobName);

                    // 如果需要获取更多 Job 详情，可以继续使用反射获取 JobDetail
                    // Method getJobDetailMethod = schedulerClass.getMethod("getJobDetail", jobKeyClass);
                    // Object jobDetail = getJobDetailMethod.invoke(stdScheduler, jobKey);
                    // 然后从 jobDetail 中获取更多信息
                }
            }

            return jobList;
        } catch (Exception e) {
            System.err.println("Error getting jobs from scheduler: " + e.getMessage());
            e.printStackTrace();
            return jobList;
        } finally {
            // 恢复原始类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
