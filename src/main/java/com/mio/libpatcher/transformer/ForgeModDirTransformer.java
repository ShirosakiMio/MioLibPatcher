package com.mio.libpatcher.transformer;

import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;

public class ForgeModDirTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "net.minecraftforge.fml.loading.ModDirTransformerDiscoverer";
    }

    @Override
    public byte[] transform(byte[] buffer) throws Throwable {
        CtClass clazz = pool.makeClass(new ByteArrayInputStream(buffer));
        CtMethod method = clazz.getDeclaredMethod("visitFile");
        method.insertBefore("{" +
                "        String name =  $1.getFileName().toString();\n" +
                "        if (name.endsWith(\".jar\")) {\n" +
                "            System.out.println(\"Loading mod: \"+ name);\n" +
                "        }" +
                "}");
        byte[] bytes = clazz.toBytecode();
        clazz.detach();
        return bytes;
    }
}
