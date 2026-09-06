package com.mio.libpatcher.jsound;

import com.mio.libpatcher.util.LogUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.spi.MixerProvider;

/**
 * MixerProvider entry point. Two discovery routes are covered:
 * 1. The standard ServiceLoader route via META-INF/services, which stock OpenJDK
 *    builds (Termux and friends) use when AudioSystem initializes its providers.
 * 2. Direct injection into the AudioSystem provider cache for JRE builds that
 *    keep a "mixers" provider array field and do not run ServiceLoader discovery.
 * Everything is best-effort and failure is logged, never fatal.
 */
public class JSoundProvider extends MixerProvider {

    private static final String AUDIO_SYSTEM_FIELD = "mixers";
    private static volatile boolean registered;

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        injectIntoAudioSystem();
        logServiceDiscovery();
    }

    /** Mirrors the injection into holder classes with a cached provider field (unit-testable). */
    static void inject(Class<?> holder, String fieldName) {
        try {
            Field field = holder.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object current = field.get(null);
            if (current == null) {
                field.set(null, new MixerProvider[]{new JSoundProvider()});
                LogUtil.info("[JSound] Installed the OpenAL MixerProvider into " + holder.getName() + "." + fieldName);
                return;
            }
            if (current instanceof MixerProvider[]) {
                MixerProvider[] providers = (MixerProvider[]) current;
                if (containsUs(providers)) {
                    return;
                }
                MixerProvider[] updated = Arrays.copyOf(providers, providers.length + 1);
                updated[providers.length] = new JSoundProvider();
                field.set(null, updated);
                LogUtil.info("[JSound] Appended the OpenAL MixerProvider to " + holder.getName() + "." + fieldName);
                return;
            }
            if (current instanceof List) {
                @SuppressWarnings("unchecked")
                List<MixerProvider> providers = (List<MixerProvider>) current;
                if (containsUs(providers.toArray())) {
                    return;
                }
                providers.add(new JSoundProvider());
                LogUtil.info("[JSound] Added the OpenAL MixerProvider to " + holder.getName() + "." + fieldName);
                return;
            }
            LogUtil.info("[JSound] Unsupported provider cache type: " + current.getClass().getName());
        } catch (NoSuchFieldException e) {
            LogUtil.info("[JSound] " + holder.getName() + "." + fieldName
                    + " is not present, using standard ServiceLoader discovery");
        } catch (Throwable t) {
            LogUtil.error("[JSound] Injection into " + holder.getName() + "." + fieldName + " failed", t);
        }
    }

    private static void injectIntoAudioSystem() {
        inject(AudioSystem.class, AUDIO_SYSTEM_FIELD);
    }

    private static boolean containsUs(Object[] providers) {
        for (Object provider : providers) {
            if (provider instanceof JSoundProvider) {
                return true;
            }
        }
        return false;
    }

    private static void logServiceDiscovery() {
        try {
            for (MixerProvider provider : ServiceLoader.load(MixerProvider.class)) {
                if (provider instanceof JSoundProvider) {
                    LogUtil.info("[JSound] MixerProvider is discoverable through ServiceLoader");
                    return;
                }
            }
            LogUtil.info("[JSound] MixerProvider not visible through ServiceLoader; injection remains the active route");
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Mixer.Info[] getMixerInfo() {
        return new Mixer.Info[]{JSoundMixer.INFO};
    }

    @Override
    public boolean isMixerSupported(Mixer.Info info) {
        return JSoundMixer.INFO.equals(info);
    }

    @Override
    public Mixer getMixer(Mixer.Info info) {
        if (info == null || JSoundMixer.INFO.equals(info)) {
            return new JSoundMixer();
        }
        throw new IllegalArgumentException("Mixer not supported: " + info);
    }
}
