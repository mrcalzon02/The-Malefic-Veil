package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Runtime only loads gate-promoted authored models. Unapproved studies never replace production meshes. */
public final class FieldDeployableAssets {
    private final Map<Integer,AnimatedGlb> rigs=new HashMap<>();
    public FieldDeployableAssets(Path root)throws IOException{
        Path folder=root.resolve("assets/game/field-deployables-approved");
        for(var bp:ThrownDeployables.catalog()){
            Path file=folder.resolve(bp.mesh()+".glb");
            if(Files.isRegularFile(file)){
                AnimatedGlb rig=new AnimatedGlb(Files.readAllBytes(file),bp.mesh());
                if(!rig.hasFieldCoordinateContract())throw new IOException("Field equipment must declare canonical Y-up/-Z-forward coordinates: "+bp.id());
                if(!rig.clips().containsAll(Arrays.stream(ThrownDeployables.State.values()).map(Enum::name).toList()))
                    throw new IOException("Incomplete field equipment animation set: "+bp.id());
                rigs.put(bp.index(),rig);
            }
        }
    }
    public boolean available(int blueprint){return rigs.containsKey(blueprint);}
    public GlbMesh pose(ThrownDeployables.View view){
        AnimatedGlb rig=rigs.get(view.blueprint());if(rig==null)return null;
        String clip=view.state().name();
        double seconds=switch(view.state()){
            case THROWN -> view.ageMs()/1000.;case IMPACT -> view.ageMs()/1000.;
            case DEPLOYING -> (view.ageMs()-ThrownDeployables.IMPACT_MS)/1000.;
            case OPERATING -> view.operationAgeMs()/1000.;case DAMAGED,DESTROYED -> view.damageAgeMs()/1000.;
            case EXPIRING -> view.ageMs()%1000/1000.;default -> view.ageMs()%2000/1000.;};
        if(ThrownDeployables.blueprint(view.blueprint()).role()==ThrownDeployables.Role.TURRET&&view.tracking()&&view.state()==ThrownDeployables.State.ACTIVE){
            clip="OPERATING";seconds=.65; // Hold the aimed resting mechanism between real attacks.
        }
        return rig.pose(clip,seconds,view.aimYaw(),view.aimPitch());
    }
}
