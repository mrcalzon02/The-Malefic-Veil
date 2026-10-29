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
    private static final int MAGIC=0x5645494c, VERSION=11, LIMIT=2_000_000;
    private DurableRealm(){}
    private record DeviceSave(int kind,double x,double y,int hp,String owner,int tier,
                              Map<SurvivalCrafting.Item,Integer> stock,
                              boolean working,long elapsedMs,int completed,String processId,long steamMs,long electricMs,String routeItem,long routeMs,String queuedRecipeId,boolean routeReverse){}
    private record CarSave(int id,double x,double y,double heading,double fuel,int hp){}
    private record NpcSave(double x,double y,int hp){}
    private record Snapshot(long seed,long timestamp,List<DeviceSave> devices,List<CarSave> cars,
        boolean[] picked,List<NpcSave> npcs,List<RaidProtection.Presence> owners,
        CargoHaulage.Snapshot cargo,byte[] arsenal){}
    public static Path file(Path root){return root.resolve("realm.dat");}
    private static Snapshot capture(ArenaSystems game)throws IOException{
        List<DeviceSave> devices=new ArrayList<>();
        for(World.Device d:game.world.devices)
            devices.add(new DeviceSave(d.kind,d.x,d.y,d.hp,
                d.ownerKey!=null&&d.ownerKey.startsWith("acct_")?d.ownerKey:"",d.tier,
                    d.kind==ArenaSystems.STORAGE_CRATE?d.storage.snapshot():Map.of(),
                    d.productionActive,d.productionMs,d.productionCompleted,d.productionRecipeId,d.steamMs,d.electricMs,
                    d.routeItem,d.routeMs,d.queuedRecipeId,d.routeReverse));
        List<CarSave> cars=new ArrayList<>();
        for(Vehicles.State c:game.vehicles.states())
            cars.add(new CarSave(c.id(),c.x(),c.y(),c.heading(),c.fuel(),c.hp()));
        boolean[] picked=new boolean[game.world.pickups.size()];
        for(int i=0;i<picked.length;i++)picked[i]=game.world.pickups.get(i).taken;
        List<NpcSave> npcs=new ArrayList<>();
        for(World.Npc n:game.world.npcs)npcs.add(new NpcSave(n.x,n.y,n.hp));
        return new Snapshot(game.world.seed,System.currentTimeMillis(),List.copyOf(devices),
            List.copyOf(cars),picked,List.copyOf(npcs),game.raidProtection.snapshot(),
            game.cargo.snapshot(),game.arsenal.save());
    }
    private static byte[] serialize(Snapshot snap)throws IOException{
        ByteArrayOutputStream payload=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(payload)){
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeLong(snap.seed);
            out.writeLong(snap.timestamp);
            out.writeInt(snap.devices.size());
            for(DeviceSave d:snap.devices){
                out.writeInt(d.kind);out.writeDouble(d.x);out.writeDouble(d.y);
                out.writeInt(d.hp);out.writeUTF(d.owner);out.writeInt(d.tier);
                out.writeInt(d.stock().size());
                for(var entry:new TreeMap<>(d.stock()).entrySet()){
                    out.writeInt(entry.getKey().ordinal());
                    out.writeInt(entry.getValue());
                }
                out.writeBoolean(d.working());
                out.writeLong(d.elapsedMs());
                out.writeInt(d.completed());
                out.writeUTF(d.processId());out.writeLong(d.steamMs());out.writeLong(d.electricMs());
                out.writeUTF(d.routeItem());out.writeLong(d.routeMs());
                out.writeUTF(d.queuedRecipeId());
                out.writeBoolean(d.routeReverse());
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
            // V2 extension, inside the SAME checksum and atomic realm snapshot.
            CargoHaulage.Snapshot cargo=snap.cargo();
            out.writeLong(cargo.clock());out.writeInt(cargo.nextDrop());
            out.writeInt(cargo.yards().size());
            for(CargoHaulage.YardSave yard:cargo.yards()){
                out.writeInt(yard.id());out.writeInt(yard.stock());out.writeLong(yard.refillAt());
            }
            out.writeInt(cargo.trucks().size());
            for(CargoHaulage.TruckSave truck:cargo.trucks()){
                out.writeInt(truck.id());out.writeUTF(truck.cargo()==null?"":truck.cargo());
            }
            out.writeInt(cargo.drops().size());
            for(CargoHaulage.Drop drop:cargo.drops()){
                out.writeInt(drop.id());out.writeDouble(drop.x());out.writeDouble(drop.y());
                out.writeUTF(drop.resource());out.writeLong(drop.createdAt());
            }
            // V3 extension: arsenal account inventory, special materials and
            // collected weapon loot are checksummed with the realm snapshot.
            out.writeInt(snap.arsenal.length);out.write(snap.arsenal);
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
            if(in.readInt()!=MAGIC)throw new IOException("Wrong save magic");
            int version=in.readInt();
            if(version!=1&&version!=2&&version!=3&&version!=4&&version!=5&&version!=6&&version!=7&&version!=8&&version!=9&&version!=10&&version!=VERSION)throw new IOException("Unsupported snapshot version");
            long seed=in.readLong(),timestamp=in.readLong(),now=System.currentTimeMillis();
            if(seed!=target.world.seed)throw new IOException("Save seed mismatch: no world reset allowed");
            if(timestamp<0||timestamp>now+300000)throw new IOException("Unreasonable snapshot timestamp");
            List<DeviceSave> devices=new ArrayList<>();
            for(int i=count(in,500);i>0;i--){
                int kind=in.readInt();double x=in.readDouble(),y=in.readDouble();
                int hp=in.readInt();String owner=in.readUTF();int tier=version>=3?in.readInt():1;
                // Parse structurally valid legacy devices even when they sit in
                // newly protected city plots. load() will archive and quarantine
                // them; active city placement is never restored.
                if(kind<0||kind>ArenaSystems.UNRAVELED_RESILIENCE_GRID||hp<0||hp>100||!Double.isFinite(x)||!Double.isFinite(y)||
                   !target.world.continent().inBounds(target.world.parcelX(x),target.world.parcelY(y))||
                   (!owner.isEmpty()&&!owner.matches("acct_[a-f0-9]{64}"))||tier<1||tier>3)
                    throw new IOException("Invalid persisted device");
                EnumMap<SurvivalCrafting.Item,Integer> stock=
                    new EnumMap<>(SurvivalCrafting.Item.class);
                if(version>=4)for(int j=count(in,CampStockpile.MAX_STACKS);j>0;j--){
                    int ordinal=in.readInt(),qty=in.readInt();
                    if(ordinal<0||ordinal>=SurvivalCrafting.Item.values().length||
                       qty<1||stock.putIfAbsent(
                           SurvivalCrafting.Item.values()[ordinal],qty)!=null)
                        throw new IOException("Invalid crate item");
                }
                if((kind!=ArenaSystems.STORAGE_CRATE&&!stock.isEmpty())||
                   !CampStockpile.valid(stock)||(hp<=0&&!stock.isEmpty()))
                    throw new IOException("Invalid crate stock or destroyed warehouse");
                boolean active=version>=5&&in.readBoolean();
                long elapsed=version>=5?in.readLong():0;
                int finished=version>=5?in.readInt():0;
                String process=version>=6?in.readUTF():
                    active?ArenaSystems.firstProcess(kind):"";
                var recipe=CraftingEraRegistry.process(process);
                long max=recipe==null?0:recipe.seconds()*1000L;
                if(process.length()>80||(active&&(recipe==null||
                   !recipe.facility().equals(ArenaSystems.factionFacility(kind))||
                   owner.isEmpty()))||
                   (!active&&!process.isEmpty())||
                   elapsed<0||elapsed>max||finished<0||finished>1000000||
                   (recipe==null&&(active||elapsed!=0||
                    (finished!=0&&ArenaSystems.firstProcess(kind)==null)))||
                   (!active&&elapsed!=0)||(active&&hp==0))
                    throw new IOException("Corrupt faction production receipt");
                long steamMs=version>=7?in.readLong():0;
                if(steamMs<0||steamMs>SteamUtility.MAX_CHARGE_MS||
                   (kind!=ArenaSystems.STEAM_BOILER&&steamMs!=0)||
                   (hp==0&&steamMs!=0)||(!owner.startsWith("acct_")&&steamMs!=0))
                    throw new IOException("Invalid host boiler steam reserve");
                long electricMs=version>=8?in.readLong():0;
                if(electricMs<0||electricMs>ElectricUtility.MAX_CHARGE_MS||
                   (kind!=ArenaSystems.GENERATOR&&electricMs!=0)||
                   (hp==0&&electricMs!=0)||
                   (!owner.startsWith("acct_")&&electricMs!=0))
                    throw new IOException("Invalid electrical reserve in realm");
                String routeItem=version>=9?in.readUTF():"";
                long routeMs=version>=9?in.readLong():0;
                if(routeItem.length()>80||routeMs<0||
                   routeMs>=ConveyorLogistics.TICK_MS||
                   (kind!=ArenaSystems.CONVEYOR_LINE&&
                    (!routeItem.isEmpty()||routeMs!=0))||
                   (hp==0&&(!routeItem.isEmpty()||routeMs!=0))||
                   (routeItem.isEmpty()&&routeMs!=0))
                    throw new IOException("Invalid conveyor logistics state");
                if(!routeItem.isEmpty()&&!routeItem.equals("AUTO"))try{
                    SurvivalCrafting.Item.valueOf(routeItem);
                }catch(IllegalArgumentException invalid){
                    throw new IOException("Invalid recorded conveyor item",invalid);
                }
                if(routeItem.equals("AUTO")&&(version<11||
                   !owner.startsWith("acct_")||hp<=0||
                   kind!=ArenaSystems.CONVEYOR_LINE))
                    throw new IOException("Invalid automatic E5 input route");
                String queued=version>=10?in.readUTF():"";
                if(queued.length()>80||(hp<=0&&!queued.isEmpty())||
                   (!queued.isEmpty()&&!owner.startsWith("acct_")))
                    throw new IOException("Invalid E5 job queue owner/status");
                if(!queued.isEmpty()){
                    if(AutomationOrders.repeating(queued)&&version<11)
                        throw new IOException("Repeat job cannot exist in a legacy E5 save");
                    String recipeId=AutomationOrders.recipe(queued);
                    if(recipeId.length()>80||!recipeId.matches("[a-z0-9-]+"))
                        throw new IOException("Invalid E5 programmed recipe syntax");
                    var next=CraftingEraRegistry.process(recipeId);
                    if(next==null||
                       next.era()!=CraftingEraRegistry.Era.AUTOMATION||
                       next.utilityDemand()<1||
                       !next.facility().equals(ArenaSystems.factionFacility(kind))||
                       CraftingEraRegistry.facility(next.facility())==null)
                        throw new IOException("Invalid E5 queued manufacturing recipe");
                }
                boolean routeReverse=version>=11&&in.readBoolean();
                if(routeReverse&&(kind!=ArenaSystems.CONVEYOR_LINE||
                    routeItem.isEmpty()||hp<=0||!owner.startsWith("acct_")))
                    throw new IOException("Invalid persisted conveyor direction");
                devices.add(new DeviceSave(kind,x,y,hp,owner,tier,Map.copyOf(stock),
                    active,elapsed,finished,process,steamMs,electricMs,routeItem,routeMs,queued,routeReverse));
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
            // Migration from the briefly-published 178-slot substitute
            // catalog: first 170 original pickups stay stable, invented
            // weapon slots are discarded rather than pretending to be arsenal.
            int legacy=target.world.pickups.size();
            if(pickupCount!=legacy&&pickupCount!=legacy+8)
                throw new IOException("Pickup catalog changed");
            boolean[] picked=new boolean[legacy];
            for(int i=0;i<pickupCount;i++){
                boolean flag=in.readBoolean();
                if(i<legacy)picked[i]=flag;
            }
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
            CargoHaulage.Snapshot cargo=null;
            if(version>=2){
                long cargoClock=in.readLong();int nextDrop=in.readInt();
                List<CargoHaulage.YardSave> yardStates=new ArrayList<>();
                for(int i=count(in,128);i>0;i--)
                    yardStates.add(new CargoHaulage.YardSave(
                        in.readInt(),in.readInt(),in.readLong()));
                List<CargoHaulage.TruckSave> truckStates=new ArrayList<>();
                for(int i=count(in,64);i>0;i--){
                    int carId=in.readInt();String value=in.readUTF();
                    truckStates.add(new CargoHaulage.TruckSave(
                        carId,value.isEmpty()?null:value));
                }
                List<CargoHaulage.Drop> dropped=new ArrayList<>();
                for(int i=count(in,128);i>0;i--)
                    dropped.add(new CargoHaulage.Drop(in.readInt(),in.readDouble(),
                        in.readDouble(),in.readUTF(),in.readLong()));
                cargo=new CargoHaulage.Snapshot(cargoClock,nextDrop,
                    List.copyOf(yardStates),List.copyOf(truckStates),List.copyOf(dropped));
                // Reject invalid cargo metadata BEFORE mutating live match.
                if(cargo.clock()<0||cargo.nextDrop()<1||
                    cargo.yards().size()!=target.cargo.sites().size()||
                    cargo.trucks().size()!=target.vehicles.states().size())
                    throw new IOException("Corrupt cargo snapshot cardinality");
                for(int i=0;i<cargo.yards().size();i++){
                    var yard=cargo.yards().get(i);
                    if(yard.id()!=i||(yard.stock()!=0&&yard.stock()!=1)||
                        yard.refillAt()<0)
                        throw new IOException("Corrupt cargo yard");
                }
                for(int i=0;i<cargo.trucks().size();i++){
                    var truck=cargo.trucks().get(i);
                    if(truck.id()!=i||(truck.cargo()!=null&&
                        !truck.cargo().equals("METAL_STOCK")&&
                        !truck.cargo().equals("MACHINE_PARTS"))||
                        truck.cargo()!=null&&cars.get(i).hp()<=80)
                        throw new IOException("Invalid persistent loaded vehicle");
                }
            }
            byte[] arsenal=new byte[0];
            if(version>=3){
                int length=count(in,1_000_000);
                if(length>in.available())throw new IOException("Truncated arsenal state");
                arsenal=in.readNBytes(length);
                target.arsenal.restore(arsenal,false); // inspect before mutation
            }
            if(in.available()!=0)throw new IOException("Unexpected snapshot trailing data");
            return new Snapshot(seed,timestamp,devices,cars,picked,npcs,owners,cargo,arsenal);
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
                Files.copy(backup,primary,StandardCopyOption.REPLACE_EXISTING);
                parse(Files.readAllBytes(primary),game);
                System.err.println("Primary save rejected ("+damaged.getMessage()+"); validated backup restored to primary");
            }catch(IOException backupError){
                IOException failure=new IOException("Both primary and backup unusable; refusing world reset",damaged);
                failure.addSuppressed(backupError);throw failure;
            }
        }
        long illegal=snapshot.devices.stream().filter(d->
            game.world.worldStructureAt(d.x,d.y,.55)).count();
        if(illegal>0){
            // Preserve the exact verified legacy snapshot independently BEFORE
            // sanitizing active runtime state or permitting subsequent saves.
            Path archive=Files.createTempFile(root,"realm-city-legacy-",".dat");
            byte[] source=Files.readAllBytes(primary);
            try{
                Files.write(archive,source);
                if(!Arrays.equals(source,Files.readAllBytes(archive)))
                    throw new IOException("Legacy world snapshot archival verification failed");
                parse(Files.readAllBytes(archive),game);
            }catch(IOException failure){
                Files.deleteIfExists(archive);
                throw new IOException("Cannot safely archive obsolete city buildings",failure);
            }
            System.err.println("Archived "+illegal+" former city structure(s) to "+
                archive+"; fixed city sector will not restore these player devices");
        }
        synchronized(game){
            for(DeviceSave d:snapshot.devices){
                if(game.world.worldStructureAt(d.x,d.y,.55))continue;
                World.Device device=new World.Device(d.x,d.y,d.kind);
                device.hp=d.hp;device.tier=d.tier;device.ownerKey=d.owner.isEmpty()?null:d.owner;
                device.storage.restore(d.stock());
                device.productionActive=d.working();
                device.productionMs=d.elapsedMs();
                device.productionCompleted=d.completed();
                device.productionRecipeId=d.processId();
                device.steamMs=d.steamMs();
                device.electricMs=d.electricMs();
                device.routeItem=d.routeItem();
                device.routeMs=d.routeMs();
                device.queuedRecipeId=d.queuedRecipeId();
                device.routeReverse=d.routeReverse();
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
            if(snapshot.cargo()!=null){
                try{game.cargo.restore(snapshot.cargo());}
                catch(IllegalArgumentException invalid){
                    throw new IOException("Corrupt cargo overlay",invalid);
                }
            }
            game.arsenal.restore(snapshot.arsenal(),true);
        }
        return true;
    }
    /** Save to a unique temp file then replace; keep the last known valid snapshot as backup. */
    public static void save(ArenaSystems game,Path root)throws IOException{
        Objects.requireNonNull(root);
        Snapshot snapshot;
        synchronized(game){snapshot=capture(game);}
        for(DeviceSave device:snapshot.devices)
            if(game.world.worldStructureAt(device.x,device.y,.55))
                throw new IOException("Refusing to save an active city-owned player structure");
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
