package veil;

import java.io.*;
import java.nio.*;
import java.util.*;

/** Bounded glTF rigid-node and morph-target animation evaluator. Preserves hierarchy, UVs and embedded atlases.
 * Skins and unsupported interpolation fail explicitly rather than freezing silently. */
public final class AnimatedGlb {
    private final Map<String,Object> root;
    private final ByteBuffer bin;
    private final List<Map<String,Object>> nodes, meshes, accessors, views;
    private final Map<String,byte[]> textures=new LinkedHashMap<>();
    private final String namespace;
    @SuppressWarnings("unchecked") private static Map<String,Object> obj(Object o){return (Map<String,Object>)o;}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> maps(Object o){return o==null?List.of():(List<Map<String,Object>>)o;}
    private static int integer(Object o){return ((Number)o).intValue();}
    private static double scalar(Object o){return ((Number)o).doubleValue();}
    public AnimatedGlb(byte[] bytes,String namespace)throws IOException {
        this.namespace=namespace;
        try {
            if(bytes.length<28||bytes.length>20_000_000)throw new IOException("Invalid animated GLB size");
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(b.getInt()!=0x46546c67||b.getInt()!=2||b.getInt()!=bytes.length)throw new IOException("Invalid GLB header");
            Map<String,Object> json=null;byte[] data=null;
            while(b.remaining()>=8){int n=b.getInt(),type=b.getInt();
                if(n<0||n>b.remaining()||(n&3)!=0)throw new IOException("Invalid GLB chunk");
                byte[] chunk=new byte[n];b.get(chunk);
                if(type==0x4e4f534a)json=obj(new GlbMesh.Parser(new String(chunk,java.nio.charset.StandardCharsets.UTF_8).trim()).parse());
                if(type==0x004e4942)data=chunk;
            }
            if(json==null||data==null||b.hasRemaining())throw new IOException("Incomplete GLB");
            root=json;bin=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            nodes=maps(root.get("nodes"));meshes=maps(root.get("meshes"));
            accessors=maps(root.get("accessors"));views=maps(root.get("bufferViews"));
            if(nodes.size()>512||meshes.size()>256||root.containsKey("skins"))throw new IOException("Unsupported or oversized rig");
            for(var n:nodes)if(n.containsKey("matrix"))throw new IOException("Rig requires TRS nodes");
            for(var a:maps(root.get("animations")))for(var sampler:maps(a.get("samplers")))
                if(!Set.of("LINEAR","STEP").contains(sampler.getOrDefault("interpolation","LINEAR")))throw new IOException("Unsupported interpolation");
            int imageId=0;
            for(var image:maps(root.get("images"))){
                if(!image.containsKey("bufferView"))throw new IOException("Deployable textures must be embedded");
                var view=views.get(integer(image.get("bufferView")));int start=integer(view.getOrDefault("byteOffset",0)),length=integer(view.get("byteLength"));
                if(start<0||length<0||(long)start+length>bin.limit())throw new IOException("Invalid embedded image");
                byte[] png=Arrays.copyOfRange(data,start,start+length);
                textures.put(namespace+"-"+imageId+++".png",png);
            }
        }catch(IOException e){throw e;}catch(RuntimeException e){throw new IOException("Malformed animated GLB",e);}
    }
    public Set<String> clips(){Set<String>s=new LinkedHashSet<>();for(var a:maps(root.get("animations")))s.add(String.valueOf(a.get("name")));return Set.copyOf(s);}
    private double[] array(Object value,double... defaults){
        if(value==null)return defaults.clone();List<?> l=(List<?>)value;
        double[] v=new double[l.size()];for(int i=0;i<v.length;i++){v[i]=scalar(l.get(i));if(!Double.isFinite(v[i]))throw new IllegalArgumentException("Nonfinite transform");}return v;
    }
    private double[][] accessor(int id,int width){
        var a=accessors.get(id);if(a.containsKey("sparse"))throw new IllegalArgumentException("Sparse accessor unsupported");
        var v=views.get(integer(a.get("bufferView")));int type=integer(a.get("componentType")),count=integer(a.get("count"));
        int size=type==5126||type==5125?4:type==5123?2:type==5121?1:0;
        if(size==0||count<1||count>200_000)throw new IllegalArgumentException("Invalid accessor");
        int start=integer(v.getOrDefault("byteOffset",0))+integer(a.getOrDefault("byteOffset",0));
        int stride=integer(v.getOrDefault("byteStride",size*width)),end=integer(v.getOrDefault("byteOffset",0))+integer(v.get("byteLength"));
        if(stride<size*width||start<0||(long)start+(long)(count-1)*stride+size*width>end||end>bin.limit())throw new IllegalArgumentException("Accessor exceeds buffer view");
        double[][] values=new double[count][width];
        for(int i=0;i<count;i++)for(int j=0;j<width;j++){int at=start+i*stride+j*size;
            double d=switch(type){case 5126->bin.getFloat(at);case 5125->Integer.toUnsignedLong(bin.getInt(at));case 5123->Short.toUnsignedInt(bin.getShort(at));default->Byte.toUnsignedInt(bin.get(at));};
            if(!Double.isFinite(d))throw new IllegalArgumentException("Nonfinite accessor");values[i][j]=d;
        }return values;
    }
    private static double[] interpolate(double[] a,double[] b,double t,boolean quat){
        double[] out=new double[a.length];double dot=0;for(int i=0;i<a.length;i++)dot+=a[i]*b[i];
        double sign=quat&&dot<0?-1:1;dot=Math.abs(dot);
        double u=1-t,v=t;
        if(quat&&dot<.9995){double theta=Math.acos(Math.min(1,dot)),sin=Math.sin(theta);u=Math.sin((1-t)*theta)/sin;v=Math.sin(t*theta)/sin;}
        double len=0;for(int i=0;i<out.length;i++){out[i]=a[i]*u+b[i]*v*sign;len+=out[i]*out[i];}
        if(quat){len=Math.sqrt(len);if(len<1e-9)throw new IllegalArgumentException("Invalid quaternion");for(int i=0;i<4;i++)out[i]/=len;}return out;
    }
    private static double[] mul(double[] a,double[] b){double[] r=new double[16];for(int c=0;c<4;c++)for(int row=0;row<4;row++)for(int k=0;k<4;k++)r[c*4+row]+=a[k*4+row]*b[c*4+k];return r;}
    private static double[] trs(double[] t,double[] q,double[] s){
        double x=q[0],y=q[1],z=q[2],w=q[3];
        return new double[]{(1-2*y*y-2*z*z)*s[0],(2*x*y+2*z*w)*s[0],(2*x*z-2*y*w)*s[0],0,
            (2*x*y-2*z*w)*s[1],(1-2*x*x-2*z*z)*s[1],(2*y*z+2*x*w)*s[1],0,
            (2*x*z+2*y*w)*s[2],(2*y*z-2*x*w)*s[2],(1-2*x*x-2*y*y)*s[2],0,t[0],t[1],t[2],1};
    }
    private double[] world(int id,double[][] local,double[][] world,boolean[] visiting){
        if(world[id]!=null)return world[id];if(visiting[id])throw new IllegalArgumentException("Node hierarchy cycle");visiting[id]=true;
        int parent=-1;for(int i=0;i<nodes.size();i++)for(Object c:(List<?>)nodes.get(i).getOrDefault("children",List.of()))if(integer(c)==id){if(parent>=0)throw new IllegalArgumentException("Multiple node parents");parent=i;}
        world[id]=parent<0?local[id]:mul(world(parent,local,world,visiting),local[id]);visiting[id]=false;return world[id];
    }
    /** Clip time is seconds, clamped; callers explicitly wrap looping states. Aim is host-owned. */
    public GlbMesh pose(String clip,double seconds,double aimYaw,double aimPitch){
        double[][] t=new double[nodes.size()][],q=new double[nodes.size()][],s=new double[nodes.size()][],weights=new double[nodes.size()][];
        for(int i=0;i<nodes.size();i++){var n=nodes.get(i);t[i]=array(n.get("translation"),0,0,0);q[i]=array(n.get("rotation"),0,0,0,1);s[i]=array(n.get("scale"),1,1,1);}
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).containsKey("mesh")){
            var mesh=meshes.get(integer(nodes.get(i).get("mesh")));
            int count=maps(maps(mesh.get("primitives")).get(0).get("targets")).size();
            weights[i]=array(nodes.get(i).getOrDefault("weights",mesh.get("weights")),new double[count]);
            if(weights[i].length!=count)throw new IllegalArgumentException("Morph weight count mismatch");
        }
        for(var a:maps(root.get("animations")))if(clip.equals(a.get("name"))){
            var samplers=maps(a.get("samplers"));for(var channel:maps(a.get("channels"))){
                var target=obj(channel.get("target"));int node=integer(target.get("node"));String path=String.valueOf(target.get("path"));
                if(!Set.of("translation","rotation","scale","weights").contains(path))throw new IllegalArgumentException("Unsupported track path");
                var sampler=samplers.get(integer(channel.get("sampler")));double[][] times=accessor(integer(sampler.get("input")),1),values=accessor(integer(sampler.get("output")),path.equals("rotation")?4:path.equals("weights")?1:3);
                if(path.equals("weights")){
                    int count=weights[node]==null?0:weights[node].length;
                    if(count==0||values.length!=times.length*count)throw new IllegalArgumentException("Invalid morph animation count");
                    double[][] grouped=new double[times.length][count];
                    for(int frame=0;frame<times.length;frame++)for(int weight=0;weight<count;weight++)grouped[frame][weight]=values[frame*count+weight][0];
                    values=grouped;
                }
                if(times.length!=values.length)throw new IllegalArgumentException("Animation count mismatch");int k=0;while(k+1<times.length&&times[k+1][0]<=seconds)k++;
                int next=Math.min(k+1,times.length-1);double dt=times[next][0]-times[k][0];double alpha=dt<=0?0:Math.max(0,Math.min(1,(seconds-times[k][0])/dt));
                if("STEP".equals(sampler.get("interpolation")))alpha=0;
                double[] value=interpolate(values[k],values[next],alpha,path.equals("rotation"));
                switch(path){case "translation"->t[node]=value;case "rotation"->q[node]=value;case "scale"->s[node]=value;case "weights"->weights[node]=value;}
            }
        }
        double[][] local=new double[nodes.size()][],world=new double[nodes.size()][];
        for(int i=0;i<nodes.size();i++){
            String name=String.valueOf(nodes.get(i).get("name"));
            local[i]=trs(t[i],q[i],s[i]);
            if(name.equals("AimYaw"))local[i]=mul(local[i],trs(new double[3],new double[]{0,Math.sin(aimYaw/2),0,Math.cos(aimYaw/2)},new double[]{1,1,1}));
            if(name.equals("AimPitch"))local[i]=mul(local[i],trs(new double[3],new double[]{Math.sin(aimPitch/2),0,0,Math.cos(aimPitch/2)},new double[]{1,1,1}));
        }
        List<GlbMesh.Surface> surfaces=new ArrayList<>();
        for(int node=0;node<nodes.size();node++)if(nodes.get(node).containsKey("mesh")){
            double[] m=world(node,local,world,new boolean[nodes.size()]);
            for(var p:maps(meshes.get(integer(nodes.get(node).get("mesh"))).get("primitives"))){
                if(integer(p.getOrDefault("mode",4))!=4)throw new IllegalArgumentException("Only triangles supported");
                var attr=obj(p.get("attributes"));double[][] xyz=accessor(integer(attr.get("POSITION")),3),uv=attr.containsKey("TEXCOORD_0")?accessor(integer(attr.get("TEXCOORD_0")),2):null;
                var targets=maps(p.get("targets"));
                if(!targets.isEmpty()){
                    if(weights[node]==null||weights[node].length!=targets.size())throw new IllegalArgumentException("Invalid primitive morph targets");
                    for(int target=0;target<targets.size();target++)if(targets.get(target).containsKey("POSITION")&&weights[node][target]!=0){
                        double[][] delta=accessor(integer(targets.get(target).get("POSITION")),3);
                        if(delta.length!=xyz.length)throw new IllegalArgumentException("Morph target vertex count mismatch");
                        for(int v=0;v<xyz.length;v++)for(int axis=0;axis<3;axis++)xyz[v][axis]+=delta[v][axis]*weights[node][target];
                    }
                }
                double[][] indices=p.containsKey("indices")?accessor(integer(p.get("indices")),1):null;
                String texture=null;int tint=0xffffff;
                if(p.containsKey("material")){
                    var material=maps(root.get("materials")).get(integer(p.get("material")));var pbr=obj(material.getOrDefault("pbrMetallicRoughness",Map.of()));
                    double[] color=array(pbr.get("baseColorFactor"),1,1,1,1);tint=((int)(255*color[0])<<16)|((int)(255*color[1])<<8)|(int)(255*color[2]);
                    if(pbr.containsKey("baseColorTexture")){int tex=integer(obj(pbr.get("baseColorTexture")).get("index"));int image=integer(maps(root.get("textures")).get(tex).get("source"));texture=namespace+"-"+image+".png";}
                }
                int count=indices==null?xyz.length:indices.length;
                for(int face=0;face+2<count;face+=3){float[] v=new float[9],u=new float[6];
                    for(int c=0;c<3;c++){int index=indices==null?face+c:(int)indices[face+c][0];double[] pos=xyz[index];
                        for(int axis=0;axis<3;axis++)v[c*3+axis]=(float)(m[axis]*pos[0]+m[4+axis]*pos[1]+m[8+axis]*pos[2]+m[12+axis]);
                        if(uv!=null){u[c*2]=(float)uv[index][0];u[c*2+1]=(float)uv[index][1];}}
                    var tri=new GlbMesh.Triangle(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],tint);
                    surfaces.add(new GlbMesh.Surface(tri,texture,u[0],u[1],u[2],u[3],u[4],u[5],tint));
                }
            }
        }
        return GlbMesh.animated(surfaces,textures);
    }
}
