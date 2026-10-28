package com.mio.libpatcher.transformer.oshi;

import com.mio.libpatcher.transformer.BaseTransformer;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * oshi 7.x（FFM，MC 26.x 携带）：{@code LinuxLibcFunctions} 在静态初始化中急切绑定全部
 * libc 符号，bionic 缺少 utmpx 系列（setutxent 等）导致 {@code ExceptionInInitializerError}，
 * 连带 SystemReport 的 processor/memory/software 三组崩溃报告信息报废（其余符号 bionic 均有）。
 *
 * <p>把 clinit 里的 {@code findOrThrow} 调用改道到 {@link OshiLibcFunctionsStub}：
 * 缺失符号回退到封闭表内的无害桩地址，其余符号行为不变。仅影响 FFM 时代的类名，
 * JNA 时代的 oshi（1.x~6.x）不含该类，补丁自然不触发。
 */
public class LinuxLibcFunctionsTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "oshi.ffm.platform.linux.LinuxLibcFunctions";
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
                if (m.getMethodName().equals("findOrThrow")) {
                    m.replace("{ $_ = (java.lang.foreign.MemorySegment) com.mio.libpatcher.transformer.oshi.OshiLibcFunctionsStub.findOrThrow($0, $1); }");
                }
            }
        });
    }
}
