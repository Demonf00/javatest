package com.example;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.MethodInfo;

//public class MyTransformer implements ClassFileTransformer {
//    @Override
//    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
//                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
//
//        if (className == null || className.startsWith("java/") || className.startsWith("sun/")) {
//            return null;
//        }
//
//        try {
//            ClassPool classPool = ClassPool.getDefault();
//            CtClass ctClass = classPool.get(className.replace("/", "."));
//
//
//            for (CtMethod method : ctClass.getDeclaredMethods()) {
//                instrumentHeapAllocation(method);
//            }
//
//            return ctClass.toBytecode();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//
//    private void instrumentHeapAllocation(CtMethod method) throws CannotCompileException {
//        method.addLocalVariable("startMemory", CtClass.longType);
//        method.insertBefore("startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();");
//        method.insertAfter(
//                "{ long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();" +
//                "System.out.println(\"Heap allocated by " + method.getLongName() + ": \" + (endMemory - startMemory) + \" bytes.\"); }"
//        );
//    }
//}

import javassist.*;

public class MyTransformer implements java.lang.instrument.ClassFileTransformer {
    private static final String COUNTER_FIELD = "__methodCounter";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            java.security.ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || className.startsWith("java/") || className.startsWith("sun/")) {
            return null;
        }

        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className.replace("/", "."));

            // 检查字段是否存在
            try {
                ctClass.getField(COUNTER_FIELD);
            } catch (NotFoundException e) {
                // 如果字段不存在，添加一个静态字段
                CtField counterField = new CtField(CtClass.intType, COUNTER_FIELD, ctClass);
                counterField.setModifiers(Modifier.STATIC);
                ctClass.addField(counterField, "0");
            }

            // 为每个方法插入代码
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                instrumentMethod(method, ctClass);
            }

            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void instrumentMethod(CtMethod method, CtClass declaringClass) throws CannotCompileException {
        method.addLocalVariable("startMemory", CtClass.longType);
        method.addLocalVariable("endMemory", CtClass.longType);
        method.addLocalVariable("startTime", CtClass.longType);

        // 插入方法开始部分的代码
        method.insertBefore(
                "startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();" +
                        "startTime = System.nanoTime();" +
                        declaringClass.getName() + "." + COUNTER_FIELD + "++;" +
                        "System.out.println(\"Entering method: " + method.getLongName() + "\");" +
                        "System.out.println(\"Call count: \" + " + declaringClass.getName() + "." + COUNTER_FIELD + ");" +
                        "System.out.println(\"Call stack: \" + java.util.Arrays.toString(Thread.currentThread().getStackTrace()));"
        );

        // 插入方法结束部分的代码
        method.insertAfter(
                "endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();" +
                        "long duration = System.nanoTime() - startTime;" +
                        "System.out.println(\"Exiting method: " + method.getLongName() + "\");" +
                        "System.out.println(\"Execution time: \" + duration + \" ns\");" +
                        "System.out.println(\"Memory allocated: \" + (endMemory - startMemory) + \" bytes\");"
        );
    }
}