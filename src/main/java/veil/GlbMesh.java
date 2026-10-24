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
    /** GPU source material record. UVs are preserved; Java2D still uses sampled RGB. */
    public record Surface(Triangle triangle,String texture,float au,float av,
                          float bu,float bv,float cu,float cv,int tintRgb) {}
    private final List<Triangle> triangles;
    private final List<Surface> surfaces;
    private final float[] bounds;
    private final Map<String,byte[]> embeddedTextures;
    private final Map<String,java.awt.image.BufferedImage> decodedTextures=new HashMap<>();
    private GlbMesh(List<Triangle> tris,float[] b,List<Surface> materials){
        this(tris,b,materials,Map.of());
    }
    private GlbMesh(List<Triangle> tris,float[] b,List<Surface> materials,
                    Map<String,byte[]> textures){
        triangles=List.copyOf(tris);bounds=b.clone();surfaces=List.copyOf(materials);
        embeddedTextures=Map.copyOf(textures);
    }
    /** PNG bytes for assets whose original atlas is embedded in the model registry. */
    public byte[] textureBytes(String texture){
        byte[] bytes=embeddedTextures.get(texture);
        return bytes==null?null:bytes.clone();
    }
    public synchronized java.awt.image.BufferedImage textureImage(String texture)throws IOException{
        if(!embeddedTextures.containsKey(texture))return null;
        java.awt.image.BufferedImage image=decodedTextures.get(texture);
        if(image!=null)return image;
        image=javax.imageio.ImageIO.read(new ByteArrayInputStream(embeddedTextures.get(texture)));
        if(image==null||image.getWidth()>4096||image.getHeight()>4096)
            throw new IOException("Invalid GGBot 128x128 source atlas");
        decodedTextures.put(texture,image);
        return image;
    }
    public static GlbMesh authored(List<Triangle> faces){
        if(faces.isEmpty())throw new IllegalArgumentException("Empty mesh");
        float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(Triangle t:faces){
            float[] v={t.ax(),t.ay(),t.az(),t.bx(),t.by(),t.bz(),t.cx(),t.cy(),t.cz()};
            for(int i=0;i<9;i++){int ax=i%3;b[ax]=Math.min(b[ax],v[i]);b[ax+3]=Math.max(b[ax+3],v[i]);}
        }
        List<Surface> plain=new ArrayList<>();
        for(Triangle face:faces)plain.add(new Surface(face,null,0,0,0,0,0,0,0xffffff));
        return new GlbMesh(faces,b,plain);
    }
    static GlbMesh animated(List<Surface> surfaces,Map<String,byte[]> textures){
        List<Triangle> faces=surfaces.stream().map(Surface::triangle).toList();
        return new GlbMesh(faces,authored(faces).bounds(),surfaces,textures);
    }
    public List<Triangle> triangles(){return triangles;}
    public List<Surface> surfaces(){return surfaces;}
    public float[] bounds(){return bounds.clone();}
    public int triangleCount(){return triangles.size();}
    // The urban source pack is immutable for the lifetime of a game process.
    // One parsed mesh object is reused across scenes, world seeds and all instances.
    // Cache is bounded by the fixed 124-model Kenney catalog.
    private static final Map<String,GlbMesh> ASSET_CACHE=new HashMap<>();
    public static synchronized GlbMesh asset(String name)throws IOException {
        if(name==null||!name.matches("[a-z0-9-]+"))throw new IOException("Invalid model name");
        GlbMesh cached=ASSET_CACHE.get(name);
        if(cached!=null)return cached;
        Path local=Path.of("assets","game","kenney",name+".glb");
        byte[] bytes;
        if(Files.isRegularFile(local))bytes=Files.readAllBytes(local);
        else {
            try(InputStream in=GlbMesh.class.getResourceAsStream("/kenney/"+name+".glb")){
                bytes=in==null?KenneyAssetPack.load().model(name+".glb"):in.readAllBytes();
            }
        }
        GlbMesh parsed=read(bytes,fileName->KenneyAssetPack.load().texture(fileName));
        if(ASSET_CACHE.size()>=124)
            throw new IOException("Unexpected mesh cache cardinality");
        ASSET_CACHE.put(name,parsed);
        return parsed;
    }
    /** Import GGBot's native Blender-compatible OBJ UV layout without generating new UVs.
     * The exact authored 128x128 PNG becomes the per-vertex surface texture.
     * Quad triangulation is a stable fan; every face preserves its original UV corners.
     */
    public static GlbMesh texturedObj(byte[] obj,byte[] png,String id)throws IOException{
        if(obj==null||png==null||id==null||!id.matches("ggbot-[a-z0-9_-]+"))
            throw new IOException("Invalid textured OBJ");
        java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
        if(image==null||image.getWidth()>4096||image.getHeight()>4096)
            throw new IOException("Not an original 128x128 car atlas");
        List<float[]> xyz=new ArrayList<>(),uv=new ArrayList<>();
        List<Triangle> tris=new ArrayList<>();
        List<Surface> surfaces=new ArrayList<>();
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        String input=new String(obj,StandardCharsets.UTF_8).replace("\uFEFF","");
        for(String raw:input.split("\\R")){
            String line=raw.trim();
            if(line.isEmpty()||line.startsWith("#"))continue;
            String[] words=line.split("\\s+");
            if(words[0].equals("v")&&words.length>=4){
                float[] v=new float[3];
                for(int i=0;i<3;i++){
                    try{v[i]=Float.parseFloat(words[i+1]);}
                    catch(NumberFormatException e){throw new IOException("Invalid OBJ coordinate",e);}
                    if(!Float.isFinite(v[i])||Math.abs(v[i])>100)
                        throw new IOException("Out-of-range car coordinate");
                    bounds[i]=Math.min(bounds[i],v[i]);bounds[i+3]=Math.max(bounds[i+3],v[i]);
                }
                xyz.add(v);
            }else if(words[0].equals("vt")&&words.length>=3){
                float[] t=new float[2];
                for(int i=0;i<2;i++){
                    try{t[i]=Float.parseFloat(words[i+1]);}
                    catch(NumberFormatException e){throw new IOException("Invalid car UV",e);}
                    if(!Float.isFinite(t[i])||Math.abs(t[i])>16)
                        throw new IOException("Out-of-range car UV");
                }
                uv.add(t);
            }else if(words[0].equals("f")){
                if(words.length<4||words.length>5)
                    throw new IOException("Unexpected GGBot polygon vertex count");
                int[][] corners=new int[words.length-1][2];
                for(int i=1;i<words.length;i++){
                    String[] idx=words[i].split("/",-1);
                    if(idx.length<2||idx[1].isEmpty())
                        throw new IOException("Missing source UV corner");
                    try{
                        corners[i-1][0]=objIndex(Integer.parseInt(idx[0]),xyz.size());
                        corners[i-1][1]=objIndex(Integer.parseInt(idx[1]),uv.size());
                    }catch(NumberFormatException e){throw new IOException("Invalid OBJ face index",e);}
                }
                for(int i=1;i+1<corners.length;i++){
                    int[][] c={corners[0],corners[i],corners[i+1]};
                    float[] a=xyz.get(c[0][0]),b=xyz.get(c[1][0]),v=xyz.get(c[2][0]);
                    float[] u1=uv.get(c[0][1]),u2=uv.get(c[1][1]),u3=uv.get(c[2][1]);
                    double u=(u1[0]+u2[0]+u3[0])/3.0;
                    double vv=(u1[1]+u2[1]+u3[1])/3.0;
                    int px=Math.floorMod((int)Math.floor(u*128),128);
                    int py=Math.floorMod((int)Math.floor((1-vv)*128),128);
                    int color=image.getRGB(px,py)&0xffffff;
                    Triangle t=new Triangle(a[0],a[1],a[2],b[0],b[1],b[2],
                        v[0],v[1],v[2],color);
                    tris.add(t);
                    surfaces.add(new Surface(t,id,
                        u1[0],u1[1],u2[0],u2[1],u3[0],u3[1],0xffffff));
                }
            }
        }
        if(tris.size()<250||tris.size()>550||uv.size()<100||xyz.size()<100)
            throw new IOException("Unexpected source mesh topology for "+id);
        return new GlbMesh(tris,bounds,surfaces,Map.of(id,png.clone()));
    }
    private static int objIndex(int oneBased,int n)throws IOException{
        int at=oneBased>0?oneBased-1:n+oneBased;
        if(at<0||at>=n)throw new IOException("OBJ face refers beyond source vertices");
        return at;
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
    @FunctionalInterface public interface TextureSource{byte[] open(String name)throws IOException;}
    public static GlbMesh read(byte[] file)throws IOException{return read(file,null);}
    public static GlbMesh read(byte[] file,TextureSource source)throws IOException {
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
        List<Object> images=root.containsKey("images")?list(root.get("images")):List.of();
        List<Object> textures=root.containsKey("textures")?list(root.get("textures")):List.of();
        Map<String,Integer> palette=new HashMap<>();
        Map<String,java.awt.image.BufferedImage> sourceImages=new HashMap<>();
        List<Triangle> triangles=new ArrayList<>();
        List<Surface> surfaces=new ArrayList<>();
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
                String faceTexture=null;
                float[] factorTint={1,1,1};
                int mat=val(prim,"material",-1);
                if(mat>=0&&mat<materials.size()){
                    Map<String,Object> material=obj(get(materials,mat));
                    Object pbr=material.get("pbrMetallicRoughness");
                    if(pbr!=null){
                        Map<String,Object> params=obj(pbr);
                        if(source!=null&&params.containsKey("baseColorTexture")){
                            int texId=num(obj(params.get("baseColorTexture")).get("index"));
                            Map<String,Object> ref=obj(get(textures,texId));
                            Map<String,Object> image=obj(get(images,val(ref,"source",0)));
                            String uri=String.valueOf(image.getOrDefault("uri",""));
                            if(uri.startsWith("Textures/")&&uri.lastIndexOf('/')==8&&uri.endsWith(".png")){
                                String tex=uri.substring(9);
                                Integer color=palette.get(tex);
                                if(color==null){
                                    color=samplePalette(source.open(tex));
                                    palette.put(tex,color);
                                }
                                rgb=color;faceTexture=tex;
                                if(!sourceImages.containsKey(tex)){
                                    java.awt.image.BufferedImage imageData=javax.imageio.ImageIO.read(
                                        new ByteArrayInputStream(source.open(tex)));
                                    if(imageData==null||imageData.getWidth()>4096||imageData.getHeight()>4096)
                                        throw new IOException("Unusable GLB texture: "+tex);
                                    sourceImages.put(tex,imageData);
                                }
                            }
                        }
                        Object factor=params.get("baseColorFactor");
                        if(factor!=null){
                            // A baseColorFactor without a texture is an actual
                            // sRGB material color, not a tint of the old neutral
                            // 0x7f8084 placeholder. This also preserves licensed
                            // Kenney texture palette tinting when one IS present.
                            if(faceTexture==null)rgb=0xffffff;
                            List<Object> color=list(factor);
                            if(color.size()>=3){
                                for(int channel=0;channel<3;channel++)
                                    factorTint[channel]=number(color.get(channel));
                                rgb=(Math.max(0,Math.min(255,(int)(((rgb>>>16)&255)*number(color.get(0)))))<<16)|
                                    (Math.max(0,Math.min(255,(int)(((rgb>>>8)&255)*number(color.get(1)))))<<8)|
                                    Math.max(0,Math.min(255,(int)((rgb&255)*number(color.get(2)))));
                            }
                        }
                    }
                    if(rgb==0x7f8084){
                        // Authoritative material association remains present. Neutral tint by material name
                        // distinguishes low-poly surfaces until image textures are wired.
                        String n=String.valueOf(material.getOrDefault("name",""));
                        int hash=n.hashCode();
                        rgb=0x686868|((hash&7)<<16)|(((hash>>>4)&7)<<8)|((hash>>>8)&7);
                    }
                }
                float[][] uv=null;
                if(faceTexture!=null&&attr.containsKey("TEXCOORD_0")){
                    Map<String,Object> texCoords=obj(get(accessors,num(attr.get("TEXCOORD_0"))));
                    if(val(texCoords,"componentType",0)==5126&&"VEC2".equals(texCoords.get("type"))
                        &&val(texCoords,"count",0)==count){
                        uv=new float[count][2];
                        int uvOffset=offset(texCoords,views,bin.length,8);
                        int uvStride=stride(texCoords,views,8);
                        for(int vertex=0;vertex<count;vertex++){
                            uv[vertex][0]=data.getFloat(uvOffset+vertex*uvStride);
                            uv[vertex][1]=data.getFloat(uvOffset+vertex*uvStride+4);
                        }
                    }
                }
                for(int i=0;i+2<indices.length;i+=3){
                    int a=indices[i],b=indices[i+1],c=indices[i+2];
                    if(a<0||b<0||c<0||a>=count||b>=count||c>=count)
                        throw new IOException("Invalid mesh index");
                    float[] v=vertices[a],w=vertices[b],z=vertices[c];
                    int faceColor=rgb;
                    if(uv!=null){
                        java.awt.image.BufferedImage texture=sourceImages.get(faceTexture);
                        double u=(uv[a][0]+uv[b][0]+uv[c][0])/3.0;
                        double vv=(uv[a][1]+uv[b][1]+uv[c][1])/3.0;
                        if(Double.isFinite(u)&&Double.isFinite(vv)){
                            u=u-Math.floor(u);vv=vv-Math.floor(vv);
                            int px=Math.min(texture.getWidth()-1,(int)(u*texture.getWidth()));
                            int py=Math.min(texture.getHeight()-1,
                                (int)((1-vv)*texture.getHeight()));
                            int texel=texture.getRGB(px,py);
                            if((texel>>>24)>=32){
                                int rr=(int)Math.max(0,Math.min(255,((texel>>>16)&255)*factorTint[0]));
                                int gg=(int)Math.max(0,Math.min(255,((texel>>>8)&255)*factorTint[1]));
                                int bb=(int)Math.max(0,Math.min(255,(texel&255)*factorTint[2]));
                                faceColor=(rr<<16)|(gg<<8)|bb;
                            }
                        }
                    }
                    Triangle tri=new Triangle(v[0],v[1],v[2],w[0],w[1],w[2],
                        z[0],z[1],z[2],faceColor);
                    triangles.add(tri);
                    int tint=(Math.max(0,Math.min(255,Math.round(factorTint[0]*255)))<<16)|
                        (Math.max(0,Math.min(255,Math.round(factorTint[1]*255)))<<8)|
                        Math.max(0,Math.min(255,Math.round(factorTint[2]*255)));
                    surfaces.add(new Surface(tri,uv==null?null:faceTexture,
                        uv==null?0:uv[a][0],uv==null?0:uv[a][1],
                        uv==null?0:uv[b][0],uv==null?0:uv[b][1],
                        uv==null?0:uv[c][0],uv==null?0:uv[c][1],tint));
                }
            }
        }
        if(triangles.isEmpty()||triangles.size()>500_000)throw new IOException("GLB has no usable triangles");
        return new GlbMesh(triangles,bounds,surfaces);
    }
    /** Source-pixel average fallback when UVs are absent; normal triangles sample actual UV texels. */
    private static int samplePalette(byte[] raw)throws IOException{
        java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(new ByteArrayInputStream(raw));
        if(image==null||image.getWidth()>4096||image.getHeight()>4096)throw new IOException("Bad source texture");
        long r=0,g=0,b=0,total=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int rgb=image.getRGB(x,y),alpha=rgb>>>24;
            r+=((rgb>>>16)&255)*(long)alpha;
            g+=((rgb>>>8)&255)*(long)alpha;
            b+=(rgb&255)*(long)alpha;
            total+=alpha;
        }
        if(total==0)return 0x7f8084;
        return ((int)(r/total)<<16)|((int)(g/total)<<8)|(int)(b/total);
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
    static final class Parser {
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
