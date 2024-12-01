package com.mio.libfixer.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

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
                boolean isDummy = true;
                try {
                    CtMethod test = clazz.getDeclaredMethod("setJNAPath");
                } catch (NotFoundException e) {
                    isDummy = false;
                }
                if (isDummy) {
                    method.setBody("{\n" +
                            "return new com.mojang.text2speech.NarratorDummy();\n" +
                            "}");
                } else {
                    method.setBody("{\n" +
                            "return EMPTY;\n" +
                            "}");
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