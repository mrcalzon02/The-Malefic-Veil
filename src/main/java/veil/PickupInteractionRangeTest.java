package veil;

/** Reproduces an on-screen spinning ammo pickup and tests the real host E path. */
public final class PickupInteractionRangeTest {
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args){
        ArenaSystems game=new ArenaSystems(20261028L);
        ArenaSystems.Player p=game.join(1);
        game.world.pickups.clear();
        boolean found=false;
        for(int x=2;x<World.SIZE-5&&!found;x++)
            for(int y=2;y<World.SIZE-5&&!found;y++){
                double px=x+.5,py=y+.5,tx=px+1.5;
                if(!game.world.canWalk(px,py)||!game.world.canWalk(tx,py)||
                   !game.world.lineClear(px,py,tx,py))continue;
                p.x=px;p.y=py;p.angle=0;p.moveAt=game.time();
                game.world.pickups.add(new World.Pickup(tx,py,2));
                found=true;
            }
        check(found,"No safe pickup test corridor");
        World.Pickup ammo=game.world.pickups.get(0);
        int before=p.pistolReserve;
        p.angle=Math.PI;
        check(!game.interactPickup(1,0),"Facing away must not collect ammo");
        p.angle=0;
        check(game.interactPickup(1,0),"Visible ammo at 1.5m must respond to E");
        check(ammo.taken&&p.pistolReserve==before+24,
            "The same interaction must award authoritative ammunition exactly once");
        check(!game.interactPickup(1,0),"Repeated E must not duplicate ammunition");
        System.out.println("PASS PICKUP_INTERACTION_HOST 1.5m aim + LOS + single-grant");
    }
}
