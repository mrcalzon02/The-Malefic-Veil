package veil;

import java.util.*;

public final class CrudeStructureIndexTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        Map<String,CrudeStructureIndex.Proxy> index=CrudeStructureIndex.entries();
        var facilities=CraftingEraRegistry.facilities();
        check(index.size()==facilities.size()+3,"no missing factory or defense primitive art plan");
        for(String id:List.of("purification-works","barracks",
                "wall-gate-system","cold-store","security-grid"))
            check(facilities.containsKey(id)&&index.containsKey(id),
                "Previously undefined blueprint needs registry cost and proxy: "+id);
        Set<CrudeStructureIndex.Primitive> forms=
            EnumSet.noneOf(CrudeStructureIndex.Primitive.class);
        for(var facility:facilities.values()){
            var p=index.get(facility.id());
            check(p!=null&&p.width()==facility.width()&&p.depth()==facility.depth(),
                "proxy must preserve true authoritative structure collider footprint");
            check(p.minimumEra()==facility.era(),"art index does not bypass industrial era");
            check(p.level50RetrofitAllowed(),"each facility has a level 50 continuation");
            forms.add(p.primitive());
        }
        check(forms.size()>=10,"crude structures should visually reflect their functions");
        for(String id:List.of("field-shelter","forager-garden","herb-plot",
            "seed-nursery","root-cellar","smokehouse","fiber-loom",
            "irrigation-works","controlled-greenhouse","motor-pool")){
            check(index.containsKey(id),"missing farm/survival/industrial asset "+id);
        }
        check(index.get("aa-fast").primitive()==CrudeStructureIndex.Primitive.ROTARY_TOWER,
            "tracking tower cannot use a generic field asset");
        check(index.get("aa-heavy").primitive()==CrudeStructureIndex.Primitive.HEAVY_AA_PEDESTAL,
            "heavy anti-air must have a distinct silhouette");
        check(index.get("automated-turret")!=null,"ground defense index preserved");
        check(index.get("forager-garden").primitive()==
              CrudeStructureIndex.Primitive.RAISED_BEDS,"gardens not generic huts");
        check(index.get("palisade-section").primitive()==
              CrudeStructureIndex.Primitive.STAKE_WALL,"walls have functional silhouette");
        System.out.println("PASS CrudeStructureIndexTest: all indexed structures have placeholder geometry plans");
    }
}
