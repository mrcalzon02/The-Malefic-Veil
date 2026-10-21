package veil;

public final class EquipmentUpgradesTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void denied(Runnable action,String expected){
        try{action.run();throw new AssertionError("Accepted forbidden upgrade: "+expected);}
        catch(IllegalStateException ex){
            check(ex.getMessage().equals(expected),"Wrong gate: "+ex.getMessage());
        }
    }
    public static void main(String[] args){
        for(EquipmentUpgrades.Class kind:EquipmentUpgrades.Class.values()){
            String id=switch(kind){
                case FACILITY -> "hand-workshop";
                case DEFENSE -> "aa-fast";
                case VEHICLE -> "ground-wagon";
            };
            int priorWork=0;
            double priorUpkeep=0;
            for(int level=1;level<50;level++){
                var p=EquipmentUpgrades.next(id,kind,level,50,
                    CraftingEraRegistry.Era.INCURSION);
                check(p.fromLevel()==level&&p.toLevel()==level+1,
                    "cannot purchase multi-level skips");
                check(p.workSeconds()>priorWork&&p.upkeepMultiplier()>priorUpkeep,
                    "more powerful objects must cost more work and upkeep");
                check(p.materials().values().stream().allMatch(n->n>0),
                    "upgrade has positive resource costs");
                priorWork=p.workSeconds();priorUpkeep=p.upkeepMultiplier();
            }
        }
        var high=EquipmentUpgrades.next("aa-heavy",EquipmentUpgrades.Class.DEFENSE,
            49,50,CraftingEraRegistry.Era.INCURSION);
        check(high.toLevel()==50&&high.visibleStage().equals("ENDGAME_SPECIALIZED"),
            "top level should be an actual industrial retrofit stage");
        denied(()->EquipmentUpgrades.next("aa-fast",EquipmentUpgrades.Class.DEFENSE,
            10,11,CraftingEraRegistry.Era.INCURSION),"OWNER_LEVEL_LOCKED");
        denied(()->EquipmentUpgrades.next("ground-wagon",EquipmentUpgrades.Class.VEHICLE,
            39,50,CraftingEraRegistry.Era.ELECTRIC),"RESEARCH_ERA_LOCKED");
        denied(()->EquipmentUpgrades.next("air-medium",EquipmentUpgrades.Class.VEHICLE,
            1,50,CraftingEraRegistry.Era.AUTOMATION),"RESEARCH_ERA_LOCKED");
        var farm=EquipmentUpgrades.next("forager-garden",EquipmentUpgrades.Class.FACILITY,
            1,2,CraftingEraRegistry.Era.ENCAMPMENT);
        check(farm.materials().containsKey("TIMBER"),"crude farms begin with local materials");
        System.out.println("PASS EquipmentUpgradesTest: all 49 paid stages x3, owner/era locks");
    }
}
