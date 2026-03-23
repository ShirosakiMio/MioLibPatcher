package com.mio.libpatcher.transformer;

import com.mio.libpatcher.util.LogUtil;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
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

    void transform(CtClass clazz) throws Throwable;

    @Override
    default byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            boolean isTargetClass = getTargetClassName().replace(".", "/").equals(className);
            if (getTargetClassNames() != null) {
                for (String name : getTargetClassNames()) {
                    if (name.replace(".", "/").equals(className)) {
                        isTargetClass = true;
                        break;
                    }
                }
            }
            if (isTargetClass) {
                LogUtil.info("Patch target class: " + className);
                CtClass clazz = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                transform(clazz);
                byte[] bytes = clazz.toBytecode();
                clazz.detach();
                return bytes;
            }
        } catch (Throwable e) {
            LogUtil.error(e.toString());
        }
        return classfileBuffer;
    }

//    default byte[] get
}
