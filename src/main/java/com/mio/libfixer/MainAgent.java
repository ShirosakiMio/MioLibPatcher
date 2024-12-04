package com.mio.libfixer;

import com.mio.libfixer.transformer.*;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

public class MainAgent {
    private static final List<String> classList = new ArrayList<>();

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Mio LibFixer is running!");
        addTransformer(inst, false);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        addTransformer(inst, true);
    }

    private static void addTransformer(Instrumentation inst, boolean isAgentmain) {
        List<BaseTransformer> transformers = new ArrayList<>();
        transformers.add(new TTSTransformer());
        transformers.add(new LibraryTransformer());
        transformers.add(new SystemInfoTransformer());
        transformers.add(new RandomPatchesTransformer());
        transformers.forEach(baseTransformer -> {
            inst.addTransformer(baseTransformer, true);
            if (isAgentmain) {
                classList.add(baseTransformer.getTargetClassName());
            }
        });
        if (isAgentmain) {
            Class<?>[] classes = inst.getAllLoadedClasses();
            for (Class<?> aClass : classes) {
                if (classList.contains(aClass.getName())) {
                    System.out.println("transform:" + aClass.getName());
                    try {
                        inst.retransformClasses(aClass);
                    } catch (UnmodifiableClassException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

}