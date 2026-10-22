package veil;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.zip.CRC32;

/**
 * Dedicated-host NPC settlers are ordinary ArenaSystems.Player actors:
 * identical collision, faction allegiance, XP, carried resources, crafting,
 * combat and building transactions. AI never submits a forged client packet.
 * Group membership is enforced by FactionGroups (max three, all same-faction allied).
 */
public final class FactionAiDirector {
    public enum Activity { EXPLORING, SEEKING_RESOURCES, HARVESTING, CRAFTING,
                           BUILDING, DEFENDING, RECOVERING }
    public enum Duty { BUILDER, PROVISIONER, SENTINEL }
    public record View(int id,CharacterRoster.Faction faction,int level,
                       String group,Activity activity,double x,double y,
                       int carried,int buildings,Duty duty) {}
    private static final int BOT_ID_BASE=-10000;
    private static final class Agent {
        int id;
        CharacterRoster.Faction faction;
        double homeX,homeY;
        int target=-1,buildings;
        long nextDecision;
        Activity activity=Activity.EXPLORING;
        String crew;
        Duty duty=Duty.BUILDER;
        boolean returningHome;
        int stuck;
    }
    private final ArenaSystems arena;
    private final WorldHarvesting harvesting;
    private final HostAiSettings.Rules rules;
    private final List<Agent> agents=new ArrayList<>();
    private final long seed;
    private long lastTick;
    private static String accountKey(long seed,int botId){
        try{
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] data=md.digest(("veil-host-ai|"+seed+"|"+botId)
                .getBytes(StandardCharsets.UTF_8));
            StringBuilder s=new StringBuilder("acct_");
            for(byte b:data)s.append(String.format(Locale.ROOT,"%02x",b&255));
            return s.toString();
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    public FactionAiDirector(ArenaSystems arena,WorldHarvesting harvesting,
                             HostAiSettings.Rules rules){
        this.arena=Objects.requireNonNull(arena);
        this.harvesting=Objects.requireNonNull(harvesting);
        this.rules=Objects.requireNonNull(rules);
        this.seed=arena.world.seed;
        var sources=harvesting.resources.nodes();
        if(rules.count()>0&&sources.isEmpty())
            throw new IllegalStateException("AI requires physical resources to gather");
        for(int i=0;i<rules.count();i++){
            Agent agent=new Agent();
            agent.id=BOT_ID_BASE-i;
            agent.faction=rules.teamFor(i);
            int sameFaction=0;
            for(Agent previous:agents)if(previous.faction==agent.faction)sameFaction++;
            agent.duty=Duty.values()[sameFaction%Duty.values().length];
            var player=arena.join(agent.id);
            String account=accountKey(seed,agent.id);
            if(!arena.assignAccount(agent.id,account)||
               !arena.enrollFaction(agent.id,agent.faction))
                throw new IllegalStateException("AI faction account unavailable");
            // Explicit host-selected starting rank; never override a higher
            // rank restored from this same character's prior game sessions.
            if(player.progression.overallLevel()<rules.level()){
                EnumMap<CharacterProgression.Skill,Long> ranks=
                    new EnumMap<>(CharacterProgression.Skill.class);
                long threshold=CharacterProgression.levelThreshold(rules.level());
                for(var skill:CharacterProgression.Skill.values())ranks.put(skill,threshold);
                player.progression.restore(new CharacterProgression.Snapshot(
                    Map.copyOf(ranks),0));
            }
            player.classId=agent.faction==CharacterRoster.Faction.THE_PENITENT?3:0;
            player.ready=true;
            if(arena.world.detailedCity(player.x,player.y)||
               !arena.world.canWalk(player.x,player.y)){
                // Bot creation is world generation, not a travel teleport.
                // Seeded initial sites are adjacent to real wilderness nodes.
                boolean located=false;
                int offset=(int)Math.floorMod(World.mix(seed+agent.id),sources.size());
                for(int j=0;j<sources.size()&&!located;j++){
                    var node=sources.get((offset+j)%sources.size());
                    if(j%3!=0&&j>8)continue;
                    double x=harvesting.nodeX(node),y=harvesting.nodeY(node);
                    for(int direction=0;direction<8;direction++){
                        double angle=Math.PI*direction/4;
                        double ax=x-1.55*Math.cos(angle),ay=y-1.55*Math.sin(angle);
                        if(!arena.world.detailedCity(ax,ay)&&arena.world.canWalk(ax,ay)&&
                           arena.world.lineClear(ax,ay,x,y)){
                            player.x=ax;player.y=ay;player.angle=angle;
                            located=true;break;
                        }
                    }
                }
                if(!located)throw new IllegalStateException("No world-valid AI start");
            }
            // A valid saved physical camp is authoritative after restart:
            // don't teleport or reset construction progress for returning AI.
            agent.homeX=player.x;agent.homeY=player.y;
            for(World.Device device:arena.world.devices){
                if(device.hp>0&&account.equals(device.ownerKey)){
                    agent.buildings++;
                    if(device.kind==ArenaSystems.CAMP_STAKE){
                        agent.homeX=device.x-1.1;agent.homeY=device.y;
                    }
                }
            }
            player.moveAt=arena.time();
            agents.add(agent);
        }
        // Genuine faction-group membership: three maximum, but separate
        // same-faction groups remain naturally allied by FactionGroups.
        for(var faction:CharacterRoster.Faction.values()){
            List<Agent> same=agents.stream().filter(a->a.faction==faction).toList();
            for(int start=0;start<same.size();start+=3){
                var founder=same.get(start);
                String account=arena.player(founder.id).accountKey;
                String groupName="AI "+faction.name().replace("THE_","").substring(0,3)+
                    " Crew "+(1+start/3);
                var crew=arena.factions.crewFor(account);
                if(crew==null)crew=arena.factions.create(account,groupName);
                if(crew==null)throw new IllegalStateException("AI crew registration failed");
                founder.crew=crew.id();
                for(int k=start+1;k<Math.min(start+3,same.size());k++){
                    Agent mate=same.get(k);
                    String other=arena.player(mate.id).accountKey;
                    if(arena.factions.crewId(other)==null&&
                       (!arena.factions.invite(account,other,arena.time())||
                        !arena.factions.accept(other,arena.time())))
                        throw new IllegalStateException("AI squad formation failed");
                    mate.crew=arena.factions.crewId(other);
                }
            }
        }
    }
    /** Crash-safe, checksum-verified carried stock. Player XP and position stay
     * in AccountDirectory's existing canonical character snapshot. */
    public synchronized void save(Path root)throws IOException{
        if(root==null)return;
        Files.createDirectories(root);
        var buffer=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(buffer)){
            out.writeInt(0x56414931); // VAI1
            out.writeLong(seed);
            out.writeInt(agents.size());
            for(var agent:agents){
                var p=arena.player(agent.id);
                if(p==null)throw new IOException("Missing live AI actor "+agent.id);
                var status=p.survival.status();
                out.writeInt(agent.id);
                out.writeUTF(p.accountKey);
                out.writeInt(status.hunger());
                out.writeInt(status.hydration());
                out.writeInt(status.inventory().size());
                for(var item:status.inventory().entrySet()){
                    out.writeUTF(item.getKey().name());
                    out.writeInt(item.getValue());
                }
            }
        }
        byte[] payload=buffer.toByteArray();
        CRC32 crc=new CRC32();crc.update(payload);
        try(var out=new DataOutputStream(buffer)){out.writeLong(crc.getValue());}
        Path to=root.resolve("ai-carry.dat");
        Path temp=Files.createTempFile(root,".ai-carry-", ".tmp");
        try{
            Files.write(temp,buffer.toByteArray());
            try{Files.move(temp,to,StandardCopyOption.REPLACE_EXISTING,
                           StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){
                Files.move(temp,to,StandardCopyOption.REPLACE_EXISTING);
            }
            if(!java.util.Arrays.equals(buffer.toByteArray(),Files.readAllBytes(to)))
                throw new IOException("AI inventory save read-back mismatch");
        }finally{Files.deleteIfExists(temp);}
    }
    public synchronized void load(Path root)throws IOException{
        if(root==null)return;
        Path file=root.resolve("ai-carry.dat");
        if(!Files.exists(file))return;
        byte[] bytes=Files.readAllBytes(file);
        if(bytes.length<24||bytes.length>64_000)
            throw new IOException("Invalid AI carry journal length");
        CRC32 crc=new CRC32();crc.update(bytes,0,bytes.length-8);
        var decoded=new HashMap<Integer,SurvivalCrafting.Status>();
        try(var in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=0x56414931||in.readLong()!=seed)
                throw new IOException("AI carry journal from a different world");
            int count=in.readInt();
            if(count<0||count>HostAiSettings.MAX_BOTS)
                throw new IOException("Invalid AI carry actor count");
            for(int i=0;i<count;i++){
                int id=in.readInt();
                String account=in.readUTF();
                int hunger=in.readInt(),water=in.readInt();
                int n=in.readInt();
                if(n<0||n>SurvivalCrafting.Item.values().length||
                   decoded.containsKey(id))throw new IOException("Corrupt AI item table");
                var inventory=new EnumMap<SurvivalCrafting.Item,Integer>(
                    SurvivalCrafting.Item.class);
                for(int j=0;j<n;j++){
                    var item=SurvivalCrafting.Item.valueOf(in.readUTF());
                    if(inventory.putIfAbsent(item,in.readInt())!=null)
                        throw new IOException("Duplicate bot item");
                }
                // Validate the entire save with throwaway inventories BEFORE
                // changing any actor or potentially exposing a half-loaded host.
                var status=new SurvivalCrafting.Status(hunger,water,Map.copyOf(inventory));
                new SurvivalCrafting().restoreStatus(status);
                var actor=arena.player(id);
                if(actor!=null&&actor.accountKey.equals(account))decoded.put(id,status);
            }
            if(in.readLong()!=crc.getValue()||in.available()!=0)
                throw new IOException("AI carry checksum failure");
        }catch(IllegalArgumentException error){
            throw new IOException("Corrupt AI inventory data",error);
        }
        for(var record:decoded.entrySet())
            arena.player(record.getKey()).survival.restoreStatus(record.getValue());
    }
    public HostAiSettings.Rules settings(){return rules;}
    public synchronized List<View> views(){
        List<View> out=new ArrayList<>();
        for(Agent agent:agents){
            ArenaSystems.Player p=arena.player(agent.id);
            if(p!=null)out.add(new View(agent.id,agent.faction,
                p.progression.overallLevel(),agent.crew,agent.activity,
                p.x,p.y,p.survival.mass(),agent.buildings,agent.duty));
        }
        return List.copyOf(out);
    }
    private boolean walk(Agent agent,ArenaSystems.Player p,double x,double y,long ms){
        double dx=x-p.x,dy=y-p.y,dist=Math.hypot(dx,dy);
        if(dist<.01)return true;
        double heading=Math.atan2(dy,dx);
        double step=Math.min(.18,Math.min(dist,2.4*ms/1000.));
        if(arena.move(agent.id,p.x+dx/dist*step,p.y+dy/dist*step,heading,0)){
            agent.stuck=0;return true;
        }
        // Obstacle avoidance remains physical, using the exact same host
        // movement/collision check as human characters.
        double side=(agent.id&1)==0?1:-1;
        for(double offset:new double[]{side*.65,-side*.65,side*1.5,-side*1.5}){
            double angle=heading+offset;
            if(arena.move(agent.id,p.x+Math.cos(angle)*step,
                           p.y+Math.sin(angle)*step,angle,0)){
                agent.stuck=0;return true;
            }
        }
        agent.stuck++;return false;
    }
    /**
     * Host-owned tactical awareness. No client-supplied positions, targets or
     * ammo; shots go through ArenaSystems.fireReport and consume real rounds.
     * Same-faction squads NEVER fire at each other; Lost monsters remain
     * allied with the Begotten in both the director and the combat engine.
     */
    private boolean combat(Agent agent,ArenaSystems.Player actor,long ms){
        // Emergency consumables are finite inventory, not free AI regeneration.
        if(actor.hp<=54&&actor.medkits>0)arena.useMedkit(agent.id);
        double vision=agent.faction==CharacterRoster.Faction.THE_UNRAVELED?7.5:11;
        double closest=vision;
        double tx=0,ty=0;
        boolean danger=false;
        for(ArenaSystems.Player other:arena.roster()){
            if(other.id==actor.id||other.dead||arena.allied(actor.id,other.id))continue;
            double distance=Math.hypot(other.x-actor.x,other.y-actor.y);
            if(distance>=closest||distance<.35||
               !arena.world.lineClear(actor.x,actor.y,other.x,other.y))continue;
            // Civilian AI defend their proximity/settlement rather than
            // pathfinding across the continent to murder arbitrary players.
            if(agent.faction==CharacterRoster.Faction.THE_UNRAVELED&&
               distance>5.5&&Math.hypot(other.x-agent.homeX,
                                        other.y-agent.homeY)>7.5)continue;
            closest=distance;tx=other.x;ty=other.y;danger=true;
        }
        if(agent.faction!=CharacterRoster.Faction.THE_BEGOTTEN)
            for(World.Npc npc:arena.world.npcs){
                if(npc.hp<=0)continue;
                double distance=Math.hypot(npc.x-actor.x,npc.y-actor.y);
                if(distance>=closest||distance<.35||
                    !arena.world.lineClear(actor.x,actor.y,npc.x,npc.y))continue;
                closest=distance;tx=npc.x;ty=npc.y;danger=true;
            }
        if(!danger)return false;
        agent.activity=Activity.DEFENDING;
        double dx=tx-actor.x,dy=ty-actor.y;
        double bearing=Math.atan2(dy,dx);
        // Retreat is actual swept movement, subject to the same host speed
        // and obstacle checks. No invulnerability, instant teleport or ammo.
        if(actor.hp<30&&closest<3.7)
            walk(agent,actor,actor.x-dx,actor.y-dy,ms);
        actor.angle=bearing;
        // A hostile farther down the ray must never make the AI shoot through
        // an allied player standing between it and the weapon muzzle.
        for(ArenaSystems.Player ally:arena.roster()){
            if(ally.id==actor.id||ally.dead||!arena.allied(actor.id,ally.id))continue;
            double ax=ally.x-actor.x,ay=ally.y-actor.y;
            double projected=ax*Math.cos(bearing)+ay*Math.sin(bearing);
            double offset=Math.abs(ax*Math.sin(bearing)-ay*Math.cos(bearing));
            if(projected>.05&&projected<closest&&offset<.48)return true;
        }
        int weapon=closest<3.2&&actor.shellClip>0?1:0;
        if((weapon==0&&actor.pistolClip==0)||(weapon==1&&actor.shellClip==0)){
            arena.reload(agent.id,weapon);
            return true;
        }
        if(closest<=12)
            arena.fireReport(agent.id,weapon,Math.max(1,arena.time()+Math.abs(agent.id)));
        return true;
    }
    /** Reconcile from surviving physical structures, never an AI-only counter. */
    private void recountBuildings(Agent agent,ArenaSystems.Player player){
        int total=0;
        for(World.Device device:arena.world.devices)
            if(device.hp>0&&player.accountKey.equals(device.ownerKey))total++;
        agent.buildings=total;
    }
    private WildernessResources.Node select(Agent a,ArenaSystems.Player p){
        WildernessResources.Node best=null;
        double score=Double.POSITIVE_INFINITY;
        for(var n:harvesting.nearby(p.x,p.y,90)){
            if(n.remaining()<1)continue;
            double x=harvesting.nodeX(n),y=harvesting.nodeY(n);
            double dist=Math.hypot(x-p.x,y-p.y);
            if(dist<.01)continue;
            var item=SurvivalCrafting.Item.valueOf(n.type().name());
            if(!p.survival.canAcquire(item,1,false))continue;
            double need=switch(item){
                case TIMBER -> p.survival.count(item)<6?-12:0;
                case FIBER -> p.survival.count(item)<6?-11:0;
                case FIELD_STONE -> p.survival.count(item)<4?-9:0;
                case EDIBLES -> p.survival.status().hunger()<70?-14:2;
                case DIRTY_WATER -> p.survival.count(item)<2?-5:3;
                default -> 3;
            };
            // Distinct, deterministic supply priorities per three-member
            // crew. Actual material availability and capacity still gate each
            // harvest; this never fabricates faction resources.
            double purpose=switch(a.duty){
                case BUILDER -> switch(item){
                    case TIMBER,FIBER,FIELD_STONE -> -7;
                    default -> 1;
                };
                case PROVISIONER -> switch(item){
                    case EDIBLES,MEDICINAL_HERBS,DIRTY_WATER -> -10;
                    default -> 1;
                };
                case SENTINEL -> switch(item){
                    case RAW_ORE,FIELD_STONE,FIBER -> -7;
                    default -> 2;
                };
            };
            double next=dist+need+purpose;
            if(next<score){score=next;best=n;}
        }
        return best;
    }
    private void decision(Agent agent,ArenaSystems.Player p,long ms){
        if(p.dead){
            agent.activity=Activity.RECOVERING;
            if(arena.respawnRemainingMs(agent.id)==0&&arena.respawn(agent.id,0))
                agent.returningHome=true;
            return;
        }
        recountBuildings(agent,p);
        if(agent.returningHome){
            agent.activity=Activity.RECOVERING;
            if(Math.hypot(p.x-agent.homeX,p.y-agent.homeY)>3.0){
                walk(agent,p,agent.homeX,agent.homeY,ms);
                return;
            }
            agent.returningHome=false;
        }
        if(combat(agent,p,ms))return;
        if(p.survival.status().hunger()<75&&p.survival.count(SurvivalCrafting.Item.EDIBLES)>0)
            p.survival.consume(SurvivalCrafting.Item.EDIBLES);
        // Real host crafting consumes gathered inventory, never grants stock.
        if(arena.time()>=p.craftReadyAt){
            if(p.survival.count(SurvivalCrafting.Item.TENT_KIT)==0&&
               arena.craftHand(agent.id,"tent"))agent.activity=Activity.CRAFTING;
            else if(p.survival.count(SurvivalCrafting.Item.CAMPFIRE)==0&&
                    arena.craftHand(agent.id,"campfire"))agent.activity=Activity.CRAFTING;
            else if(p.survival.count(SurvivalCrafting.Item.CRATE_KIT)==0&&
                    arena.craftHand(agent.id,"crate"))agent.activity=Activity.CRAFTING;
        }
        // Carry the actually crafted kit back across the same continuous
        // terrain before authorizing a physical, character-owned field camp.
        if(agent.buildings==0&&p.survival.count(SurvivalCrafting.Item.TENT_KIT)>0){
            if(Math.hypot(p.x-agent.homeX,p.y-agent.homeY)>1.6){
                agent.activity=Activity.BUILDING;
                walk(agent,p,agent.homeX,agent.homeY,ms);
            } else if(p.cash>=35&&p.scrap>=1){
                for(int step=0;step<8;step++){
                    p.angle=(step*Math.PI/4);
                    var result=arena.buildOutcome(agent.id,ArenaSystems.CAMP_STAKE);
                    if(result.built()){
                        agent.buildings=1;
                        agent.activity=Activity.BUILDING;
                        break;
                    }
                }
            }
            return;
        }
        // Three-character crews trade real inventory at arm's reach.
        // These are material handoffs, NOT free remote production.
        if(agent.buildings>=1&&p.survival.mass()>=25){
            var crew=arena.factions.crewFor(p.accountKey);
            if(crew!=null&&!crew.founder().equals(p.accountKey)){
                ArenaSystems.Player leader=null;
                for(Agent member:agents){
                    var candidate=arena.player(member.id);
                    if(candidate!=null&&candidate.accountKey.equals(crew.founder()))
                        leader=candidate;
                }
                if(leader!=null&&!leader.dead){
                    double range=Math.hypot(p.x-leader.x,p.y-leader.y);
                    if(range>1.7){
                        agent.activity=Activity.SEEKING_RESOURCES;
                        walk(agent,p,leader.x,leader.y,ms);
                        return;
                    }
                    for(var item:SurvivalCrafting.Item.values()){
                        if(item==SurvivalCrafting.Item.TENT_KIT)continue;
                        int spare=p.survival.count(item)-2;
                        if(spare>0)p.survival.transferTo(leader.survival,item,
                            Math.min(8,spare));
                    }
                }
            }
        }
        if(agent.buildings==1&&p.cash>=60&&p.scrap>=3){
            if(Math.hypot(p.x-agent.homeX,p.y-agent.homeY)>2.5){
                walk(agent,p,agent.homeX,agent.homeY,ms);
            }else{
                for(int step=0;step<8;step++){
                    p.angle=(step*Math.PI/4);
                    if(arena.buildOutcome(agent.id,ArenaSystems.TURRET).built()){
                        agent.buildings++;agent.activity=Activity.BUILDING;break;
                    }
                }
            }
        }
        if(agent.target<0||harvesting.resources.node(agent.target)==null||
           harvesting.resources.node(agent.target).remaining()<1||
           agent.stuck>45||arena.time()>=agent.nextDecision){
            var source=select(agent,p);
            agent.target=source==null?-1:source.id();
            agent.nextDecision=arena.time()+6000;
            agent.stuck=0;
        }
        if(agent.target>=0){
            var resource=harvesting.resources.node(agent.target);
            double x=harvesting.nodeX(resource),y=harvesting.nodeY(resource);
            double distance=Math.hypot(p.x-x,p.y-y);
            if(distance>1.5){
                agent.activity=Activity.SEEKING_RESOURCES;
                walk(agent,p,x,y,ms);
            }else {
                // Actual authored ray / LOS / cooldown / capacity are checked
                // *again* inside WorldHarvesting.harvest.
                p.angle=Math.atan2(y-p.y,x-p.x);
                agent.activity=Activity.HARVESTING;
                var earned=harvesting.harvest(p,resource.id());
                if(earned.accepted())agent.nextDecision=arena.time()+1800;
            }
        } else {
            agent.activity=Activity.EXPLORING;
            double angle=(agent.id*13+arena.time()/11000L)%63*.13;
            double radius=5+Math.floorMod(agent.id,7);
            walk(agent,p,agent.homeX+Math.cos(angle)*radius,
                agent.homeY+Math.sin(angle)*radius,ms);
        }
    }
    /** Called after the ordinary ArenaSystems advance, never on clients. */
    public synchronized void advance(long millis){
        if(millis<=0||millis>1000)throw new IllegalArgumentException("Invalid host AI frame");
        lastTick+=millis;
        // AI awareness runs at 5 Hz per agent and still moves at the real
        // host speed cap. No wall clipping or instantaneous resource delivery.
        if(lastTick%200!=0)return;
        for(Agent agent:agents){
            var p=arena.player(agent.id);
            if(p!=null)decision(agent,p,200);
        }
    }
}
