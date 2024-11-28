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
        
        if (className == null || className.startsWith("java/") || className.startsWith("sun/")) {
            return null;
        }

        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className.replace("/", "."));

            
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                instrumentHeapAllocation(method);
            }

            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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