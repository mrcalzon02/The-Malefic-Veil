package veil;

/** Deterministic real ArenaSystems tick, target avoidance and NPC wire proof. */
public final class LostBoneShivTest {
    private record Fixture(ArenaSystems host,ArenaSystems.Player player,World.Npc npc){}
    private static int assertions;
    private static void check(boolean condition,String message){
        assertions++;
        if(!condition)throw new AssertionError(message);
    }
    private static Fixture fixture(String kind){
        ArenaSystems host=new ArenaSystems(42L);
        check(host.world.npcs.stream().anyMatch(n->"lost-drifter".equals(n.kind)),
            "actual ruin-owned host roster must contain classified Drifters");
        // Suppress unrelated attackers without changing the real host tick.
        for(World.Npc other:host.world.npcs)other.hp=0;
        ArenaSystems.Player player=host.join(9);
        World.Npc npc=new World.Npc(2.4,1.5);
        npc.kind=kind;
        host.world.npcs.add(npc);
        check(host.world.canWalk(npc.x,npc.y)&&
              host.clear(npc.x,npc.y,player.x,player.y),
              "spawn fixture must have genuine unobstructed geometry");
        return new Fixture(host,player,npc);
    }
    private static void timingAndSingleHit(){
        Fixture f=fixture("lost-drifter");
        f.host.advance(100);
        check(f.npc.attackPhase==LostBoneShiv.WINDUP&&f.npc.attackMs==300,
            "Drifter must start with visible 300ms windup");
        check(f.player.hp==100&&f.player.sanity==100,"no damage on tell");
        f.host.advance(200);
        check(f.player.hp==100&&f.npc.attackPhase==LostBoneShiv.WINDUP,
            "no damage before full windup");
        f.host.advance(100);
        check(f.player.hp==88&&f.player.sanity==99,
            "one actual 12HP/1 sanity strike after 300ms");
        check(f.npc.attackPhase==LostBoneShiv.RECOVERY&&f.npc.attackMs==650,
            "exact 650ms recovery after impact");
        f.host.advance(649);
        check(f.player.hp==88&&f.npc.attackPhase==LostBoneShiv.RECOVERY,
            "no second strike inside recovery");
        f.host.advance(1);
        check(f.npc.attackPhase==LostBoneShiv.IDLE,"recovery expires");
        f.host.advance(100);
        check(f.npc.attackPhase==LostBoneShiv.WINDUP&&f.player.hp==88,
            "next strike must begin a new windup, never double hit");
    }
    private static void sidestep(){
        Fixture f=fixture("lost-drifter");
        f.host.advance(100);
        check(f.host.move(9,1.5,1.95,0,0),"first legal host movement");
        f.host.advance(100);
        check(f.host.move(9,1.5,2.4,0,0),"second legal host movement");
        f.host.advance(200);
        check(Math.hypot(f.player.x-f.npc.x,f.player.y-f.npc.y)<1.4,
            "sidestep fixture remains within reach");
        check(f.player.hp==100&&f.player.sanity==100,
            "lateral movement away from committed aim must avoid shiv");
    }
    private static void rangeAndCover(){
        Fixture far=fixture("lost-drifter");
        far.host.advance(100);
        far.player.y=3.5; // host-owned fixture: test impact validation only
        far.host.advance(300);
        check(far.player.hp==100,"out-of-range strike misses");
        Fixture blocked=fixture("lost-drifter");
        blocked.host.advance(100);
        blocked.host.world.devices.add(new World.Device(1.95,1.5,ArenaSystems.BARRICADE));
        check(!blocked.host.clear(blocked.npc.x,blocked.npc.y,
            blocked.player.x,blocked.player.y),"actual barricade blocks LOS");
        blocked.host.advance(300);
        check(blocked.player.hp==100&&blocked.npc.attackPhase==LostBoneShiv.RECOVERY,
            "blocked strike misses but still pays recovery");
    }
    private static void allianceAndGrace(){
        Fixture allied=fixture("lost-drifter");
        check(allied.host.assignAccount(9,"acct_"+"a".repeat(64)),
            "host authenticates faction owner");
        check(allied.host.enrollFaction(9,CharacterRoster.Faction.THE_BEGOTTEN),
            "Begotten enrollment succeeds");
        allied.host.advance(1000);
        check(allied.npc.attackPhase==LostBoneShiv.IDLE&&allied.player.hp==100,
            "Lost must not target Begotten player");
        Fixture grace=fixture("lost-drifter");
        grace.player.protectedUntil=1000;
        grace.host.advance(100);
        grace.host.advance(300);
        check(grace.player.hp==100&&grace.player.sanity==100,
            "existing spawn protection blocks hostile strike");
        Fixture other=fixture("lost-stalker");
        other.npc.x=2.2;
        other.host.advance(100);
        check(other.npc.attackPhase==LostBoneShiv.IDLE&&other.player.hp==92,
            "other Lost families retain legacy host melee");
    }
    private static void wireAndRegistry(){
        Fixture f=fixture("lost-drifter");
        f.host.advance(100);
        String wire=LanServer.npcPacket(45,f.npc);
        NetLink.NpcEvent event=NetLink.decodeNpc(
            wire.split(java.util.regex.Pattern.quote("|")));
        check(event!=null&&event.index()==45&&event.attackPhase()==1&&
              event.attackTargetId()==f.player.id,
              "host snapshot must replicate exact windup and target");
        NetLink.NpcEvent legacy=NetLink.decodeNpc(
            "NPC|0|1.5|1.5|80".split(java.util.regex.Pattern.quote("|")));
        check(legacy!=null&&legacy.attackPhase()==0&&legacy.attackTargetId()==-1,
            "old NPC snapshots remain parseable");
        check(NetLink.decodeNpc(
            "NPC|0|1.5|1.5|80|99|-1".split(java.util.regex.Pattern.quote("|")))==null,
            "invalid attack phase must be rejected");
        int live=0;
        for(LostArsenal.Weapon weapon:LostArsenal.all())
            if(weapon.runtimeIntegrated())live++;
        check(live==1&&LostArsenal.get("lost-drifter-bone-shiv").runtimeIntegrated(),
            "exactly one of 25 Lost weapons may claim live host integration");
    }
    public static void main(String[] args){
        timingAndSingleHit();
        sidestep();
        rangeAndCover();
        allianceAndGrace();
        wireAndRegistry();
        System.out.println("PASS LostBoneShivTest "+assertions+
            ": host windup, movement, LOS, alliance, recovery and wire");
    }
}
