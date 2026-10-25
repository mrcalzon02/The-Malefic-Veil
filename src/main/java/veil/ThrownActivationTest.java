package veil;

import java.lang.reflect.Field;

/** Actual craft/throw/effect checks at the impact/unfold boundary for every faction/role. */
public final class ThrownActivationTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final Field CLOCK;
    static {try{CLOCK=ArenaSystems.class.getDeclaredField("clock");CLOCK.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static void tick(ArenaSystems host,long ms)throws Exception{
        CLOCK.setLong(host,host.time()+ms);host.thrown.advance(ms);
    }
    private static String account(char c){return "acct_"+String.valueOf(c).repeat(64);}
    private static ArenaSystems prepare(ThrownDeployables.Blueprint bp){
        ArenaSystems host=new ArenaSystems(20261024);var maker=host.join(1);
        check(host.assignAccount(1,account((char)('a'+bp.faction().ordinal()))),"Maker account");
        check(host.enrollFaction(1,CharacterRoster.Faction.values()[bp.faction().ordinal()]),"Maker faction");
        boolean found=false;
        for(int x=4;x<World.SIZE-6&&!found;x++)for(int y=4;y<World.SIZE-6&&!found;y++){
            double px=x+.5,py=y+.5;
            if(host.world.canWalk(px-1,py)&&host.world.canWalk(px+3.5,py)&&host.world.lineClear(px-1,py,px+3.5,py)){
                maker.x=px;maker.y=py;maker.angle=0;found=true;
            }
        }
        check(found,"Safe throw position");
        if(bp.station()!=SurvivalCrafting.Station.HAND){
            var station=new World.Device(maker.x-1,maker.y,bp.station()==SurvivalCrafting.Station.FIRE?ArenaSystems.FIELD_FIRE:ArenaSystems.FIELD_BENCH);
            station.ownerId=1;station.ownerKey=maker.accountKey;host.world.devices.add(station);
        }
        for(var input:bp.ingredients().entrySet())check(maker.survival.acquire(input.getKey(),input.getValue(),true),"Raw input acquisition");
        check(host.thrown.craft(1,bp.role().ordinal()).ok(),"Real craft "+bp.id());
        check(host.thrown.throwKit(1,bp.role().ordinal()).ok(),"Real throw "+bp.id());
        return host;
    }
    public static void main(String[] args)throws Exception{
        int tested=0;
        for(var bp:ThrownDeployables.catalog()){
            var host=prepare(bp);tick(host,700);var landed=host.thrown.views().get(0);
            check(landed.remainingMs()==ThrownDeployables.LIFETIME_MS,"Landing life clock "+bp.id());
            check(landed.state()==ThrownDeployables.State.IMPACT,"Impact state "+bp.id());
            var maker=host.player(1);maker.x=landed.x()-.4;maker.y=landed.y();maker.hp=50;maker.sanity=50;maker.protectedUntil=0;
            var enemy=host.join(2);check(host.assignAccount(2,account('f')),"Enemy account");
            check(host.enrollFaction(2,bp.faction()==ThrownDeployables.Faction.UNRAVELED?CharacterRoster.Faction.THE_PENITENT:CharacterRoster.Faction.THE_UNRAVELED),"Enemy faction");
            enemy.x=landed.x()-.8;enemy.y=landed.y();enemy.protectedUntil=0;
            for(var npc:host.world.npcs)npc.hp=0;
            tick(host,ThrownDeployables.ACTIVATION_MS-1);var before=host.thrown.views().get(0);
            check(before.operationAgeMs()==-1,"Operation began during unfolding "+bp.id());
            check(maker.hp==50&&maker.sanity==50&&enemy.hp==100&&enemy.sanity==100,"Effect leaked before activation "+bp.id());
            check(!host.thrown.occupied(landed.x(),landed.y()),"Invisible full-size cover "+bp.id());
            check(host.thrown.moveFactor(enemy)==1&&host.thrown.visibilityFactor(enemy)==1,"Premature field effect "+bp.id());
            check(host.thrown.absorb(maker,10)==0,"Premature shield interception "+bp.id());
            int stock=before.ammoStock();tick(host,1);var active=host.thrown.views().get(0);
            check(active.remainingMs()==ThrownDeployables.LIFETIME_MS-ThrownDeployables.ACTIVATION_MS,"Lifetime restarted at activation");
            switch(bp.role()){
                case WALL,BARRICADE -> {
                    check(host.thrown.occupied(landed.x(),landed.y()),"Completed cover absent");
                    check(host.thrown.blocksLine(landed.x()-2,landed.y(),landed.x()+2,landed.y()),"Completed cover does not block shots");
                    if(bp.role()==ThrownDeployables.Role.BARRICADE)check(host.thrown.moveFactor(enemy)<1,"Active barricade slow absent");
                }
                case HEALTH -> check(maker.hp>50&&active.operationAgeMs()==0,"Successful healing/event absent");
                case TURRET -> {
                    check(enemy.hp<100&&active.operationAgeMs()==0,"Host attack/event absent");
                    check(active.tracking(),"Target lock absent from authoritative snapshot");
                    double dx=enemy.x-active.x(),dy=enemy.y-active.y(),distance=Math.hypot(dx,dy);
                    // glTF right-handed Y rotation followed by the production instance yaw.
                    double angle=active.heading()-active.aimYaw();
                    check((Math.sin(angle)*dx-Math.cos(angle)*dy)/distance>.99999,"Rendered -Z muzzle points away from the host target");
                    int hp=enemy.hp;enemy.y+=.20;tick(host,10);var tracked=host.thrown.views().get(0);
                    check(tracked.tracking()&&Math.abs(tracked.aimYaw()-active.aimYaw())>.01,"Moving target is not tracked between attacks");
                    check(enemy.hp==hp&&tracked.operationAgeMs()==10,"Tracking generated a decorative attack/event");
                    enemy.dead=true;tick(host,10);check(!host.thrown.views().get(0).tracking(),"Dead target remained acquired");
                }
                case MIASMA -> {
                    check(active.operationAgeMs()==0,"Active emission event absent");
                    if(bp.faction()==ThrownDeployables.Faction.UNRAVELED)check(host.thrown.visibilityFactor(enemy)<1,"Active vapor visibility absent");
                    else check(enemy.hp<100||enemy.sanity<100,"Active miasma effect absent");
                }
                case SHIELD -> check(host.thrown.absorb(maker,10)>0,"Completed shield does not intercept");
                case AMMO -> {
                    int reserve=host.arsenal.view(1).reserve()[0];tick(host,1000);var supplied=host.thrown.views().get(0);
                    check(supplied.ammoStock()<stock&&host.arsenal.view(1).reserve()[0]>reserve,"Finite host resupply absent");
                    check(supplied.operationAgeMs()==0,"Successful supply event absent");
                }
            }
            tested++;
        }
        System.out.println("ThrownActivationTest OK: "+tested+" real recipes/throws, landing lifetime, no premature effects/collision, active role effects and successful-operation events");
    }
}
