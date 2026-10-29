package veil;
import java.util.function.BooleanSupplier;

/** Real sockets: weapon collection/selection/fire, inventory, effects, bench and specialist crafting. */
public final class ArsenalNetworkTest {
    static void until(BooleanSupplier condition,String why)throws Exception{long end=System.nanoTime()+5_000_000_000L;while(System.nanoTime()<end){if(condition.getAsBoolean())return;Thread.sleep(20);}throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        try(LanServer server=new LanServer(0,20261020);NetLink a=new NetLink("127.0.0.1",server.port());NetLink b=new NetLink("127.0.0.1",server.port())){
            var h=server.match;h.world.npcs.clear();until(()->a.arsenal()!=null,"inventory arrives");
            a.arsenalCraft(9);until(()->a.arsenalResults.contains("OWN_BENCH_REQUIRED"),"craft denied without a bench");
            int index=1;var drop=h.arsenal.loot.get(index);h.player(a.id).x=drop.x;h.player(a.id).y=drop.y;a.arsenalPickup(index);
            until(()->a.arsenal()!=null&&(a.arsenal().owned()&2)!=0,"weapon collected by host");until(()->b.arsenalPicked.contains(index),"weapon removed for other client");
            a.arsenalSelect(1);until(()->a.arsenal().selected()==1,"selection acknowledged");
            h.player(a.id).x=1.5;h.player(a.id).y=1.5;h.player(a.id).angle=0;h.player(b.id).x=4.0;h.player(b.id).y=1.5;
            a.arsenalFire(false);until(()->h.player(b.id).hp<100,"new arsenal shot deals authoritative damage");until(()->b.self()!=null&&b.self().health()<100,"damage reaches victim");until(()->!b.arsenalEffects().isEmpty(),"firing effects replicated");
            a.fire(1);until(()->!a.shots.isEmpty(),"compatibility reply");if(a.shots.poll().fired())throw new AssertionError("Legacy command bypassed ownership");
            // Place the bench using the same host placement checks as the J key.
            var p=h.player(a.id);p.cash=1000;p.scrap=500;boolean placed=false;
            for(CityLayout.Plot lot:h.world.city.plots()){
                if(lot.template().category()!=CityLayout.Category.OPEN||lot.template().variant()!=2)continue;
                double x=(lot.x()+lot.right())*CityLayout.CELL/2.0+.5,y=(lot.y()+lot.bottom())*CityLayout.CELL/2.0+.5;
                if(!h.world.canWalk(x-1.1,y))continue;p.x=x-1.1;p.y=y;p.angle=0;p.moveAt=h.time();if(h.reviewBuild(a.id,4).allowed()){placed=true;break;}
            }if(!placed)throw new AssertionError("No valid bench site");a.build(4);until(()->h.world.devices.stream().anyMatch(d->d.kind==4),"bench built through socket");
            var state=ArsenalSystemsTest.state(h);state.materials[1]=100;state.materials[2]=100;state.materials[3]=100;
            a.selectFaction(CharacterRoster.Faction.THE_PENITENT);until(()->a.arsenal().faction()==1,"faction acknowledged");a.arsenalCraft(32);until(()->a.arsenal().materials()[4]>0,"frame component crafted");a.arsenalCraft(33);until(()->a.arsenal().materials()[5]>0,"power cell crafted");a.arsenalCraft(9);
            until(()->(a.arsenal().owned()&(1L<<9))!=0&&a.arsenal().factionLocked(),"specialist weapon fabricated and faction locked");a.arsenalSelect(9);until(()->a.arsenal().selected()==9,"crafted weapon equipped");
            a.arsenalSelect(31);Thread.sleep(150);if(a.arsenal().selected()!=9)throw new AssertionError("Unowned weapon selected");
            System.out.println("PASS ArsenalNetworkTest: real two-client loot, ownership, damage, effects, bench, materials, faction and crafting");
        }
    }
}
