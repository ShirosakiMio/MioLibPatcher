package com.mio.libpatcher.transformer;

import javassist.NotFoundException;
import javassist.CtClass;
import javassist.CtMethod;

public class NarratorGuardTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "net.minecraft.client.GameNarrator";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        try {
            // 26.x 起 checkStatus 在系统语音不可用时经原生对话框质询是否继续，
            // 桌面对话框在安卓上不可用，返回失败会抛异常导致游戏拒绝启动；
            // 复述不可用已由设置页置灰与 toast 呈现，这里清空启动质询。
            CtMethod method = clazz.getDeclaredMethod("checkStatus");
            method.setBody("{}");
        } catch (NotFoundException e) {
            // 旧版本不存在 checkStatus 方法，无需处理
        }
    }
}