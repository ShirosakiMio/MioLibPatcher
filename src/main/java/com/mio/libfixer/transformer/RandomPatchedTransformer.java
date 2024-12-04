package com.mio.libfixer.transformer;

import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;

public class RandomPatchedTransformer implements BaseTransformer {

    @Override
    public String getTargetClassName() {
        return "com.therandomlabs.randompatches.client.WindowIconHandler";
    }

    @Override
    public byte[] transform(byte[] buffer) {
        try {
            CtClass clazz = pool.makeClass(new ByteArrayInputStream(buffer));
            CtMethod method = clazz.getDeclaredMethod("setWindowIcon");
            method.setBody("{}");
            byte[] bytes = clazz.toBytecode();
            clazz.detach();
            return bytes;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return buffer;
    }
}