package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Host-authoritative AI faction selection; no client may alter these rules. */
public final class HostAiSettings {
    public enum Distribution { BALANCED, SINGLE_FACTION, CUSTOM }
    public static final int MAX_BOTS=24;
    public record Rules(int count, Distribution distribution, int faction, int level,
                        int unraveled, int penitent, int begotten) {
        public Rules {
            if(count<0||count>MAX_BOTS||distribution==null||level<1||level>50||
               faction<1||faction>3||unraveled<0||penitent<0||begotten<0||
               unraveled>MAX_BOTS||penitent>MAX_BOTS||begotten>MAX_BOTS)
                throw new IllegalArgumentException("Invalid host AI setup");
            if(distribution==Distribution.CUSTOM&&unraveled+penitent+begotten!=count)
                throw new IllegalArgumentException("Custom AI counts must total --ai-count");
        }
        public int[] counts(){
            if(distribution==Distribution.CUSTOM)return new int[]{unraveled,penitent,begotten};
            int[] result=new int[3];
            if(distribution==Distribution.SINGLE_FACTION)result[faction-1]=count;
            else for(int i=0;i<count;i++)result[i%3]++;
            return result;
        }
        public CharacterRoster.Faction teamFor(int slot){
            if(slot<0||slot>=count)throw new IllegalArgumentException("AI index");
            int[] remaining=counts();
            if(distribution==Distribution.BALANCED)
                return CharacterRoster.Faction.values()[slot%3];
            for(int i=0;i<3;i++){
                if(slot<remaining[i])return CharacterRoster.Faction.values()[i];
                slot-=remaining[i];
            }
            throw new IllegalStateException("Invalid AI roster");
        }
        public String description(){
            return "AI="+count+" mode="+distribution+" factions="+
                Arrays.toString(counts())+" level="+level+" group-cap=3";
        }
    }
    private HostAiSettings(){}
    public static Rules defaults(){return new Rules(0,Distribution.BALANCED,1,1,0,0,0);}
    /** Dedicated CLI flags and normal launcher system properties are equivalent. */
    public static Rules fromOptions(Collection<String> flags){
        Map<String,String> values=new HashMap<>();
        for(String flag:flags){
            if(!flag.startsWith("--ai-")||!flag.contains("="))
                throw new IllegalArgumentException("Unknown AI host option "+flag);
            int split=flag.indexOf('=');
            String name=flag.substring(5,split),value=flag.substring(split+1);
            if(!Set.of("count","mode","team","level","split").contains(name)||
               value.isBlank()||values.putIfAbsent(name,value)!=null)
                throw new IllegalArgumentException("Invalid or duplicate AI option "+flag);
        }
        int count=parse(values.getOrDefault("count","0"),0,MAX_BOTS);
        int level=parse(values.getOrDefault("level","1"),1,50);
        int team=parse(values.getOrDefault("team","1"),1,3);
        Distribution mode;
        try{mode=Distribution.valueOf(values.getOrDefault("mode","BALANCED").toUpperCase(Locale.ROOT));}
        catch(IllegalArgumentException ex){throw new IllegalArgumentException(
            "AI mode must be BALANCED, SINGLE_FACTION or CUSTOM",ex);}
        int[] split={0,0,0};
        if(values.containsKey("split")){
            String[] s=values.get("split").split(",",-1);
            if(s.length!=3)throw new IllegalArgumentException("Use --ai-split=unraveled,penitent,begotten");
            for(int i=0;i<3;i++)split[i]=parse(s[i],0,MAX_BOTS);
            if(mode!=Distribution.CUSTOM)throw new IllegalArgumentException("AI split requires CUSTOM mode");
        }else if(mode==Distribution.CUSTOM)throw new IllegalArgumentException("CUSTOM mode requires --ai-split");
        return new Rules(count,mode,team,level,split[0],split[1],split[2]);
    }
    public static Rules fromProperties(){
        List<String> args=new ArrayList<>();
        for(String name:List.of("count","mode","team","level","split")){
            String value=System.getProperty("veil.ai."+name);
            if(value!=null)args.add("--ai-"+name+"="+value);
        }
        return fromOptions(args);
    }
    private static int parse(String raw,int min,int max){
        int n;
        try{n=Integer.parseInt(raw);}catch(NumberFormatException error){
            throw new IllegalArgumentException("Noninteger AI setting: "+raw,error);
        }
        if(n<min||n>max)throw new IllegalArgumentException("AI setting out of bounds "+raw);
        return n;
    }
    /** Store exactly what was selected, rather than changing an old host silently. */
    public static void save(Path root,Rules r)throws IOException {
        if(root==null)return;
        Files.createDirectories(root);
        Path path=root.resolve("ai-host-settings.properties");
        String text="schema=1\ncount="+r.count()+"\nmode="+r.distribution()+
            "\nteam="+r.faction()+"\nlevel="+r.level()+"\nsplit="+
            r.unraveled()+","+r.penitent()+","+r.begotten()+"\n";
        Path temp=Files.createTempFile(root,".ai-host-", ".tmp");
        try{
            Files.writeString(temp,text);
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
            if(!text.equals(Files.readString(path)))throw new IOException("AI settings read-back mismatch");
        }finally{Files.deleteIfExists(temp);}
    }
}
