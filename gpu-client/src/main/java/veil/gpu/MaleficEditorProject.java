package veil.gpu;

import org.beckenburg.editor.EditorProject;
import veil.GGBotCarPack;
import veil.KenneyAssetPack;
import veil.UrbanScene;
import veil.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Malefic Veil adapter for the ACTUAL independently published LevelEditorKit.
 * Engine-independent history, palette, asset URI policy and tools are NOT copied.
 * This game's format stores a render-only authoring layer separate from generated
 * city assets and the multiplayer host's immutable collision/structure rules.
 */
public final class MaleficEditorProject implements EditorProject<MaleficEditorProject.Snapshot> {
    public record Placement(String id,String model,double x,double z,
                            double yaw,double scale,double elevation) {
        public UrbanScene.ModelInstance instance(){
            return new UrbanScene.ModelInstance(model,x,z,yaw,scale,1,elevation);
        }
    }
    public record Snapshot(List<Placement> placements,int nextId) {
        public Snapshot{placements=List.copyOf(placements);}
    }
    private static final int MAX_PLACEMENTS=2048;
    private final long seed;
    private final Path file;
    private final Set<String> allowed;
    private volatile List<Placement> published=List.of();
    private int nextId=1;

    public static Path documentFor(long seed){
        return Path.of(System.getProperty("veil.editor.dir","worlds/editor"))
            .resolve("seed-"+seed).resolve("city-A.properties");
    }
    public MaleficEditorProject(World world)throws IOException{
        Objects.requireNonNull(world);
        this.seed=world.seed;
        file=documentFor(seed).toAbsolutePath().normalize();
        Set<String> models=new TreeSet<>();
        for(String name:KenneyAssetPack.load().modelNames())
            if(name.endsWith(".glb"))models.add(name.substring(0,name.length()-4));
        if(GGBotCarPack.available())models.addAll(GGBotCarPack.required().names());
        allowed=Set.copyOf(models);
        if(allowed.isEmpty())throw new IOException("No licensed editor source models");
        load();
    }
    private void valid(Placement item){
        if(item==null||!item.id().matches("edit-[1-9][0-9]{0,8}")||
            !allowed.contains(item.model())||
            !Double.isFinite(item.x())||!Double.isFinite(item.z())||
            !Double.isFinite(item.yaw())||!Double.isFinite(item.scale())||
            !Double.isFinite(item.elevation())||
            Math.abs(item.x())>8192||Math.abs(item.z())>8192||
            Math.abs(item.elevation())>512||Math.abs(item.yaw())>100000||
            item.scale()<.05||item.scale()>12)
            throw new IllegalArgumentException("Invalid authored model placement");
    }
    private synchronized void publish(List<Placement> next,int nextId){
        if(next.size()>MAX_PLACEMENTS||nextId<1||nextId>10_000_000)
            throw new IllegalArgumentException("Editor document exceeds safe size");
        Set<String> ids=new HashSet<>();
        for(Placement item:next){
            valid(item);
            if(!ids.add(item.id()))throw new IllegalArgumentException("Duplicate editor ID");
        }
        published=List.copyOf(next);this.nextId=nextId;
    }
    public List<Placement> placements(){return published;}
    public Set<String> modelIds(){return allowed;}
    public String id(){return "malefic-veil/city-A/"+seed;}
    public String title(){return "The Malefic Veil — City Authoring";}
    public Path documentPath(){return file;}
    public List<Node> hierarchy(){
        List<Node> nodes=new ArrayList<>();
        nodes.add(new Node("generated-city-A","GENERATED","City A generated base (protected)",66,0,66));
        for(Placement item:published)
            nodes.add(new Node(item.id(),"PROP",item.id()+"  "+item.model(),
                item.x(),item.elevation(),item.z()));
        return List.copyOf(nodes);
    }
    public List<Asset> assets(){
        List<Asset> assets=new ArrayList<>();
        for(String id:allowed){
            String category=id.startsWith("road-")?"Roads":
                id.startsWith("tree-")?"Nature":
                id.startsWith("truck-")||id.startsWith("ggbot-")?"Vehicles":
                id.startsWith("detail-")?"Details":"Structures";
            assets.add(new Asset("assets://models/"+id,category,id));
        }
        assets.sort(Comparator.comparing(Asset::category).thenComparing(Asset::label));
        return List.copyOf(assets);
    }
    public synchronized Snapshot snapshot(){return new Snapshot(published,nextId);}
    public synchronized void restore(Snapshot copy){
        Objects.requireNonNull(copy);
        publish(copy.placements(),copy.nextId());
    }
    public static String modelFromAsset(Asset asset){
        String prefix="assets://models/";
        if(!asset.uri().startsWith(prefix))
            throw new IllegalArgumentException("Not a source mesh: "+asset.uri());
        return asset.uri().substring(prefix.length());
    }
    public synchronized Placement add(String model,double x,double z,
                                      double yaw,double scale,double elevation){
        if(nextId>=10_000_000)throw new IllegalStateException("Editor ID limit");
        Placement item=new Placement("edit-"+nextId,model,x,z,yaw,scale,elevation);
        List<Placement> updated=new ArrayList<>(published);
        updated.add(item);publish(updated,nextId+1);
        return item;
    }
    public synchronized Placement get(String id){
        for(Placement item:published)if(item.id().equals(id))return item;
        throw new IllegalArgumentException("No such authored object: "+id);
    }
    public synchronized void transform(String id,double x,double z,
                                       double yaw,double scale,double elevation){
        List<Placement> updated=new ArrayList<>(published);
        int idx=-1;
        for(int i=0;i<updated.size();i++)
            if(updated.get(i).id().equals(id)){idx=i;break;}
        if(idx<0)throw new IllegalArgumentException("Cannot transform generated object "+id);
        Placement item=updated.get(idx);
        updated.set(idx,new Placement(id,item.model(),x,z,yaw,scale,elevation));
        publish(updated,nextId);
    }
    public synchronized void delete(String id){
        List<Placement> updated=new ArrayList<>(published);
        if(!updated.removeIf(item->item.id().equals(id)))
            throw new IllegalArgumentException("Cannot delete a generated object");
        publish(updated,nextId);
    }
    public synchronized void load() throws IOException {
        if(!Files.exists(file)){publish(List.of(),1);return;}
        Properties properties=new Properties();
        try(InputStream input=Files.newInputStream(file)){properties.load(input);}
        if(!"1".equals(properties.getProperty("format"))||
            !Long.toString(seed).equals(properties.getProperty("seed")))
            throw new IOException("Wrong editor format or incompatible source seed");
        int count,next;
        try{
            count=Integer.parseInt(properties.getProperty("count","-1"));
            next=Integer.parseInt(properties.getProperty("next","-1"));
        }catch(NumberFormatException bad){throw new IOException("Invalid editor counts",bad);}
        if(count<0||count>MAX_PLACEMENTS||next<1||next>10_000_000)
            throw new IOException("Invalid editor document limits");
        List<Placement> values=new ArrayList<>();
        try{
            for(int i=0;i<count;i++){
                String key="prop."+i+".";
                values.add(new Placement(properties.getProperty(key+"id"),
                    properties.getProperty(key+"model"),
                    Double.parseDouble(properties.getProperty(key+"x")),
                    Double.parseDouble(properties.getProperty(key+"z")),
                    Double.parseDouble(properties.getProperty(key+"yaw")),
                    Double.parseDouble(properties.getProperty(key+"scale")),
                    Double.parseDouble(properties.getProperty(key+"elevation"))));
            }
            publish(values,next);
        }catch(RuntimeException bad){throw new IOException("Invalid saved editor geometry",bad);}
    }
    @Override public synchronized void save() throws IOException {
        Files.createDirectories(file.getParent());
        Properties properties=new Properties();
        properties.setProperty("format","1");
        properties.setProperty("seed",Long.toString(seed));
        properties.setProperty("count",Integer.toString(published.size()));
        properties.setProperty("next",Integer.toString(nextId));
        for(int i=0;i<published.size();i++){
            Placement item=published.get(i);String k="prop."+i+".";
            properties.setProperty(k+"id",item.id());
            properties.setProperty(k+"model",item.model());
            properties.setProperty(k+"x",Double.toString(item.x()));
            properties.setProperty(k+"z",Double.toString(item.z()));
            properties.setProperty(k+"yaw",Double.toString(item.yaw()));
            properties.setProperty(k+"scale",Double.toString(item.scale()));
            properties.setProperty(k+"elevation",Double.toString(item.elevation()));
        }
        Path temporary=Files.createTempFile(file.getParent(),".city-",".tmp");
        try{
            try(OutputStream out=Files.newOutputStream(temporary)){properties.store(out,
                "The Malefic Veil developer geometry — render-only, not host collision");}
            try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException unsupported){
                Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{Files.deleteIfExists(temporary);}
    }
}
