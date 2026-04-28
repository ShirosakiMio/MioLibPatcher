package com.mio.libpatcher.transformer;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class VeilImGuiTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "foundry.veil.impl.client.imgui.VeilImGuiImpl";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        try {
            CtMethod loadLibraryMethod = clazz.getDeclaredMethod("setImGuiPath");
            loadLibraryMethod.setBody("{}");
        } catch (NotFoundException ignored) {
        }
    }
}
