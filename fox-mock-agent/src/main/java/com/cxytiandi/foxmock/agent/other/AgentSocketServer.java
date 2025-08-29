package com.cxytiandi.foxmock.agent.other;

import com.cxytiandi.foxmock.agent.utils.SpringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

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
                        Object beanFactory = SpringUtils.getBeanFactory();
                        if (beanFactory == null) {
                            out.println("ERROR: Spring BeanFactory not found in target JVM.");
                            continue;
                        }
                        //  invoke:com.example.TargetClass:targetMethod
                        String[] parts = command.split(":");
                        String className = parts[1];
                        String methodName = parts[2];
                        // 通过 Bean 名称获取，避免代理线程类加载器无法加载应用类导致的 CNF 异常
                        String beanName = SpringUtils.getBeanName(className);
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
                        Method method = target.getClass().getMethod(methodName);
                        method.invoke(target);

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
