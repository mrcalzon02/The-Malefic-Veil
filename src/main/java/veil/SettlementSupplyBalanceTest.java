package veil;

public final class SettlementSupplyBalanceTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        check(SettlementSupplyBalance.MAX_NORMAL_FACILITIES==28,
            "owner cap is 28 normal facilities");
        check(SettlementSupplyBalance.MAX_DEFENSES==10,"max defensive slots is ten");
        check(SettlementSupplyBalance.powerPerDefense(50)==32,"top tower power draw");
        check(SettlementSupplyBalance.ammoPerDefense(50)==7,"top tower munitions draw");
        check(!SettlementSupplyBalance.canFullySupplyWithoutTrade(10,50),
            "no 28-slot base can sustain ten max-level towers alone");
        var fortress=SettlementSupplyBalance.plan(10,50,8,12);
        check(fortress.powerProduced()>=fortress.powerDemand(),
            "fortress can reserve enough slots for power plants");
        check(fortress.importedMunitionUnits()>0&&!fortress.feasibleLocally()&&
              fortress.feasibleWithImports(),
            "fortress must import actual ammunition stock to stay powered and armed");
        check(SettlementSupplyBalance.canFullySupplyWithoutTrade(1,1),
            "starting one-tower defense can be supported locally");
        int best=999;
        for(int generation=0;generation<=20;generation++)
            for(int munitions=0;munitions<=20-generation;munitions++){
                var plan=SettlementSupplyBalance.plan(10,50,generation,munitions);
                if(plan.feasibleWithImports())
                    best=Math.min(best,plan.importedMunitionUnits());
                check(!plan.feasibleLocally(),
                    "no permutation can evade the specialization cap");
            }
        check(best>0,"every full 10x50 battery needs other settlements");
        System.out.println("PASS SettlementSupplyBalanceTest: 28 slots cannot self-sustain full-level defense");
    }
}
