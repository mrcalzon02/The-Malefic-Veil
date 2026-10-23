package veil;

import java.util.*;

/**
 * Host-side settlement charter and construction-capacity authority. Not a
 * terrain/structure spawner. registerVerifiedClaim and recordPlaced are trusted
 * transaction hooks for the future persistent wilderness zone service; NEVER
 * expose them as client commands or call them before geometry and cost commit.
 */
public final class SettlementAuthority {
    public record Identity(String characterId,String accountKey,
                           CharacterRoster.Faction faction,int level) {}
    @FunctionalInterface public interface Directory {
        /** Must resolve from server-owned character profiles, including offline founders. */
        Identity resolve(String characterId);
    }
    public enum Kind { FACILITY, DEFENSE, QUEUE, EXTENSION, MONUMENT }
    public record Capacity(int facilities,int defenses,int queues,int extensions,int monuments) {}
    public record Result(boolean accepted,String reason,long revision,Capacity capacity) {}
    public record Charter(String id,String founderCharacterId,String founderAccountKey,
                          CharacterRoster.Faction faction,long revision,
                          Map<String,Set<String>> builderGrants,
                          Map<String,String> structures) {}
    public record Snapshot(List<Charter> charters) {}

    /** Each blueprint is server-classified; never take kind/cost from the client. */
    private static final Map<String,Kind> CATALOG=new LinkedHashMap<>();
    static {
        // Authoritative build whitelist follows the versioned actual
        // industry/farm registry, not a small stale manually copied subset.
        // The heart is a charter prerequisite and primitive wall panels
        // belong to separate modular construction budgets.
        for(String id:CraftingEraRegistry.facilities().keySet())
            if(!Set.of("camp-stake","claim-heart","monument-graveyard",
                       "palisade-section").contains(id))
                CATALOG.put(id,Kind.FACILITY);
        // No costless ghost blueprints: all normal facility IDs must be
        // versioned and material-costed in CraftingEraRegistry.
        for(String id:new String[]{"aa-fast","aa-heavy","automated-turret"})
            CATALOG.put(id,Kind.DEFENSE);
        CATALOG.put("production-queue",Kind.QUEUE);
        CATALOG.put("claim-extension",Kind.EXTENSION);
        CATALOG.put("monument-graveyard",Kind.MONUMENT);
    }
    private static final Map<String,Kind> BLUEPRINTS=Map.copyOf(CATALOG);
    private static final class Mutable {
        String id,founder,account;
        CharacterRoster.Faction faction;
        long revision=1;
        final Map<String,Set<String>> grants=new TreeMap<>();
        final Map<String,String> structures=new TreeMap<>();
        Charter view(){
            return new Charter(id,founder,account,faction,revision,
                Map.copyOf(grants),Map.copyOf(structures));
        }
    }
    private final Directory directory;
    private final Map<String,Mutable> bases=new TreeMap<>();
    public SettlementAuthority(Directory directory) {
        this.directory=Objects.requireNonNull(directory);
    }
    public static Map<String,Kind> blueprintKinds(){return BLUEPRINTS;}
    public static Capacity capacityForLevel(int level){
        if(level<1||level>50)throw new IllegalArgumentException("Character level 1..50 required");
        if(level==50)return new Capacity(28,10,6,2,1);
        if(level>=40)return new Capacity(26,9,5,2,1);
        if(level>=30)return new Capacity(22,7,4,1,1);
        if(level>=20)return new Capacity(16,4,3,1,1);
        if(level>=10)return new Capacity(10,2,2,0,1);
        if(level>=5)return new Capacity(6,1,1,0,1);
        return new Capacity(4,1,1,0,1);
    }
    private static boolean id(String value){
        return value!=null&&value.matches("[a-zA-Z0-9_-]{3,96}");
    }
    private static boolean account(String value){
        return value!=null&&value.matches("acct_[a-f0-9]{64}");
    }
    private Identity verified(String characterId){
        if(!id(characterId))return null;
        Identity i=directory.resolve(characterId);
        return i!=null&&characterId.equals(i.characterId())&&account(i.accountKey())&&
            i.faction()!=null&&i.level()>=1&&i.level()<=50?i:null;
    }
    private static int count(Mutable m,Kind kind) {
        int n=0;
        for(String name:m.structures.values())if(BLUEPRINTS.get(name)==kind)n++;
        return n;
    }
    private Result result(Mutable m,boolean ok,String why){
        Identity owner=verified(m.founder);
        Capacity cap=capacityForLevel(owner==null?1:owner.level());
        return new Result(ok,why,m.revision,cap);
    }
    /** Only call after a physical wilderness claim heart has passed placement and was committed. */
    public synchronized boolean registerVerifiedClaim(String settlementId,String founderCharacter){
        Identity i=verified(founderCharacter);
        if(i==null||!id(settlementId)||bases.containsKey(settlementId)||bases.size()>=2048)
            return false;
        for(Mutable b:bases.values())if(b.founder.equals(founderCharacter))return false;
        Mutable base=new Mutable();
        base.id=settlementId;base.founder=i.characterId();
        base.account=i.accountKey();base.faction=i.faction();
        bases.put(settlementId,base);
        return true;
    }
    public synchronized Charter charter(String settlementId){
        Mutable b=bases.get(settlementId);
        return b==null?null:b.view();
    }
    public synchronized List<Charter> charters(){
        return bases.values().stream().map(Mutable::view).toList();
    }
    /**
     * Only a founder may explicitly grant blueprint-specific build rights.
     * Same-faction affiliation alone never adds building access.
     */
    public synchronized Result setBuilderGrant(String settlementId,String actor,
                String target,Set<String> permitted,long expectedRevision) {
        Mutable b=bases.get(settlementId);
        if(b==null)return new Result(false,"NO_SETTLEMENT",0,null);
        Identity owner=verified(b.founder),a=verified(actor),t=verified(target);
        if(owner==null||!owner.accountKey().equals(b.account)||owner.faction()!=b.faction)
            return result(b,false,"OWNER_IDENTITY_UNAVAILABLE");
        if(a==null||!actor.equals(b.founder)||!a.accountKey().equals(b.account))
            return result(b,false,"NOT_FOUNDER");
        if(b.revision!=expectedRevision)return result(b,false,"STALE_REVISION");
        if(t==null||t.faction()!=b.faction||target.equals(b.founder))
            return result(b,false,"TARGET_NOT_ELIGIBLE");
        if(permitted==null||permitted.size()>BLUEPRINTS.size()||
            !BLUEPRINTS.keySet().containsAll(permitted))return result(b,false,"UNKNOWN_BLUEPRINT");
        if(permitted.isEmpty())b.grants.remove(target);
        else b.grants.put(target,Set.copyOf(permitted));
        b.revision++;
        return result(b,true,"OK");
    }
    private Result review(Mutable b,String actor,String blueprint,long expectedRevision){
        if(b.revision!=expectedRevision)return result(b,false,"STALE_REVISION");
        Kind kind=blueprint==null?null:BLUEPRINTS.get(blueprint);
        if(kind==null)return result(b,false,"UNKNOWN_BLUEPRINT");
        Identity founder=verified(b.founder),who=verified(actor);
        if(founder==null||!founder.accountKey().equals(b.account)||
           founder.faction()!=b.faction)return result(b,false,"OWNER_IDENTITY_UNAVAILABLE");
        if(who==null||who.faction()!=b.faction)return result(b,false,"FACTION_DENIED");
        if(!actor.equals(b.founder) &&
           !b.grants.getOrDefault(actor,Set.of()).contains(blueprint))
            return result(b,false,"BUILD_NOT_AUTHORIZED");
        Capacity cap=capacityForLevel(founder.level());
        int occupied=count(b,kind);
        int limit=switch(kind){
            case FACILITY -> cap.facilities();
            case DEFENSE -> cap.defenses();
            case QUEUE -> cap.queues();
            case EXTENSION -> cap.extensions();
            case MONUMENT -> cap.monuments();
        };
        if(occupied>=limit)return result(b,false,"OWNER_LEVEL_CAP");
        return result(b,true,"OK");
    }
    public synchronized Result reviewPlacement(String settlementId,String actor,
             String blueprint,long expectedRevision) {
        Mutable b=bases.get(settlementId);
        return b==null?new Result(false,"NO_SETTLEMENT",0,null):
            review(b,actor,blueprint,expectedRevision);
    }
    /**
     * Record the completed, host-verified placement inside the SAME durable
     * world/inventory transaction. Site geometry and price are validated by
     * that caller; this registry cannot spawn a structure or charge an item.
     */
    public synchronized Result recordPlaced(String settlementId,String actor,
             String structureId,String blueprint,long expectedRevision) {
        Mutable b=bases.get(settlementId);
        if(b==null)return new Result(false,"NO_SETTLEMENT",0,null);
        if(!id(structureId))return result(b,false,"INVALID_STRUCTURE_ID");
        String prior=b.structures.get(structureId);
        if(prior!=null) {
            Identity who=verified(actor);
            if(who==null||who.faction()!=b.faction||
                (!actor.equals(b.founder)&&
                 (blueprint==null||!b.grants.getOrDefault(actor,Set.of()).contains(blueprint))))
                return result(b,false,"BUILD_NOT_AUTHORIZED");
            boolean replay=prior.equals(blueprint);
            return result(b,replay,replay?"ALREADY_RECORDED":"STRUCTURE_CONFLICT");
        }
        Result checked=review(b,actor,blueprint,expectedRevision);
        if(!checked.accepted())return checked;
        b.structures.put(structureId,blueprint);
        b.revision++;
        return result(b,true,"OK");
    }
    /** Only the trusted destruction/dismantling transaction frees the slot. */
    public synchronized Result recordRemoved(String settlementId,String structureId,
                                              long expectedRevision) {
        Mutable b=bases.get(settlementId);
        if(b==null)return new Result(false,"NO_SETTLEMENT",0,null);
        if(b.revision!=expectedRevision)return result(b,false,"STALE_REVISION");
        if(!b.structures.containsKey(structureId))return result(b,false,"NOT_FOUND");
        b.structures.remove(structureId);
        b.revision++;
        return result(b,true,"OK");
    }
    /** Transfer needs founder authorization; no automatic account/crew takeover. */
    public synchronized Result transferFounder(String settlementId,String actor,
                                                String successor,long expectedRevision) {
        Mutable b=bases.get(settlementId);
        if(b==null)return new Result(false,"NO_SETTLEMENT",0,null);
        Identity from=verified(actor),to=verified(successor);
        if(from==null||!b.founder.equals(actor)||!b.account.equals(from.accountKey()))
            return result(b,false,"NOT_FOUNDER");
        if(b.revision!=expectedRevision)return result(b,false,"STALE_REVISION");
        if(to==null||to.faction()!=b.faction||successor.equals(actor))
            return result(b,false,"TARGET_NOT_ELIGIBLE");
        for(Mutable other:bases.values())if(other!=b&&other.founder.equals(successor))
            return result(b,false,"ALREADY_FOUNDING");
        b.grants.remove(successor);b.grants.remove(actor);
        b.founder=successor;b.account=to.accountKey();
        b.revision++;return result(b,true,"OK");
    }
    public synchronized Snapshot snapshot(){
        return new Snapshot(charters());
    }
    /** Validated cold-start restore; no mutations if one charter is corrupted. */
    public synchronized void restore(Snapshot snapshot) {
        if(snapshot==null||snapshot.charters()==null||snapshot.charters().size()>2048)
            throw new IllegalArgumentException("Invalid charter store");
        Map<String,Mutable> validated=new TreeMap<>();
        Set<String> owners=new HashSet<>();
        for(Charter source:snapshot.charters()){
            if(source==null||!id(source.id())||source.revision()<1||
               validated.containsKey(source.id())||!owners.add(source.founderCharacterId())||
               source.builderGrants()==null||source.structures()==null||
               source.builderGrants().size()>128||source.structures().size()>64)
                throw new IllegalArgumentException("Invalid charter identity or size");
            Identity owner=verified(source.founderCharacterId());
            if(owner==null||!owner.accountKey().equals(source.founderAccountKey())||
                owner.faction()!=source.faction())
                throw new IllegalArgumentException("Invalid charter owner");
            Mutable m=new Mutable();
            m.id=source.id();m.founder=source.founderCharacterId();
            m.account=source.founderAccountKey();m.faction=source.faction();
            m.revision=source.revision();
            for(var grant:source.builderGrants().entrySet()){
                Identity member=verified(grant.getKey());
                if(member==null||member.faction()!=m.faction||member.characterId().equals(m.founder)||
                   grant.getValue()==null||grant.getValue().isEmpty()||
                   !BLUEPRINTS.keySet().containsAll(grant.getValue()))
                    throw new IllegalArgumentException("Invalid builder grant");
                m.grants.put(grant.getKey(),Set.copyOf(grant.getValue()));
            }
            for(var item:source.structures().entrySet()){
                if(!id(item.getKey())||item.getValue()==null||
                   !BLUEPRINTS.containsKey(item.getValue()))
                    throw new IllegalArgumentException("Invalid structure allocation");
                m.structures.put(item.getKey(),item.getValue());
            }
            // Excess after a hardcore reset is intentionally retained,
            // but blocks new placements and needs host-side suspension.
            validated.put(m.id,m);
        }
        bases.clear();bases.putAll(validated);
    }
}
