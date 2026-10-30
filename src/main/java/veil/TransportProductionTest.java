package veil;

import java.util.*;

/** Real production DAG, distinct faction access and energy/time checks. */
public final class TransportProductionTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void manufacture(IndustrialWorkstation station,
            TransportProduction.Product product,CharacterRoster.Faction faction){
        var p=TransportProduction.recipe(product,faction);
        for(var e:p.inputs().entrySet()){
            check(station.deposit(e.getKey(),e.getValue()),
                "Verified industrial ingredient could not be loaded");
        }
        String request="test-"+p.id();
        check(station.start(request,p.id()),"Paid industrial order rejected "+p.id());
        check(station.advance(1000,0,true)==IndustrialWorkstation.State.NO_POWER,
            "Factory produced transport part without utility supply");
        check(station.active().elapsedMs()==0,
            "Transport power shortage must pause real work");
        long ticks=p.seconds();
        for(long i=0;i<ticks;i++)
            station.advance(1000,p.utilityDemand(),true);
        check(station.active()==null&&station.count(p.output())==p.quantity(),
            "Transport recipe did not produce its actual output "+p.id());
        check(!station.start(request,p.id()),
            "Idempotency broken: repeated fabrication receipt accepted");
    }
    public static void main(String[] args){
        CraftingEraRegistry.validate();
        TransportProduction.validate();
        Set<String> models=new HashSet<>();
        int factories=0;
        for(var faction:CharacterRoster.Faction.values()){
            var yard=CraftingEraRegistry.facility(TransportProduction.transportWorks(faction));
            var shipyard=CraftingEraRegistry.facility(TransportProduction.shipyard(faction));
            check(yard.requires().contains("rail-freight-depot"),
                "Transport factory bypasses rail progression");
            check(shipyard.requires().contains("harbor-drydock")&&
                shipyard.requires().contains(TransportProduction.transportWorks(faction)),
                "Shipyard bypasses freight and maritime infrastructure");
            check(shipyard.utilityDemand()>=40,"Free shipyard utility");
            factories+=2;
            for(var kind:TransportProduction.Product.values()){
                var p=TransportProduction.recipe(kind,faction);
                check(models.add(p.output()),"Duplicate faction transport product");
                check(p.inputs().size()>=2&&p.quantity()>0,
                    "Free transport manufacturing "+p.id());
                check(p.era()==CraftingEraRegistry.Era.INCURSION,
                    "Pre-endgame carrier or locomotive bypass");
                check(p.utilityDemand()>=10,"Transport operates without industrial power");
            }
            check(TransportProduction.recipe(TransportProduction.Product.CARRIER,faction)
                    .inputs().get(TransportProduction.item(
                        TransportProduction.Product.CARRIER_KEEL,faction))==1,
                "Carrier has no manufactured keel");
        }
        check(models.size()==33&&factories==6,"Transport faction catalog incomplete");
        var own=CharacterRoster.Faction.THE_PENITENT;
        var station=new IndustrialWorkstation(TransportProduction.transportWorks(own),
            TransportProduction.industryFaction(own));
        var enemy=TransportProduction.recipe(TransportProduction.Product.RAIL_SECTION,
            CharacterRoster.Faction.THE_BEGOTTEN);
        check(!station.start("forged-enemy-order",enemy.id()),
            "Foreign faction could produce opponent's biological rails");
        manufacture(station,TransportProduction.Product.RAIL_SECTION,own);
        var ship=new IndustrialWorkstation(TransportProduction.shipyard(own),
            TransportProduction.industryFaction(own));
        manufacture(ship,TransportProduction.Product.CARRIER_KEEL,own);
        String keel=TransportProduction.item(TransportProduction.Product.CARRIER_KEEL,own);
        check(ship.withdraw(keel,1),"Finished keel must transfer as a real item");
        var finalProcess=TransportProduction.recipe(TransportProduction.Product.CARRIER,own);
        for(var input:finalProcess.inputs().entrySet()){
            if(input.getKey().equals(keel))continue;
            check(ship.deposit(input.getKey(),input.getValue()),"Could not deliver carrier goods");
        }
        check(!ship.start("carrier-no-keel",finalProcess.id()),
            "Carrier was forged without its manufactured keel");
        check(ship.deposit(keel,1),"Real keel handoff failed");
        check(ship.start("carrier-completion",finalProcess.id()),"Carrier assembly denied");
        check(ship.advance(1000,0,true)==IndustrialWorkstation.State.NO_POWER,
            "Powerless shipyard created a hull");
        for(long i=0;i<finalProcess.seconds();i++)
            ship.advance(1000,finalProcess.utilityDemand(),true);
        check(ship.count(finalProcess.output())==1,
            "Carrier not manufactured after real intermediate and metered assembly");
        check(ship.withdraw(finalProcess.output(),1)&&
            !ship.withdraw(finalProcess.output(),1),
            "Finished aircraft carrier duplicated after one physical transfer");
        System.out.println("PASS TRANSPORT_PRODUCTION 9 facilities, 33 faction recipes, "+
            "bounded power, real keel and no duplicate carrier");
    }
}
