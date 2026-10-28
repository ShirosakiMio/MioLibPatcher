package com.mio.libpatcher.transformer.oshi;

import com.mio.libpatcher.transformer.BaseTransformer;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * oshi 7.x（FFM）：Android 无 libudev，{@code LinuxOperatingSystemFFM.HAS_UDEV} 的探测
 * 只是打一行 "Did not find udev library" 警告后返回 false。把该探测直接置 false，
 * UdevFunctions 类完全不再加载，CPU 枚举走 sysfs 回退（/sys/devices/system/cpu 在 Android 可读）。
 */
public class LinuxOperatingSystemFFMTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "oshi.software.os.linux.LinuxOperatingSystemFFM";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        CtConstructor clinit = clazz.getClassInitializer();
        if (clinit == null) {
            return;
        }
        clinit.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("isAvailable")) {
                    m.replace("{ $_ = false; }");
                }
            }
        });
    }
}
