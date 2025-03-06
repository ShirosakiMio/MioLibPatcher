package com.mio.libpatcher.transformer;

import javassist.ClassPool;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

public interface BaseTransformer extends ClassFileTransformer {
    ClassPool pool = ClassPool.getDefault();

    String getTargetClassName();

    default List<String> getTargetClassNames() {
        return null;
    }

    byte[] transform(byte[] buffer) throws Throwable;

    @Override
    default byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (getTargetClassName().replace(".", "/").equals(className)) {
                return transform(classfileBuffer);
            }
            if (getTargetClassNames() != null) {
                for (String name : getTargetClassNames()) {
                    if (name.replace(".", "/").equals(className)) {
                        return transform(classfileBuffer);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return classfileBuffer;
    }

//    default byte[] get
}
