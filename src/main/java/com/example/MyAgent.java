package com.example;

import java.lang.instrument.Instrumentation;

public class MyAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        // 在 JVM 启动时加载代理
        System.out.println("Agent is running");
        inst.addTransformer(new MyTransformer());
    }
}