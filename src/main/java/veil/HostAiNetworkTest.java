package veil;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Real TCP/LAN client receives AI metadata and positions without fake clients. */
public final class HostAiNetworkTest {
    private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    private static void eventually(BooleanSupplier condition)throws Exception{
        for(int i=0;i<150;i++){
            if(condition.getAsBoolean())return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out awaiting actual host AI packets");
    }
    public static void main(String[] args)throws Exception{
        var settings=HostAiSettings.fromOptions(List.of(
            "--ai-count=3","--ai-mode=BALANCED","--ai-level=5"));
        try(LanServer host=new LanServer(0,20261021L,null,false,settings);
            NetLink client=new NetLink("127.0.0.1",host.port())){
            eventually(()->client.aiActors.size()==3&&
                client.others.keySet().containsAll(client.aiActors.keySet()));
            check(host.match.roster().size()==4,
                "Three AI actors plus one actual LAN human actor");
            Set<String> factions=new HashSet<>();
            for(var ai:client.aiActors.entrySet()){
                check(ai.getKey()<0,"AI peer IDs must never overlap authenticated socket IDs");
                check(ai.getValue().level()==5,"Host-selected level transmitted authentically");
                factions.add(ai.getValue().faction());
            }
            check(factions.size()==3,"Balanced three AI show all three distinct factions");
            check(host.aiStatus().contains("group-cap=3"),"Dedicated host AI console status");
            check(host.ai.views().size()==3,"AI roster is server-owned");
        }
        System.out.println("PASS HostAiNetworkTest: real TCP AI state packets, three factions and assigned levels");
    }
}
