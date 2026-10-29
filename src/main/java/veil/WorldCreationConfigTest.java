package veil;

import java.io.*;
import java.nio.file.*;

/** World creation settings must persist and reject rule corruption. */
public final class WorldCreationConfigTest {
    private static void check(boolean ok,String error){if(!ok)throw new AssertionError(error);}
    public static void main(String[] args)throws Exception{
        WorldCreationConfig config=new WorldCreationConfig("New Settlement",42L,
            false,"solo","normal","normal",1,6);
        check(config.folder().equals("New_Settlement"),"Stable world save folder");
        check(config.maxPlayers()==1&&config.aiPopulation()==6,"Requested defaults");
        check(!config.hardcore(),"Normal game mode");
        check(!WorldCreationConfig.defaults().hostType().equals("multi"),
            "Default world must not expose a public server");
        boolean rejected=false;
        try{new WorldCreationConfig("../escape",12,false,"solo",
            "normal","normal",1,6);}
        catch(IllegalArgumentException exception){rejected=true;}
        check(rejected,"Path traversal world name accepted");
        rejected=false;
        try{new WorldCreationConfig("World",12,false,"solo",
            "normal","normal",12,6);}
        catch(IllegalArgumentException exception){rejected=true;}
        check(rejected,"Solo world with multiple slots accepted");
        System.out.println("PASS WorldCreationConfigTest");
    }
}
