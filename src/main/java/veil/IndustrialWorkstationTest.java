package veil;

import java.util.*;
/** Reproducible industrial reservation, interruptions, power failure and restart. */
public final class IndustrialWorkstationTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var press=new IndustrialWorkstation("metal-press",
            CraftingEraRegistry.Faction.UNRAVELED);
        check(press.deposit("METAL_BILLET",3)&&press.deposit("FASTENERS",2),
            "Metal press inputs");
        check(!press.start("req-0","controller-board"),
            "Cross-machine recipe forged");
        check(press.start("req-1","driveshaft"),"Can't reserve legitimate forging inputs");
        check(press.count("METAL_BILLET")==0&&press.count("FASTENERS")==0,
            "Industrial job did not debit reserved source materials");
        check(!press.start("req-1","driveshaft"),"Replay minted second work order");
        check(press.advance(1000,0,true)==IndustrialWorkstation.State.NO_POWER,
            "Factory ignores missing steam/mechanical power");
        check(press.active().elapsedMs()==0,"Unpowered machine progressed");
        check(press.advance(1000,2,false)==IndustrialWorkstation.State.NO_SERVICE,
            "Destroyed/disabled factory kept processing");
        check(press.abort(),"An unstarted job can return materials");
        check(press.count("METAL_BILLET")==3&&press.count("FASTENERS")==2,
            "Zero-progress cancellation lost starter materials");
        check(!press.start("req-1","driveshaft"),"Old idempotency key reused");
        check(press.start("req-2","driveshaft"),"New valid order cannot start");
        check(press.advance(1000,2,true)==IndustrialWorkstation.State.RUNNING,
            "Mechanical press never starts");
        var interrupted=press.snapshot();
        var restored=new IndustrialWorkstation("metal-press",CraftingEraRegistry.Faction.UNRAVELED);
        restored.restore(interrupted);
        check(restored.active()!=null&&restored.active().elapsedMs()==1000,
            "Machine lost partial input-reserved progress on restart");
        check(!restored.start("req-2","driveshaft"),"Restart duplicated active job");
        check(restored.advance(1000,0,true)==IndustrialWorkstation.State.NO_POWER,
            "Restart skips power control");
        for(int i=0;i<420;i++)restored.advance(1000,2,true);
        check(restored.active()==null&&restored.count("DRIVE_SHAFT")==1&&restored.completed()==1,
            "Production completion after restart lost or duplicated output");
        for(int i=0;i<20;i++)restored.advance(1000,2,true);
        check(restored.count("DRIVE_SHAFT")==1&&restored.completed()==1,
            "Extra idle ticks duplicate metal parts");
        var saved=restored.snapshot();
        var after=new IndustrialWorkstation("metal-press",CraftingEraRegistry.Faction.UNRAVELED);
        after.restore(saved);
        check(after.count("DRIVE_SHAFT")==1&&after.completed()==1&&
            !after.start("req-2","driveshaft"),
            "Saved completed job minted duplicates");
        check(after.withdraw("DRIVE_SHAFT",1)&&!after.withdraw("DRIVE_SHAFT",1),
            "One manufactured part taken twice");
        check(after.deposit("METAL_BILLET",3)&&after.deposit("FASTENERS",2),
            "Second production resources");
        check(after.start("req-3","driveshaft"),"Second request");
        after.advance(500,2,true);
        check(after.abort(),"Active job cannot be scrapped");
        check(after.count("METAL_BILLET")==0&&after.count("FASTENERS")==0,
            "Partial-progress abort refunded burned inputs");

        var incursion=new IndustrialWorkstation("incursion-anchor",
            CraftingEraRegistry.Faction.BEGOTTEN);
        check(!incursion.start("breach-1","breach-seed"),
            "Begotten anchor starts without 3 controllers, 8 nutrients and cargo");
        check(incursion.deposit("ASSEMBLY_CONTROLLER",3)&&
            incursion.deposit("NUTRIENT_STOCK",8)&&
            incursion.deposit("METAL_STOCK",4)&&
            incursion.deposit("MACHINE_PARTS",3),"Real endgame input feeds");
        check(incursion.start("breach-1","breach-seed"),"Valid expensive breach can't start");
        check(incursion.advance(1000,24,true)==IndustrialWorkstation.State.NO_POWER,
            "Incursion runs without required 25 utility budget");
        check(incursion.advance(1000,25,true)==IndustrialWorkstation.State.RUNNING,
            "Incursion failed to start consuming paid service");
        check(incursion.count("ASSEMBLY_CONTROLLER")==0&&
            incursion.count("NUTRIENT_STOCK")==0&&
            incursion.count("METAL_STOCK")==0,
            "Start of incursion didn't actually consume expensive inputs");
        try{new IndustrialWorkstation("incursion-anchor",CraftingEraRegistry.Faction.PENITENT);
            throw new AssertionError("Serial-killer faction stole Begotten reality machine");
        }catch(IllegalArgumentException expected){}
        var dread=new IndustrialWorkstation("dread-works",
            CraftingEraRegistry.Faction.PENITENT);
        check(!dread.start("dread-1","dread-ward"),
            "Dread works minted fuel without verified encounter imprints");
        System.out.println("PASS IndustrialWorkstationTest: host-side reservations, damage/power stop, "+
            "crash replay, single output and gated flesh-incursion industrial costs");
    }
}