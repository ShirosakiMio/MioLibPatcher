package com.mio.libfixer;

import com.mio.libfixer.transformer.LibraryTransformer;
import com.mio.libfixer.transformer.TTSTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class MainAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Mio LibFixer is running!");
        addTransformer(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        addTransformer(inst);
        Class<?>[] classes = inst.getAllLoadedClasses();
        for (Class<?> aClass : classes) {
            if (aClass.getName().equals("com.mojang.text2speech.Narrator") || aClass.getName().equals("org.lwjgl.system.Library")) {
                System.out.println("transform:" + aClass.getName());
                inst.retransformClasses(aClass);
                break;
            }
        }
    }

    private static void addTransformer(Instrumentation inst) {
        inst.addTransformer(new TTSTransformer(), true);
        inst.addTransformer(new LibraryTransformer(), true);
    }

}