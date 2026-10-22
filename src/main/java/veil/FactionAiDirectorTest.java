package veil;

import java.nio.file.*;
import java.util.*;

/** Host-only AI population, group cap and actual material-gathering regression. */
public final class FactionAiDirectorTest {
    private static void check(boolean valid,String why){
        if(!valid)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        var balanced=HostAiSettings.fromOptions(List.of("--ai-count=5",
            "--ai-mode=BALANCED","--ai-level=7"));
        check(Arrays.equals(balanced.counts(),new int[]{2,2,1}),
            "Five AI must split evenly across the three factions");
        var monsters=HostAiSettings.fromOptions(List.of("--ai-count=6",
            "--ai-mode=SINGLE_FACTION","--ai-team=3","--ai-level=4"));
        check(Arrays.equals(monsters.counts(),new int[]{0,0,6}),
            "Six AI all on faction three means all Begotten");
        check(monsters.teamFor(0)==CharacterRoster.Faction.THE_BEGOTTEN&&
              monsters.teamFor(5)==CharacterRoster.Faction.THE_BEGOTTEN,
            "Forced Begotten team for every AI");
        var custom=HostAiSettings.fromOptions(List.of("--ai-count=6",
            "--ai-mode=CUSTOM","--ai-split=1,2,3"));
        check(Arrays.equals(custom.counts(),new int[]{1,2,3}),
            "Explicit team distribution");
        for(var invalid:List.of(
            List.of("--ai-count=25"),
            List.of("--ai-count=6","--ai-mode=CUSTOM","--ai-split=2,2,1"),
            List.of("--ai-count=3","--ai-level=51"),
            List.of("--ai-count=6","--ai-mode=SINGLE_FACTION","--ai-team=4"))){
            try{HostAiSettings.fromOptions(invalid);throw new AssertionError("Accepted bad AI rule "+invalid);}
            catch(IllegalArgumentException ok){}
        }
        Path temp=Files.createTempDirectory("veil-ai-config-");
        try{
            HostAiSettings.save(temp,monsters);
            String saved=Files.readString(temp.resolve("ai-host-settings.properties"));
            check(saved.contains("SINGLE_FACTION")&&saved.contains("team=3"),
                "Host AI setup must be persisted, not client-side");
            check(HostAiSettings.bind(temp,monsters).equals(monsters),
                "Exactly matching AI population must restart without change");
            for(var different:List.of(
                    HostAiSettings.fromOptions(List.of("--ai-count=5","--ai-mode=BALANCED")),
                    HostAiSettings.fromOptions(List.of("--ai-count=6",
                        "--ai-mode=SINGLE_FACTION","--ai-team=2","--ai-level=4")))){
                try{
                    HostAiSettings.bind(temp,different);
                    throw new AssertionError("Allowed silent AI faction/count migration");
                }catch(java.io.IOException expected){
                    check(expected.getMessage().contains("migration"),
                        "Mismatched host settings must explain migration requirement");
                }
            }
            check(Files.readString(temp.resolve("ai-host-settings.properties")).equals(saved),
                "Rejected host changes must never overwrite verified saved settings");
        }finally{
            try(var tree=Files.walk(temp)){
                tree.sorted(Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception error){throw new IllegalStateException(error);}
                });
            }
        }
        ArenaSystems arena=new ArenaSystems(20261021L);
        arena.world.npcs.clear();
        var gathering=new WorldHarvesting(arena.world);
        var director=new FactionAiDirector(arena,gathering,monsters);
        check(director.views().size()==6,"Six actual host actors must be created");
        check(arena.roster().size()==6,"Bots must be actual server actors, not fake UI tokens");
        var crews=arena.factions.allCrews();
        check(crews.size()==2&&crews.stream().allMatch(c->c.members().size()==3),
            "6 faction three bots form 2 squads of 3; not 6-person squads");
        // Squad mates must physically begin near their founder to perform
        // real later material handoffs, without cross-map travel cheats.
        var list=director.views();
        for(int i=0;i<list.size();i+=3){
            var founder=list.get(i);
            for(int j=i+1;j<Math.min(i+3,list.size());j++){
                var mate=list.get(j);
                check(Math.hypot(mate.x()-founder.x(),mate.y()-founder.y())<9.0,
                    "Three-character AI squads must be located together in the same world");
                check(arena.world.lineClear(founder.x(),founder.y(),mate.x(),mate.y()),
                    "AI group spawn must have physical line of sight for initial cooperation");
            }
        }
        for(var bot:director.views()){
            var player=arena.player(bot.id());
            check(player!=null&&player.id<0&&!arena.world.detailedCity(player.x,player.y),
                "AI must begin in actual continuous wilderness, not client teleport scene");
            check(arena.world.canWalk(player.x,player.y),"AI begins on real traversable terrain");
            check(player.progression.overallLevel()==4,"Configured character levels applied to actors");
            check(arena.factions.faction(player.accountKey)==bot.faction(),
                "Faction assignment is real authoritative affiliation");
            check(arena.allied(-10000,bot.id()),"Begotten must be permanently allied");
        }
        // Four members cannot be forced into a three-character squad.
        var extra=arena.join(77);
        String fourth="acct_"+("a".repeat(64));
        check(arena.assignAccount(77,fourth)&&
              arena.enrollFaction(77,CharacterRoster.Faction.THE_BEGOTTEN),
            "Fourth distinct Begotten account registered");
        String founder=arena.player(-10000).accountKey;
        check(!arena.factions.invite(founder,fourth,arena.time()),
            "Exactly three members maximum per crew");
        int startingNodes=gathering.resources.snapshot().depletion().size();
        double oldX=director.views().get(0).x(),oldY=director.views().get(0).y();
        for(int step=0;step<250;step++){
            arena.advance(200);
            gathering.advance(200);
            director.advance(200);
        }
        boolean moved=director.views().stream().anyMatch(v->
            Math.hypot(v.x()-oldX,v.y()-oldY)>.4);
        int stock=0;
        for(var bot:director.views())
            stock+=arena.player(bot.id()).survival.mass();
        check(moved||stock>0,"AI must physically move or gather in shared world");
        check(stock>0||gathering.resources.snapshot().depletion().size()>startingNodes,
            "Actual harvesting must occur: no pretend resources");
        check(arena.factions.allCrews().stream().allMatch(c->c.members().size()<=3),
            "The crew cap must remain true after autonomous simulation");
        Path stockSave=Files.createTempDirectory("veil-ai-stock-");
        try{
            director.save(stockSave);
            ArenaSystems afterBoot=new ArenaSystems(20261021L);
            afterBoot.world.npcs.clear();
            var restoredHarvest=new WorldHarvesting(afterBoot.world);
            var restoredDirector=new FactionAiDirector(afterBoot,restoredHarvest,monsters);
            restoredDirector.load(stockSave);
            for(var view:director.views()){
                check(arena.player(view.id()).survival.status().equals(
                    afterBoot.player(view.id()).survival.status()),
                    "Real AI carried materials and hunger must survive a host reboot");
            }
            byte[] damaged=Files.readAllBytes(stockSave.resolve("ai-carry.dat"));
            damaged[damaged.length-1]^=0x5f;
            Files.write(stockSave.resolve("ai-carry.dat"),damaged);
            try{
                restoredDirector.load(stockSave);
                throw new AssertionError("Tampered AI resource journal accepted");
            }catch(java.io.IOException expected){}
        }finally{
            try(var entries=Files.walk(stockSave)){
                entries.sorted(Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}
                    catch(Exception error){throw new IllegalStateException(error);}
                });
            }
        }
        // A physical camp cannot appear without a genuinely crafted kit.
        var builder=arena.player(-10000);
        int previouslyBuilt=arena.world.devices.size();
        builder.cash=150;builder.scrap=4;
        for(int angle=0;angle<8;angle++){
            builder.angle=angle*Math.PI/4;
            if(arena.reviewBuild(builder.id,ArenaSystems.CAMP_STAKE).reason()
                .equals("REQUIRES_CRAFTED_TENT"))break;
        }
        boolean placedWithoutKit=arena.buildOutcome(builder.id,ArenaSystems.CAMP_STAKE).built();
        check(!placedWithoutKit,"Host cannot magically build field camp without crafted tent");
        check(arena.world.devices.size()==previouslyBuilt,
            "Failed camp build must never create phantom structure");
        check(builder.survival.acquire(SurvivalCrafting.Item.TENT_KIT,1,false),
            "Simulate actually carried tent kit for cost/debit regression");
        boolean physicallyBuilt=false;
        for(int direction=0;direction<8;direction++){
            builder.angle=direction*Math.PI/4;
            if(arena.buildOutcome(builder.id,ArenaSystems.CAMP_STAKE).built()){
                physicallyBuilt=true;break;
            }
        }
        check(physicallyBuilt,"An owned field camp must be placeable on legitimate wilderness");
        check(builder.survival.count(SurvivalCrafting.Item.TENT_KIT)==0,
            "Physical camp must debit real player-carried crafted material");
        var device=arena.world.devices.get(arena.world.devices.size()-1);
        check(device.kind==ArenaSystems.CAMP_STAKE&&
              builder.accountKey.equals(device.ownerKey),"Camp is physical and character-owned");
        check(!arena.buildOutcome(builder.id,ArenaSystems.CAMP_STAKE).built(),
            "A character cannot found an unlimited number of camps");
        System.out.println("PASS FactionAiDirectorTest: count/team/level host rules, real NPC actors, "+
            "squads of three, world movement, honest harvesting and durable settings");
    }
}
