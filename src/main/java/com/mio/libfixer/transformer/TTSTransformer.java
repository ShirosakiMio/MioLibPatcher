package com.mio.libfixer.transformer;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class TTSTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className.equals("com/mojang/text2speech/Narrator")) {
            try {
                ClassPool pool = ClassPool.getDefault();
                CtClass clazz = pool.get("com.mojang.text2speech.Narrator");
                CtMethod method = clazz.getDeclaredMethod("getNarrator");
                try {
                    method.setBody("{ return new com.mojang.text2speech.NarratorDummy(); }");
                } catch (CannotCompileException e) {
                    method.setBody("{ return EMPTY; }");
                }
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