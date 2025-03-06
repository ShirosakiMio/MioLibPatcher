package com.mio.libpatcher.transformer;

import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;

public class RandomPatchesTransformer implements BaseTransformer {

    @Override
    public String getTargetClassName() {
        return "com.therandomlabs.randompatches.client.WindowIconHandler";
    }

    @Override
    public byte[] transform(byte[] buffer) throws Throwable {
        CtClass clazz = pool.makeClass(new ByteArrayInputStream(buffer));
        CtMethod method = clazz.getDeclaredMethod("setWindowIcon", new CtClass[]{});
        method.setBody("{}");
        byte[] bytes = clazz.toBytecode();
        clazz.detach();
        return bytes;
    }
}