package com.mio.libfixer.transformer;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

import java.io.ByteArrayInputStream;

public class SystemInfoTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "net.vulkanmod.vulkan.SystemInfo";
    }

    @Override
    public byte[] transform(byte[] buffer) {
        try {
            CtClass clazz;
            try {
                clazz = pool.get("net.vulkanmod.vulkan.SystemInfo");
            } catch (NotFoundException e) {
                clazz = pool.makeClass(new ByteArrayInputStream(buffer));
            }
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