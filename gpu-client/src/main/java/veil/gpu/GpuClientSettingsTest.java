package veil.gpu;

import java.nio.file.*;

/** Test local option persistence and defaults without a graphics device. */
public final class GpuClientSettingsTest {
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("malefic-settings-");
        try{
            System.setProperty("veil.settings.path",dir.resolve("client.properties").toString());
            GpuClientSettings defaults=GpuClientSettings.load();
            check(defaults.distance==145&&defaults.vsync,"Client defaults");
            defaults.master=55;defaults.music=33;defaults.distance=220;
            defaults.invertY=true;defaults.windowSize=2;defaults.save();
            var loaded=GpuClientSettings.load();
            check(loaded.music==33&&loaded.master==55&&loaded.distance==220&&
                loaded.invertY&&loaded.windowSize==2,"Save/load must round trip");
            Files.writeString(GpuClientSettings.path(),"master=10000\ndistance=-1\nmusic=INVALID\n");
            loaded=GpuClientSettings.load();
            check(loaded.master==100&&loaded.distance==45&&loaded.music==58,
                "Corrupt settings must be bounded and retain defaults");
            System.out.println("PASS GpuClientSettingsTest validated persistent user settings");
        }finally{
            try(var paths=Files.walk(dir)){
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception ignored){}
                });
            }
            System.clearProperty("veil.settings.path");
        }
    }
}
