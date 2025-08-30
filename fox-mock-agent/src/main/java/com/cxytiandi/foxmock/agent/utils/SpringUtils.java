package com.cxytiandi.foxmock.agent.utils;


import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SpringUtils {
    private static Object beanFactory;
    public static Object getBeanFactory() {
        if (beanFactory != null)
            return beanFactory;
        try {
            System.out.println("...........................Mock-Server");
            System.out.println("...........................Mock-Server11");
            ClassLoader appCl = resolveAppClassLoader();
            System.out.println("[Agent] resolved App ClassLoader: " + (appCl == null ? "null" : appCl.getClass().getName()));
            Class<?> defaultListableBeanFactoryClass = Class.forName("org.springframework.beans.factory.support.DefaultListableBeanFactory", false, appCl);
            System.out.println("...........................Mock-Server1");
            Field serializableFactories = defaultListableBeanFactoryClass.getDeclaredField("serializableFactories");
            System.out.println("...........................Mock-Server2");
            serializableFactories.setAccessible(true);
            Object o = serializableFactories.get(null);
            System.out.println("...........................Mock-Server3");
            if (o instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) o;
                Set<? extends Map.Entry<?, ?>> entries = map.entrySet();
                Iterator<? extends Map.Entry<?, ?>> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    System.out.println("...........................Mock-Server4");
                    Map.Entry<?, ?> next = iterator.next();
                    Object value = next.getValue();
                    if (value instanceof Reference) {
                        Object bf = ((Reference<?>) value).get();
                        if (bf != null) {
                            beanFactory = bf;
                            break;
                        }
                    }
                }
            }
            // Fallback: LiveBeansView.applicationContexts
            if (beanFactory == null) {
                try {
                    Class<?> lbv = Class.forName("org.springframework.context.support.LiveBeansView", false, appCl);
                    Field ctxs = lbv.getDeclaredField("applicationContexts");
                    ctxs.setAccessible(true);
                    Object ctxSetObj = ctxs.get(null);
                    if (ctxSetObj instanceof Set) {
                        Set<?> ctxSet = (Set<?>) ctxSetObj;
                        for (Object ctx : ctxSet) {
                            if (ctx == null) continue;
                            try {
                                try {
                                    beanFactory = ctx.getClass().getMethod("getAutowireCapableBeanFactory").invoke(ctx);
                                } catch (NoSuchMethodException ignore) {
                                    beanFactory = ctx.getClass().getMethod("getBeanFactory").invoke(ctx);
                                }
                                if (beanFactory != null) break;
                            } catch (Throwable ignoreOne) {}
                        }
                    }
                } catch (Throwable ignore) {}
            }
        } catch (Exception|NoClassDefFoundError e) {
            System.out.println("...........................Mock-Server err: " + e.getMessage());
        }
        return beanFactory;
    }

    private static ClassLoader resolveAppClassLoader() {
        // 1) 当前线程 TCCL
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (isSpringVisible(cl)) return cl;
        // 2) 系统类加载器
        cl = ClassLoader.getSystemClassLoader();
        if (isSpringVisible(cl)) return cl;
        // 3) 遍历所有线程的 TCCL
        try {
            Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            for (Thread t : all.keySet()) {
                try {
                    ClassLoader tccl = t.getContextClassLoader();
                    if (isSpringVisible(tccl)) return tccl;
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        // 4) 遍历父类加载器链
        cl = Thread.currentThread().getContextClassLoader();
        while (cl != null) {
            if (isSpringVisible(cl)) return cl;
            cl = cl.getParent();
        }
        return Thread.currentThread().getContextClassLoader();
    }

    private static boolean isSpringVisible(ClassLoader cl) {
        if (cl == null) return false;
        try {
            Class.forName("org.springframework.context.ApplicationContext", false, cl);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static String getBeanName(String className) {
        String[] parts = className.split("\\.");
        String simpleClassName = parts[parts.length - 1];
        String firstLetter = simpleClassName.substring(0, 1).toLowerCase();
        String restOfName = simpleClassName.substring(1);
        String result = firstLetter + restOfName;
        return result;
    }
}
