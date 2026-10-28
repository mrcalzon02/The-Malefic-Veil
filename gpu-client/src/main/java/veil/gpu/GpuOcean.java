package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import veil.ContinentVista;
import veil.VeilOcean;
import veil.WorldDayNight;
import java.util.*;

/**
 * Native OpenGL presentation of the SAME independent water SDK used for
 * continental sea selection and the weather-responsive triangular wave field.
 * Read-only and render-only: actors, shore collision and resources stay host
 * authoritative. Per-chunk VBOs are reused instead of making GL objects
 * each frame. Transparent water is depth-tested against the opaque world.
 */
public final class GpuOcean implements AutoCloseable {
    private static final String VERTEX="""
        #version 330 core
        in vec3 a_position;
        in vec4 a_color;
        uniform mat4 u_projView;
        uniform vec3 u_eye;
        out vec4 v_color;
        out float v_distance;
        void main(){
            gl_Position=u_projView*vec4(a_position,1.0);
            v_color=a_color;
            v_distance=distance(u_eye,a_position);
        }
        """;
    private static final String FRAGMENT="""
        #version 330 core
        in vec4 v_color;
        in float v_distance;
        uniform vec3 u_haze;
        uniform vec3 u_sun;
        uniform float u_distance;
        uniform float u_storm;
        out vec4 fragColor;
        void main(){
            float fog=smoothstep(u_distance*.45,u_distance,v_distance);
            vec3 lit=v_color.rgb*mix(vec3(.56),u_sun,.44)*u_storm;
            fragColor=vec4(mix(lit,u_haze,fog*.8),v_color.a);
        }
        """;
    private static final int STEP=4,STRIDE=7;
    private record Chunk(Mesh mesh,double[] points,float[] vertices,
                         int vertexCount,double midX,double midZ,double width){}
    private final ShaderProgram shader;
    private final VeilOcean water;
    private final List<Chunk> chunks=new ArrayList<>();
    private int visible,triangles;

    public GpuOcean(VeilOcean water,ContinentVista vista){
        this.water=Objects.requireNonNull(water);
        Objects.requireNonNull(vista);
        shader=new ShaderProgram(VERTEX,FRAGMENT);
        if(!shader.isCompiled())throw new IllegalStateException("Ocean shader: "+shader.getLog());
        var seen=new HashSet<Long>();
        for(ContinentVista.Patch patch:vista.patches()){
            // Some generated city foundation patches share the terrain list.
            // The source ocean mask alone determines eligible surface cells.
            ArrayList<Double> coordinates=new ArrayList<>();
            for(int z=patch.top();z<patch.top()+patch.depth();z+=STEP)
                for(int x=patch.left();x<patch.left()+patch.width();x+=STEP){
                    if(!water.wet(x+STEP*.5,z+STEP*.5))continue;
                    long cell=(((long)x)<<32)^(z&0xffffffffL);
                    if(!seen.add(cell))continue;
                    double[][] corners={{x,z},{x+STEP,z},{x+STEP,z+STEP},
                        {x,z},{x+STEP,z+STEP},{x,z+STEP}};
                    for(double[] point:corners){
                        coordinates.add(point[0]);coordinates.add(point[1]);
                    }
                }
            if(coordinates.isEmpty())continue;
            double[] points=new double[coordinates.size()];
            for(int i=0;i<points.length;i++)points[i]=coordinates.get(i);
            int count=points.length/2;
            Mesh mesh=new Mesh(Mesh.VertexDataType.VertexBufferObjectWithVAO,false,
                count,0,VertexAttribute.Position(),VertexAttribute.ColorUnpacked());
            chunks.add(new Chunk(mesh,points,new float[count*STRIDE],count,
                patch.left()+patch.width()*.5,patch.top()+patch.depth()*.5,
                Math.max(patch.width(),patch.depth())));
        }
        System.out.println("OCEAN_WATER_SDK_READY chunks="+chunks.size()+
            " source=seeded-coastline animation=shared-water-module");
    }
    public int render(PerspectiveCamera camera,WorldDayNight.Conditions time,
                      double seconds,String weather,float distance){
        shader.bind();
        shader.setUniformMatrix("u_projView",camera.combined);
        shader.setUniformf("u_eye",camera.position);
        shader.setUniformf("u_haze",time.hazeRed(),time.hazeGreen(),time.hazeBlue());
        shader.setUniformf("u_sun",time.sunRed(),time.sunGreen(),time.sunBlue());
        shader.setUniformf("u_distance",distance);
        boolean storm="THUNDERSTORM".equals(weather)||"HEAVY_RAIN".equals(weather);
        shader.setUniformf("u_storm",storm?.68f:1f);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDepthMask(false);
        visible=0;triangles=0;
        var waves=water.effects(weather);
        for(Chunk chunk:chunks){
            double dx=camera.position.x-chunk.midX,dz=camera.position.z-chunk.midZ;
            double radius=distance+chunk.width;
            if(dx*dx+dz*dz>radius*radius)continue;
            for(int i=0;i<chunk.vertexCount;i++){
                double x=chunk.points[i*2],z=chunk.points[i*2+1];
                int c=water.color(x,z,seconds,weather);
                int v=i*STRIDE;
                chunk.vertices[v]=(float)x;
                chunk.vertices[v+1]=(float)waves.surface(x,z,seconds).offset();
                chunk.vertices[v+2]=(float)z;
                chunk.vertices[v+3]=((c>>>16)&255)/255f;
                chunk.vertices[v+4]=((c>>>8)&255)/255f;
                chunk.vertices[v+5]=(c&255)/255f;
                chunk.vertices[v+6]=((c>>>24)&255)/255f;
            }
            chunk.mesh.setVertices(chunk.vertices);
            chunk.mesh.render(shader,GL20.GL_TRIANGLES,0,chunk.vertexCount);
            visible++;triangles+=chunk.vertexCount/3;
        }
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        return visible;
    }
    public int visibleChunks(){return visible;}
    public int visibleTriangles(){return triangles;}
    @Override public void close(){
        for(Chunk chunk:chunks)chunk.mesh.dispose();
        chunks.clear();shader.dispose();
    }
}
