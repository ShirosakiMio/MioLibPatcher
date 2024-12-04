package com.mio.libfixer.transformer;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

public class TTSTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "com.mojang.text2speech.Narrator";
    }

    @Override
    public byte[] transform(byte[] buffer) {
        try {
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
        return buffer;
    }
}