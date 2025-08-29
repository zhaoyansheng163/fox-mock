package com.cxytiandi.foxmock.agent;

import com.cxytiandi.foxmock.agent.constant.FoxMockConstant;
import com.cxytiandi.foxmock.agent.model.FoxMockAgentArgs;
import com.cxytiandi.foxmock.agent.other.AgentSocketServer;
import com.cxytiandi.foxmock.agent.storage.StorageHelper;
import com.cxytiandi.foxmock.agent.transformer.MockClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock 入口
 *
 * @作者 尹吉欢
 * @个人微信 jihuan900
 * @微信公众号 猿天地
 * @GitHub https://github.com/yinjihuan
 * @作者介绍 http://cxytiandi.com/about
 * @时间 2022-04-18 22:17
 */
public class FoxMockAgent {

//    static {
//        System.setProperty("arthas.logback.configurationFile", "bak/foxmock-logback.xml.txt");
//    }
    

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("fox-mock.refresh.worker");
            t.setDaemon(true);
            return t;
        }
    });

    private static AtomicBoolean isFirstLoad = new AtomicBoolean(true);

    private static String foxMockAgentArgs;

    public static void premain(final String agentArgs, final Instrumentation inst) {
        try {
            foxMockAgentArgs = agentArgs;
            System.out.println(String.format("[FoxMockAgent.premain] begin, agentArgs: %s, Instrumentation:%s", agentArgs, inst));
            main(agentArgs, inst);

            if (isFirstLoad.get()) {
                isFirstLoad.compareAndSet(true, false);
                executor.scheduleAtFixedRate(() -> {
                    main(foxMockAgentArgs, inst);
                }, 10, 10, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            System.out.println("FoxMockAgent.premain exception  errr----");
        }
    }

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
       try {
           foxMockAgentArgs = agentArgs;
           System.out.println(String.format("[FoxMockAgent.agentmain] begin, agentArgs: %s, Instrumentation:%s", agentArgs, inst));

           main(agentArgs, inst);
           System.out.println("[Agent] Dynamically attached to JVM!");
           // 启动Socket服务器，监听9999端口
           AgentSocketServer.start(9999);

           if (isFirstLoad.get()) {
               isFirstLoad.compareAndSet(true, false);
               executor.scheduleAtFixedRate(() -> {
                   main(foxMockAgentArgs, inst);
               }, 10, 10, TimeUnit.SECONDS);
           }
       } catch (Exception e) {
           System.out.println("FoxMockAgent.agentmain exception");
       }
    }

    private synchronized static void main(final String agentArgs, final Instrumentation inst) {
        System.out.println("start load data");
        boolean success = StorageHelper.loadAllData(new FoxMockAgentArgs(agentArgs));
        if (!success) {
            System.out.println("load data fail");
            return;
        }

        if (isFirstLoad.get()) {
            MockClassFileTransformer mockClassFileTransformer = new MockClassFileTransformer();
            inst.addTransformer(mockClassFileTransformer, true);
        }

        preLoadClass();

        Set<String> mockClassNames = StorageHelper.getMockClassNames();
        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        for (Class clz : allLoadedClasses) {
            if (mockClassNames.contains(clz.getName())) {
                try {
                    System.out.println("retransformClass {}");
                    inst.retransformClasses(clz);
                } catch (UnmodifiableClassException e) {
                    System.out.println("retransformClasses exception");
                    throw new RuntimeException("retransformClasses exception", e);
                }
            }
        }

        System.out.println("foxMock agent run completely");
    }

    private static void preLoadClass() {
        try {
            Class.forName(FoxMockConstant.IBATIS_BASE_EXECUTOR);
        } catch (ClassNotFoundException e) {
            System.out.println("{} ClassNotFoundException");
        }

        try {
            Class.forName(FoxMockConstant.IBATIS_CACHING_EXECUTOR);
        } catch (ClassNotFoundException e) {
            System.out.println("{} ClassNotFoundException");
        }
    }
}
