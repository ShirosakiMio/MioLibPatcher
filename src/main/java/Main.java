import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.makeClass(Files.newInputStream(Paths.get("E:/SystemInfo.class")));
        CtConstructor constructor = clazz.getClassInitializer();
        constructor.setBody("{cpuInfo = \"\";}");
        Files.write(Paths.get("./SystemInfo.class"), clazz.toBytecode());
    }
}
