package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Dedicated-host account, hardcore skill and faction crew directory.
 * Separate CRC-framed source of truth for character/crew metadata. This file
 * does not replace realm.dat; a future cross-zone transaction journal must
 * make inventory/crew/world transfers atomic across their durable domains.
 */
public final class AccountDirectory {
    private static final int MAGIC=0x56414331,VERSION=1,MAX=2_000_000;
    private AccountDirectory(){}
    private record State(long seed,boolean hardcore,
        Map<String,ArenaSystems.Profile> profiles,
        Map<String,CharacterProgression.Snapshot> skills,
        FactionGroups.Snapshot groups){}
    private static boolean account(String key){return key.matches("acct_[a-f0-9]{64}");}
    private static State capture(ArenaSystems arena){
        return new State(arena.world.seed,arena.hardcore,arena.profilesSnapshot(),
            arena.skillsSnapshot(),arena.factions.snapshot());
    }
    private static void profile(DataOutputStream out,ArenaSystems.Profile p)throws IOException{
        out.writeInt(p.hp());out.writeInt(p.sanity());out.writeInt(p.cash());
        out.writeInt(p.scrap());out.writeInt(p.medkits());out.writeInt(p.pistolReserve());
        out.writeInt(p.shellReserve());out.writeInt(p.pistolClip());out.writeInt(p.shellClip());
        out.writeInt(p.classId());out.writeInt(p.frags());out.writeBoolean(p.dead());
        out.writeDouble(p.x());out.writeDouble(p.y());out.writeDouble(p.angle());
    }
    private static ArenaSystems.Profile profile(DataInputStream in)throws IOException{
        return new ArenaSystems.Profile(in.readInt(),in.readInt(),in.readInt(),in.readInt(),
            in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),
            in.readInt(),in.readBoolean(),in.readDouble(),in.readDouble(),in.readDouble());
    }
    private static byte[] serialize(State state)throws IOException{
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(buffer)){
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeLong(state.seed());
            out.writeBoolean(state.hardcore());
            out.writeInt(state.profiles().size());
            for(var entry:new TreeMap<>(state.profiles()).entrySet()){
                out.writeUTF(entry.getKey());profile(out,entry.getValue());
            }
            out.writeInt(state.skills().size());
            for(var entry:new TreeMap<>(state.skills()).entrySet()){
                out.writeUTF(entry.getKey());
                out.writeLong(entry.getValue().aliveMs());
                for(CharacterProgression.Skill skill:CharacterProgression.Skill.values())
                    out.writeLong(entry.getValue().experience().get(skill));
            }
            FactionGroups.Snapshot factions=state.groups();
            out.writeInt(factions.factions().size());
            for(var entry:new TreeMap<>(factions.factions()).entrySet()){
                out.writeUTF(entry.getKey());out.writeInt(entry.getValue().ordinal());
            }
            out.writeLong(factions.nextCrewId());out.writeInt(factions.crews().size());
            for(FactionGroups.Crew crew:factions.crews()){
                out.writeUTF(crew.id());out.writeUTF(crew.name());
                out.writeInt(crew.faction().ordinal());out.writeUTF(crew.founder());
                out.writeInt(crew.members().size());
                for(var member:new TreeMap<>(crew.members()).entrySet()){
                    out.writeUTF(member.getKey());out.writeInt(member.getValue().ordinal());
                }
            }
        }
        CRC32 crc=new CRC32();crc.update(buffer.toByteArray());
        try(DataOutputStream out=new DataOutputStream(buffer)){out.writeLong(crc.getValue());}
        byte[] payload=buffer.toByteArray();
        if(payload.length>MAX)throw new IOException("Account directory capacity exceeded");
        return payload;
    }
    private static int count(DataInputStream in,int max)throws IOException{
        int size=in.readInt();
        if(size<0||size>max)throw new IOException("Corrupt directory count "+size);
        return size;
    }
    private static <E extends Enum<E>> E enumeration(E[] values,int index)throws IOException{
        if(index<0||index>=values.length)throw new IOException("Corrupt enum ordinal");
        return values[index];
    }
    private static State parse(byte[] bytes,ArenaSystems target)throws IOException{
        if(bytes.length<25||bytes.length>MAX)throw new IOException("Invalid account data size");
        CRC32 crc=new CRC32();crc.update(bytes,0,bytes.length-8);
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw new IOException("Unknown accounts format");
            long seed=in.readLong();boolean hardcore=in.readBoolean();
            if(seed!=target.world.seed||hardcore!=target.hardcore)
                throw new IOException("Account realm seed or hardcore mode mismatch");
            Map<String,ArenaSystems.Profile> profiles=new TreeMap<>();
            for(int i=count(in,10000);i>0;i--){
                String key=in.readUTF();ArenaSystems.Profile player=profile(in);
                if(!account(key)||!ArenaSystems.validProfile(player)||profiles.putIfAbsent(key,player)!=null)
                    throw new IOException("Invalid account profile");
            }
            Map<String,CharacterProgression.Snapshot> skills=new TreeMap<>();
            for(int i=count(in,10000);i>0;i--){
                String key=in.readUTF();long alive=in.readLong();
                EnumMap<CharacterProgression.Skill,Long> values=new EnumMap<>(CharacterProgression.Skill.class);
                for(CharacterProgression.Skill skill:CharacterProgression.Skill.values())
                    values.put(skill,in.readLong());
                var snapshot=new CharacterProgression.Snapshot(Map.copyOf(values),alive);
                CharacterProgression verification=new CharacterProgression();
                try{verification.restore(snapshot);}
                catch(IllegalArgumentException bad){throw new IOException("Invalid skill record",bad);}
                if(!account(key)||skills.putIfAbsent(key,snapshot)!=null)
                    throw new IOException("Repeated account skill record");
            }
            Map<String,CharacterRoster.Faction> allegiance=new TreeMap<>();
            for(int i=count(in,10000);i>0;i--){
                String key=in.readUTF();
                var faction=enumeration(CharacterRoster.Faction.values(),in.readInt());
                if(!account(key)||allegiance.putIfAbsent(key,faction)!=null)
                    throw new IOException("Repeated allegiance record");
            }
            long nextId=in.readLong();List<FactionGroups.Crew> crews=new ArrayList<>();
            for(int i=count(in,2048);i>0;i--){
                String id=in.readUTF(),name=in.readUTF();
                var faction=enumeration(CharacterRoster.Faction.values(),in.readInt());
                String founder=in.readUTF();Map<String,FactionGroups.Role> members=new TreeMap<>();
                for(int j=count(in,32);j>0;j--){
                    String who=in.readUTF();
                    var role=enumeration(FactionGroups.Role.values(),in.readInt());
                    if(members.putIfAbsent(who,role)!=null)throw new IOException("Repeated member");
                }
                crews.add(new FactionGroups.Crew(id,name,faction,founder,Map.copyOf(members)));
            }
            long checksum=in.readLong();
            if(checksum!=crc.getValue()||in.available()!=0)
                throw new IOException("Account directory CRC/trailing bytes mismatch");
            FactionGroups.Snapshot factionState=new FactionGroups.Snapshot(
                Map.copyOf(allegiance),List.copyOf(crews),nextId);
            try{new FactionGroups().restore(factionState);}
            catch(IllegalArgumentException bad){throw new IOException("Invalid faction crews",bad);}
            return new State(seed,hardcore,Map.copyOf(profiles),Map.copyOf(skills),factionState);
        }catch(EOFException invalid){throw new IOException("Truncated account directory",invalid);}
    }
    private static void write(ArenaSystems target,Path folder)throws IOException{
        Files.createDirectories(folder);
        Path main=folder.resolve("accounts.dat"),backup=folder.resolve("accounts.bak");
        byte[] data=serialize(capture(target));
        parse(data,target); // read-back logical validation before overwriting anything
        Path temp=Files.createTempFile(folder,"accounts-",".tmp");
        try{
            Files.write(temp,data);
            if(Files.exists(main)){
                parse(Files.readAllBytes(main),target); // never overwrite corrupt unreviewed save
                Files.copy(main,backup,StandardCopyOption.REPLACE_EXISTING);
            }
            try{Files.move(temp,main,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException unsupported){
                Files.move(temp,main,StandardCopyOption.REPLACE_EXISTING);
            }
            parse(Files.readAllBytes(main),target);
        }finally{Files.deleteIfExists(temp);}
    }
    public static synchronized void save(ArenaSystems arena,Path folder)throws IOException{
        synchronized(arena){write(arena,folder);}
    }
    public static synchronized boolean load(ArenaSystems arena,Path folder)throws IOException{
        Path main=folder.resolve("accounts.dat"),backup=folder.resolve("accounts.bak");
        if(!Files.exists(main)&&!Files.exists(backup))return false;
        State state;
        try{state=parse(Files.readAllBytes(main),arena);}
        catch(IOException broken){
            try{
                byte[] recovered=Files.readAllBytes(backup);
                state=parse(recovered,arena);
                Files.copy(backup,main,StandardCopyOption.REPLACE_EXISTING);
                parse(Files.readAllBytes(main),arena);
            }catch(IOException failure){
                IOException blocked=new IOException("Account directory and backup corrupt; refuse reset",broken);
                blocked.addSuppressed(failure);throw blocked;
            }
        }
        synchronized(arena){
            arena.restoreProfiles(state.profiles());
            arena.restoreSkills(state.skills());
            arena.factions.restore(state.groups());
        }
        return true;
    }
}
