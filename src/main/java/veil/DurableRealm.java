package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Atomic, checksummed persistent arena overlay. This saves mutable world objects only;
 * seed-generated buildings/roads are reconstructed from the immutable world seed.
 * Corrupt snapshots fail closed and may recover from an independently validated .bak.
 */
public final class DurableRealm {
    private static final int MAGIC=0x5645494c, VERSION=1, LIMIT=2_000_000;
    private DurableRealm(){}
    private record DeviceSave(int kind,double x,double y,int hp,String owner){}
    private record CarSave(int id,double x,double y,double heading,double fuel,int hp){}
    private record NpcSave(double x,double y,int hp){}
    private record Snapshot(long seed,long timestamp,List<DeviceSave> devices,List<CarSave> cars,
        boolean[] picked,List<NpcSave> npcs,List<RaidProtection.Presence> owners){}
    public static Path file(Path root){return root.resolve("realm.dat");}
    private static Snapshot capture(ArenaSystems game){
        List<DeviceSave> devices=new ArrayList<>();
        for(World.Device d:game.world.devices)
            devices.add(new DeviceSave(d.kind,d.x,d.y,d.hp,
                d.ownerKey!=null&&d.ownerKey.startsWith("acct_")?d.ownerKey:""));
        List<CarSave> cars=new ArrayList<>();
        for(Vehicles.State c:game.vehicles.states())
            cars.add(new CarSave(c.id(),c.x(),c.y(),c.heading(),c.fuel(),c.hp()));
        boolean[] picked=new boolean[game.world.pickups.size()];
        for(int i=0;i<picked.length;i++)picked[i]=game.world.pickups.get(i).taken;
        List<NpcSave> npcs=new ArrayList<>();
        for(World.Npc n:game.world.npcs)npcs.add(new NpcSave(n.x,n.y,n.hp));
        return new Snapshot(game.world.seed,System.currentTimeMillis(),List.copyOf(devices),
            List.copyOf(cars),picked,List.copyOf(npcs),game.raidProtection.snapshot());
    }
    private static byte[] serialize(Snapshot snap)throws IOException{
        ByteArrayOutputStream payload=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(payload)){
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeLong(snap.seed);
            out.writeLong(snap.timestamp);
            out.writeInt(snap.devices.size());
            for(DeviceSave d:snap.devices){
                out.writeInt(d.kind);out.writeDouble(d.x);out.writeDouble(d.y);
                out.writeInt(d.hp);out.writeUTF(d.owner);
            }
            out.writeInt(snap.cars.size());
            for(CarSave c:snap.cars){
                out.writeInt(c.id);out.writeDouble(c.x);out.writeDouble(c.y);
                out.writeDouble(c.heading);out.writeDouble(c.fuel);out.writeInt(c.hp);
            }
            out.writeInt(snap.picked.length);
            for(boolean flag:snap.picked)out.writeBoolean(flag);
            out.writeInt(snap.npcs.size());
            for(NpcSave n:snap.npcs){
                out.writeDouble(n.x);out.writeDouble(n.y);out.writeInt(n.hp);
            }
            out.writeInt(snap.owners.size());
            for(RaidProtection.Presence p:snap.owners){
                out.writeUTF(p.account());out.writeLong(p.lastLogoutUtcMs());out.writeInt(p.sessions());
            }
        }
        byte[] bytes=payload.toByteArray();
        if(bytes.length>LIMIT-8)throw new IOException("Snapshot exceeds storage cap");
        CRC32 crc=new CRC32();crc.update(bytes);
        ByteArrayOutputStream framed=new ByteArrayOutputStream();
        framed.write(bytes);
        try(DataOutputStream out=new DataOutputStream(framed)){out.writeLong(crc.getValue());}
        return framed.toByteArray();
    }
    private static int count(DataInputStream in,int max)throws IOException{
        int n=in.readInt();if(n<0||n>max)throw new IOException("Invalid snapshot collection length");
        return n;
    }
    private static Snapshot parse(byte[] bytes,ArenaSystems target)throws IOException{
        if(bytes.length<40||bytes.length>LIMIT)throw new IOException("Invalid snapshot size");
        long recorded;
        try(DataInputStream trailer=new DataInputStream(new ByteArrayInputStream(bytes,bytes.length-8,8))){
            recorded=trailer.readLong();
        }
        CRC32 crc=new CRC32();crc.update(bytes,0,bytes.length-8);
        if(recorded!=crc.getValue())throw new IOException("Snapshot checksum mismatch");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-8))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw new IOException("Unsupported snapshot version");
            long seed=in.readLong(),timestamp=in.readLong(),now=System.currentTimeMillis();
            if(seed!=target.world.seed)throw new IOException("Save seed mismatch: no world reset allowed");
            if(timestamp<0||timestamp>now+300000)throw new IOException("Unreasonable snapshot timestamp");
            List<DeviceSave> devices=new ArrayList<>();
            for(int i=count(in,500);i>0;i--){
                int kind=in.readInt();double x=in.readDouble(),y=in.readDouble();
                int hp=in.readInt();String owner=in.readUTF();
                if(kind<0||kind>3||hp<0||hp>100||!Double.isFinite(x)||!Double.isFinite(y)||
                   target.world.worldStructureAt(x,y,.55)||
                   (!owner.isEmpty()&&!owner.matches("acct_[a-f0-9]{64}")))
                    throw new IOException("Invalid persisted device");
                devices.add(new DeviceSave(kind,x,y,hp,owner));
            }
            List<CarSave> cars=new ArrayList<>();
            int carCount=count(in,64);
            if(carCount!=target.vehicles.states().size())throw new IOException("Vehicle catalog changed");
            for(int i=0;i<carCount;i++){
                int id=in.readInt();double x=in.readDouble(),y=in.readDouble(),
                    heading=in.readDouble(),fuel=in.readDouble();int hp=in.readInt();
                if(id!=i||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(heading)||
                   !Double.isFinite(fuel)||fuel<0||fuel>100||hp<0||hp>200||
                   !target.world.canWalk(x,y))
                    throw new IOException("Invalid persisted vehicle");
                cars.add(new CarSave(id,x,y,heading,fuel,hp));
            }
            int pickupCount=count(in,10000);
            if(pickupCount!=target.world.pickups.size())throw new IOException("Pickup catalog changed");
            boolean[] picked=new boolean[pickupCount];
            for(int i=0;i<picked.length;i++)picked[i]=in.readBoolean();
            int npcCount=count(in,5000);
            if(npcCount!=target.world.npcs.size())throw new IOException("NPC catalog changed");
            List<NpcSave> npcs=new ArrayList<>();
            for(int i=0;i<npcCount;i++){
                double x=in.readDouble(),y=in.readDouble();int hp=in.readInt();
                if(!Double.isFinite(x)||!Double.isFinite(y)||hp < -10000||hp>1000||
                   !target.world.canWalk(x,y))throw new IOException("Invalid persisted NPC");
                npcs.add(new NpcSave(x,y,hp));
            }
            List<RaidProtection.Presence> owners=new ArrayList<>();
            for(int i=count(in,10000);i>0;i--){
                String account=in.readUTF();long loggedOut=in.readLong();int sessions=in.readInt();
                if(loggedOut<0||loggedOut>now||sessions<0||sessions>10000||
                   !account.matches("[a-zA-Z0-9:_-]{1,96}"))throw new IOException("Invalid owner presence");
                owners.add(new RaidProtection.Presence(account,loggedOut,sessions));
            }
            if(in.available()!=0)throw new IOException("Unexpected snapshot trailing data");
            return new Snapshot(seed,timestamp,devices,cars,picked,npcs,owners);
        }catch(EOFException e){throw new IOException("Incomplete world snapshot",e);}
    }
    /** Only call before accepting connections. Refuse corrupt data when no valid backup exists. */
    public static boolean load(ArenaSystems game,Path root)throws IOException{
        Path primary=file(root),backup=root.resolve("realm.bak");
        if(!Files.exists(primary)&&!Files.exists(backup))return false;
        Snapshot snapshot;
        try{snapshot=parse(Files.readAllBytes(primary),game);}
        catch(IOException damaged){
            try{
                snapshot=parse(Files.readAllBytes(backup),game);
                System.err.println("Primary save rejected ("+damaged.getMessage()+"); valid backup recovered");
            }catch(IOException backupError){
                IOException failure=new IOException("Both primary and backup unusable; refusing world reset",damaged);
                failure.addSuppressed(backupError);throw failure;
            }
        }
        synchronized(game){
            for(DeviceSave d:snapshot.devices){
                World.Device device=new World.Device(d.x,d.y,d.kind);
                device.hp=d.hp;device.ownerKey=d.owner.isEmpty()?null:d.owner;
                game.world.devices.add(device);
            }
            for(CarSave car:snapshot.cars){
                if(!game.vehicles.restore(car.id,car.x,car.y,car.heading,car.fuel,car.hp))
                    throw new IOException("Cannot restore vehicle collider; map revision unsafe");
            }
            for(int i=0;i<snapshot.picked.length;i++)
                game.world.pickups.get(i).taken=snapshot.picked[i];
            for(int i=0;i<snapshot.npcs.size();i++){
                NpcSave save=snapshot.npcs.get(i);World.Npc npc=game.world.npcs.get(i);
                npc.x=save.x;npc.y=save.y;npc.hp=save.hp;
            }
            try{game.raidProtection.restore(snapshot.owners,System.currentTimeMillis());}
            catch(IllegalArgumentException ex){throw new IOException("Invalid raid presence data",ex);}
        }
        return true;
    }
    /** Save to a unique temp file then replace; keep the last known valid snapshot as backup. */
    public static void save(ArenaSystems game,Path root)throws IOException{
        Objects.requireNonNull(root);
        Snapshot snapshot;
        synchronized(game){snapshot=capture(game);}
        byte[] bytes=serialize(snapshot);
        // Validate before touching an existing save.
        parse(bytes,game);
        Files.createDirectories(root);
        Path main=file(root),backup=root.resolve("realm.bak");
        Path temp=Files.createTempFile(root,"realm-",".tmp");
        try{
            Files.write(temp,bytes);
            if(!Arrays.equals(Files.readAllBytes(temp),bytes))
                throw new IOException("Snapshot write verification failed");
            if(Files.exists(main)){
                parse(Files.readAllBytes(main),game);
                Files.copy(main,backup,StandardCopyOption.REPLACE_EXISTING);
            }
            try{Files.move(temp,main,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){
                Files.move(temp,main,StandardCopyOption.REPLACE_EXISTING);
            }
            parse(Files.readAllBytes(main),game);
        }finally{Files.deleteIfExists(temp);}
    }
}
