package veil;

import java.nio.file.*;
import java.util.*;

/** Read-only per-account settlement-wide production report: stable indexed
 * pages, real factory/conveyor holds, privacy, V11 restore, actual TCP wire. */
public final class ProductionOverviewTest {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static World.Device add(ArenaSystems game,int kind,
            double x,double y,String owner){
        var d=new World.Device(x,y,kind);
        d.ownerKey=owner;
        game.world.devices.add(d);
        return d;
    }
    private static void until(java.util.function.BooleanSupplier check,String why)
            throws Exception{
        long deadline=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<deadline){
            if(check.getAsBoolean())return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out: "+why);
    }
    private static void clean(Path root)throws Exception{
        try(var paths=Files.walk(root)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261020L;
        final String owner="acct_"+"e".repeat(64),foreign="acct_"+"a".repeat(64);
        final String repeat=AutomationOrders.repeat("precision-feedlines");
        ArenaSystems game=new ArenaSystems(seed);
        var player=game.join(1);
        check(game.assignAccount(1,owner)&&
            game.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Owner account/faction missing");
        double[] point=FactionFirstProductionTest.site(game);
        double x=point[0],y=point[1];
        add(game,ArenaSystems.CAMP_STAKE,x-2,y-2,owner);
        var stock=add(game,ArenaSystems.STORAGE_CRATE,x+1,y+.1,owner);
        // The GPU camp menu now exposes ten-at-once and full-stack requests.
        // Verify those quantities remain one atomic host-owned transaction,
        // not ten client-side mutations or an item minting shortcut.
        int priorFiber=player.survival.count(SurvivalCrafting.Item.FIBER);
        check(player.survival.acquire(SurvivalCrafting.Item.FIBER,10,true),
            "Unable to stage genuine carried bulk material");
        player.x=x;player.y=y;
        check(game.transferCampStock(1,false,SurvivalCrafting.Item.FIBER,10)&&
            stock.storage.count(SurvivalCrafting.Item.FIBER)==10&&
            player.survival.count(SurvivalCrafting.Item.FIBER)==priorFiber,
            "Ten-item host deposit failed to debit real carried inventory once");
        check(!game.transferCampStock(1,false,SurvivalCrafting.Item.FIBER,128)&&
            stock.storage.count(SurvivalCrafting.Item.FIBER)==10,
            "Unaffordable full-stack deposit altered physical stock");
        check(game.transferCampStock(1,true,SurvivalCrafting.Item.FIBER,10)&&
            stock.storage.count(SurvivalCrafting.Item.FIBER)==0&&
            player.survival.count(SurvivalCrafting.Item.FIBER)==priorFiber+10,
            "Ten-item host withdrawal did not return exactly the stored inventory");
        check(player.survival.debit(SurvivalCrafting.Item.FIBER,10),
            "Cannot reset paid transfer fixture without a phantom item");
        var bus=add(game,ArenaSystems.ELECTRIC_BUS,x,y+3,owner);
        var generator=add(game,ArenaSystems.GENERATOR,x+.8,y+3,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        var first=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x,y,owner);
        var second=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x+5,y,owner);
        add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x+2,y+3,foreign);
        var belt=add(game,ArenaSystems.CONVEYOR_LINE,x+2,y-3,owner);
        var third=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x+10,y,owner);
        var fourth=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x+15,y,owner);
        var secondBelt=add(game,ArenaSystems.CONVEYOR_LINE,x+12,y-3,owner);
        add(game,ArenaSystems.CONVEYOR_LINE,x+2,y-4,foreign);
        player.x=x;player.y=y;
        check(game.programNextAutomation(1,repeat),
            "Owner could not queue canonical repeating E5 recipe");
        var queued=game.productionOverview(1,0);
        check(queued.total()==6&&queued.entries().size()==4,
            "All and only six owned live production nodes must be reported");
        check(queued.entries().get(0).index()==game.world.devices.indexOf(first)&&
            queued.entries().get(1).index()==game.world.devices.indexOf(second)&&
            queued.entries().get(2).index()==game.world.devices.indexOf(belt)&&
            queued.entries().get(3).index()==game.world.devices.indexOf(third),
            "Production pages changed stable world device order or included foreign property");
        var firstEntry=queued.entries().get(0);
        generator.electricMs=1;
        var lowCharge=game.productionOverview(1,0).entries().get(0);
        check(lowCharge.state().equals("WAIT_POWER")&&
            lowCharge.remedy().equals("RESTORE_ELECTRIC_SUPPLY")&&
            first.queuedRecipeId.equals(repeat)&&generator.electricMs==1,
            "Remote overview suggested a queued machine can run on sub-tick power");
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        check(firstEntry.state().equals("WAIT_INPUT")&&
            firstEntry.recipe().equals("precision-feedlines")&&
            firstEntry.detail().equals("CONDUCTOR_WIRE_3")&&
            firstEntry.next().equals("REPEAT_precision-feedlines")&&
            firstEntry.remedy().equals("STOCK_REQUIRED_INPUT_NEAR_SITE"),
            "Overview lost real queued recipe, shortage or repeat authorization");
        check(queued.entries().get(2).state().equals("DISABLED")&&
            queued.entries().get(2).facility().equals("CONVEYOR_LINE")&&
            queued.entries().get(2).remedy().equals("PROGRAM_CONVEYOR_ROUTE_LOCALLY")&&
            queued.entries().get(1).remedy().equals("PROGRAM_WORKSITE_LOCALLY"),
            "Overview omitted real conveyor status");
        // The remedy must be based on physical, same-owner stock, never
        // foreign-account warehouses or an invented material source.
        var foreignCrate=add(game,ArenaSystems.STORAGE_CRATE,x+40,y+40,foreign);
        check(foreignCrate.storage.delivered(SurvivalCrafting.Item.CONDUCTOR_WIRE,4),
            "Could not stage foreign test inputs");
        check(game.productionOverview(1,0).entries().get(0).remedy().equals(
            "STOCK_REQUIRED_INPUT_NEAR_SITE"),
            "Overview leaked a different account's remote supply");
        var remote=add(game,ArenaSystems.STORAGE_CRATE,x+42,y+42,owner);
        int sourceIndex=game.world.devices.indexOf(remote);
        check(remote.storage.delivered(SurvivalCrafting.Item.CONDUCTOR_WIRE,3),
            "Could not stage authentic owner stock");
        var remoteBefore=remote.storage.snapshot();
        check(game.productionOverview(1,0).entries().get(0).remedy().equals(
            "TRANSFER_FROM_CRATE_"+sourceIndex)&&
            remote.storage.snapshot().equals(remoteBefore),
            "Owner's real remote input source was missed or report moved stock");
        // Supplying remote material is NOT a remedy while every local
        // receiving crate is full. The report must expose this physical
        // blocker without consuming the source or any stored ingredients.
        check(stock.storage.delivered(SurvivalCrafting.Item.EDIBLES,
            CampStockpile.MAX_UNITS),"Cannot saturate genuine local input warehouse");
        var blockedCrateBefore=stock.storage.snapshot();
        check(game.productionOverview(1,0).entries().get(0).remedy().equals(
            "FREE_NEARBY_INPUT_CRATE_SPACE")&&
            stock.storage.snapshot().equals(blockedCrateBefore)&&
            remote.storage.snapshot().equals(remoteBefore),
            "Full input warehouse was incorrectly described as reachable remote stock");
        check(stock.storage.consume(Map.of(
            SurvivalCrafting.Item.EDIBLES,CampStockpile.MAX_UNITS)),
            "Could not reopen local factory receiving space");
        check(game.productionOverview(1,0).entries().get(0).remedy().equals(
            "TRANSFER_FROM_CRATE_"+sourceIndex),
            "Freeing the worksite warehouse did not restore the physical stock recommendation");
        check(remote.storage.consume(Map.of(SurvivalCrafting.Item.CONDUCTOR_WIRE,2)),
            "Could not stage partial source inventory");
        check(game.productionOverview(1,0).entries().get(0).remedy().equals(
            "PARTIAL_INPUT_AT_CRATE_"+sourceIndex),
            "Partial remote inputs were incorrectly claimed sufficient");
        game.world.devices.remove(remote);
        game.world.devices.remove(foreignCrate);
        // The factory is short of conductor wire, but its queued job has
        // a physically reachable, same-owner AUTO belt source. Diagnosis
        // must recognize that actual transport path before saying to haul.
        var feederSource=add(game,ArenaSystems.STORAGE_CRATE,x-7,y,owner);
        var feeder=add(game,ArenaSystems.CONVEYOR_LINE,x-4,y,owner);
        int feederIndex=game.world.devices.indexOf(feeder);
        check(feederSource.storage.delivered(SurvivalCrafting.Item.CONDUCTOR_WIRE,3),
            "Could not stage real automatic conveyor stock");
        player.x=feeder.x;player.y=feeder.y;
        check(game.programConveyor(1,"AUTO")&&feeder.routeItem.equals("AUTO"),
            "Owner could not program source-connected AUTO belt");
        player.x=x;player.y=y;
        check(game.productionOverview(1,0).entries().get(0).remedy().equals(
            "AUTO_FEED_VIA_CONVEYOR_"+feederIndex),
            "Blocked factory failed to identify an actual owned AUTO feeder");
        for(int n=0;n<5;n++)game.advance(1000);
        check(stock.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==1&&
            feederSource.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==2&&
            first.queuedRecipeId.equals(repeat)&&!first.productionActive,
            "Suggested AUTO recovery did not move exactly one real paid unit");
        // Clean this isolated feeder test before checking durable overview
        // pagination, original power and inventory read-only invariants.
        check(stock.storage.consume(Map.of(SurvivalCrafting.Item.CONDUCTOR_WIRE,1)),
            "Could not reset receiver after paid conveyor movement");
        game.world.devices.remove(feeder);
        game.world.devices.remove(feederSource);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        // Atomic recipe payment requires every ingredient in ONE local crate,
        // even when separate nearby crates jointly hold the entire recipe.
        var split=add(game,ArenaSystems.STORAGE_CRATE,x-1,y,owner);
        check(stock.storage.delivered(SurvivalCrafting.Item.CONDUCTOR_WIRE,3)&&
            split.storage.delivered(SurvivalCrafting.Item.DRIVE_SHAFT,1),
            "Could not stage split inputs");
        var divided=game.productionOverview(1,0).entries().get(0);
        check(divided.state().equals("WAIT_COMBINE")&&
            divided.remedy().equals("COMBINE_ALL_INGREDIENTS_IN_ONE_CRATE"),
            "Split owned inputs were not diagnosed as an atomic-payment blockage");
        game.world.devices.remove(split);
        check(stock.storage.consume(Map.of(SurvivalCrafting.Item.CONDUCTOR_WIRE,3)),
            "Could not restore unstocked overview fixture");
        var later=game.productionOverview(1,1);
        check(later.total()==6&&later.entries().size()==2&&
            later.entries().get(0).index()==game.world.devices.indexOf(fourth)&&
            later.entries().get(1).index()==game.world.devices.indexOf(secondBelt),
            "Second page not stable or did not include final owned conveyor");
        check(game.productionOverview(1,2).entries().isEmpty()&&
            game.productionOverview(1,2).total()==6,
            "Out-of-range page must not invent devices");
        check(game.productionOverview(1,-1).total()==0&&
            game.productionOverview(1,10_001).total()==0,
            "Invalid page bypassed bounded report");
        var unknown=game.join(2);
        unknown.x=x;unknown.y=y;
        check(game.productionOverview(2,0).entries().isEmpty(),
            "Unauthenticated visitor enumerated private worksites");
        check(game.assignAccount(2,foreign),"Independent foreign account missing");
        check(game.productionOverview(2,0).total()==2&&
            game.productionOverview(2,0).entries().stream().noneMatch(e->
                e.index()==game.world.devices.indexOf(first)),
            "Foreign account saw owner production records");
        player.dead=true;
        check(game.productionOverview(1,0).total()==0,
            "Dead account remotely inspected its settlement");
        player.dead=false;
        player.x=x+100;player.y=y+100;
        check(game.productionOverview(1,0).total()==6,
            "Settlement overview incorrectly required local machine proximity");
        var originalStock=stock.storage.snapshot();
        long remaining=generator.electricMs;
        check(("PRODUCTION_OVERVIEW|"+game.productionOverview(1,0).wire()).split("\\|").length==36&&
            stock.storage.snapshot().equals(originalStock)&&generator.electricMs==remaining&&
            first.queuedRecipeId.equals(repeat),
            "Read-only overview mutated stock, energy or future work");
        third.hp=0;
        check(game.productionOverview(1,0).total()==5,
            "Destroyed production worksites must not be listed as operational");
        third.hp=100;
        Path root=Files.createTempDirectory("veil-production-overview-");
        try{
            HostCheckpoint.save(game,root);
            var saved=new ArenaSystems(seed);
            check(HostCheckpoint.load(saved,root),"V11 host checkpoint rejected overview fixture");
            var resumed=saved.join(1);
            check(saved.assignAccount(1,owner),"Restart lost factory owner");
            resumed.x=x+100;resumed.y=y+100;
            var restored=saved.productionOverview(1,0);
            check(restored.total()==6&&
                restored.entries().get(0).state().equals("WAIT_INPUT")&&
                restored.entries().get(0).next().equals("REPEAT_precision-feedlines")&&
                saved.productionOverview(1,1).entries().size()==2,
                "Cold restart lost durable orders or stable production page traversal");
        }finally{clean(root);}
        // The live LAN path must honor the actual AUTH_OK identity; no test
        // account is injected before the server's authentication handshake.
        try(LanServer server=new LanServer(0,seed);
            NetLink client=new NetLink("127.0.0.1",server.port())){
            until(()->client.accountKey()!=null,"real TCP account authentication");
            String identity=client.accountKey();
            synchronized(server.match){
                for(int i=0;i<5;i++)
                    add(server.match,ArenaSystems.INDUSTRIAL_WORKSHOP,
                        x+i*4,y,identity);
            }
            client.requestProductionOverview(0);
            until(()->client.productionOverview!=null,"TCP first production page");
            check(client.productionOverview.total()==5&&
                client.productionOverview.entries().size()==4&&
                client.productionOverview.page()==0&&
                client.productionOverview.entries().get(0).remedy().equals(
                    "PROGRAM_WORKSITE_LOCALLY"),
                "LAN failed to deserialize the authentic first four own worksites");
            client.requestProductionOverview(1);
            until(()->client.productionOverview!=null&&
                client.productionOverview.page()==1,"TCP second production page");
            check(client.productionOverview.entries().size()==1&&
                client.productionOverview.total()==5,
                "LAN second page dropped or duplicated production entries");
            try(NetLink stranger=new NetLink("127.0.0.1",server.port())){
                stranger.requestProductionOverview(0);
                until(()->stranger.productionOverview!=null,
                    "TCP foreign/duplicate guest report");
                check(stranger.productionOverview.total()==0&&
                    stranger.productionOverview.entries().isEmpty(),
                    "Network report exposed a different account's industry");
            }
        }
        System.out.println("PASS ProductionOverviewTest: owned multi-page factory/belt states, shortages, privacy, V11 and TCP");
    }
}
