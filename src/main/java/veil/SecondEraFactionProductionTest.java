package veil;

import java.nio.file.*;
import java.util.*;

/** True E2 server processes and V6 receipts for all three faction economies.
 * Trust-port fixture provides prerequisite world buildings and gathered
 * cross-chain material, never bypasses actual host build/production actions. */
public final class SecondEraFactionProductionTest {
    static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void simulate(ArenaSystems host,int seconds){
        for(int s=0;s<seconds;s++){
            host.advance(1000);
            if(s%40==39)for(var player:host.roster())if(!player.dead){
                var needs=player.survival.status();
                if(needs.hunger()<65){
                    require(player.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,true),
                        "Supply fixture could not feed operator");
                    require(host.consumeSurvival(player.id,SurvivalCrafting.Item.EDIBLES),
                        "Operator failed server food consumption");
                }
                if(needs.hydration()<65){
                    require(player.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,true),
                        "Supply fixture could not hydrate operator");
                    require(host.consumeSurvival(player.id,SurvivalCrafting.Item.CLEAN_WATER),
                        "Operator failed server hydration consumption");
                }
            }
        }
    }
    static World.Device fixture(ArenaSystems host,int kind,double x,double y,String owner){
        World.Device device=new World.Device(x,y,kind);
        device.ownerKey=owner;host.world.devices.add(device);return device;
    }
    static void cost(CampStockpile crate,Map<String,Integer> ingredients){
        for(var e:ingredients.entrySet())
            require(crate.delivered(SurvivalCrafting.Item.valueOf(e.getKey()),
                e.getValue()),"Physical crate cannot accept authentic cost "+e);
    }
    static World.Device physicallyBuild(ArenaSystems host,ArenaSystems.Player p,
                                        int kind,double x,double y){
        // Choose real unobstructed construction tiles within camp and funded
        // crate reach; never forge a completed factory via a client command.
        for(double dx=-3.5;dx<=3.5;dx+=.7)
            for(double dy=-3.5;dy<=3.5;dy+=.7){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                if(host.reviewBuild(p.id,kind).allowed()&&host.build(p.id,kind))
                    return host.world.devices.get(host.world.devices.size()-1);
            }
        throw new AssertionError("Could not legally build physically funded E2 "+kind);
    }
    static void run(int stage,CharacterRoster.Faction faction)throws Exception{
        long seed=20261020L;
        String key="acct_"+"c".repeat(63)+(stage-25);
        ArenaSystems host=new ArenaSystems(seed);
        var owner=host.join(1);
        require(host.assignAccount(1,key)&&host.enrollFaction(1,faction),
            "E2 operator requires actual authenticated faction");
        double[] place=FactionFirstProductionTest.site(host);
        double x=place[0],y=place[1];
        var camp=fixture(host,ArenaSystems.CAMP_STAKE,x-2.9,y,key);
        var crate=fixture(host,ArenaSystems.STORAGE_CRATE,x+1.8,y,key);
        int first=switch(stage){
            case ArenaSystems.UNRAVELED_MACHINIST_BAY ->
                ArenaSystems.UNRAVELED_SCRAP_OUTPOST;
            case ArenaSystems.PENITENT_RELIQUARY ->
                ArenaSystems.PENITENT_VOTIVE_ALTAR;
            case ArenaSystems.BEGOTTEN_SPORE_BASIN ->
                ArenaSystems.BEGOTTEN_BROOD_NEST;
            default -> throw new IllegalArgumentException("Incorrect test faction");
        };
        var source=fixture(host,first,x-1.2,y,key);
        var next=CraftingEraRegistry.facility(ArenaSystems.factionFacility(stage));
        cost(crate.storage,next.materials());
        owner.x=x-.4;owner.y=y;
        String missing=host.reviewBuild(1,stage).reason();
        require(missing.equals("MISSING_PHYSICAL_PREREQUISITE"),
            "E2 illegally bypassed its physical prerequisites: "+missing);
        for(String predecessor:next.requires()){
            if(predecessor.equals(ArenaSystems.factionFacility(first)))continue;
            int kind=ArenaSystems.kindForFacility(predecessor);
            require(kind>=0,"Unavailable physical E2 dependency "+predecessor);
            fixture(host,kind,x+6.6,y+3,key);
        }
        var outsider=host.join(2);
        require(host.assignAccount(2,"acct_"+"b".repeat(64)),
            "Adversarial E2 player requires account");
        var rival=stage==ArenaSystems.PENITENT_RELIQUARY?
            CharacterRoster.Faction.THE_BEGOTTEN:CharacterRoster.Faction.THE_PENITENT;
        require(host.enrollFaction(2,rival),"Rival should have opposing faction");
        outsider.x=owner.x;outsider.y=owner.y;
        require(host.reviewBuild(2,stage).reason().equals("WRONG_FACTION_FACILITY"),
            "Wrong faction allowed to construct restricted E2 worksite");
        World.Device work=physicallyBuild(host,owner,stage,x,y);
        require(work.hp==100&&key.equals(work.ownerKey),
            "Host failed to own physical E2 output");
        for(var e:next.materials().entrySet())
            require(crate.storage.count(SurvivalCrafting.Item.valueOf(e.getKey()))==0,
                "Exact E2 cost not deducted "+e.getKey());
        require(!host.startFactionBatch(2,
            ArenaSystems.firstProcess(stage)),"Rival activated another faction machine");

        // Produce the real first-tier input instead of directly minting the
        // unique faction artifact. Shared upstream salvage remains a fixture
        // until all generic utility modules are fully integrated.
        var beginning=CraftingEraRegistry.process(ArenaSystems.firstProcess(first));
        var second=CraftingEraRegistry.process(ArenaSystems.firstProcess(stage));
        require(second!=null&&second.facility().equals(next.id()),
            "E2 process does not belong to its distinct physical facility");
        var signature=SurvivalCrafting.Item.valueOf(beginning.output());
        int batches=(second.inputs().get(signature.name())+
            beginning.quantity()-1)/beginning.quantity();
        for(int n=0;n<batches;n++){
            cost(crate.storage,beginning.inputs());
            owner.x=source.x+.5;owner.y=source.y;
            require(host.startFactionBatch(1,beginning.id()),
                "Host E1 output reservation failed");
            simulate(host,beginning.seconds());
            require(!source.productionActive,
                "First era work order did not complete and clear receipt");
        }
        require(crate.storage.count(signature)>=second.inputs().get(signature.name()),
            "Real E1 production did not supply E2 recipe");
        for(var ingredient:second.inputs().entrySet())
            if(!ingredient.getKey().equals(signature.name()))
                cost(crate.storage,Map.of(ingredient.getKey(),ingredient.getValue()));

        owner.x=work.x+.5;owner.y=work.y;
        require(host.startFactionBatch(1,second.id()),"Live E2 process refused paid inputs");
        require(!host.startFactionBatch(1,second.id()),
            "Duplicate machine work order accepted while active");
        simulate(host,90);
        require(work.productionActive&&work.productionMs==90_000&&
            work.productionRecipeId.equals(second.id()),
            "E2 elapsed time or registered recipe mismatched");

        Path root=Files.createTempDirectory("veil-second-era-live-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restored=new ArenaSystems(seed);
            require(HostCheckpoint.load(restored,root),"Paired E2 realm checkpoint unreadable");
            restored.join(1);
            require(restored.assignAccount(1,key),"E2 owner identity lost on load");
            World.Device live=restored.world.devices.stream()
                .filter(d->d.kind==stage).findFirst().orElseThrow();
            World.Device warehouse=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            require(live.productionActive&&live.productionMs==90_000&&
                second.id().equals(live.productionRecipeId),
                "V6 restart lost E2 recipe selection and production receipt");
            var p=restored.player(1);
            p.x=live.x+.5;p.y=live.y;
            simulate(restored,second.seconds()-90);
            var product=SurvivalCrafting.Item.valueOf(second.output());
            require(!live.productionActive&&live.productionRecipeId.isEmpty()&&
                live.productionCompleted==1&&warehouse.storage.count(product)==
                    second.quantity(),
                "Second era final product not delivered once to physical storage");
            simulate(restored,5);
            require(warehouse.storage.count(product)==second.quantity(),
                "Idle machine replayed output");
            HostCheckpoint.save(restored,root);
            ArenaSystems secondLoad=new ArenaSystems(seed);
            require(HostCheckpoint.load(secondLoad,root),"Finished E2 checkpoint unreadable");
            World.Device survived=secondLoad.world.devices.stream()
                .filter(d->d.kind==stage).findFirst().orElseThrow();
            require(!survived.productionActive&&survived.productionCompleted==1,
                "Completed E2 job restored as active");
            require(secondLoad.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(product)).sum()==second.quantity(),
                "E2 account/realm restart duplicated finished inventory");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
        System.out.println("PASS distinct second era: "+faction+" "+second.id());
    }
    public static void main(String[] args)throws Exception{
        run(ArenaSystems.UNRAVELED_MACHINIST_BAY,CharacterRoster.Faction.THE_UNRAVELED);
        run(ArenaSystems.PENITENT_RELIQUARY,CharacterRoster.Faction.THE_PENITENT);
        run(ArenaSystems.BEGOTTEN_SPORE_BASIN,CharacterRoster.Faction.THE_BEGOTTEN);
    }
}
