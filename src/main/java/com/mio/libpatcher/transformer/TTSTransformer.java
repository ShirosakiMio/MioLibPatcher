package com.mio.libpatcher.transformer;

import com.mio.libpatcher.util.LogUtil;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

public class TTSTransformer implements BaseTransformer {
    private static final String BRIDGE_CLASS = "com.movtery.text2speech_bridge.AndroidNarrator";

    @Override
    public String getTargetClassName() {
        return "com.mojang.text2speech.Narrator";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        CtMethod method = clazz.getDeclaredMethod("getNarrator");
        if (bridgeClassAvailable(clazz)) {
            // 桥接实现类存在于游戏 classpath 时，将工厂改写为返回桥接单例，
            // 由桥接实现经 JNI 通道调用安卓系统语音合成
            LogUtil.info("TTS bridge detected, wiring Narrator to " + BRIDGE_CLASS);
            method.setBody("{ return " + BRIDGE_CLASS + ".getInstance(); }");
            return;
        }
        // 桥接类不在 classpath 时维持禁用行为，避免游戏尝试加载不可用的平台实现
        LogUtil.info("TTS bridge not found, narrator will be disabled");
        try {
            method.setBody("{ return new com.mojang.text2speech.NarratorDummy(); }");
        } catch (CannotCompileException e) {
            method.setBody("{ return EMPTY; }");
        }
    }

    private boolean bridgeClassAvailable(CtClass clazz) {
        try {
            // 以目标类所在的 ClassPool 为准，避免共享池的缓存与路径差异干扰判断
            return clazz.getClassPool().get(BRIDGE_CLASS) != null;
        } catch (Exception e) {
            return false;
        }
    }
}