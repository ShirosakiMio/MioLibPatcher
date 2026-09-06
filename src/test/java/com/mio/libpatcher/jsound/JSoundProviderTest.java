package com.mio.libpatcher.jsound;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.spi.MixerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSoundProviderTest {

    /** Injection target with a provider cache shaped like some Android JRE builds. */
    private static final class Holder {
        static MixerProvider[] arrayField;
        static List<MixerProvider> listField = new ArrayList<>();
    }

    @Test
    void serviceFileIsOnTheClasspath() throws Exception {
        // The JDK itself (resources.jar on JDK 8) ships a same-named service file,
        // so ours must be checked among all of them, not as the first entry.
        String expected = "com.mio.libpatcher.jsound.JSoundProvider";
        boolean found = false;
        java.util.Enumeration<java.net.URL> resources = JSoundProviderTest.class.getClassLoader()
                .getResources("META-INF/services/javax.sound.sampled.spi.MixerProvider");
        while (resources.hasMoreElements() && !found) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    resources.nextElement().openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        found |= expected.equals(trimmed);
                    }
                }
            }
        }
        assertTrue(found, "SPI service file with the JSound provider is missing from the classpath");
    }

    @Test
    void serviceLoaderDiscoversProvider() {
        List<MixerProvider> found = new ArrayList<>();
        for (MixerProvider provider : ServiceLoader.load(MixerProvider.class)) {
            found.add(provider);
        }
        assertTrue(found.stream().anyMatch(p -> p instanceof JSoundProvider),
                "ServiceLoader should discover the provider from the SPI file");
    }

    @Test
    void injectionIntoNullArrayInstallsProvider() throws Exception {
        Holder.arrayField = null;
        JSoundProvider.inject(Holder.class, "arrayField");
        assertEquals(1, Holder.arrayField.length);
        assertTrue(Holder.arrayField[0] instanceof JSoundProvider);
    }

    @Test
    void injectionIntoPopulatedArrayAppendsProvider() throws Exception {
        Holder.arrayField = new MixerProvider[]{new MixerProvider() {
            @Override
            public Mixer.Info[] getMixerInfo() {
                return new Mixer.Info[0];
            }

            @Override
            public Mixer getMixer(Mixer.Info info) {
                throw new UnsupportedOperationException();
            }
        }};
        JSoundProvider.inject(Holder.class, "arrayField");
        assertEquals(2, Holder.arrayField.length);
        assertTrue(Holder.arrayField[1] instanceof JSoundProvider);

        // Idempotent: injecting again must not add a second instance.
        JSoundProvider.inject(Holder.class, "arrayField");
        assertEquals(2, Holder.arrayField.length);
    }

    @Test
    void injectionIntoMissingFieldIsNoOp() throws Exception {
        // Must log and return instead of throwing.
        JSoundProvider.inject(Holder.class, "noSuchField");
    }

    @Test
    void mixerSupportsWildcardAndConcreteFormats() {
        JSoundProvider provider = new JSoundProvider();
        Mixer mixer = provider.getMixer(null);
        assertNotNull(mixer);
        assertSame(JSoundMixer.INFO, mixer.getMixerInfo());

        // The shape AudioSystem.getClip() asks for: 16-bit signed stereo, unspecified rate.
        AudioFormat wildcard = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED, 16, 2, 4, AudioSystem.NOT_SPECIFIED, true);
        assertTrue(mixer.isLineSupported(new DataLine.Info(Clip.class, wildcard)));

        AudioFormat unsupported = new AudioFormat(AudioFormat.Encoding.ULAW, 8000f, 8, 1, 1, 8000f, false);
        assertFalse(mixer.isLineSupported(new DataLine.Info(Clip.class, unsupported)));

        Mixer.Info[] infos = provider.getMixerInfo();
        assertEquals(1, infos.length);
        assertSame(JSoundMixer.INFO, infos[0]);
        assertTrue(provider.isMixerSupported(JSoundMixer.INFO));
    }

    @Test
    void audioSystemSeesTheMixerOnStockJvm() {
        boolean found = Arrays.stream(AudioSystem.getMixerInfo())
                .anyMatch(info -> JSoundMixer.INFO.equals(info));
        assertTrue(found, "AudioSystem should expose the JSound mixer through ServiceLoader discovery");
    }
}
