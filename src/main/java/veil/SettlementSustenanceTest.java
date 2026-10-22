package veil;

/** Faction restriction, predictable hourly burn and high-skill solo viability. */
public final class SettlementSustenanceTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(long seed:new long[]{11,42,20261021}){
            var world=new World(seed);
            var service=new SettlementSustenance(world);
            check(service.nodes().equals(new SettlementSustenance(new World(seed)).nodes()),
                "Seeded caches differ");
            for(var node:service.nodes())
                check(world.city.categoryAt((int)node.x(),(int)node.y())!=
                    CityLayout.Category.SAFE,"Cache in safe plot");
            check(service.register("base-unraveled",CharacterRoster.Faction.THE_UNRAVELED,50),
                "Cannot register");
            check(!service.deposit("base-unraveled",SettlementSustenance.Material.LOST_SOUL,5),
                "Wrong faction material");
            check(service.deposit("base-unraveled",
                SettlementSustenance.Material.RESONANCE_SHARD,12),"Cannot deposit");
            service.advance("base-unraveled",SettlementSustenance.HOUR);
            check(service.account("base-unraveled").stored()==6,"Wrong hourly debit");
            service.advance("base-unraveled",SettlementSustenance.HOUR);
            check(service.account("base-unraveled").stored()==0,"Second debit");
            service.advance("base-unraveled",SettlementSustenance.HOUR);
            check(!service.account("base-unraveled").active(),"No dormant state");
            check(service.register("base-monsters",CharacterRoster.Faction.THE_BEGOTTEN,50),
                "Cannot register monsters");
            check(service.lostKill("base-monsters",50)==4,
                "Max scavenging soul bonus");
            check(service.lostKill("base-monsters",50)==4,"Soul collection failed");
            check(service.account("base-monsters").stored()==8,"Soul credit missing");
            check(SettlementSustenance.hourlyCost(9)==0&&
                SettlementSustenance.hourlyCost(10)==1&&
                SettlementSustenance.hourlyCost(50)==6,"Level brackets");
        }
        System.out.println("PASS SettlementSustenanceTest");
    }
}
