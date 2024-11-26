package com.example;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class MyTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // 过滤 JDK 系统类
        if (className == null || className.startsWith("java/") || className.startsWith("sun/")) {
            return null;
        }

        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className.replace("/", "."));

            // 遍历类的所有方法，插入堆分配监控逻辑
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                instrumentHeapAllocation(method);
            }

            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
    //                         ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    //     // 我们只对 ExampleClass 进行修改
    //     System.out.println("Transform");
    //     if (!className.equals("com/example/ExampleClass")) {
    //         System.out.println(className);
    //         return null;
    //     }

    //     try {
    //         // 使用 Javassist 修改字节码
    //         System.out.println("Get class");
    //         ClassPool classPool = ClassPool.getDefault();
    //         classPool.appendClassPath("target/classes");
    //         CtClass ctClass = classPool.get("com.example.ExampleClass");

    //         System.out.println(ctClass.getDeclaredMethods());
    //         System.out.println("Get classqqq");
    //         // 获取所有方法
    //         for (CtMethod method : ctClass.getDeclaredMethods()) {
    //             System.out.println("Method " + method.getName() + " started");
    //             // 在方法开始和结束时插入日志
    //             method.insertBefore("{ System.out.println(\"Method " + method.getName() + " started\"); }");
    //             method.insertAfter("{ System.out.println(\"Method " + method.getName() + " finished\"); }");
    //         }

    //         // 返回修改后的字节码
    //         return ctClass.toBytecode();
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }

    //     return null;
    // }

    private void instrumentHeapAllocation(CtMethod method) throws CannotCompileException {
        method.addLocalVariable("startMemory", CtClass.longType);
        method.insertBefore("startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();");
        method.insertAfter(
                "{ long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();" +
                "System.out.println(\"Heap allocated by " + method.getLongName() + ": \" + (endMemory - startMemory) + \" bytes.\"); }"
        );
    }
}