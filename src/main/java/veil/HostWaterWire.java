package veil;

import dev.mrcalzon.water.WaterDocument;
import dev.mrcalzon.water.WaterPlacement;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Server-to-client, join-time authored-water snapshot. Each original SDK
 * placement is an independent bounded line; a revision-checked final frame
 * publishes the complete document atomically, never a partial shoreline.
 * Client input has NO matching host command. This is visual geometry, not
 * water physics or a hot-reload subscription.
 */
public final class HostWaterWire {
    private static final int MAX_FRAME=12000;
    private HostWaterWire(){}

    public static List<String> lines(WaterDocument document){
        String revision=document.revision();
        List<String> result=new ArrayList<>();
        result.add("WATER_BEGIN|"+revision+"|"+document.placements().size());
        int index=0;
        for(WaterPlacement placement:document.placements()){
            String serialized=new WaterDocument(document.level(),List.of(placement)).encode();
            String data=Base64.getUrlEncoder().withoutPadding()
                .encodeToString(serialized.getBytes(StandardCharsets.UTF_8));
            String frame="WATER_BODY|"+(index++)+"|"+data;
            if(frame.length()>MAX_FRAME)
                throw new IllegalArgumentException("Authored water body exceeds LAN frame budget");
            result.add(frame);
        }
        result.add("WATER_END|"+revision);
        return List.copyOf(result);
    }

    /** Confined to one client's socket reader; exposes only a full verified value. */
    public static final class Receiver {
        private final String level;
        private String revision;
        private int expected;
        private List<WaterPlacement> pending;
        public Receiver(long worldSeed){level="seed-"+worldSeed;}
        private void reset(){revision=null;expected=0;pending=null;}

        /** Returns non-null only after the end frame proves seed and revision. */
        public WaterDocument accept(String frame){
            if(frame==null||frame.length()>MAX_FRAME){reset();return null;}
            String[] s=frame.split("\\|",-1);
            try{
                switch(s[0]){
                    case "WATER_BEGIN" -> {
                        reset();
                        if(s.length!=3||!s[1].matches("[0-9a-f]{64}"))
                            return null;
                        int count=Integer.parseInt(s[2]);
                        if(count<0||count>64)return null;
                        revision=s[1];expected=count;pending=new ArrayList<>();
                    }
                    case "WATER_BODY" -> {
                        if(s.length!=3||pending==null||pending.size()>=expected||
                           Integer.parseInt(s[1])!=pending.size())throw new IllegalArgumentException();
                        byte[] data=Base64.getUrlDecoder().decode(s[2]);
                        if(data.length>MAX_FRAME)throw new IllegalArgumentException();
                        WaterDocument part=WaterDocument.read(new ByteArrayInputStream(data));
                        if(!level.equals(part.level())||part.placements().size()!=1)
                            throw new IllegalArgumentException();
                        pending.add(part.placements().get(0));
                    }
                    case "WATER_END" -> {
                        if(s.length!=2||pending==null||pending.size()!=expected||
                           !s[1].equals(revision))throw new IllegalArgumentException();
                        WaterDocument complete=new WaterDocument(level,pending);
                        if(!complete.revision().equals(revision))
                            throw new IllegalArgumentException();
                        reset();
                        return complete;
                    }
                    default -> {return null;}
                }
            }catch(Exception malformed){reset();}
            return null;
        }
    }
}
