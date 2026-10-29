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
            Role.LIGHT,1184,"1f2cbbe3c5d5f235b85e9e875303490d0c20731dfeefb888ca43123c9da20bab"),
        new Spec("unraveled_harrier",CharacterRoster.Faction.THE_UNRAVELED,
            Role.MEDIUM,1344,"147512788b4910d89fb188ba8062c997fc15cde0baacf30b816df77861a65a3d"),
        new Spec("unraveled_leviathan",CharacterRoster.Faction.THE_UNRAVELED,
            Role.HEAVY,1816,"c778fee1bb65e2b94233571deeeb7037cf8da08f408ee9d809631785fb42e25b"),
        new Spec("penitent_shrike",CharacterRoster.Faction.THE_PENITENT,
            Role.LIGHT,1314,"bd686a17cb919a98972a5f987c3f5854f5e427cfef7536424f9711735ff80200"),
        new Spec("penitent_censer",CharacterRoster.Faction.THE_PENITENT,
            Role.MEDIUM,1396,"6efcd34f5c8e18df75156a0d458c4b8ccd66816573b6ae1fbbeb03056d643f90"),
        new Spec("penitent_cathedral",CharacterRoster.Faction.THE_PENITENT,
            Role.HEAVY,2690,"8292e71535e73c26a650cf234b661246fb7d1129326b7051f723f11cc5b12835"),
        new Spec("begotten_gnat",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.LIGHT,1758,"9399db743030f9bf18e082f19b9d6680b26a72174f5da1ba8608a3a3ad89994d"),
        new Spec("begotten_mantis",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.MEDIUM,1620,"dbc3ef259c29f13be40fbcedf8a9dca7494f51d0e2130951613a89c5fac955ef"),
        new Spec("begotten_behemoth",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.HEAVY,3804,"0d43f38ed32256d23206ef4c6dc38a1d2b51327730881f7dcfc8bf92bcb12112")
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
