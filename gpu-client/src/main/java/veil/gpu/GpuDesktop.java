package veil.gpu;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Opt-in hardware-accelerated desktop preview. Never required by LanServer.
 * The original Java2D client remains an independent, runnable fallback.
 */
public final class GpuDesktop {
    private GpuDesktop() {}
    public static void main(String[] args) {
        boolean smoke=false,gallery=false;
        long seed=20261020L;
        for(String arg:args) {
            if("--smoke".equals(arg))smoke=true;
            else if("--gallery".equals(arg))gallery=true;
            else if(arg.startsWith("--seed="))seed=Long.parseLong(arg.substring(7));
            else throw new IllegalArgumentException("Unknown argument: "+arg);
        }
        Lwjgl3ApplicationConfiguration cfg=new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("THE MALEFIC VEIL | PSX GPU");
        cfg.setWindowedMode(960,600);
        cfg.setResizable(true);
        cfg.useVsync(true);
        cfg.setForegroundFPS(60);
        cfg.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,3);
        if(smoke&&gallery)throw new IllegalArgumentException("Choose smoke or gallery");
        new Lwjgl3Application(new GpuKenneyPreview(seed,smoke,gallery),cfg);
    }
}