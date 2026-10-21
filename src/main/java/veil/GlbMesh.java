package veil;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Minimal actual glTF 2.0 GLB triangle reader for static PSX geometry.
 * Coordinates: glTF X,Y(up),Z. No skinning, node transform, or texture loading yet.
 * Rejects malformed/truncated assets; never executes content from a GLB.
 */
public final class GlbMesh {
    public record Triangle(float ax,float ay,float az,float bx,float by,float bz,
        float cx,float cy,float cz,int rgb){}
    private final List<Triangle> triangles;
    private final float[] bounds;
    private GlbMesh(List<Triangle> tris,float[] b){triangles=List.copyOf(tris);bounds=b.clone();}
    public List<Triangle> triangles(){return triangles;}
    public float[] bounds(){return bounds.clone();}
    public int triangleCount(){return triangles.size();}
    public static GlbMesh asset(String name)throws IOException {
        if(!name.matches("[a-z0-9-]+"))throw new IOException("Invalid model name");
        Path local=Path.of("assets","game","kenney",name+".glb");
        byte[] bytes;
        if(Files.isRegularFile(local))bytes=Files.readAllBytes(local);
        else {
            try(InputStream in=GlbMesh.class.getResourceAsStream("/kenney/"+name+".glb")){
                if(in==null)throw new FileNotFoundException("Missing curated GLB: "+name);
                bytes=in.readAllBytes();
            }
        }
        return read(bytes);
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> obj(Object v)throws IOException {
        if(!(v instanceof Map<?,?> m))throw new IOException("Expected GLB JSON object");
        return (Map<String,Object>)m;
    }
    @SuppressWarnings("unchecked")
    private static List<Object> list(Object v)throws IOException {
        if(!(v instanceof List<?> l))throw new IOException("Expected GLB JSON array");
        return (List<Object>)l;
    }
    private static int num(Object v)throws IOException {
        if(!(v instanceof Number n))throw new IOException("Expected GLB JSON number");
        return n.intValue();
    }
    private static float number(Object v)throws IOException{
        if(!(v instanceof Number n))throw new IOException("Expected float");
        return n.floatValue();
    }
    private static Object get(List<Object> list,int index)throws IOException {
        if(index<0||index>=list.size())throw new IOException("Accessor out of bounds");
        return list.get(index);
    }
    private static int val(Map<String,Object> m,String k,int fallback)throws IOException {
        return m.containsKey(k)?num(m.get(k)):fallback;
    }
    public static GlbMesh read(byte[] file)throws IOException {
        if(file.length<28||file.length>100_000_000)throw new IOException("Invalid GLB size");
        ByteBuffer glb=ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        if(glb.getInt()!=0x46546c67||glb.getInt()!=2||glb.getInt()!=file.length)
            throw new IOException("Expected complete glTF 2 GLB file");
        byte[] json=null,bin=null;
        while(glb.remaining()>=8){
            int len=glb.getInt(),type=glb.getInt();
            if(len<0||len>glb.remaining()||(len&3)!=0)throw new IOException("Invalid GLB chunk length");
            byte[] body=new byte[len];glb.get(body);
            if(type==0x4e4f534a&&json==null)json=body;
            else if(type==0x004e4942&&bin==null)bin=body;
        }
        if(glb.hasRemaining()||json==null||bin==null)throw new IOException("Missing JSON/BIN chunk");
        Map<String,Object> root=obj(new Parser(new String(json,StandardCharsets.UTF_8).trim()).parse());
        List<Object> accessors=list(root.get("accessors"));
        List<Object> views=list(root.get("bufferViews"));
        List<Object> meshes=list(root.get("meshes"));
        List<Object> materials=root.containsKey("materials")?list(root.get("materials")):List.of();
        List<Triangle> triangles=new ArrayList<>();
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        ByteBuffer data=ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        for(Object mesh:meshes){
            for(Object primitive:list(obj(mesh).get("primitives"))){
                Map<String,Object> prim=obj(primitive);
                if(val(prim,"mode",4)!=4)continue; // Only TRIANGLES
                Map<String,Object> attr=obj(prim.get("attributes"));
                int accessorId=num(attr.get("POSITION"));
                Map<String,Object> position=obj(get(accessors,accessorId));
                if(val(position,"componentType",0)!=5126||!"VEC3".equals(position.get("type")))
                    throw new IOException("Positions must be float32 VEC3");
                int count=val(position,"count",0);
                if(count<=0||count>500_000)throw new IOException("Invalid vertex count");
                float[][] vertices=new float[count][3];
                int posOffset=offset(position,views,bin.length,12);
                int stride=stride(position,views,12);
                for(int i=0;i<count;i++)for(int xyz=0;xyz<3;xyz++){
                    float v=data.getFloat(posOffset+i*stride+xyz*4);
                    if(!Float.isFinite(v)||Math.abs(v)>10000)throw new IOException("Nonfinite vertex");
                    vertices[i][xyz]=v;
                    bounds[xyz]=Math.min(bounds[xyz],v);
                    bounds[xyz+3]=Math.max(bounds[xyz+3],v);
                }
                int[] indices;
                if(prim.containsKey("indices")){
                    Map<String,Object> accessor=obj(get(accessors,num(prim.get("indices"))));
                    int n=val(accessor,"count",0),component=val(accessor,"componentType",0);
                    int element=component==5125?4:component==5123?2:component==5121?1:0;
                    if(element==0||n<0||n>1_500_000)throw new IOException("Unsupported indices");
                    int start=offset(accessor,views,bin.length,element);
                    int by=stride(accessor,views,element);
                    indices=new int[n];
                    for(int i=0;i<n;i++){
                        int at=start+i*by;
                        indices[i]=component==5125?data.getInt(at):component==5123?
                            Short.toUnsignedInt(data.getShort(at)):Byte.toUnsignedInt(data.get(at));
                    }
                }else{
                    indices=new int[count];for(int i=0;i<count;i++)indices[i]=i;
                }
                int rgb=0x7f8084;
                int mat=val(prim,"material",-1);
                if(mat>=0&&mat<materials.size()){
                    Map<String,Object> material=obj(get(materials,mat));
                    Object pbr=material.get("pbrMetallicRoughness");
                    if(pbr!=null){
                        Object factor=obj(pbr).get("baseColorFactor");
                        if(factor!=null){
                            List<Object> color=list(factor);
                            if(color.size()>=3){
                                rgb=(Math.max(0,Math.min(255,(int)(number(color.get(0))*255)))<<16)|
                                    (Math.max(0,Math.min(255,(int)(number(color.get(1))*255)))<<8)|
                                    Math.max(0,Math.min(255,(int)(number(color.get(2))*255)));
                            }
                        }
                    }
                    if(!material.containsKey("pbrMetallicRoughness")||
                        !obj(material.get("pbrMetallicRoughness")).containsKey("baseColorFactor")){
                        // Authoritative material association remains present. Neutral tint by material name
                        // distinguishes low-poly surfaces until image textures are wired.
                        String n=String.valueOf(material.getOrDefault("name",""));
                        int hash=n.hashCode();
                        rgb=0x686868|((hash&7)<<16)|(((hash>>>4)&7)<<8)|((hash>>>8)&7);
                    }
                }
                for(int i=0;i+2<indices.length;i+=3){
                    int a=indices[i],b=indices[i+1],c=indices[i+2];
                    if(a<0||b<0||c<0||a>=count||b>=count||c>=count)
                        throw new IOException("Invalid mesh index");
                    float[] v=vertices[a],w=vertices[b],z=vertices[c];
                    triangles.add(new Triangle(v[0],v[1],v[2],w[0],w[1],w[2],
                        z[0],z[1],z[2],rgb));
                }
            }
        }
        if(triangles.isEmpty()||triangles.size()>500_000)throw new IOException("GLB has no usable triangles");
        return new GlbMesh(triangles,bounds);
    }
    private static int stride(Map<String,Object> accessor,List<Object> views,int width)throws IOException {
        Map<String,Object> view=obj(get(views,num(accessor.get("bufferView"))));
        int value=val(view,"byteStride",width);
        if(value<width||value>1024)throw new IOException("Bad buffer stride");
        return value;
    }
    private static int offset(Map<String,Object> accessor,List<Object> views,int length,int width)throws IOException {
        Map<String,Object> view=obj(get(views,num(accessor.get("bufferView"))));
        int start=val(view,"byteOffset",0)+val(accessor,"byteOffset",0);
        int step=stride(accessor,views,width);
        int count=val(accessor,"count",0);
        if(start<0||count<1||(long)start+(long)(count-1)*step+width>length)
            throw new IOException("Buffer view out of bounds");
        return start;
    }

    /** Strict bounded object/array/number/string parser. No external JSON dependency. */
    private static final class Parser {
        private final String text;
        private int i;
        Parser(String text){this.text=text;}
        Object parse()throws IOException{
            Object v=value();space();
            if(i!=text.length())throw new IOException("Trailing JSON garbage");
            return v;
        }
        private void space(){while(i<text.length()&&Character.isWhitespace(text.charAt(i)))i++;}
        private Object value()throws IOException{
            space();
            if(i>=text.length())throw new IOException("Incomplete JSON");
            char c=text.charAt(i);
            if(c=='{')return object();
            if(c=='[')return array();
            if(c=='"')return string();
            if(c=='t'&&text.startsWith("true",i)){i+=4;return true;}
            if(c=='f'&&text.startsWith("false",i)){i+=5;return false;}
            if(c=='n'&&text.startsWith("null",i)){i+=4;return null;}
            int start=i;
            while(i<text.length()&&"-+.eE0123456789".indexOf(text.charAt(i))>=0)i++;
            if(start==i)throw new IOException("Invalid JSON token at "+i);
            try{return Double.parseDouble(text.substring(start,i));}
            catch(NumberFormatException ex){throw new IOException("Bad JSON number",ex);}
        }
        private Map<String,Object> object()throws IOException{
            Map<String,Object> map=new LinkedHashMap<>();
            i++;space();if(match('}'))return map;
            while(true){
                space();String key=string();space();expect(':');
                map.put(key,value());space();
                if(match('}'))return map;
                expect(',');
            }
        }
        private List<Object> array()throws IOException{
            List<Object> values=new ArrayList<>();
            i++;space();if(match(']'))return values;
            while(true){
                values.add(value());space();if(match(']'))return values;expect(',');
            }
        }
        private String string()throws IOException{
            expect('"');StringBuilder s=new StringBuilder();
            while(i<text.length()){
                char c=text.charAt(i++);
                if(c=='"')return s.toString();
                if(c=='\\'){
                    if(i>=text.length())throw new IOException("Bad JSON escape");
                    char e=text.charAt(i++);
                    switch(e){
                        case '"','\\','/' -> s.append(e);
                        case 'n' -> s.append('\n');
                        case 'r' -> s.append('\r');
                        case 't' -> s.append('\t');
                        case 'b' -> s.append('\b');
                        case 'f' -> s.append('\f');
                        case 'u' -> {
                            if(i+4>text.length())throw new IOException("Bad unicode escape");
                            try{s.append((char)Integer.parseInt(text.substring(i,i+4),16));}
                            catch(NumberFormatException x){throw new IOException("Bad unicode",x);}
                            i+=4;
                        }
                        default -> throw new IOException("Bad JSON escape");
                    }
                }else s.append(c);
            }
            throw new IOException("Unterminated JSON string");
        }
        private boolean match(char c){if(i<text.length()&&text.charAt(i)==c){i++;return true;}return false;}
        private void expect(char c)throws IOException{space();if(!match(c))throw new IOException("Expected '"+c+"' at "+i);}
    }
}
