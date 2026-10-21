package veil;

import java.util.*;

/**
 * Server-authoritative allegiance and private crew administration.
 * ALL crews in one playable faction are permanently allied, without treaties,
 * war declarations, friendly-fire toggles or defections.
 *
 * Only authenticated stable accounts may enroll or own a crew. An unenrolled
 * legacy peer is neutral, not secretly an Unraveled civilian.
 */
public final class FactionGroups {
    public enum Role { FOUNDER, OFFICER, MEMBER }
    public record Crew(String id,String name,CharacterRoster.Faction faction,
                       String founder,Map<String,Role> members){}
    public record Invite(String crewId,String account,long expiresAtMs){}
    public record Snapshot(Map<String,CharacterRoster.Faction> factions,
                           List<Crew> crews,long nextCrewId){}
    private static final int MAX_CREWS=2048,MAX_MEMBERS=32,MAX_INVITES=256;
    private final Map<String,CharacterRoster.Faction> allegiance=new TreeMap<>();
    private final Map<String,MutableCrew> crews=new LinkedHashMap<>();
    private final Map<String,String> membership=new HashMap<>();
    private final Map<String,Invite> invites=new HashMap<>();
    private long nextCrewId=1;
    private static final class MutableCrew{
        String id,name,founder;
        CharacterRoster.Faction faction;
        final LinkedHashMap<String,Role> members=new LinkedHashMap<>();
        Crew view(){return new Crew(id,name,faction,founder,Map.copyOf(members));}
    }
    private static boolean account(String s){
        return s!=null&&s.matches("acct_[a-f0-9]{64}");
    }
    private static boolean name(String s){
        return s!=null&&s.matches("[A-Za-z0-9][A-Za-z0-9 _'-]{2,31}");
    }
    public synchronized CharacterRoster.Faction faction(String account){
        return allegiance.get(account);
    }
    /** Permanent selection. Cannot switch from Civilian to Killer or Monster. */
    public synchronized boolean enroll(String account,CharacterRoster.Faction faction){
        if(!account(account)||faction==null)return false;
        CharacterRoster.Faction old=allegiance.get(account);
        if(old!=null)return old==faction;
        allegiance.put(account,faction);
        return true;
    }
    public synchronized boolean allied(String a,String b){
        CharacterRoster.Faction f=allegiance.get(a);
        return f!=null&&f==allegiance.get(b);
    }
    public synchronized String crewId(String account){return membership.get(account);}
    public synchronized Crew crewFor(String account){
        MutableCrew c=crews.get(membership.get(account));
        return c==null?null:c.view();
    }
    public synchronized List<Crew> allCrews(){
        return crews.values().stream().map(MutableCrew::view).toList();
    }
    public synchronized Crew create(String founder,String name){
        if(!account(founder)||!name(name)||membership.containsKey(founder)||
           !allegiance.containsKey(founder)||crews.size()>=MAX_CREWS)return null;
        for(MutableCrew other:crews.values())
            if(other.faction==allegiance.get(founder)&&other.name.equalsIgnoreCase(name))
                return null;
        MutableCrew crew=new MutableCrew();
        crew.id="crew_"+nextCrewId++;
        crew.name=name;crew.founder=founder;crew.faction=allegiance.get(founder);
        crew.members.put(founder,Role.FOUNDER);
        crews.put(crew.id,crew);membership.put(founder,crew.id);
        return crew.view();
    }
    public synchronized boolean invite(String inviter,String target,long now){
        MutableCrew crew=crews.get(membership.get(inviter));
        if(crew==null||!account(target)||now<0||
            membership.containsKey(target)||crew.members.size()>=MAX_MEMBERS||
            !crew.faction.equals(allegiance.get(target))||
            crew.members.get(inviter)==Role.MEMBER||invites.size()>=MAX_INVITES)return false;
        invites.put(target,new Invite(crew.id,target,now+300_000));
        return true;
    }
    public synchronized Invite invitation(String account,long now){
        Invite pending=invites.get(account);
        return pending==null||pending.expiresAtMs()<now?null:pending;
    }
    public synchronized boolean accept(String account,long now){
        Invite invite=invitation(account,now);
        if(invite==null||membership.containsKey(account))return false;
        MutableCrew crew=crews.get(invite.crewId());
        if(crew==null||crew.members.size()>=MAX_MEMBERS||
            !crew.faction.equals(allegiance.get(account)))return false;
        crew.members.put(account,Role.MEMBER);
        membership.put(account,crew.id);invites.remove(account);
        return true;
    }
    public synchronized boolean promote(String officer,String member){
        MutableCrew crew=crews.get(membership.get(officer));
        if(crew==null||!crew.founder.equals(officer)||
           crew.members.get(member)!=Role.MEMBER)return false;
        crew.members.put(member,Role.OFFICER);return true;
    }
    public synchronized boolean remove(String actor,String target){
        MutableCrew crew=crews.get(membership.get(actor));
        if(crew==null||!crew.members.containsKey(target))return false;
        boolean self=actor.equals(target);
        if(crew.founder.equals(target))return false; // explicit handoff/disband only
        if(!self&&(crew.members.get(actor)==null||crew.members.get(actor)==Role.MEMBER||
           crew.members.get(target)==Role.OFFICER&&!crew.founder.equals(actor)))return false;
        crew.members.remove(target);membership.remove(target);
        return true; // faction allegiance survives crew membership changes
    }
    public synchronized boolean transfer(String founder,String successor){
        MutableCrew crew=crews.get(membership.get(founder));
        if(crew==null||!crew.founder.equals(founder)||founder.equals(successor)||
           !crew.members.containsKey(successor))return false;
        crew.members.put(founder,Role.OFFICER);
        crew.members.put(successor,Role.FOUNDER);crew.founder=successor;
        return true;
    }
    public synchronized boolean disband(String founder){
        MutableCrew crew=crews.get(membership.get(founder));
        if(crew==null||!crew.founder.equals(founder))return false;
        for(String account:crew.members.keySet())membership.remove(account);
        crews.remove(crew.id);
        invites.entrySet().removeIf(e->e.getValue().crewId().equals(crew.id));
        return true;
    }
    public synchronized Snapshot snapshot(){
        return new Snapshot(Map.copyOf(allegiance),allCrews(),nextCrewId);
    }
    public synchronized void restore(Snapshot snap){
        if(snap==null||snap.nextCrewId()<1||snap.factions().size()>100000||
           snap.crews().size()>MAX_CREWS)throw new IllegalArgumentException("Invalid crew file");
        Map<String,CharacterRoster.Faction> factions=new TreeMap<>();
        for(var entry:snap.factions().entrySet()){
            if(!account(entry.getKey())||entry.getValue()==null)
                throw new IllegalArgumentException("Bad faction enrollment");
            factions.put(entry.getKey(),entry.getValue());
        }
        Map<String,MutableCrew> checked=new LinkedHashMap<>();
        Set<String> occupied=new HashSet<>();
        for(Crew data:snap.crews()){
            if(data==null||data.id()==null||!data.id().matches("crew_[0-9]+")||
                checked.containsKey(data.id())||!name(data.name())||
                !account(data.founder())||data.faction()==null||
                data.members().size()<1||data.members().size()>MAX_MEMBERS||
                data.members().get(data.founder())!=Role.FOUNDER||
                factions.get(data.founder())!=data.faction())
                throw new IllegalArgumentException("Invalid crew snapshot");
            MutableCrew c=new MutableCrew();
            c.id=data.id();c.name=data.name();c.faction=data.faction();c.founder=data.founder();
            int founders=0;
            for(var member:data.members().entrySet()){
                if(!account(member.getKey())||member.getValue()==null||
                   factions.get(member.getKey())!=data.faction()||
                   !occupied.add(member.getKey()))
                    throw new IllegalArgumentException("Cross-faction or duplicate crew member");
                if(member.getValue()==Role.FOUNDER)founders++;
                c.members.put(member.getKey(),member.getValue());
            }
            if(founders!=1)throw new IllegalArgumentException("Multiple crew founders");
            checked.put(c.id,c);
            if(Long.parseLong(c.id.substring(5))>=snap.nextCrewId())
                throw new IllegalArgumentException("Crew ID sequence rewound");
        }
        allegiance.clear();allegiance.putAll(factions);
        crews.clear();crews.putAll(checked);
        membership.clear();
        for(MutableCrew c:crews.values())for(String who:c.members.keySet())membership.put(who,c.id);
        invites.clear();nextCrewId=snap.nextCrewId();
    }
}
