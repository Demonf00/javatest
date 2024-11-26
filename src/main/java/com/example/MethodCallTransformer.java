package com.example;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCallTransformer implements ClassFileTransformer {

    // 记录方法调用次数的全局哈希表
    private static final ConcurrentHashMap<String, Integer> methodCallCount = new ConcurrentHashMap<>();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // 过滤不感兴趣的类
        if (className == null || className.startsWith("jdk") || className.startsWith("java/") || className.startsWith("sun/")) {
            return null; // 不处理 JDK 或系统类
        }

        try {
            // System.out.println("1");
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className.replace("/", "."));

            // System.out.println(ctClass.getName());
            // 遍历类中所有的方法
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                // 为每个方法插入计数逻辑
                instrumentMethod(method);
                instrumentHeapAllocation(method);
            }

            // System.out.println("3");

            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void instrumentMethod(CtMethod method) throws CannotCompileException {
        String methodName = method.getLongName();

        // 在方法开头插入计数逻辑
        method.insertBefore(
                "{ com.example.MethodCallTransformer.incrementCallCount(\"" + methodName + "\"); }"
        );
        // System.out.println("4");

    }

    // 计数逻辑
    public static void incrementCallCount(String methodName) {
        methodCallCount.merge(methodName, 1, Integer::sum);
    }

    // 打印调用统计
    public static void printCallStats() {
        System.out.println("Method Call Statistics:");
        methodCallCount.forEach((method, count) -> {
            System.out.println(method + " was called " + count + " times.");
        });
    }

    private void instrumentHeapAllocation(CtMethod method) throws CannotCompileException {
        method.addLocalVariable("startMemory", CtClass.longType);
        method.insertBefore("startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();");
        method.insertAfter(
                "{ long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();" +
                "System.out.println(\"Heap allocated by " + method.getLongName() + ": \" + (endMemory - startMemory) + \" bytes.\"); }"
        );
    }
}