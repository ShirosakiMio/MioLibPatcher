package com.mio.libpatcher.transformer.oshi;

import com.mio.libpatcher.transformer.BaseTransformer;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.ByteArrayInputStream;

public class ProcessorIdentifierTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "oshi.hardware.CentralProcessor$ProcessorIdentifier";
    }

    @Override
    public byte[] transform(byte[] buffer) throws Throwable {
        CtClass clazz;
        try {
            clazz = pool.get(getTargetClassName());
        } catch (NotFoundException e) {
            clazz = pool.makeClass(new ByteArrayInputStream(buffer));
        }
        CtMethod method = clazz.getDeclaredMethod("getName");
        method.setBody("{return System.getProperty(\"cpu.name\",\"\");}");
        byte[] bytes = clazz.toBytecode();
        clazz.detach();
        return bytes;
    }
}