package com.mio.libfixer.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

public class SystemInfoTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className.equals("net/vulkanmod/vulkan/SystemInfo")) {
            try {
                ClassPool pool = ClassPool.getDefault();
                CtClass clazz = pool.get("net.vulkanmod.vulkan.SystemInfo");
                CtConstructor constructor = clazz.getClassInitializer();
                constructor.setBody("{cpuInfo = \"\";}");
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