package veil;

import java.nio.file.*;
import java.util.*;

public final class WildernessExpeditionTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        long seed=20261021L;
        var layout=ContinentLayout.generate(seed);
        var realm=new WildernessExpedition(layout);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player first=host.join(1),second=host.join(2);
        var origin=realm.enter(1,first);
        check(origin!=null&&realm.walkable(origin.x(),origin.y()),"spawn on accessible wilderness");
        check(realm.enter(2,second)!=null,"second player shares persistent node state");
        var target=realm.focus(1);
        check(target!=null&&target.kind()==InteractionFocus.Kind.RESOURCE,
            "first steps expose Beckenburg centre-ray harvest focus");
        check(!realm.harvest(1,first,target.id()+99_999).accepted(),
            "remote client cannot spoof resource ID");
        var before=realm.resources.node(target.id());
        var result=realm.harvest(1,first,target.id());
        check(result.accepted()&&result.node().remaining()==before.remaining()-1,
            "host-authoritative harvest decrements the physical resource");
        check(first.survival.count(result.item())==1,"real survival inventory receives the material");
        check(realm.resources.node(target.id()).remaining()==result.node().remaining(),
            "second player sees one shared depleting resource");
        check(!realm.harvest(1,first,target.id()).accepted(),"one hit per 800ms");
        check(!realm.move(1,first,origin.x()+30,origin.y(),origin.angle(),0),
            "teleport movement denied");
        check(!realm.move(1,first,-1,-1,origin.angle(),0),
            "ocean and outside-map rejected");
        check(realm.move(1,first,origin.x(),origin.y(),origin.angle()+.1,0),
            "standing still may rotate to face resource");
        realm.advance(900);
        check(realm.harvest(1,first,target.id()).accepted(),"continued harvest with cooldown");
        Path tmp=Files.createTempDirectory("veil-wilderness");
        try{
            realm.save(tmp);
            var loaded=new WildernessExpedition(layout);
            loaded.load(tmp);
            check(realm.resources.snapshot().depletion().equals(
                loaded.resources.snapshot().depletion()),"depletion persists across host restart");
            check(loaded.resources.node(target.id()).remaining()==
                realm.resources.node(target.id()).remaining(),"no duplicate stock after restart");
        }finally{
            try(var files=Files.list(tmp)){
                files.forEach(p->{try{Files.deleteIfExists(p);}catch(Exception e){throw new IllegalStateException(e);}});
            }
            Files.deleteIfExists(tmp);
        }
        realm.leave(1);
        check(!realm.harvest(1,first,target.id()).accepted(),
            "cannot harvest after exiting wilderness");
        System.out.println("PASS WildernessExpeditionTest: actual focus, inventory, range, cooldown, anti-teleport, shared depletion, persistence");
    }
}
