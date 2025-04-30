package com.mio.libpatcher.transformer;

import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;

public class FabricLoaderTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "net.fabricmc.loader.impl.gui.FabricGuiEntry";
    }

    @Override
    public byte[] transform(byte[] buffer) throws Throwable {
        CtClass clazz = pool.makeClass(new ByteArrayInputStream(buffer));
        CtMethod[] methods = clazz.getDeclaredMethods("displayError");
        if (methods != null) {
            for (CtMethod method : methods) {
                method.setBody("{System.exit(1);}");
            }
        }
        byte[] bytes = clazz.toBytecode();
        clazz.detach();
        return bytes;
    }
}