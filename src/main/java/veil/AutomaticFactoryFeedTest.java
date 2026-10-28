package veil;

import java.nio.file.*;
import java.util.*;

/** Real two-stage E5 production -> physical sorter/conveyor -> paid E5 input.
 * Includes prioritized non-nearest crates, backwards transport, hostile
 * storage isolation, queued source monitoring and V11 cold-restart receipts. */
public final class AutomaticFactoryFeedTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static World.Device add(ArenaSystems host,int kind,double x,double y,
            String owner){
        var d=new World.Device(x,y,kind);d.ownerKey=owner;
        host.world.devices.add(d);return d;
    }
    private static double[] site(ArenaSystems host){
        double[] offsets={-8,-5,-2,0,2,5,8};
        for(int x=-110;x<=170;x+=4)for(int y=-110;y<=170;y+=4){
            boolean safe=true;
            for(double dx:offsets){
                if(host.world.worldStructureAt(x+dx,y,.55)||
                   !host.world.continent().inBounds(
                       host.world.parcelX(x+dx),host.world.parcelY(y))){
                    safe=false;break;
                }
            }
            double[][] extra={{-8,-3},{-4,2.5},{4,2.5},{0,3},{0,.5},{-4.7,.4}};
            for(double[] pos:extra)
                if(host.world.worldStructureAt(x+pos[0],y+pos[1],.55)||
                   !host.world.continent().inBounds(
                       host.world.parcelX(x+pos[0]),host.world.parcelY(y+pos[1])))
                    safe=false;
            if(safe&&host.world.canWalk(x-8,y)&&
               host.world.canWalk(x,y)&&host.world.canWalk(x+8,y))
                return new double[]{x,y};
        }
        throw new AssertionError("No accessible wilderness two-stage factory site");
    }
    private static void tick(ArenaSystems host,int seconds){
        for(int n=0;n<seconds;n++){
            host.advance(1000);
            for(var player:host.roster()){
                if(player.dead)continue;
                var status=player.survival.status();
                if(status.hunger()<45){
                    check(player.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,true),
                        "Food fixture could not sustain production worker");
                    check(host.consumeSurvival(player.id,SurvivalCrafting.Item.EDIBLES),
                        "Host refused valid survival meal");
                }
                if(status.hydration()<45){
                    check(player.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,true),
                        "Water fixture could not sustain production worker");
                    check(host.consumeSurvival(player.id,SurvivalCrafting.Item.CLEAN_WATER),
                        "Host refused valid hydration");
                }
            }
        }
    }
    private static void stock(World.Device crate,String item,int qty){
        check(crate.storage.delivered(SurvivalCrafting.Item.valueOf(item),qty),
            "Cannot stage paid "+item+" x"+qty);
    }
    private static void cleanup(Path root)throws Exception{
        try(var paths=Files.walk(root)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        String owner="acct_"+"e".repeat(64),foreign="acct_"+"a".repeat(64);
        var host=new ArenaSystems(seed);
        var player=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "E5 operator must be an authenticated faction member");
        double[] point=site(host);double x=point[0],y=point[1];
        add(host,ArenaSystems.CAMP_STAKE,x-8,y-3,owner);
        // Deliberately register an earlier and closer crate that cannot
        // reach the downstream belt. Production must NOT default to it.
        var decoy=add(host,ArenaSystems.STORAGE_CRATE,x-7.5,y+.4,owner);
        var nearWest=add(host,ArenaSystems.STORAGE_CRATE,x-2,y,owner);
        var actualSource=add(host,ArenaSystems.STORAGE_CRATE,x-5,y,owner);
        var nearEast=add(host,ArenaSystems.STORAGE_CRATE,x+2,y,owner);
        var actualTarget=add(host,ArenaSystems.STORAGE_CRATE,x+5,y,owner);
        var alien=add(host,ArenaSystems.STORAGE_CRATE,x-4.7,y+.4,foreign);
        var belt=add(host,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        add(host,ArenaSystems.AUTOMATIC_SORTER,x,y+.5,owner);
        var producer=add(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x-8,y,owner);
        var consumer=add(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x+8,y,owner);
        add(host,ArenaSystems.ELECTRIC_BUS,x-4,y+2.5,owner);
        add(host,ArenaSystems.ELECTRIC_BUS,x+4,y+2.5,owner);
        var generator=add(host,ArenaSystems.GENERATOR,x,y+3.0,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        stock(actualSource,"DRIVE_SHAFT",1);
        stock(actualSource,"CONDUCTOR_WIRE",3);
        stock(actualSource,"TRACKING_CIRCUIT",1);
        stock(alien,"PRECISION_FEEDLINE",9);
        stock(actualTarget,"HARDENED_ALLOY",2);
        stock(actualTarget,"FASTENERS",4);
        player.x=producer.x;player.y=producer.y;
        check(host.startFactionBatch(1,"precision-feedlines"),
            "Upstream real manufacturer refused exact paid E5 inputs");
        check(actualSource.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==0&&
            actualSource.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==0,
            "Upstream fabrication failed to debit real input warehouse");
        // Finish all but the last paid second before programming delivery.
        // This proves actual host output placement sees a live downstream
        // demand instead of merely depending on warehouse insertion order.
        tick(host,CraftingEraRegistry.process("precision-feedlines").seconds()-1);
        player.x=consumer.x;player.y=consumer.y;
        check(host.programNextAutomation(1,"ballistic-resupply"),
            "Downstream Unraveled E5 receipt failed to queue");
        player.x=belt.x;player.y=belt.y;
        var hostile=host.join(2);
        hostile.x=belt.x;hostile.y=belt.y;
        check(!host.programConveyor(2,"AUTO",false),
            "Unowned faction attempted to configure a private auto feeder");
        check(host.programConveyor(1,"AUTO",false)&&belt.routeItem.equals("AUTO"),
            "Real conveyor refused pending-job automatic routing");
        tick(host,1);
        check(!producer.productionActive&&producer.productionCompleted==1&&
            actualSource.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==2&&
            decoy.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==0,
            "Paid output was stranded in the first wrong crate instead of the conveyor source");
        var plan=ConveyorLogistics.autoInput(belt,host.world.devices);
        check(plan!=null&&plan.route().source()==actualSource&&
            plan.route().destination()==actualTarget,
            "Auto routing chose nearest wrong-side/other-owner crate rather than useful stock");
        consumer.queuedRecipeId=AutomationOrders.repeat("ballistic-resupply");
        check(ConveyorLogistics.autoInput(belt,host.world.devices)!=null,
            "A repeat-mode factory must remain eligible for AUTO input feeding");
        // Switch back to one shot for this original V11 delivery regression.
        consumer.queuedRecipeId="ballistic-resupply";
        tick(host,3);
        check(belt.routeMs==4000&&!consumer.productionActive&&
            actualTarget.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==0,
            "Factory was funded early without a completed paid conveyor tick");
        Path root=Files.createTempDirectory("veil-auto-e5-chain-");
        try{
            HostCheckpoint.save(host,root);
            var loaded=new ArenaSystems(seed);
            check(HostCheckpoint.load(loaded,root),
                "V11 AUTO route and queued factory failed checkpoint recovery");
            var resumed=loaded.join(1);
            check(loaded.assignAccount(1,owner),"Saved machine ownership lost");
            resumed.x=x;resumed.y=y;
            World.Device movedSource=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x==x-5)
                .findFirst().orElseThrow();
            World.Device movedTarget=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x==x+5)
                .findFirst().orElseThrow();
            World.Device movedBelt=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.CONVEYOR_LINE)
                .findFirst().orElseThrow();
            World.Device movedConsumer=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP&&d.x==x+8)
                .findFirst().orElseThrow();
            World.Device movedProducer=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP&&d.x==x-8)
                .findFirst().orElseThrow();
            World.Device movedAlien=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&foreign.equals(d.ownerKey))
                .findFirst().orElseThrow();
            check(movedBelt.routeItem.equals("AUTO")&&movedBelt.routeMs==4000&&
                movedConsumer.queuedRecipeId.equals("ballistic-resupply"),
                "Unfinished AUTO tick or work order disappeared in V11");
            tick(loaded,1);
            check(movedConsumer.productionActive&&
                movedConsumer.queuedRecipeId.isEmpty()&&
                movedConsumer.productionMs==0&&
                movedSource.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==1&&
                movedSource.storage.count(SurvivalCrafting.Item.TRACKING_CIRCUIT)==0&&
                movedTarget.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==0&&
                movedTarget.storage.count(SurvivalCrafting.Item.HARDENED_ALLOY)==0&&
                movedAlien.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==9,
                "Real sorted auto delivery/debit did not complete downstream recipe exactly once");
            check(ConveyorLogistics.autoInput(movedBelt,loaded.world.devices)==null,
                "Auto feeder kept spraying unrequested materials into downstream warehouse");
            // Reverse the same belt to restock the preceding workshop. The
            // original receiving crate is now the physically paid source.
            stock(movedTarget,"DRIVE_SHAFT",1);
            stock(movedTarget,"CONDUCTOR_WIRE",3);
            resumed.x=movedProducer.x;resumed.y=movedProducer.y;
            check(loaded.programNextAutomation(1,"precision-feedlines"),
                "Upstream machine failed to accept second paid batch order");
            resumed.x=x;resumed.y=y;
            check(loaded.programConveyor(1,"AUTO",true)&&movedBelt.routeReverse,
                "Host rejected reverse automatic E5 delivery");
            tick(loaded,10);
            check(movedSource.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==1||
                  movedProducer.productionActive,
                "Reverse AUTO failed to return physical input stock");
            tick(loaded,1); // released grid allocation after final sorted tick
            check(movedProducer.productionActive&&
                movedProducer.queuedRecipeId.isEmpty()&&
                movedTarget.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==0&&
                movedTarget.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==0,
                "Reverse E5 factory input chain did not launch exactly once");
            HostCheckpoint.save(loaded,root);
            var checked=new ArenaSystems(seed);
            check(HostCheckpoint.load(checked,root),
                "Second AUTO receipt did not survive restart");
            check(checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP&&d.productionActive)
                .count()==2&&
                checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.CONVEYOR_LINE)
                .allMatch(d->d.routeItem.equals("AUTO")&&d.routeReverse),
                "Dual active E5 receipts or programmed reverse direction lost");

            // Complete both already-paid E5 manufacturing receipts, rather
            // than ending the chain test after downstream factory launch.
            // First prove a genuine grid outage cannot advance either job.
            var finishWorker=checked.join(1);
            check(checked.assignAccount(1,owner),"Completion worker lost saved owner");
            var finishedProducer=checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP&&d.x==x-8)
                .findFirst().orElseThrow();
            var finishedConsumer=checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP&&d.x==x+8)
                .findFirst().orElseThrow();
            var finishedGenerator=checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.GENERATOR).findFirst().orElseThrow();
            var outputWarehouse=checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x==x+5)
                .findFirst().orElseThrow();
            long producerBefore=finishedProducer.productionMs;
            long consumerBefore=finishedConsumer.productionMs;
            finishedGenerator.electricMs=0; // explicit physical outage fixture
            tick(checked,7);
            check(finishedProducer.productionMs==producerBefore&&
                finishedConsumer.productionMs==consumerBefore&&
                finishedProducer.factoryHold.equals("WAIT_POWER")&&
                finishedConsumer.factoryHold.equals("WAIT_POWER"),
                "A depleted E5 generator advanced a paid production receipt");

            // Restore power via the ACTUAL authenticated generator action,
            // consuming physical fuel and stored steam startup pressure.
            var steam=add(checked,ArenaSystems.STEAM_BOILER,x+.4,y+3.5,owner);
            steam.steamMs=120_000;
            var fuel=add(checked,ArenaSystems.STORAGE_CRATE,x,y+4.0,owner);
            stock(fuel,"CHARCOAL",3);
            stock(fuel,"CLEAN_WATER",2);
            finishWorker.x=finishedGenerator.x;finishWorker.y=finishedGenerator.y;
            check(checked.chargeGenerator(1)&&
                finishedGenerator.electricMs==ElectricUtility.MAX_CHARGE_MS&&
                steam.steamMs==120_000-ElectricUtility.STEAM_STARTUP_MS&&
                fuel.storage.count(SurvivalCrafting.Item.CHARCOAL)==0&&
                fuel.storage.count(SurvivalCrafting.Item.CLEAN_WATER)==0,
                "Restart manufactured electricity without fuel or pressurized steam");
            // A finished downstream recipe cannot claim completion if its
            // only reachable same-owner output crate is physically full.
            stock(outputWarehouse,"EDIBLES",CampStockpile.MAX_UNITS);
            tick(checked,CraftingEraRegistry.process("ballistic-resupply").seconds());
            check(finishedProducer.productionCompleted==2&&
                !finishedProducer.productionActive&&
                finishedConsumer.productionActive&&
                finishedConsumer.productionMs==
                    CraftingEraRegistry.process("ballistic-resupply").seconds()*1000L&&
                finishedConsumer.factoryHold.equals("WAIT_OUTPUT")&&
                finishedConsumer.productionCompleted==0&&
                outputWarehouse.storage.count(SurvivalCrafting.Item.BALLISTIC_SUPPLY)==0,
                "Completed downstream work bypassed full physical output storage");
            check(outputWarehouse.storage.consume(
                Map.of(SurvivalCrafting.Item.EDIBLES,CampStockpile.MAX_UNITS)),
                "Could not free real output warehouse space");
            tick(checked,1);
            int physicalSupply=checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&
                    owner.equals(d.ownerKey))
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.BALLISTIC_SUPPLY))
                .sum();
            int physicalFeedlines=checked.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&
                    owner.equals(d.ownerKey))
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE))
                .sum();
            check(!finishedConsumer.productionActive&&
                finishedConsumer.productionCompleted==1&&
                physicalSupply==2&&physicalFeedlines==3&&
                movedAlien.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==9,
                "Recovery minted output, double-spent upstream goods or touched foreign inventory");
            HostCheckpoint.save(checked,root);
            var last=new ArenaSystems(seed);
            check(HostCheckpoint.load(last,root),"Finished two-stage receipts failed V11 restart");
            check(last.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP)
                .noneMatch(d->d.productionActive)&&
                last.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP)
                .mapToInt(d->d.productionCompleted).sum()==3&&
                last.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey))
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.BALLISTIC_SUPPLY))
                .sum()==2,
                "Cold restart repeated, erased or duplicated completed downstream E5 output");
        }finally{cleanup(root);}
        System.out.println("PASS AutomaticFactoryFeedTest: paid two-stage E5 completion, real grid outage/refuel, output backpressure, sorted reverse routing, V11 restart, owner isolation");
    }
}
