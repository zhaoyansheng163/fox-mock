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
                        //  invoke:com.example.TargetClass:targetMethod
                        String[] parts = command.split(":");
                        String className = parts[1];
                        String methodName = parts[2];
                        // 优先通过 Class 类型获取 Bean，避免依赖具体 Bean 名
                        Class<?> targetClass = Class.forName(className);
                        Method getBeanByType = beanFactory.getClass().getMethod("getBean", Class.class);
                        Object target = getBeanByType.invoke(beanFactory, targetClass);
                        Method method = targetClass.getMethod(methodName);
                        method.invoke(target);

                        // 解析和执行指令
                        String response = executeCommand(command);

                        // 将执行结果返回给客户端
                        out.println(response);
                    } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
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
