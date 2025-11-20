package com.mio.libpatcher.transformer;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class SQLTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "";
    }

    @Override
    public List<String> getTargetClassNames() {
        List<String> list = new ArrayList<>();
        list.add("dh_sqlite.util.OSInfo");
        list.add("org.rfresh.sqlite.util.OSInfo");
        list.add("org.sqlite.util.OSInfo");
        return list;
    }

    @Override
    public byte[] transform(byte[] buffer) throws Throwable {
        CtClass clazz;
        clazz = pool.makeClass(new ByteArrayInputStream(buffer));
        CtMethod method = clazz.getDeclaredMethod("isAndroid");
        method.setBody("{return true;}");
        byte[] bytes = clazz.toBytecode();
        clazz.detach();
        return bytes;
    }
}