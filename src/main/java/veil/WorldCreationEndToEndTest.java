package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Actual creation -> durable host -> LAN join -> save -> restart -> reconnect. */
public final class WorldCreationEndToEndTest {
    private static void check(boolean ok,String description){
        if(!ok)throw new AssertionError(description);
    }
    public static void main(String[] args)throws Exception{
        String name="E2E_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        var settings=new WorldCreationConfig(name,42L,false,
            "solo","normal","normal",1,0);
        Path directory=settings.directory();
        try {
            settings.saveNew();
            check(WorldCreationConfig.load(directory).equals(settings),
                "World settings did not survive disk reload");
            check(WorldCreationConfig.list().contains(settings),
                "Saved world missing from world selector");
            var bots=HostAiSettings.defaults();
            try(LanServer server=new LanServer(0,settings.seed(),directory,
                    settings.hardcore(),bots)){
                check(server.match.world.seed==settings.seed(),"Host used wrong seed");
                try(NetLink player=new NetLink("127.0.0.1",server.port())){
                    check(player.seed==settings.seed(),"Connected client seed mismatch");
                    player.sceneReady();
                    player.selectFaction(CharacterRoster.Faction.THE_UNRAVELED);
                }
                server.saveNow();
            }
            check(Files.exists(directory.resolve("host-rules.dat")),
                "Host hardcore rules not persisted");
            try(LanServer restarted=new LanServer(0,settings.seed(),directory,
                    settings.hardcore(),bots);
                NetLink player=new NetLink("127.0.0.1",restarted.port())){
                check(player.seed==settings.seed(),"Restarted world changed seed");
            }
            boolean rejected=false;
            try(LanServer invalid=new LanServer(0,settings.seed()+1,directory,
                    settings.hardcore(),bots)){}
            catch(IOException expected){rejected=true;}
            check(rejected,"Host accepted a changed saved seed");
            rejected=false;
            try(LanServer invalid=new LanServer(0,settings.seed(),directory,
                    true,bots)){}
            catch(IOException expected){rejected=true;}
            check(rejected,"Host accepted a changed hardcore rule");
            System.out.println("PASS WorldCreationEndToEndTest create-save-restart-join");
        }finally{
            if(Files.exists(directory)){
                try(var files=Files.walk(directory)){
                    for(Path path:files.sorted(Comparator.reverseOrder()).toList())
                        Files.deleteIfExists(path);
                }
            }
        }
    }
}
