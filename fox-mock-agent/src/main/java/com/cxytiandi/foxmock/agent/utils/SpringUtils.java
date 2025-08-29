package org.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SpringUtils {
    private static DefaultListableBeanFactory beanFactory;
    public static DefaultListableBeanFactory getBeanFactory() {
        if (beanFactory != null)
            return beanFactory;
        try {
            System.out.println("...........................Mock-Server:");
            Class<DefaultListableBeanFactory> defaultListableBeanFactoryClass = DefaultListableBeanFactory.class;
            Field serializableFactories = defaultListableBeanFactoryClass.getDeclaredField("serializableFactories");
            serializableFactories.setAccessible(true);
            Map<String, Reference<DefaultListableBeanFactory>> o = (Map<String, Reference<DefaultListableBeanFactory>>)serializableFactories.get((Object)null);
            Set<Map.Entry<String, Reference<DefaultListableBeanFactory>>> entries = o.entrySet();
            Iterator<Map.Entry<String, Reference<DefaultListableBeanFactory>>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Reference<DefaultListableBeanFactory>> next = iterator.next();
                Reference<DefaultListableBeanFactory> value = next.getValue();
                DefaultListableBeanFactory defaultListableBeanFactory = value.get();
                assert defaultListableBeanFactory != null;
                beanFactory = defaultListableBeanFactory;
            }
        } catch (Exception|NoClassDefFoundError e) {
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
