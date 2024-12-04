package com.mio.libfixer.transformer;

import javassist.CtClass;
import javassist.CtConstructor;

public class SystemInfoTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "net.vulkanmod.vulkan.SystemInfo";
    }

    @Override
    public byte[] transform(byte[] buffer) {
        try {
            CtClass clazz = pool.get("net.vulkanmod.vulkan.SystemInfo");
            CtConstructor constructor = clazz.getClassInitializer();
            constructor.setBody("{cpuInfo = \"\";}");
            byte[] bytes = clazz.toBytecode();
            clazz.detach();
            return bytes;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return buffer;
    }
}