package com.cxytiandi.foxmock.agent.utils;


/**
 * @作者 尹吉欢
 * @个人微信 jihuan900
 * @微信公众号 猿天地
 * @GitHub https://github.com/yinjihuan
 * @作者介绍 http://cxytiandi.com/about
 * @时间 2022-05-14 22:09
 */
public class ClassUtils {


    public static Class<?> forNameByFormat(String className) {
        try {
            String formatClassName = formatClassName(className);
            // 1) 当前线程 TCCL（Agent 在调用处已切换为目标类加载器）
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                try {
                    return cl.loadClass(formatClassName);
                } catch (ClassNotFoundException ignore) {}
            }
            // 2) 目标类所在类的类加载器（若调用方传入的是业务对象的方法）
            try {
                Class<?> holder = ClassUtils.class;
                ClassLoader holderCl = holder.getClassLoader();
                if (holderCl != null) {
                    return holderCl.loadClass(formatClassName);
                }
            } catch (Throwable ignore) {}
            // 3) 系统类加载器
            try {
                ClassLoader sysCl = ClassLoader.getSystemClassLoader();
                if (sysCl != null) {
                    return sysCl.loadClass(formatClassName);
                }
            } catch (Throwable ignore) {}
            // 4) 最后使用 Class.forName
            return Class.forName(formatClassName);
        } catch (ClassNotFoundException e) {
            System.out.println("className {} not found");
        }
        return null;
    }

    public static String formatClassName(String className) {
        return className.replace('/', '.');
    }

}
