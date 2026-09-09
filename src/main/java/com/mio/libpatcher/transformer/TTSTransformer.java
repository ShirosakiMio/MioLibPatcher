package com.mio.libpatcher.transformer;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * 原本用于在缺失 flite 引擎时禁用复述功能；启动器提供 libflite 桥接安卓 TTS 后不再需要，
 * 补丁默认关闭，仅当显式指定 -Dmiolibpatcher.ttsDisable=true 时保留旧行为。
 */
public class TTSTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "com.mojang.text2speech.Narrator";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        if (!Boolean.parseBoolean(System.getProperty("miolibpatcher.ttsDisable", "false"))) {
            return;
        }
        CtMethod method = clazz.getDeclaredMethod("getNarrator");
        try {
            method.setBody("{ return new com.mojang.text2speech.NarratorDummy(); }");
        } catch (CannotCompileException e) {
            method.setBody("{ return EMPTY; }");
        }
    }
}
