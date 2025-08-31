package com.cxytiandi.foxmock.agent.other;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

import com.cxytiandi.foxmock.agent.utils.ScheduledUtils;
import org.apache.commons.lang3.StringUtils;

import static com.cxytiandi.foxmock.agent.utils.SpringUtils.getBeanFactory;
import static com.cxytiandi.foxmock.agent.utils.SpringUtils.getBeanName;

public class AgentSocketServer {
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
                        //System.out.println("111111:" + beanFactory);
                        //  invoke:com.example.TargetClass:targetMethod
                        String[] parts = command.split(":");
                        String type = parts[0];
                        String className = parts[1];
                        String methodName = parts[2];
                        if((type != null) && type.equalsIgnoreCase("quartz") ){
                            ScheduledUtils.triggerQuartzJob(className);
                            return;
                        }
                        // 通过 Bean 名称获取，避免代理线程类加载器无法加载应用类导致的 CNF 异常
                        String beanName = getBeanName(className);
                        System.out.println("beanName:" + beanName + "  className:" + className + "  methodName:" + methodName);
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
                        System.out.println("ssssssssssstarget:" + target);
                        Method method = target.getClass().getMethod(methodName);
                        System.out.println("sssssssssssmethod:" + method);
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


}
