package com.mio.libpatcher.transformer;

import com.mio.libpatcher.util.LogUtil;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * 游戏复述的截停实现：NarratorLinux 的打断只靠 executionBatch 批次号跳过尚未开始的句子，
 * 正在播放的句子无法中止。启动器的 flite 桥接将朗读改为异步排队后，
 * clear() 需额外经 JNA 调用 flite 库的 flite_cancel 符号，通知桥接立即停止系统 TTS 的当前朗读并清空排队文本。
 * 桌面端真实 flite 库没有该符号，注入的调用失败时静默忽略。
 */
public class TTSCancelTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "com.mojang.text2speech.NarratorLinux";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        if (!Boolean.parseBoolean(System.getProperty("miolibpatcher.ttsCancel", "true"))) {
            return;
        }
        if (pool.find("com.sun.jna.NativeLibrary") == null) {
            LogUtil.error("TTSCancel: JNA is not visible to the ClassPool, narrator cancel patch skipped");
            return;
        }
        CtMethod method = clazz.getDeclaredMethod("clear");
        method.setBody(
                "{ executionBatch.incrementAndGet();"
                        + " try { com.sun.jna.NativeLibrary.getInstance(\"flite\")"
                        + " .getFunction(\"flite_cancel\").invokeVoid(new Object[0]); }"
                        + " catch (Throwable t) {} }");
    }
}
