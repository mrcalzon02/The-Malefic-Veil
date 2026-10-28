package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import veil.ContinentVista;
import veil.VeilOcean;
import veil.WorldDayNight;
import dev.mrcalzon.water.WaterEffects;
import dev.mrcalzon.water.WaterModule;
import dev.mrcalzon.water.WaterTextures;
import java.util.*;

/**
 * Static GPU geometry for the independent Beckenburg water model. No
 * per-frame Java wave sampling, vertex rewriting, or VBO uploads. The vertex
 * shader executes the same two-phase coherent-noise wave formula and the
 * fragment shader samples the SDK's authentic dual pixelated ripple sheets.
 * Biome validation and coastal bathymetry are computed once at world load.
 * This is visual-only; collision and weather authority stay with the host.
 */
public final class GpuOcean implements AutoCloseable {
    private static final String VERTEX="""
        #version 330 core
        in vec3 a_position;
        in vec2 a_texCoord0;
        uniform mat4 u_projView;
        uniform vec3 u_eye;
        uniform float u_time;
        uniform float u_amplitude;
        uniform float u_waveSpeed;
        uniform float u_noiseScale;
        uniform float u_shoreAmplitude;
        uniform float u_shorePeriod;
        uniform float u_shoreWavelength;
        uniform float u_shoreSlope;
        out vec3 v_world;
        out float v_depth;
        out float v_distance;
        float waveNoiseHash(ivec2 p){
            uint h=uint(p.x)*374761393u+uint(p.y)*668265263u+1274126177u;
            h=(h^(h>>13u))*1274126177u;
            return float((h^(h>>16u))&16777215u)/16777215.0;
        }
        float coherentNoise(vec2 p){
            ivec2 cell=ivec2(floor(p));
            vec2 f=fract(p);
            f=f*f*(3.0-2.0*f);
            return mix(mix(waveNoiseHash(cell),waveNoiseHash(cell+ivec2(1,0)),f.x),
                       mix(waveNoiseHash(cell+ivec2(0,1)),waveNoiseHash(cell+ivec2(1,1)),f.x),f.y);
        }
        void main(){
            vec2 pos=a_position.xz;
            float n=coherentNoise(pos*u_noiseScale);
            float secondary=coherentNoise(pos*.14);
            float rolling=.72*sin((pos.x+pos.y)*.2+u_time*u_waveSpeed+n*10.0);
            float chop=.28*sin(pos.x*1.1+pos.y*.75-u_time*u_waveSpeed*1.3+secondary*3.0);
            float shore=u_shoreAmplitude*
                sin(6.28318530718*(a_texCoord0.x/u_shoreSlope/u_shoreWavelength-u_time/u_shorePeriod))*
                exp(-a_texCoord0.x/.75);
            v_world=vec3(pos.x,(rolling+chop)*u_amplitude+shore,pos.y);
            v_depth=a_texCoord0.x;
            v_distance=distance(u_eye,v_world);
            gl_Position=u_projView*vec4(v_world,1.0);
        }
        """;
    private static final String FRAGMENT="""
        #version 330 core
        in vec3 v_world;
        in float v_depth;
        in float v_distance;
        uniform sampler2D u_layerA;
        uniform sampler2D u_layerB;
        uniform vec4 u_layerInfoA;
        uniform vec4 u_layerInfoB;
        uniform float u_pixelsA;
        uniform float u_pixelsB;
        uniform float u_time;
        uniform vec3 u_eye;
        uniform vec3 u_shallow;
        uniform vec3 u_deep;
        uniform vec3 u_reflection;
        uniform vec3 u_haze;
        uniform vec3 u_sun;
        uniform float u_distance;
        uniform float u_storm;
        uniform float u_absorption;
        uniform float u_minOpacity;
        uniform float u_maxOpacity;
        uniform float u_blend;
        uniform float u_detail;
        uniform float u_fresnel;
        uniform float u_foamDepth;
        uniform float u_foamOpacity;
        uniform float u_shorePeriod;
        uniform float u_shoreWavelength;
        uniform float u_shoreSlope;
        out vec4 fragColor;
        float waveNoiseHash(ivec2 p){
            uint h=uint(p.x)*374761393u+uint(p.y)*668265263u+1274126177u;
            h=(h^(h>>13u))*1274126177u;
            return float((h^(h>>16u))&16777215u)/16777215.0;
        }
        float coherentNoise(vec2 p){
            ivec2 cell=ivec2(floor(p));
            vec2 f=fract(p); f=f*f*(3.0-2.0*f);
            return mix(mix(waveNoiseHash(cell),waveNoiseHash(cell+ivec2(1,0)),f.x),
                       mix(waveNoiseHash(cell+ivec2(0,1)),waveNoiseHash(cell+ivec2(1,1)),f.x),f.y);
        }
        void main(){
            vec2 uvA=v_world.xz*u_layerInfoA.xy+u_time*u_layerInfoA.zw;
            vec2 uvB=v_world.xz*u_layerInfoB.xy+u_time*u_layerInfoB.zw;
            float layerA=texture(u_layerA,floor(uvA*u_pixelsA)/u_pixelsA).r;
            float layerB=texture(u_layerB,floor(uvB*u_pixelsB)/u_pixelsB).r;
            float textureTone=mix(layerA,layerB,u_blend);
            float attenuation=1.0-exp(-u_absorption*v_depth);
            float opacity=mix(u_minOpacity,u_maxOpacity,attenuation);
            vec3 tint=mix(u_shallow,u_deep,attenuation);
            tint*=1.0+(textureTone*2.0-1.0)*u_detail;
            float breakup=max(0.0,(coherentNoise(v_world.xz*3.0+
                vec2(u_time*.08,-u_time*.11))-.3)/.7);
            float crest=pow(max(0.0,sin(6.28318530718*
                (v_depth/u_shoreSlope/u_shoreWavelength-u_time/u_shorePeriod))),3.0);
            float foam=clamp(1.0-v_depth/u_foamDepth,0.0,1.0)*
                breakup*(.25+.75*crest)*u_foamOpacity;
            float grazing=1.0-abs(normalize(u_eye-v_world).y);
            float reflection=u_fresnel*(.02+.98*pow(clamp(grazing,0.0,1.0),5.0));
            tint=mix(tint,u_reflection,reflection);
            opacity+= (1.0-opacity)*reflection;
            tint=mix(tint,vec3(.725,.784,.769),foam);
            opacity+= (1.0-opacity)*foam*.8;
            vec3 lit=tint*mix(vec3(.56),u_sun,.44)*u_storm;
            float fog=smoothstep(u_distance*.45,u_distance,v_distance);
            fragColor=vec4(mix(lit,u_haze,fog*.8),clamp(opacity,0.0,1.0));
        }
        """;
    private static final int STEP=2, STRIDE=5;
    private record Chunk(Mesh mesh,int vertexCount,double midX,double midZ,double width){}
    /** CPU submission time is NOT GPU wall time; record geometry and upload
     * counts separately from the whole-frame diagnostics in the viewport. */
    public record Stats(int visibleChunks,int drawnTriangles,int drawCalls,
                        long vertexUploadBytes,long cpuSubmitNanos,
                        long allocatedStaticBytes){}
    private final ShaderProgram shader;
    private final VeilOcean water;
    private final Texture layerA,layerB;
    private final List<Chunk> chunks=new ArrayList<>();
    private long staticBytes;
    private Stats last=new Stats(0,0,0,0,0,0);

