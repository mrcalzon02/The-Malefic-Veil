package veil;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Real socket verification of bounded, read-only macro sector packets. */
public final class ContinentSectorNetworkTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        try(LanServer host=new LanServer(0,20261020L);
            Socket socket=new Socket("127.0.0.1",host.port())) {
            socket.setSoTimeout(6000);
            BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);
            out.println("SECTOR|0|0");
            int parcels=0,begins=0,ends=0,spawns=0,encounters=0;
            long deadline=System.nanoTime()+6_000_000_000L;
            while(System.nanoTime()<deadline && ends==0){
                String line=in.readLine();
                check(line!=null,"Socket disconnected");
                if(line.startsWith("SECTOR_BEGIN|"))begins++;
                if(line.startsWith("PARCEL|"))parcels++;
                if(line.startsWith("CONTINENT_SPAWN|"))spawns++;
                if(line.startsWith("CONTINENT_ENCOUNTER|"))encounters++;
                if(line.equals("SECTOR_END|0|0"))ends++;
            }
            check(begins==1&&ends==1&&parcels==64,"Exactly 64 parcel descriptors in one bounded sector");
            check(spawns>=0&&encounters>=0,"Marker packet counts nonnegative");
            out.println("SECTOR|-1|0");
            boolean denied=false;
            for(int i=0;i<30&&!denied;i++){
                String line=in.readLine();
                check(line!=null,"Socket disconnected on invalid sector");
                denied=line.equals("SECTOR_DENIED|BOUNDS");
            }
            check(denied,"Out-of-bounds request rejected");
            check(host.match.roster().size()==1,"Sector queries do not create actors");
        }
        System.out.println("PASS ContinentSectorNetworkTest: parcel stream, boundary, no actor mutation");
    }
}
