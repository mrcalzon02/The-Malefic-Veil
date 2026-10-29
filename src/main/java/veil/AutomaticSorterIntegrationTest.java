package veil;

import java.nio.file.*;
import java.util.*;

/** A real same-owner E5 sorter doubles a powered conveyor's throughput,
 * consuming twice the electrical demand, without minting transported stock. */
public final class AutomaticSorterIntegrationTest {
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    private static World.Device device(ArenaSystems world,int kind,
                                       double x,double y,String owner){
        var d=new World.Device(x,y,kind);d.ownerKey=owner;world.world.devices.add(d);return d;
    }
    private static void step(ArenaSystems world,int seconds){
        for(int n=0;n<seconds;n++)world.advance(1000);
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        String owner="acct_"+"c".repeat(64);
        ArenaSystems host=new ArenaSystems(seed);
        var operator=host.join(1);
        check(host.assignAccount(1,owner),"Owner account missing");
        double[] at=FactionFirstProductionTest.site(host);
        double x=at[0],y=at[1];
        var source=device(host,ArenaSystems.STORAGE_CRATE,x-2.6,y,owner);
        var target=device(host,ArenaSystems.STORAGE_CRATE,x+2.6,y,owner);
        var conveyor=device(host,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        var sorter=device(host,ArenaSystems.AUTOMATIC_SORTER,x+.4,y+.6,owner);
        device(host,ArenaSystems.ELECTRIC_BUS,x+.4,y+2,owner);
        var generator=device(host,ArenaSystems.GENERATOR,x-.6,y+3.1,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        check(source.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,5),
            "Cannot stage physical source goods");
        check(ConveyorLogistics.sorted(conveyor,host.world.devices),
            "A living close sorter should enhance the real conveyor");
        operator.x=x;operator.y=y;
        check(host.programConveyor(1,"MACHINE_PARTS"),"Owner could not program conveyor");
        step(host,5);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==3&&
            target.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2,
            "Sorter must deliver two paid units per completed cycle");
        // Almost-empty plants must not fund an entire conveyor tick.
        generator.electricMs=1;
        step(host,1);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==3&&
            target.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2&&
            conveyor.routeMs==0&&generator.electricMs==1&&
            conveyor.factoryHold.equals("WAIT_POWER"),
            "One millisecond of electricity granted free sorted belt progress");
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        sorter.hp=0;
        check(!ConveyorLogistics.sorted(conveyor,host.world.devices),
            "Destroyed sorter still accelerates logistics");
        step(host,5);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2&&
            target.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==3,
            "Damaged sorter must restore basic single-item belt throughput");
        sorter.hp=100;
        Path root=Files.createTempDirectory("veil-e5-sorter-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),"Physical sorter save cannot recover");
            var input=restored.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&
                d.x<x).findFirst().orElseThrow();
            var output=restored.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&
                d.x>x).findFirst().orElseThrow();
            step(restored,5);
            check(input.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==0&&
                output.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==5,
                "Recovered E5 sorter must finish the remaining two physical items");
            step(restored,10);
            check(output.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==5,
                "Empty routed stock must not produce phantom items");
            HostCheckpoint.save(restored,root);
            ArenaSystems last=new ArenaSystems(seed);
            check(HostCheckpoint.load(last,root),"Paired recovery of sorted material failed");
            check(last.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)).sum()==5,
                "E5 sorter minted or lost stock after restart");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
        System.out.println("PASS AutomaticSorterIntegrationTest: powered sorter 2x throughput, damage downgrade, persistent stock and no replay");
    }
}
