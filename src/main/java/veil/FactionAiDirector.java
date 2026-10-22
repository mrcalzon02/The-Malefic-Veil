package veil;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * Dedicated-host NPC settlers are ordinary ArenaSystems.Player actors:
 * identical collision, faction allegiance, XP, carried resources, crafting,
 * combat and building transactions. AI never submits a forged client packet.
 * Group membership is enforced by FactionGroups (max three, all same-faction allied).
 */
public final class FactionAiDirector {
    public enum Activity { EXPLORING, SEEKING_RESOURCES, HARVESTING, CRAFTING,
                           BUILDING, DEFENDING, RECOVERING }
    public record View(int id,CharacterRoster.Faction faction,int level,
                       String group,Activity activity,double x,double y,
                       int carried,int buildings) {}
    private static final int BOT_ID_BASE=-10000;
    private static final class Agent {
        int id;
        CharacterRoster.Faction faction;
        double homeX,homeY;
        int target=-1,buildings;
        long nextDecision;
        Activity activity=Activity.EXPLORING;
        String crew;
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
            agent.homeX=player.x;agent.homeY=player.y;
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
    public HostAiSettings.Rules settings(){return rules;}
    public synchronized List<View> views(){
        List<View> out=new ArrayList<>();
        for(Agent agent:agents){
            ArenaSystems.Player p=arena.player(agent.id);
            if(p!=null)out.add(new View(agent.id,agent.faction,
                p.progression.overallLevel(),agent.crew,agent.activity,
                p.x,p.y,p.survival.mass(),agent.buildings));
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
            double next=dist+need;
            if(next<score){score=next;best=n;}
        }
        return best;
    }
    private void decision(Agent agent,ArenaSystems.Player p,long ms){
        if(p.dead){
            agent.activity=Activity.RECOVERING;
            if(arena.respawnRemainingMs(agent.id)==0)arena.respawn(agent.id,0);
            return;
        }
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
        // Build only after physically crafting a camp kit from harvested
        // materials; use the regular cost/placement/collision transaction.
        // These early field structures are not yet permanent settlement claims.
        if(agent.buildings<2&&p.survival.count(SurvivalCrafting.Item.TENT_KIT)>0&&
           p.cash>=45&&p.scrap>=1&&Math.hypot(p.x-agent.homeX,p.y-agent.homeY)<8){
            int kind=agent.buildings==0?ArenaSystems.BARRICADE:ArenaSystems.HEALTH_STATION;
            var built=arena.buildOutcome(agent.id,kind);
            if(built.built()){agent.buildings++;agent.activity=Activity.BUILDING;}
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
