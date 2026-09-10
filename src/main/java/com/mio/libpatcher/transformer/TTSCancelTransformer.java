package com.mio.libpatcher.transformer;

import javassist.CtClass;
import javassist.CtMethod;

/**
 * 游戏复述的截停实现：NarratorLinux 的打断只靠 executionBatch 批次号跳过尚未开始的句子，
 * 正在播放的句子无法中止。在 clear() 前置经 JNA 调用 flite 库的 flite_cancel 符号，
 * 通知桥接立即停止系统 TTS 的当前朗读；桌面端真实 flite 库没有该符号，调用失败时静默忽略。
 *
 * <p>注入体只引用 JDK 类型并以反射驱动 JNA：com.sun.jna 是否可见因游戏加载器
 * （Knot/ModLauncher）而异，编译期引用会在 javassist 解析时失败，导致整个补丁被跳过。</p>
 */
public class TTSCancelTransformer implements BaseTransformer {
    // javassist 编译器不支持 varargs 解析，反射调用的参数表必须用显式数组
    private static final String HOOK =
            "{ try {"
                    + " Object lib = Class.forName(\"com.sun.jna.NativeLibrary\")"
                    + "   .getMethod(\"getInstance\", new Class[]{String.class}).invoke(null, new Object[]{\"flite\"});"
                    + " Object fn = lib.getClass().getMethod(\"getFunction\", new Class[]{String.class}).invoke(lib, new Object[]{\"flite_cancel\"});"
                    + " fn.getClass().getMethod(\"invokeVoid\", new Class[]{Object[].class}).invoke(fn, new Object[]{new Object[0]});"
                    + " } catch (Throwable t) {} }";

    @Override
    public String getTargetClassName() {
        return "com.mojang.text2speech.NarratorLinux";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        if (!Boolean.parseBoolean(System.getProperty("miolibpatcher.ttsCancel", "true"))) {
            return;
        }
        // insertBefore 保留原方法体，兼容各代 NarratorLinux（含无 executionBatch 的 1.13.9-）
        clazz.getDeclaredMethod("clear").insertBefore(HOOK);
    }
}
