package GraphicalTTTwithOOnSFX;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public enum SoundEffect {
    EAT_FOOD("GraphicalTTTwithOOnSFX/audio/eatfood.wav"),
    EXPLODE("GraphicalTTTwithOOnSFX/audio/explode.wav"),
    DIE("GraphicalTTTwithOOnSFX/audio/die.wav");

    public static enum Volume {
        MUTE, LOW, MEDIUM, HIGH
    }

    public static Volume volume = Volume.LOW;

    private Clip clip;

    private SoundEffect(String soundFileName) {
        try {
            URL url = this.getClass().getClassLoader().getResource(soundFileName);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (volume != Volume.MUTE) {
            if (clip.isRunning())
                clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    static void initGame() {
        values();
    }

    private static Clip bgmClip;
    private static final String BGM_FILE_PATH = "GraphicalTTTwithOOnSFX/audio/bgm.wav";

    public static void loadAndPlayBGM() {
        try {
            URL url = SoundEffect.class.getClassLoader().getResource(BGM_FILE_PATH);
            if (url == null) {
                System.err.println("BGM file not found: " + BGM_FILE_PATH);
                return;
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioInputStream);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void stopBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
    }
}