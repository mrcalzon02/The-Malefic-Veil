package veil;

/**
 * One live Lost opening attack. Only the ruin-owned "lost-drifter" actor
 * family may execute this profile; the other 24 design weapons remain inert.
 * Called from ArenaSystems.advance on the authoritative host clock.
 */
public final class LostBoneShiv {
    private static final LostArsenal.Weapon SHIV=LostArsenal.get("lost-drifter-bone-shiv");
    public static final int IDLE=0,WINDUP=1,RECOVERY=2;
    private static final long WINDUP_MS=300,RECOVERY_MS=650;
    /** A committed stab misses a player who sidesteps the marked point. */
    private static final double AIM_RADIUS=.60;

    private LostBoneShiv(){}

    /** True when the Drifter's current tick was consumed by an attack phase. */
    static boolean advance(ArenaSystems host,World.Npc npc,
                           ArenaSystems.Player target,long ms){
        if(!"lost-drifter".equals(npc.kind)||npc.hp<=0)return false;
        if(npc.attackPhase==WINDUP){
            npc.attackMs=Math.max(0,npc.attackMs-ms);
            if(npc.attackMs==0){
                ArenaSystems.Player victim=host.player(npc.attackTargetId);
                // Evaluate actual current position and LOS at impact, not
                // the original client target or a stale aim packet.
                if(victim!=null&&!victim.dead&&
                   host.factions.faction(victim.accountKey)!=
                       CharacterRoster.Faction.THE_BEGOTTEN&&
                   Math.hypot(victim.x-npc.x,victim.y-npc.y)<=SHIV.reach()&&
                   Math.hypot(victim.x-npc.attackAimX,
                              victim.y-npc.attackAimY)<=AIM_RADIUS&&
                   host.clear(npc.x,npc.y,victim.x,victim.y))
                    host.damage(victim,SHIV.hpDamage(),SHIV.sanityDamage());
                npc.attackPhase=RECOVERY;
                npc.attackMs=RECOVERY_MS;
                npc.attackTargetId=-1;
            }
            return true;
        }
        if(npc.attackPhase==RECOVERY){
            npc.attackMs=Math.max(0,npc.attackMs-ms);
            if(npc.attackMs==0)npc.attackPhase=IDLE;
            return true;
        }
        if(target==null||target.dead||
           host.factions.faction(target.accountKey)==
               CharacterRoster.Faction.THE_BEGOTTEN||
           Math.hypot(target.x-npc.x,target.y-npc.y)>SHIV.reach()||
           !host.clear(npc.x,npc.y,target.x,target.y))return false;
        npc.attackPhase=WINDUP;
        npc.attackTargetId=target.id;
        npc.attackAimX=target.x;
        npc.attackAimY=target.y;
        npc.attackMs=WINDUP_MS;
        return true;
    }
}
