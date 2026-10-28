package com.mio.libpatcher.transformer.oshi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * oshi-core-ffm（MC 26.x 携带）libc 符号查找的 Android 兜底。
 *
 * <p>oshi 的 FFM 绑定类在静态初始化中急切绑定全部 libc 符号，任一缺失即整类
 * {@code ExceptionInInitializerError}。Android bionic 无 utmpx 登记记账 API，
 * {@code LinuxLibcFunctions} 必然初始化失败，连带 SystemReport 的 processor/memory/software
 * 三组崩溃报告信息全部报废。
 *
 * <p>本类被注入到 {@code LinuxLibcFunctions.<clinit>} 的 findOrThrow 调用位，接收的
 * {@code lookup} 即 oshi 的 LIBC（{@code Linker.defaultLookup()}）。兜底直接在同一个
 * lookup 上解析桩符号（桩符号本身必然是 libc 导出），不自行触碰 Linker——
 * {@code nativeLinker()} 的实现类是 jdk.internal 模块内部类，经 getClass() 反射会撞
 * 模块导出限制。兜底表为封闭集合，仅覆盖 bionic 确认缺失的符号，未列出的符号保持
 * 原生抛错语义（oshi 自身的可选符号 try/catch 逻辑不受影响）：
 * <ul>
 * <li>{@code ()void} 型 utmpx 操作（setutxent/endutxent/pututxline/utmpxname）→ getpid：
 *     无参原生调用，返回值被 void 描述符忽略；</li>
 * <li>{@code ()ptr} 型 getutxent → getlogin：无参且 Android 上恒返回 NULL，
 *     等价于空的 utmp 数据库，oshi 的会话枚举得到空集。</li>
 * </ul>
 *
 * <p>{@code SymbolLookup.find} 属公开导出 API，经反射调用合法；本类整体保持在补丁主体的
 * Java 8 工具链下编译，注入源中的 MemorySegment 类型解析发生在游戏 JVM（jre25）内。
 */
public final class OshiLibcFunctionsStub {

    /** 缺失符号 → 候选桩符号（按序取第一个存在者，均在 bionic 上验证导出）。 */
    private static final Map<String, List<String>> STUBS = new HashMap<>();

    static {
        STUBS.put("setutxent", Arrays.asList("getpid"));
        STUBS.put("endutxent", Arrays.asList("getpid"));
        STUBS.put("pututxline", Arrays.asList("getpid"));
        STUBS.put("utmpxname", Arrays.asList("getpid"));
        STUBS.put("getutxent", Arrays.asList("getlogin", "dlerror"));
    }

    private static volatile Method sFind;

    private OshiLibcFunctionsStub() {
    }

    /**
     * 等价于 {@code lookup.findOrThrow(name)}，符号缺失时在同一 libc lookup 上解析桩符号地址。
     * 返回值实际为 {@code java.lang.foreign.MemorySegment}，以 Object 承载以避免编译期依赖 FFM API。
     */
    public static Object findOrThrow(Object lookup, String name) throws NoSuchElementException {
        Optional<?> found = find(lookup, name);
        if (found.isPresent()) {
            return found.get();
        }
        List<String> candidates = STUBS.get(name);
        if (candidates != null) {
            for (String stub : candidates) {
                Optional<?> segment = find(lookup, stub);
                if (segment.isPresent()) {
                    return segment.get();
                }
            }
        }
        throw new NoSuchElementException("Symbol not found: " + name);
    }

    private static Optional<?> find(Object lookup, String name) {
        try {
            Method find = sFind;
            if (find == null) {
                find = Class.forName("java.lang.foreign.SymbolLookup").getMethod("find", String.class);
                sFind = find;
            }
            return (Optional<?>) find.invoke(lookup, name);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
