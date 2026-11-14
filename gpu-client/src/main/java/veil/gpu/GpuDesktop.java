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
        // Branded application/taskbar icon from the original PSX artwork.
        cfg.setWindowIcon("veil-original-art/pack02/malefic-veil-64.png");
        cfg.setWindowedMode(1280,720);
        cfg.setResizable(true);
        // Production uses its 60 Hz cap; the explicit CI ocean benchmark
        // must be uncapped or the paired baseline/ocean timings both report
        // exactly 16.67 ms regardless of real rendering cost.
        boolean oceanBenchmark=Boolean.getBoolean("veil.gpu.oceanBenchmark");
        cfg.useVsync(!oceanBenchmark);
        cfg.setForegroundFPS(oceanBenchmark?0:60);
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
        boolean lanWaterSmoke=false;
        boolean frontendSmoke=false,frontendFlowSmoke=false;
        long seed=20261020L;
        char city='A';
        for(String arg:args){
            if("--smoke".equals(arg))smoke=true;
            else if("--gallery".equals(arg))gallery=true;
            else if("--play-smoke".equals(arg))playSmoke=true;
            else if("--lan-water-smoke".equals(arg))lanWaterSmoke=true;
            else if("--frontend-smoke".equals(arg))frontendSmoke=true;
            else if("--frontend-flow-smoke".equals(arg))frontendFlowSmoke=true;
            else if("--editor".equals(arg))editor=true;
            else if("--editor-smoke".equals(arg))editorSmoke=true;
            else if(arg.startsWith("--seed="))seed=Long.parseLong(arg.substring(7));
            else if(arg.startsWith("--city=")&&arg.length()==8){
                city=Character.toUpperCase(arg.charAt(7));
                if(city<'A'||city>'G')throw new IllegalArgumentException("Unknown city "+city);
            }else throw new IllegalArgumentException("Unknown argument: "+arg);
        }
        if((smoke?1:0)+(gallery?1:0)+(playSmoke?1:0)+(editor?1:0)+
            (editorSmoke?1:0)+(frontendSmoke?1:0)+(frontendFlowSmoke?1:0)+
           (lanWaterSmoke?1:0)>1)
            throw new IllegalArgumentException("Choose one smoke/gallery/play-smoke/editor mode");
        if(frontendSmoke||frontendFlowSmoke){
            new Lwjgl3Application(new GpuFrontEnd(frontendSmoke,frontendFlowSmoke),config());
        }else if(editor||editorSmoke){
            if(city!='A')throw new IllegalArgumentException("Editor document currently targets City A");
            if(editorSmoke)
                System.setProperty("veil.editor.dir","build/test-visuals/editor-kit-smoke");
            new Lwjgl3Application(GpuKenneyPreview.editableWorld(seed,editorSmoke),config());
        }else if(lanWaterSmoke){
            // Saved host file -> two real TCP clients -> actual gameplay GPU.
            System.setProperty("veil.weather.mute","true");
            System.setProperty("veil.gpu.lanWaterSmoke","true");
            java.nio.file.Path temp=java.nio.file.Files.createTempDirectory("malefic-host-water-gl-");
            try{
                var cityVista=new veil.ContinentVista(new veil.World(seed),'A');
                double level=cityVista.sample(66,66).elevation()+5.0;
                var document=new dev.mrcalzon.water.WaterDocument("seed-"+seed,
                    java.util.List.of(new dev.mrcalzon.water.WaterPlacement(
                        "courtyard-pool",66,level,66,32,32,0,
                        new dev.mrcalzon.water.WaterSettings(),
                        dev.mrcalzon.water.WaterPlacement.Shape.OCTAGON)));
                document.save(temp.resolve("city-A.water"));
                try(veil.LanServer host=new veil.LanServer(0,seed,temp);
                    veil.NetLink left=new veil.NetLink("127.0.0.1",host.port());
                    veil.NetLink right=new veil.NetLink("127.0.0.1",host.port())){
                    long deadline=System.nanoTime()+8_000_000_000L;
                    while(System.nanoTime()<deadline&&
                          (!document.equals(left.authoredWater())||
                           !document.equals(right.authoredWater())))
                        Thread.sleep(20);
                    if(!document.equals(left.authoredWater())||
                       !document.equals(right.authoredWater())||
                       !document.revision().equals(host.authoredWater.snapshot().revision()))
                        throw new IllegalStateException("Two real LAN seats did not receive selected host water");
                    System.out.println("HOST_WATER_GPU_LAN seed="+seed+
                        " tcpClients=2 body="+document.placements().get(0).id()+
                        " revision="+document.revision());
                    final MaleficVeil[] session=new MaleficVeil[1];
                    final long lanSeed=seed;
                    SwingUtilities.invokeAndWait(()->
                        session[0]=MaleficVeil.gpuLanWaterSmokeSession(lanSeed,left));
                    new Lwjgl3Application(new GpuKenneyPreview(session[0],true),config());
                }
            }finally{
                try(var entries=java.nio.file.Files.walk(temp)){
                    for(var file:entries.sorted(java.util.Comparator.reverseOrder()).toList())
                        java.nio.file.Files.deleteIfExists(file);
                }
            }
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
