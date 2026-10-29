package veil;

import java.util.*;

/** Source building ownership, respawn cooldown, local caps and no pop-in checks. */
public final class LostSpawnSitesTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(long seed:new long[]{1L,42L,887766L,20261020L,99913L,2468L}){
            ArenaSystems game=new ArenaSystems(seed);
            World world=game.world;
            check(world.npcs.size()==45,"Stable v1 NPC network/save catalog");
            var sites=world.lostSites.sites();
            check(sites.size()>0,"Lost ruin sites missing");
            check(world.lostSites.totalAlive()<=45,"Global Lost cap violated");
            for(var site:sites){
                check(site.building()!=null&&!site.building().isBlank(),"Site needs ruin identity");
                check(world.city.categoryAt(site.x(),site.y())==CityLayout.Category.HAZARD,
                    "Lost must spawn from a specific fixed hazard ruin");
                check(site.alive()<=site.capacity()&&site.capacity()>0&&site.capacity()<=4,
                    "Site cap must be explicitly bounded");
            }
            for(World.Npc npc:world.npcs){
                check(npc.sourceSite>=0&&npc.sourceSite<sites.size(),
                    "Encounter lacks building origin");
                check(npc.kind.startsWith("lost-"),"NPC faction must be The Lost");
            }
            World.Npc doomed=world.npcs.stream().filter(n->n.hp>0).findFirst().orElseThrow();
            int siteId=doomed.sourceSite;
            doomed.hp=0;
            int first=world.lostSites.generation();
            var anchor=sites.get(siteId);
            // A killed NPC must not instantly respawn, even with elapsed old cooldown.
            long now=Math.max(60_000L,anchor.nextSpawnAt()+1);
            world.lostSites.advance(now,List.of());
            check(doomed.hp<=0,"No immediate replacement after killed NPC");
            int oldSlots=world.npcs.size();
            world.lostSites.advance(now+44_000,List.of());
            check(doomed.hp<=0,"Cooldown not finished");
            world.lostSites.advance(now+45_001,List.of());
            check(world.npcs.size()==oldSlots,"Respawn must reuse existing network slot");
            check(world.lostSites.generation()>=first,"Generation regressed");
            // Site may wait for neighborhood cap; a live player blocks pop-in.
            doomed.hp=0;
            world.lostSites.advance(now+90_000,List.of(new double[]{anchor.x()+.5,anchor.y()+.5}));
            check(doomed.hp<=0,"Must not spawn in a player's face");
            System.out.println("PASS LostSpawnSitesTest seed="+seed+" ruins="+sites.size()+
                " live="+world.lostSites.totalAlive());
        }
    }
}
