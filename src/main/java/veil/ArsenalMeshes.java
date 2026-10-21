package veil;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.*;

/** Textured primitive component meshes, rigid poses and first-person projection. */
public final class ArsenalMeshes {
    record Part(String role,int group,float[] pivot,java.util.List<GlbMesh.Triangle> triangles){}
    private final Map<Integer,java.util.List<Part>> parts=new HashMap<>();
    private final Map<String,GlbMesh> world=new HashMap<>();
    public java.util.List<Part> parts(int w){return parts.computeIfAbsent(w,this::load);}
    private java.util.List<Part> load(int w){
        String file=ArsenalCatalog.get(w).id()+".vmesh";
        try(InputStream stream=Files.isRegularFile(Path.of("assets/game/arsenal",file))?Files.newInputStream(Path.of("assets/game/arsenal",file)):ArsenalMeshes.class.getResourceAsStream("/arsenal/"+file)){
            if(stream==null)throw new IOException("Missing working arsenal asset "+file);
            try(DataInputStream in=new DataInputStream(new BufferedInputStream(stream))){
                if(in.readInt()!=0x4d564153||in.readInt()!=2)throw new IOException("Unsupported component mesh");int count=in.readInt();if(count<1||count>160)throw new IOException("Component count");
                java.util.List<Part> out=new ArrayList<>();int total=0;
                for(int j=0;j<count;j++){String role=in.readUTF();int group=in.readInt();float[] pivot={in.readFloat(),in.readFloat(),in.readFloat()};int n=in.readInt();if(group<0||group>2||n<1||(total+=n)>100000)throw new IOException("Component bounds");java.util.List<GlbMesh.Triangle> t=new ArrayList<>();
                    for(int k=0;k<n;k++){float[] v=new float[9];for(int a=0;a<9;a++){v[a]=in.readFloat();if(!Float.isFinite(v[a])||Math.abs(v[a])>3)throw new IOException("Invalid vertex");}t.add(new GlbMesh.Triangle(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],in.readInt()));}out.add(new Part(role,group,pivot,java.util.List.copyOf(t)));
                }if(in.read()!=-1)throw new IOException("Trailing component bytes");return java.util.List.copyOf(out);
            }
        }catch(IOException ex){throw new IllegalStateException("Working arsenal unavailable",ex);}
    }
    public Map<String,GlbMesh> worldModels(){return Collections.unmodifiableMap(world);}
    public String worldId(int w){String id=ArsenalCatalog.get(w).id();world.computeIfAbsent(id,k->posed(w,0,0));return id;}
    public GlbMesh posed(int w,int action,double phase){
        java.util.List<GlbMesh.Triangle> out=new ArrayList<>();double pulse=Math.sin(Math.PI*Math.min(1,Math.max(0,phase)));String mechanism=ArsenalCatalog.get(w).mechanism();
        for(Part part:parts(w))for(GlbMesh.Triangle t:part.triangles){
            float[][] v={{t.ax(),t.ay(),t.az()},{t.bx(),t.by(),t.bz()},{t.cx(),t.cy(),t.cz()}};
            for(float[] p:v){
                if(part.group==1){
                    if((action==1||action==4)&&java.util.List.of("rotor","rings","disc","cylinder").contains(mechanism))rotate(p,part.pivot,mechanism.equals("disc")?1:2,phase*Math.PI*4);
                    if((action==1||action==4)&&java.util.List.of("slide","bolt","pump").contains(mechanism))p[2]-=.045*pulse;
                    if(action==3&&java.util.List.of("hinge","drum","cylinder").contains(mechanism))rotate(p,part.pivot,0,.55*pulse);
                }
                if(part.group==2&&action==3)p[1]-=.28*pulse;
                if(action==1||action==4){p[2]-=.016*pulse;p[1]+=.006*pulse;}
                if(action==3)rotate(p,new float[]{0,0,0},2,-.13*pulse);
            }
            out.add(new GlbMesh.Triangle(v[0][0],v[0][1],v[0][2],v[1][0],v[1][1],v[1][2],v[2][0],v[2][1],v[2][2],t.rgb()));
        }return GlbMesh.authored(out);
    }
    static void rotate(float[] p,float[] pivot,int axis,double angle){int a=(axis+1)%3,b=(axis+2)%3;double x=p[a]-pivot[a],y=p[b]-pivot[b],c=Math.cos(angle),s=Math.sin(angle);p[a]=(float)(pivot[a]+x*c-y*s);p[b]=(float)(pivot[b]+x*s+y*c);}
    private record Face(int[] x,int[] y,int rgb,double depth){}
    public void draw(Graphics2D g,int w,ArsenalSystems.View state,long actionAge,int width,int height,double bob){
        int action=state==null?0:state.action();var d=ArsenalCatalog.get(w);double duration=action==3?d.reloadMs():action==4?Math.min(400,d.alternate().cooldown()):Math.min(400,d.primary().cooldown());
        double phase=actionAge/Math.max(50,duration);if(action!=3&&phase>1)action=0;phase=Math.min(1,Math.max(0,phase));
        GlbMesh mesh=posed(w,action,phase);float[] bounds=mesh.bounds();double scale=.8/Math.max(1,(bounds[5]-bounds[2])/1.05);
        java.util.List<Face> faces=new ArrayList<>();
        for(var t:mesh.triangles()){
            float[][] v={{t.ax(),t.ay(),t.az()},{t.bx(),t.by(),t.bz()},{t.cx(),t.cy(),t.cz()}};int[] x=new int[3],y=new int[3];double zsum=0;
            for(int i=0;i<3;i++){double f=v[i][2]*scale,z=Math.max(.20,.95+f),r=.20+v[i][0]*scale-f*.23,u=-.28+v[i][1]*scale+f*.12;x[i]=(int)(width*.5+r/z*height*.95);y[i]=(int)(height*.52-u/z*height*.95+bob);zsum+=z;}
            double ax=t.bx()-t.ax(),ay=t.by()-t.ay(),az=t.bz()-t.az(),bx=t.cx()-t.ax(),by=t.cy()-t.ay(),bz=t.cz()-t.az();double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,norm=Math.sqrt(nx*nx+ny*ny+nz*nz);double light=norm<1e-8?.7:.65+.35*Math.abs((nx*.3+ny*.8-nz*.4)/norm);
            int c=t.rgb();int rgb=((int)((c>>16&255)*light)<<16)|((int)((c>>8&255)*light)<<8)|(int)((c&255)*light);faces.add(new Face(x,y,rgb,zsum/3));
        }faces.sort((a,b)->Double.compare(b.depth,a.depth));for(Face f:faces){g.setColor(new Color(f.rgb));g.fillPolygon(f.x,f.y,3);}
    }
}
