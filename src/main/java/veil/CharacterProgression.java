package veil;

import java.util.*;

/** Host-side RPG progression. Lifetime ability XP persists; current-life streak does not. */
public final class CharacterProgression {
    public enum Skill { MARKSMANSHIP, ATHLETICS, ENGINEERING, SCAVENGING, SURVIVAL }
    public enum Threat { FADED, EQUAL, DANGEROUS, DEADLY }
    public record Snapshot(Map<Skill,Long> experience,long aliveMs) {}
    public record View(int level,long experience,long nextLevelExperience,
                       long aliveMs,double bonus,Map<Skill,Integer> skills) {}
    public record ThreatMark(Threat threat,String icon,String colorHex,String label) {}
    private static final int CAP=50;
    // Steeply increasing permanent progression; level 50 is a long-term achievement.
    private final EnumMap<Skill,Long> experience=new EnumMap<>(Skill.class);
    private final EnumMap<Skill,Long> lastReward=new EnumMap<>(Skill.class);
    private long aliveMs;
    public CharacterProgression(){for(Skill s:Skill.values())experience.put(s,0L);}
    public static long levelThreshold(int level){
        if(level<=1)return 0;
        if(level>CAP+1)throw new IllegalArgumentException("Unbounded level");
        long n=level-1L;
        return 400L*n*n*n+120L*n*n;
    }
    public static int levelFor(long total){
        if(total<0)throw new IllegalArgumentException("Negative experience");
        int level=1;
        while(level<CAP&&total>=levelThreshold(level+1))level++;
        return level;
    }
    public synchronized int level(Skill skill){return levelFor(experience.get(skill));}
    public synchronized int overallLevel(){
        // Never reach huge combat bonuses by grinding a single low-risk activity.
        long aggregate=0;
        for(long earned:experience.values())aggregate+=earned;
        return levelFor(aggregate/Skill.values().length);
    }
    public synchronized double multiplier(){
        // +0.1x every 30 minutes alive, caps at +1.5x after 7.5h.
        return 1.0+Math.min(15,aliveMs/1_800_000L)*.10;
    }
    public synchronized void aliveTick(long ms){
        if(ms<=0||ms>1000)throw new IllegalArgumentException("Unbounded host progression tick");
        aliveMs=Math.min(Long.MAX_VALUE-ms,aliveMs)+ms;
    }
    public synchronized void died(){aliveMs=0;}
    /** Meaningful server-verified event only; debounces repeated triggers. */
    public synchronized long reward(Skill skill,long base,long hostClock,long minimumInterval){
        Objects.requireNonNull(skill);
        if(base<1||base>200||hostClock<0||minimumInterval<0)
            throw new IllegalArgumentException("Invalid authoritative XP award");
        Long previous=lastReward.get(skill);
        if(previous!=null&&(hostClock<previous||hostClock-previous<minimumInterval))return 0;
        lastReward.put(skill,hostClock);
        long gained=Math.min(500L,Math.round(base*multiplier()));
        experience.put(skill,Math.min(levelThreshold(CAP+1),experience.get(skill)+gained));
        return gained;
    }
    public synchronized double movementMultiplier(){
        return 1+Math.min(.15,(level(Skill.ATHLETICS)-1)*.003);
    }
    public synchronized double damageMultiplier(){
        return 1+Math.min(.20,(level(Skill.MARKSMANSHIP)-1)*.004);
    }
    public synchronized double reloadMultiplier(){
        return Math.max(.80,1-(level(Skill.MARKSMANSHIP)-1)*.004);
    }
    public synchronized double craftingMultiplier(){
        return Math.max(.75,1-(level(Skill.ENGINEERING)-1)*.005);
    }
    public synchronized View view(){
        int lvl=overallLevel();
        long average=0;
        Map<Skill,Integer> levels=new EnumMap<>(Skill.class);
        for(Skill skill:Skill.values()){
            average+=experience.get(skill);
            levels.put(skill,level(skill));
        }
        average/=Skill.values().length;
        return new View(lvl,average,levelThreshold(Math.min(CAP+1,lvl+1)),
            aliveMs,multiplier(),Map.copyOf(levels));
    }
    public synchronized Snapshot snapshot(){return new Snapshot(Map.copyOf(experience),aliveMs);}
    public synchronized void restore(Snapshot saved){
        if(saved==null||saved.aliveMs()<0||saved.aliveMs()>31_536_000_000L||
            saved.experience().size()!=Skill.values().length)
            throw new IllegalArgumentException("Invalid character skills");
        for(Skill skill:Skill.values()){
            Long value=saved.experience().get(skill);
            if(value==null||value<0||value>levelThreshold(CAP+1))
                throw new IllegalArgumentException("Corrupt skill XP");
        }
        experience.clear();experience.putAll(saved.experience());
        aliveMs=saved.aliveMs();
        lastReward.clear(); // New connection must not invent offline XP.
    }
    public static ThreatMark threat(int playerLevel,int npcLevel){
        if(playerLevel<1||npcLevel<1)throw new IllegalArgumentException("Invalid threat level");
        int delta=npcLevel-playerLevel;
        if(delta>5)return new ThreatMark(Threat.DEADLY,"☠","#D93A38","LETHAL");
        if(delta>0)return new ThreatMark(Threat.DANGEROUS,"!","#F3A33A","DANGEROUS");
        if(delta< -5)return new ThreatMark(Threat.FADED,"○","#8DD8F5","TRIVIAL");
        return new ThreatMark(Threat.EQUAL,"◆","#66CF75","EVEN");
    }
    /** Stable creature challenge rating from its source plot, variant, and archetype. */
    public static int monsterLevel(long worldSeed,World.Npc npc){
        int base=switch(npc.kind){
            case "lost-brute" -> 13;
            case "lost-wailer" -> 8;
            case "lost-stalker" -> 6;
            default -> 3;
        };
        long hash=World.mix(worldSeed^((long)npc.sourceSite*0x9e3779b97f4a7c15L));
        return Math.max(1,base+(int)Math.floorMod(hash,9)-3);
    }
}
