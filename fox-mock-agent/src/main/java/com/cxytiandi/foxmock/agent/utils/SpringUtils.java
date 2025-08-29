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
            System.out.println("...........................Mock-Server:");
            Class<?> defaultListableBeanFactoryClass = Class.forName("org.springframework.beans.factory.support.DefaultListableBeanFactory");
            Field serializableFactories = defaultListableBeanFactoryClass.getDeclaredField("serializableFactories");
            serializableFactories.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Reference<Object>> factories = (Map<String, Reference<Object>>) serializableFactories.get(null);
            Set<Map.Entry<String, Reference<Object>>> entries = factories.entrySet();
            Iterator<Map.Entry<String, Reference<Object>>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Reference<Object>> next = iterator.next();
                Reference<Object> value = next.getValue();
                Object defaultListableBeanFactory = value.get();
                if (defaultListableBeanFactory != null) {
                    beanFactory = defaultListableBeanFactory;
                    break;
                }
            }
            if (beanFactory == null) {
                // 方案二：通过 LiveBeansView 捕获到的 ApplicationContext 获取 BeanFactory
                try {
                    Class<?> liveBeansViewClass = Class.forName("org.springframework.context.support.LiveBeansView");
                    Field applicationContextsField = liveBeansViewClass.getDeclaredField("applicationContexts");
                    applicationContextsField.setAccessible(true);
                    Object ctxSetObj = applicationContextsField.get(null);
                    if (ctxSetObj instanceof Set) {
                        Set<?> ctxSet = (Set<?>) ctxSetObj;
                        for (Object ctx : ctxSet) {
                            if (ctx == null) continue;
                            try {
                                // 优先尝试 getAutowireCapableBeanFactory
                                try {
                                    beanFactory = ctx.getClass().getMethod("getAutowireCapableBeanFactory").invoke(ctx);
                                } catch (NoSuchMethodException ignore) {
                                    // 其次尝试 getBeanFactory（如 ConfigurableApplicationContext)
                                    beanFactory = ctx.getClass().getMethod("getBeanFactory").invoke(ctx);
                                }
                                if (beanFactory != null) break;
                            } catch (Throwable ignoreOne) {
                                // 尝试下一个上下文
                            }
                        }
                    }
                } catch (Throwable ignore) {
                    // LiveBeansView 方式不可用，忽略
                }
            }
        } catch (Throwable e) {
            System.out.println("...........................Mock-Server errr");
        }
        return beanFactory;
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
