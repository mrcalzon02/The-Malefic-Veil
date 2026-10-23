package veil.gpu;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Validated client-only options, never exposed as authoritative world rules. */
public final class GpuClientSettings {
    public int master=90,music=58,effects=85,ambience=80,ui=80,voice=85;
    public int distance=145,lights=8,fieldOfView=74,sensitivity=100;
    public boolean invertY=false,vsync=true,fullscreen=false;
    public int windowSize=0; // 0=960x600 1=1280x800 2=1600x900
    public static final int[][] WINDOWS={{960,600},{1280,800},{1600,900}};
    public static Path path(){
        String override=System.getProperty("veil.settings.path");
        if(override!=null&&!override.isBlank())return Path.of(override).toAbsolutePath();
        return Path.of(System.getProperty("user.home"),".malefic-veil","client.properties");
    }
    private static int number(Properties p,String key,int fallback,int low,int high){
        String raw=p.getProperty(key);
        if(raw==null)return fallback;
        try{return Math.max(low,Math.min(high,Integer.parseInt(raw)));}
        catch(NumberFormatException e){return fallback;}
    }
    private static boolean yes(Properties p,String key,boolean fallback){
        String value=p.getProperty(key);
        return value==null?fallback:Boolean.parseBoolean(value);
    }
    public static GpuClientSettings load(){
        GpuClientSettings c=new GpuClientSettings();
        if(!Files.isRegularFile(path()))return c;
        Properties p=new Properties();
        try(InputStream input=Files.newInputStream(path())){p.load(input);}
        catch(IOException unreadable){System.err.println("Cannot load optional client settings: "+unreadable);return c;}
        c.master=number(p,"master",c.master,0,100);
        c.music=number(p,"music",c.music,0,100);
        c.effects=number(p,"effects",c.effects,0,100);
        c.ambience=number(p,"ambience",c.ambience,0,100);
        c.ui=number(p,"ui",c.ui,0,100);
        c.voice=number(p,"voice",c.voice,0,100);
        c.distance=number(p,"distance",c.distance,45,240);
        c.lights=number(p,"lights",c.lights,0,8);
        c.fieldOfView=number(p,"fieldOfView",c.fieldOfView,55,100);
        c.sensitivity=number(p,"sensitivity",c.sensitivity,20,200);
        c.windowSize=number(p,"windowSize",c.windowSize,0,WINDOWS.length-1);
        c.invertY=yes(p,"invertY",c.invertY);
        c.vsync=yes(p,"vsync",c.vsync);
        c.fullscreen=yes(p,"fullscreen",c.fullscreen);
        return c;
    }
    public void save()throws IOException {
        validate();
        Properties p=new Properties();
        p.setProperty("master",Integer.toString(master));
        p.setProperty("music",Integer.toString(music));
        p.setProperty("effects",Integer.toString(effects));
        p.setProperty("ambience",Integer.toString(ambience));
        p.setProperty("ui",Integer.toString(ui));
        p.setProperty("voice",Integer.toString(voice));
        p.setProperty("distance",Integer.toString(distance));
        p.setProperty("lights",Integer.toString(lights));
        p.setProperty("fieldOfView",Integer.toString(fieldOfView));
        p.setProperty("sensitivity",Integer.toString(sensitivity));
        p.setProperty("windowSize",Integer.toString(windowSize));
        p.setProperty("invertY",Boolean.toString(invertY));
        p.setProperty("vsync",Boolean.toString(vsync));
        p.setProperty("fullscreen",Boolean.toString(fullscreen));
        Path file=path(),dir=file.getParent();
        Files.createDirectories(dir);
        Path tmp=Files.createTempFile(dir,".client-", ".tmp");
        try{
            try(OutputStream stream=Files.newOutputStream(tmp)){p.store(stream,
                "Malefic Veil: client-only graphics, accessibility and audio settings");}
            try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException exception){
                Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
    public void validate(){
        int[] nums={master,music,effects,ambience,ui,voice};
        for(int value:nums)if(value<0||value>100)
            throw new IllegalArgumentException("Audio percentage must be between 0 and 100");
        if(distance<45||distance>240||lights<0||lights>8||
           fieldOfView<55||fieldOfView>100||sensitivity<20||sensitivity>200||
           windowSize<0||windowSize>=WINDOWS.length)
            throw new IllegalArgumentException("Invalid client display settings");
    }
    public float musicGain(){return master*music/10000f;}
    public float effectsGain(){return master*effects/10000f;}
    public float ambienceGain(){return master*ambience/10000f;}
    public float uiGain(){return master*ui/10000f;}
    public float voiceGain(){return master*voice/10000f;}
}
