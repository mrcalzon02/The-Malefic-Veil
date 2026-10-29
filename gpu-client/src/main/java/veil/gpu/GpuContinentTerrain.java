package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import veil.ContinentVista;
import veil.WorldDayNight;
import java.util.*;

/**
 * Source-seeded, GPU-rendered continental terrain patches beyond the city.
 * The 132x132 playable world still has authoritative border collision until
 * server terrain sectors/streamed actor simulation are implemented.
 */
public final class GpuContinentTerrain implements AutoCloseable {
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
        uniform vec3 u_dayColor;
        uniform float u_ambient;
        uniform float u_distance;
        out vec4 fragColor;
        void main(){
            float fog=smoothstep(u_distance*.45,u_distance,v_distance);
            vec3 lit=v_color.rgb*(u_dayColor*u_ambient);
            fragColor=vec4(mix(lit,u_haze,fog),1.0);
        }
        """;
    private record Buffer(Mesh mesh,int vertexCount,double midX,double midZ,double size){}
    private final ShaderProgram shader;
    private final List<Buffer> geometry=new ArrayList<>();
    private int lastPatches,lastTriangles;
    public GpuContinentTerrain(ContinentVista vista){
        Objects.requireNonNull(vista);
        shader=new ShaderProgram(VERTEX,FRAGMENT);
        if(!shader.isCompiled())throw new IllegalStateException("Continent shader: "+shader.getLog());
        for(ContinentVista.Patch patch:vista.patches()){
            Mesh mesh=new Mesh(Mesh.VertexDataType.VertexBufferObjectWithVAO,true,
                patch.triangles()*3,0,VertexAttribute.Position(),VertexAttribute.ColorUnpacked());
            mesh.setVertices(patch.copyVertices());
            geometry.add(new Buffer(mesh,patch.triangles()*3,
                patch.left()+patch.width()/2.0,patch.top()+patch.depth()/2.0,patch.width()));
        }
        if(geometry.isEmpty())throw new IllegalStateException("Continent lacks terrain patches");
        System.out.println("CONTINENT_VISTA_READY patches="+geometry.size()+
            " source=ContinentLayout projection=City_A visibleTerrain=render_only");
    }
    public int render(PerspectiveCamera camera,WorldDayNight.Conditions time,float distance){
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        shader.bind();
        shader.setUniformMatrix("u_projView",camera.combined);
        shader.setUniformf("u_eye",camera.position);
        shader.setUniformf("u_haze",time.hazeRed(),time.hazeGreen(),time.hazeBlue());
        shader.setUniformf("u_dayColor",time.sunRed(),time.sunGreen(),time.sunBlue());
        shader.setUniformf("u_ambient",(float)time.ambient());
        shader.setUniformf("u_distance",distance);
        lastPatches=0;lastTriangles=0;
        for(Buffer data:geometry){
            double dx=data.midX-camera.position.x,dz=data.midZ-camera.position.z;
            double far=distance+data.size;
            if(dx*dx+dz*dz>far*far)continue;
            // No triangle work for remote terrain even though source patches are cached.
            data.mesh.render(shader,GL20.GL_TRIANGLES,0,data.vertexCount);
            lastPatches++;
            lastTriangles+=data.vertexCount/3;
        }
        return lastPatches;
    }
    public int visibleTriangles(){return lastTriangles;}
    public int visiblePatches(){return lastPatches;}
    @Override public void close(){
        for(Buffer buffer:geometry)buffer.mesh.dispose();
        geometry.clear();
        shader.dispose();
    }
}
