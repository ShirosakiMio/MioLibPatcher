package com.mio.libpatcher.transformer.oshi;

import com.mio.libpatcher.transformer.BaseTransformer;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Arrays;
import java.util.List;

/**
 * 兼容两个时代的 oshi Linux CPU 实现：
 *
 * <ul>
 * <li>{@code oshi.software.os.linux.proc.CentralProcessor}（oshi 1.x，部分老 mod 捆绑）：
 * oshi 1.3+ 在静态块中按 /proc/cpuinfo 的 "core id" 行统计 CPU 数，ARM 设备没有该字段会得到 0，
 * 随后构造函数直接抛 IllegalArgumentException；替换构造函数与 getName 绕开设备信息读取。</li>
 * <li>{@code oshi.hardware.platform.linux.LinuxCentralProcessor}（oshi 6.x，Minecraft 自带）：
 * 构造时用 {@code Files.find} 递归遍历 /sys/devices/system/cpu/，受限设备（如 Android/SELinux）下
 * 子目录不可读会抛 UncheckedIOException（RuntimeException，oshi 内部的 catch 捕获不到），
 * 导致处理器构造失败；替换 initProcessorCounts 不再读取任何设备信息。</li>
 * </ul>
 */
public class CentralProcessor implements BaseTransformer {
    @Override
    public List<String> getTargetClassNames() {
        return Arrays.asList(
                "oshi.software.os.linux.proc.CentralProcessor",
                "oshi.hardware.platform.linux.LinuxCentralProcessor");
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        if (clazz.getName().equals("oshi.software.os.linux.proc.CentralProcessor")) {
            transformOshi1x(clazz);
        } else {
            transformOshi6x(clazz);
        }
    }

    /**
     * oshi 1.x：CPU 名称改用系统属性，并替换 int 构造函数。
     */
    private static void transformOshi1x(CtClass clazz) throws Throwable {
        CtMethod nameMethod = clazz.getDeclaredMethod("getName");
        nameMethod.setBody("{return System.getProperty(\"cpu.name\",\"\");}");
        // oshi 1.3+ 构造函数依赖静态块统计出的 CPU 数（ARM 设备 /proc/cpuinfo 无 "core id" 字段
        // 会得到 0），构造时直接抛异常；替换为固定初始化，不再读取 /proc/stat。
        try {
            CtConstructor ctor = clazz.getDeclaredConstructor(new CtClass[]{CtClass.intType});
            ctor.setBody("{this.processorNumber = $1;"
                    + "this.curProcTicks = new long[4];"
                    + "this.prevProcTicks = new long[4];"
                    + "this.procTickTime = System.currentTimeMillis();}");
        } catch (NotFoundException ignored) {
            // oshi 1.2 为无参构造且不读取设备信息，无需处理
        }
        // oshi 1.5+ 的核心数统计同样依赖 /proc/cpuinfo，改用 JVM 的 ActiveProcessorCount
        try {
            CtMethod countMethod = clazz.getDeclaredMethod("getLogicalProcessorCount");
            countMethod.setBody("{return java.lang.Runtime.getRuntime().availableProcessors();}");
        } catch (NotFoundException ignored) {
            // oshi 1.4 及更早版本无该方法，无需处理
        }
    }

    /**
     * oshi 6.x：替换 initProcessorCounts，逻辑处理器数量取 JVM 的
     * {@code -XX:ActiveProcessorCount}（Runtime.availableProcessors），绕开 /sys、/proc 读取。
     */
    private static void transformOshi6x(CtClass clazz) throws Throwable {
        CtMethod method = clazz.getDeclaredMethod("initProcessorCounts");
        // 不同 oshi 版本的返回类型不同：6.2.x 为 Pair，6.4.x 为 Triplet，6.6.x 为 Quartet，
        // 按实际签名生成对应的固定返回值（空拓扑由 AbstractCentralProcessor 的 failsafe 兜底）。
        String returnType = method.getReturnType().getName();
        String tupleCtor;
        if (returnType.endsWith("Quartet")) {
            tupleCtor = "new oshi.util.tuples.Quartet(logProcs, null, null, new java.util.ArrayList())";
        } else if (returnType.endsWith("Triplet")) {
            tupleCtor = "new oshi.util.tuples.Triplet(logProcs, null, null)";
        } else {
            tupleCtor = "new oshi.util.tuples.Pair(logProcs, null)";
        }
        String body = "{"
                + "java.util.List logProcs = new java.util.ArrayList();"
                + "int n = java.lang.Runtime.getRuntime().availableProcessors();"
                + "for (int i = 0; i < n; i++) {"
                + "logProcs.add(new oshi.hardware.CentralProcessor$LogicalProcessor(i, i, 0));"
                + "}"
                + "return " + tupleCtor + ";"
                + "}";
        method.setBody(body);
    }
}
