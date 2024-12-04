package com.mio.libfixer.transformer;

import javassist.ClassPool;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public interface BaseTransformer extends ClassFileTransformer {
    ClassPool pool = ClassPool.getDefault();

    String getTargetClassName();

    byte[] transform(byte[] buffer);

    @Override
    default byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (getTargetClassName().replace(".", "/").equals(className)) {
            return transform(classfileBuffer);
        }
        return classfileBuffer;
    }
}