    private static Texture upload(WaterModule.Texture source){
        Pixmap image=new Pixmap(source.width(),source.height(),Pixmap.Format.RGBA8888);
        int[] pixels=source.rgb();
        for(int z=0;z<source.height();z++)
            for(int x=0;x<source.width();x++)
                image.drawPixel(x,z,(pixels[z*source.width()+x]<<8)|0xff);
        Texture result=new Texture(image);
        image.dispose();
        result.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        result.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
        return result;
    }
    private static void vertex(ArrayList<Float> out,double x,double z,VeilOcean water){
        out.add((float)x);
        out.add(0f);
        out.add((float)z);
        out.add((float)Math.max(0.0,VeilOcean.SEA_LEVEL-water.bottom(x,z)));
        out.add(0f);
    }
    public GpuOcean(VeilOcean water,ContinentVista vista){
        this.water=Objects.requireNonNull(water);
        Objects.requireNonNull(vista);
        shader=new ShaderProgram(VERTEX,FRAGMENT);
        if(!shader.isCompiled())throw new IllegalStateException("Ocean shader: "+shader.getLog());
        layerA=upload(WaterTextures.builtin("water://ripple-a"));
        layerB=upload(WaterTextures.builtin("water://ripple-b"));
        var seen=new HashSet<Long>();
        for(ContinentVista.Patch patch:vista.patches()){
            var vertices=new ArrayList<Float>();
            for(int z=patch.top();z<patch.top()+patch.depth();z+=STEP)
                for(int x=patch.left();x<patch.left()+patch.width();x+=STEP){
                    // Source-biome interiors must all be wet. This bounds
                    // water quads to real parcels, including irregular coasts.
                    if(!water.wet(x+.02,z+.02)||
                       !water.wet(x+STEP-.02,z+.02)||
                       !water.wet(x+.02,z+STEP-.02)||
                       !water.wet(x+STEP-.02,z+STEP-.02))continue;
                    long cell=(((long)x)<<32)^(z&0xffffffffL);
                    if(!seen.add(cell))continue;
                    vertex(vertices,x,z,water);
                    vertex(vertices,x+STEP,z,water);
                    vertex(vertices,x+STEP,z+STEP,water);
                    vertex(vertices,x,z,water);
                    vertex(vertices,x+STEP,z+STEP,water);
                    vertex(vertices,x,z+STEP,water);
                }
            if(vertices.isEmpty())continue;
            float[] packed=new float[vertices.size()];
            for(int i=0;i<packed.length;i++)packed[i]=vertices.get(i);
            int count=packed.length/STRIDE;
            Mesh mesh=new Mesh(Mesh.VertexDataType.VertexBufferObjectWithVAO,true,
                count,0,VertexAttribute.Position(),VertexAttribute.TexCoords(0));
            mesh.setVertices(packed); // Exactly once at world load.
            staticBytes+=(long)packed.length*Float.BYTES;
            chunks.add(new Chunk(mesh,count,
                patch.left()+patch.width()*.5,patch.top()+patch.depth()*.5,
                Math.max(patch.width(),patch.depth())));
        }
        last=new Stats(0,0,0,0,0,staticBytes);
        System.out.println("OCEAN_WATER_SDK_READY chunks="+chunks.size()+
            " staticBytes="+staticBytes+
            " source=seeded-coastline waves=vertex-shader color=dual-texture-gpu");
    }
    private static void tint(ShaderProgram shader,String name,int color){
        shader.setUniformf(name,((color>>>16)&255)/255f,
            ((color>>>8)&255)/255f,(color&255)/255f);
    }
    private static void layer(ShaderProgram shader,String name,WaterEffects.Layer l){
        shader.setUniformf(name,(float)l.scaleX(),(float)l.scaleZ(),
            (float)l.scrollX(),(float)l.scrollZ());
    }
    public int render(PerspectiveCamera camera,WorldDayNight.Conditions day,
                      double seconds,String preset,float distance){
        long start=System.nanoTime();
        WaterEffects effects=water.effects(preset);
        WaterModule.Style style=water.module().bodies().get(0).style();
        shader.bind();
        shader.setUniformMatrix("u_projView",camera.combined);
        shader.setUniformf("u_eye",camera.position);
        shader.setUniformf("u_haze",day.hazeRed(),day.hazeGreen(),day.hazeBlue());
        shader.setUniformf("u_sun",day.sunRed(),day.sunGreen(),day.sunBlue());
        shader.setUniformf("u_distance",distance);
        boolean storm="THUNDERSTORM".equals(preset)||"HEAVY_RAIN".equals(preset);
        shader.setUniformf("u_storm",storm?.68f:1f);
        shader.setUniformf("u_time",(float)seconds);
        shader.setUniformf("u_amplitude",(float)effects.amplitude());
        shader.setUniformf("u_waveSpeed",(float)effects.speed());
        shader.setUniformf("u_noiseScale",(float)effects.noiseScale());
        shader.setUniformf("u_shoreAmplitude",(float)effects.shoreline().amplitude());
        shader.setUniformf("u_shorePeriod",(float)effects.shoreline().period());
        shader.setUniformf("u_shoreWavelength",(float)effects.shoreline().wavelength());
        shader.setUniformf("u_shoreSlope",(float)effects.shoreline().slope());
        shader.setUniformf("u_absorption",(float)style.absorption());
        shader.setUniformf("u_minOpacity",(float)style.minimumOpacity());
        shader.setUniformf("u_maxOpacity",(float)style.maximumOpacity());
        shader.setUniformf("u_blend",(float)effects.blend());
        shader.setUniformf("u_detail",(float)effects.detail());
        shader.setUniformf("u_fresnel",(float)effects.fresnel());
        shader.setUniformf("u_foamDepth",(float)effects.foamDepth());
        shader.setUniformf("u_foamOpacity",(float)effects.foamOpacity());
        tint(shader,"u_shallow",style.shallow());
        tint(shader,"u_deep",style.deep());
        tint(shader,"u_reflection",effects.reflection());
        layer(shader,"u_layerInfoA",effects.first());
        layer(shader,"u_layerInfoB",effects.second());
        shader.setUniformf("u_pixelsA",effects.first().pixels());
        shader.setUniformf("u_pixelsB",effects.second().pixels());
        shader.setUniformi("u_layerA",0);
        shader.setUniformi("u_layerB",1);
        layerA.bind(0);
        layerB.bind(1);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDepthMask(false);
        int drawn=0,triangles=0;
        try{
            for(Chunk chunk:chunks){
                double dx=camera.position.x-chunk.midX,dz=camera.position.z-chunk.midZ;
                double limit=distance+chunk.width;
                if(dx*dx+dz*dz>limit*limit)continue;
                chunk.mesh.render(shader,GL20.GL_TRIANGLES,0,chunk.vertexCount);
                drawn++;triangles+=chunk.vertexCount/3;
            }
        }finally{
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glDisable(GL20.GL_BLEND);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        }
        last=new Stats(drawn,triangles,drawn,0,System.nanoTime()-start,staticBytes);
        return drawn;
    }
    public Stats stats(){return last;}
    public int visibleChunks(){return last.visibleChunks();}
    public int visibleTriangles(){return last.drawnTriangles();}
    @Override public void close(){
        for(Chunk chunk:chunks)chunk.mesh.dispose();
        chunks.clear();
        layerA.dispose();layerB.dispose();shader.dispose();
    }
}
