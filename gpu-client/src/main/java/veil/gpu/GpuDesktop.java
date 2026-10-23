package veil.gpu;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import javax.swing.SwingUtilities;
import veil.MaleficVeil;

/**
 * The actual desktop entry uses the existing host/world/faction creation UI,
 * then presents its authoritative networked game via the GPU scene.
 * --gallery / --smoke remain isolated deterministic renderer tests.
 */
public final class GpuDesktop {
    private GpuDesktop(){}
    private static Lwjgl3ApplicationConfiguration config(){
        Lwjgl3ApplicationConfiguration cfg=new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("THE MALEFIC VEIL");
        cfg.setWindowedMode(960,600);
        cfg.setResizable(true);
        cfg.useVsync(true);
        cfg.setForegroundFPS(60);
        cfg.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,3);
        return cfg;
    }
    /** Called after the same validated solo / host / join session is created. */
    public static void launchGame(MaleficVeil authoritativeGame){
        if(authoritativeGame==null)throw new IllegalArgumentException("Missing gameplay");
        Thread gpu=new Thread(()->{
            try{
                new Lwjgl3Application(new GpuKenneyPreview(authoritativeGame,false),config());
            }catch(Throwable problem){
                System.err.println("GPU game startup failed: "+problem);
                problem.printStackTrace();
                authoritativeGame.gpuShutdown();
            }
        },"malefic-live-gpu");
        gpu.setDaemon(false);
        gpu.start();
    }
    /** Shares the exact terrain/instanced GPU renderer as playable worlds. */
    public static void launchEditor(long seed){
        Thread thread=new Thread(()->new Lwjgl3Application(
            GpuKenneyPreview.editableWorld(seed,false),config()),
            "malefic-editor-gpu");
        thread.setDaemon(false);thread.start();
    }
    public static void main(String[] args)throws Exception{
        boolean smoke=false,gallery=false,playSmoke=false,editor=false,editorSmoke=false;
        boolean frontendSmoke=false;
        long seed=20261020L;
        char city='A';
        for(String arg:args){
            if("--smoke".equals(arg))smoke=true;
            else if("--gallery".equals(arg))gallery=true;
            else if("--play-smoke".equals(arg))playSmoke=true;
            else if("--frontend-smoke".equals(arg))frontendSmoke=true;
            else if("--editor".equals(arg))editor=true;
            else if("--editor-smoke".equals(arg))editorSmoke=true;
            else if(arg.startsWith("--seed="))seed=Long.parseLong(arg.substring(7));
            else if(arg.startsWith("--city=")&&arg.length()==8){
                city=Character.toUpperCase(arg.charAt(7));
                if(city<'A'||city>'G')throw new IllegalArgumentException("Unknown city "+city);
            }else throw new IllegalArgumentException("Unknown argument: "+arg);
        }
        if((smoke?1:0)+(gallery?1:0)+(playSmoke?1:0)+(editor?1:0)+
            (editorSmoke?1:0)+(frontendSmoke?1:0)>1)
            throw new IllegalArgumentException("Choose one smoke/gallery/play-smoke/editor mode");
        if(frontendSmoke){
            new Lwjgl3Application(new GpuFrontEnd(true),config());
        }else if(editor||editorSmoke){
            if(city!='A')throw new IllegalArgumentException("Editor document currently targets City A");
            if(editorSmoke)
                System.setProperty("veil.editor.dir","build/test-visuals/editor-kit-smoke");
            new Lwjgl3Application(GpuKenneyPreview.editableWorld(seed,editorSmoke),config());
        }else if(playSmoke){
            System.setProperty("veil.weather.mute","true");
            final MaleficVeil[] session=new MaleficVeil[1];
            final long requestedSeed=seed;
            SwingUtilities.invokeAndWait(()->session[0]=MaleficVeil.gpuSmokeSession(requestedSeed));
            new Lwjgl3Application(new GpuKenneyPreview(session[0],true),config());
        }else if(smoke||gallery){
            new Lwjgl3Application(new GpuKenneyPreview(seed,smoke,gallery,city),config());
        }else{
            if(city!='A')throw new IllegalArgumentException("--city applies only to gallery");
            // The first visible window is the actual OpenGL city and native
            // in-engine mode/world/class/options menu, never a Swing dialog.
            new Lwjgl3Application(new GpuFrontEnd(),config());
        }
    }
}
