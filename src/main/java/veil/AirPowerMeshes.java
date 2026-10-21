package veil;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * The nine genuinely modeled PSX faction aircraft. Loads source-authored GLB
 * triangle geometry, not billboards, concept-sheet sprites, or flight actors.
 *
 * All geometry is baked in glTF metres (X right, Y up, nose toward negative Z).
 * Flight components/animation pivots will be split after static-art acceptance.
 */
public final class AirPowerMeshes {
    public enum Role { LIGHT, MEDIUM, HEAVY }
    public record Spec(String id,CharacterRoster.Faction faction,Role role,
                       int triangles,String sha256) {}
    private static final List<Spec> REGISTRY=List.of(
        new Spec("unraveled_needlewing",CharacterRoster.Faction.THE_UNRAVELED,
            Role.LIGHT,732,"70c22be7ee0720befb15c683a393ee373250e014b93557fb915afcd79950058e"),
        new Spec("unraveled_harrier",CharacterRoster.Faction.THE_UNRAVELED,
            Role.MEDIUM,796,"8545e14b3b8ad342af1c61492223a86baf24bb939c7011adb8ee1bc1cf4b499d"),
        new Spec("unraveled_leviathan",CharacterRoster.Faction.THE_UNRAVELED,
            Role.HEAVY,1104,"86850fede5fa017e5f6b5d346ed6f97b5e5e57893677ef275d186bf6957ffee3"),
        new Spec("penitent_shrike",CharacterRoster.Faction.THE_PENITENT,
            Role.LIGHT,1056,"be808313d38184670d841074b9088978b06ff41117fa8cc3e9a3f6672e51d00c"),
        new Spec("penitent_censer",CharacterRoster.Faction.THE_PENITENT,
            Role.MEDIUM,1114,"13f85c07ee19b6d17fedc5cd11129d498a1fab9f44d7a6fab9b9f407035085e7"),
        new Spec("penitent_cathedral",CharacterRoster.Faction.THE_PENITENT,
            Role.HEAVY,2312,"bfce9a55081b76ecf6e86b5a45042a7f45e5be26c374743874170344da529f3a"),
        new Spec("begotten_gnat",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.LIGHT,1626,"3c3e36db4a9f4c0904b605d29d299f2399a4ed8ea6193ea5556ff48009f53c95"),
        new Spec("begotten_mantis",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.MEDIUM,1524,"d02162101f2f62884e00a6c65b7133a5ef57c0c46a3919e01ff635965a42b8b6"),
        new Spec("begotten_behemoth",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.HEAVY,3544,"ecfc45c2c9bf5e7ca3a68eb709ebeb77ddc8ef625ddb543a4ff0718de8f0babf")
    );
    private AirPowerMeshes(){}
    public static List<Spec> specs(){return REGISTRY;}
    public static Spec spec(String id){
        for(Spec s:REGISTRY)if(s.id().equals(id))return s;
        return null;
    }
    public static Map<String,GlbMesh> load(Path repositoryRoot)throws IOException{
        Objects.requireNonNull(repositoryRoot);
        Path dir=repositoryRoot.resolve("assets/game/aircraft");
        Map<String,GlbMesh> result=new LinkedHashMap<>();
        for(Spec s:REGISTRY){
            Path source=dir.resolve(s.id()+".glb");
            if(!Files.isRegularFile(source))throw new IOException(
                "Missing required aircraft model "+source);
            byte[] bytes=Files.readAllBytes(source);
            if(bytes.length<2000||bytes.length>1_000_000)
                throw new IOException("Invalid PSX aircraft file size "+s.id());
            String checksum=sha256(bytes);
            if(!checksum.equals(s.sha256()))
                throw new IOException("Aircraft model integrity failure "+s.id());
            GlbMesh mesh=GlbMesh.read(bytes);
            if(mesh.triangleCount()!=s.triangles()||
               mesh.triangleCount()>4000)
                throw new IOException("Changed polygon contract "+s.id());
            float[] box=mesh.bounds();
            for(float n:box)if(!Float.isFinite(n))
                throw new IOException("Nonfinite airframe bound "+s.id());
            if(box[0]>=box[3]||box[1]>=box[4]||box[2]>=box[5])
                throw new IOException("Degenerate aircraft "+s.id());
            if(result.put(s.id(),mesh)!=null)
                throw new IOException("Duplicate airframe "+s.id());
        }
        return Collections.unmodifiableMap(result);
    }
    private static String sha256(byte[] bytes)throws IOException{
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out=new StringBuilder(digest.length*2);
            for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
            return out.toString();
        }catch(NoSuchAlgorithmException error){
            throw new IOException("JDK lacks SHA-256",error);
        }
    }
}
