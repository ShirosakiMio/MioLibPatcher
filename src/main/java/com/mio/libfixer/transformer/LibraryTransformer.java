package com.mio.libfixer.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class LibraryTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className.equals("org/lwjgl/system/Library")) {
            try {
                ClassPool pool = ClassPool.getDefault();
                CtClass clazz = pool.get("org.lwjgl.system.Library");
                CtMethod method = clazz.getDeclaredMethod("checkHash");
                method.setBody("{}");
                byte[] bytes = clazz.toBytecode();
                clazz.detach();
                return bytes;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return classfileBuffer;
    }
}