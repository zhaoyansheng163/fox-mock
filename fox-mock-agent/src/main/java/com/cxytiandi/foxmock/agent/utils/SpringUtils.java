//package com.cxytiandi.foxmock.agent.utils;
//
//import org.springframework.beans.factory.support.DefaultListableBeanFactory;
//
//import java.lang.ref.Reference;
//import java.lang.reflect.Field;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//
//public class SpringUtils {
//    private static Object beanFactory;
//    public static Object getBeanFactory() {
//        if (beanFactory != null)
//            return beanFactory;
//        try {
//            System.out.println("...........................Mock-Server");
//            System.out.println("...........................Mock-Server11");
//            ClassLoader appCl = Thread.currentThread().getContextClassLoader();
//            System.out.println("...........................Mock-appCl");
//            if (appCl == null) appCl = ClassLoader.getSystemClassLoader();
//            System.out.println("...........................Mock-appCl1");
//            Class<?> defaultListableBeanFactoryClass = Class.forName("org.springframework.beans.factory.support.DefaultListableBeanFactory", false, appCl);
//            //Class<DefaultListableBeanFactory> defaultListableBeanFactoryClass = DefaultListableBeanFactory.class;
//            System.out.println("...........................Mock-Server1");
//            Field serializableFactories = defaultListableBeanFactoryClass.getDeclaredField("serializableFactories");
//            System.out.println("...........................Mock-Server2");
//            serializableFactories.setAccessible(true);
//            Map<String, Reference<DefaultListableBeanFactory>> o = (Map<String, Reference<DefaultListableBeanFactory>>)serializableFactories.get((Object)null);
//            System.out.println("...........................Mock-Server3");
//            Set<Map.Entry<String, Reference<DefaultListableBeanFactory>>> entries = o.entrySet();
//            Iterator<Map.Entry<String, Reference<DefaultListableBeanFactory>>> iterator = entries.iterator();
//            while (iterator.hasNext()) {
//                System.out.println("...........................Mock-Server4");
//                Map.Entry<String, Reference<DefaultListableBeanFactory>> next = iterator.next();
//                Reference<DefaultListableBeanFactory> value = next.getValue();
//                DefaultListableBeanFactory defaultListableBeanFactory = value.get();
//                assert defaultListableBeanFactory != null;
//                beanFactory = defaultListableBeanFactory;
//            }
//        } catch (Exception|NoClassDefFoundError e) {
//            System.out.println("...........................Mock-Server err");
//        }
//        return beanFactory;
//    }
//
//    public static String getBeanName(String className) {
//        String[] parts = className.split("\\.");
//        String simpleClassName = parts[parts.length - 1];
//        String firstLetter = simpleClassName.substring(0, 1).toLowerCase();
//        String restOfName = simpleClassName.substring(1);
//        String result = firstLetter + restOfName;
//        return result;
//    }
//}
