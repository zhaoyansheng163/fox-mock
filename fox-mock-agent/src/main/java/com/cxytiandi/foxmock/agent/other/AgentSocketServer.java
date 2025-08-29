package com.cxytiandi.foxmock.agent.other;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AgentSocketServer {
    private static Object beanFactory;
    public static void start(int port) {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[Agent] Socket server started on port " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        // 读取客户端发送的指令
                        String command = in.readLine();
                        System.out.println("[Agent] Received command: " + command);
                        Object beanFactory = getBeanFactory();
                        if (beanFactory == null) {
                            out.println("ERROR: Spring BeanFactory not found in target JVM.");
                            continue;
                        }
                        System.out.println("111111:{}");
                        //  invoke:com.example.TargetClass:targetMethod
                        String[] parts = command.split(":");
                        String className = parts[1];
                        String methodName = parts[2];
                        // 通过 Bean 名称获取，避免代理线程类加载器无法加载应用类导致的 CNF 异常
                        String beanName = getBeanName(className);
                        System.out.println("beanName:{}  className:{}  methodName:{}");
                        Object target;
                        try {
                            Method getBeanByName = beanFactory.getClass().getMethod("getBean", String.class);
                            target = getBeanByName.invoke(beanFactory, beanName);
                        } catch (Throwable nameGetEx) {
                            // 名称获取失败，尝试按类型
                            ClassLoader appCl = beanFactory.getClass().getClassLoader();
                            Class<?> targetClass;
                            try {
                                targetClass = appCl.loadClass(className);
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                            Method getBeanByType = beanFactory.getClass().getMethod("getBean", Class.class);
                            target = getBeanByType.invoke(beanFactory, targetClass);
                        }
                        System.out.println("ssssssssssstarget:{}");
                        Method method = target.getClass().getMethod(methodName);
                        System.out.println("sssssssssssmethod:{}");
                        // 在调用前切换 TCCL，确保被调用方法内部使用到的类加载（例如 JsonUtils -> ClassUtils）能加载到业务类
                        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
                        try {
                            ClassLoader targetCl = target.getClass().getClassLoader();
                            if (targetCl != null) {
                                Thread.currentThread().setContextClassLoader(targetCl);
                            }
                            method.invoke(target);
                        } finally {
                            Thread.currentThread().setContextClassLoader(oldCl);
                        }

                        // 解析和执行指令
                        String response = executeCommand(command);

                        // 将执行结果返回给客户端
                        out.println(response);
                    } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // 继承/设置为应用类加载器，避免反射加载到 Agent 自身类加载器
        try {
            ClassLoader appCl = Thread.currentThread().getContextClassLoader();
            if (appCl == null) appCl = ClassLoader.getSystemClassLoader();
            serverThread.setContextClassLoader(appCl);
        } catch (Throwable ignore) {}
        serverThread.setDaemon(true); // 设置为守护线程，主JVM退出时它也会退出
        serverThread.start();
    }

    private static String executeCommand(String command) {
        // 解析指令，例如格式 "invoke:ClassName:MethodName"
        String[] parts = command.split(":");
        if (parts[0].equals("invoke") && parts.length == 3) {
            // 调用目标方法...
            return "Successfully invoked " + parts[1] + "." + parts[2];
        }
        return "Unknown command: " + command;
    }

    public static Object getBeanFactory() {
        if (beanFactory != null)
            return beanFactory;
        try {
            System.out.println("...........................Mock-Server");
            System.out.println("...........................Mock-Server11");
            ClassLoader appCl = resolveAppClassLoader();
            System.out.println("[Agent] resolved App ClassLoader: {}");
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
            System.out.println("...........................Mock-Server err");
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
