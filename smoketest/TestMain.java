import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Desktop smoke test: run with -javaagent:MioLibPatcher.jar and the LWJGL 3.2.2
 * jars (+ windows natives) on the classpath. Verifies the ServiceLoader discovery
 * route and real playback through SourceDataLine and Clip.
 */
public class TestMain {

    public static void main(String[] args) throws Exception {
        System.out.println("JVM: " + System.getProperty("java.version")
                + " / " + System.getProperty("java.vm.name"));

        boolean found = false;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println("  mixer: " + info.getName() + " / " + info.getVendor());
            if ("Mio JSound (OpenAL)".equals(info.getName())) {
                found = true;
            }
        }
        if (!found) {
            throw new AssertionError("JSound mixer was not discovered by AudioSystem");
        }
        System.out.println("[OK] JSound mixer discovered through AudioSystem");

        AudioFormat fmt = new AudioFormat(44100f, 16, 2, true, false);

        // --- SourceDataLine: one second of 440 Hz ---
        SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
        System.out.println("[OK] SourceDataLine acquired: " + line.getLineInfo());
        line.open(fmt, 65536);
        line.start();
        byte[] buf = sine(fmt, 1.0, 440.0);
        int written = 0;
        while (written < buf.length) {
            written += line.write(buf, written, Math.min(8192, buf.length - written));
        }
        line.drain();
        long us = line.getMicrosecondPosition();
        System.out.println("[OK] SourceDataLine played " + buf.length + " bytes, position=" + us + "us");
        if (us < 800000) {
            throw new AssertionError("SourceDataLine position too small: " + us);
        }
        line.close();

        // --- Clip: half a second of 523 Hz, looped ---
        Clip clip = AudioSystem.getClip();
        System.out.println("[OK] Clip acquired: " + clip.getLineInfo());
        byte[] clipBuf = sine(fmt, 0.5, 523.25);
        clip.open(fmt, clipBuf, 0, clipBuf.length);
        clip.loop(2);
        clip.start();
        long deadline = System.currentTimeMillis() + 8000;
        while (clip.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        long clipPos = clip.getMicrosecondPosition();
        System.out.println("[OK] Clip stopped at " + clipPos + "us (frameLength="
                + clip.getMicrosecondLength() + "us)");
        if (clipPos < 900000) {
            throw new AssertionError("Clip did not complete two loops: " + clipPos);
        }
        clip.close();

        System.out.println("ALL OK");
    }

    static byte[] sine(AudioFormat fmt, double seconds, double freq) {
        int frameSize = fmt.getFrameSize();
        int frames = (int) (fmt.getSampleRate() * seconds);
        byte[] out = new byte[frames * frameSize];
        for (int f = 0; f < frames; f++) {
            double v = Math.sin(2 * Math.PI * freq * f / fmt.getSampleRate()) * 0.35;
            short s = (short) (v * 32767);
            int o = f * frameSize;
            out[o] = (byte) (s & 0xFF);
            out[o + 1] = (byte) ((s >> 8) & 0xFF);
            out[o + 2] = (byte) (s & 0xFF);
            out[o + 3] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }
}
