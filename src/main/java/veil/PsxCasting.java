package veil;

import java.util.*;

/** Provenance-backed STATIC source-FBX casting candidates.
 * These are not game-ready meshes; no archive asset is bundled by this class. */
public final class PsxCasting {
    public enum Review { PROVISIONAL, NEEDS_MODIFICATION, POOR_ROLE_FIT }
    public record Candidate(String classId,String sourcePath,int triangles,Review review) {
        public String sourceFile(){return sourcePath.substring(sourcePath.lastIndexOf('/')+1);}
        /** A source candidate is never a playable runtime GLB by itself. */
        public boolean runtimeIntegrated(){return false;}
    }
    private static final String PREFIX="Characters_psx_1.2/Characters_psx_02/Models/No-Rig/";
    private static Candidate pick(String role,String group,String file,int tris,Review review){
        return new Candidate(role,PREFIX+group+"/"+file+".fbx",tris,review);
    }
    private static final List<Candidate> CAST=List.of(
        pick("unraveled-warden","Male","Character_17_Police",896,Review.PROVISIONAL),
        pick("unraveled-stitcher","Female","Character_23_Female_Doctor",1208,Review.PROVISIONAL),
        pick("unraveled-hollow","Male","Character_31",898,Review.POOR_ROLE_FIT),
        pick("penitent-confessor","Killer_M","Character_Killer_03",956,Review.NEEDS_MODIFICATION),
        pick("penitent-flagellant","Killer_M","Character_Killer_06",896,Review.NEEDS_MODIFICATION),
        pick("penitent-votary","Killer_M","Character_Killer_09",1432,Review.POOR_ROLE_FIT),
        pick("begotten-harrower","Killer_M","Character_Monster_04",1328,Review.PROVISIONAL),
        pick("begotten-devourer","Killer_M","Character_Monster_08",1474,Review.PROVISIONAL),
        pick("begotten-mourner","Killer_M","Character_Monster_05",856,Review.PROVISIONAL)
    );
    private PsxCasting(){}
    public static List<Candidate> all(){return CAST;}
    public static Candidate forClass(String classId){
        for(Candidate c:CAST)if(c.classId().equals(classId))return c;
        return null;
    }
}
