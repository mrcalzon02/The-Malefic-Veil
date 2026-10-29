package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import veil.WorldDayNight;

/** Full-screen procedural retro sky gradient, sun/moon glow and sparse stars.
 * Uses a single two-triangle GPU pass and no texture uploads per frame. */
public final class GpuSkybox implements AutoCloseable {
    private static final String VERTEX="""
        #version 330 core
        in vec2 a_position;
        out vec2 v_uv;
        void main() {
            v_uv=a_position*0.5+0.5;
            gl_Position=vec4(a_position,0.999,1.0);
        }
        """;
    private static final String FRAGMENT="""
        #version 330 core
        in vec2 v_uv;
        uniform vec3 u_zenith;
        uniform vec3 u_horizon;
        uniform float u_daylight;
        uniform float u_hour;
        out vec4 fragColor;
        float hash2(vec2 p){
            return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);
        }
        void main(){
            float altitude=clamp((v_uv.y-0.23)/0.77,0.0,1.0);
            vec3 sky=mix(u_horizon,u_zenith,smoothstep(0.0,1.0,altitude));
            float angle=(u_hour-6.0)/24.0*6.2831853;
            vec2 starPoint=vec2(.5+cos(angle)*.42,.30+sin(angle)*.50);
            float sunlight=exp(-length((v_uv-starPoint)*vec2(1.6,1.0))*28.0);
            sky+=vec3(1.0,0.66,0.40)*sunlight*u_daylight*.65;
            vec2 moon=vec2(.5-cos(angle)*.40,.45-sin(angle)*.38);
            float moonGlow=exp(-length((v_uv-moon)*vec2(1.9,1.2))*65.0);
            sky+=vec3(.35,.47,.72)*moonGlow*(1.0-u_daylight)*.35;
            vec2 starCell=floor(v_uv*vec2(164.0,85.0));
            float star=step(.997,hash2(starCell));
            sky+=vec3(.39,.45,.59)*star*(1.0-u_daylight)*step(.37,v_uv.y);
            fragColor=vec4(clamp(sky,0.0,1.0),1.0);
        }
        """;
    private final Mesh quad;
    private final ShaderProgram shader;
    public GpuSkybox(){
        shader=new ShaderProgram(VERTEX,FRAGMENT);
        if(!shader.isCompiled())throw new IllegalStateException("PSX sky shader: "+shader.getLog());
        quad=new Mesh(true,6,0,new VertexAttribute(VertexAttributes.Usage.Position,2,"a_position"));
        quad.setVertices(new float[]{-1,-1,1,-1,1,1,-1,-1,1,1,-1,1});
    }
    public void render(WorldDayNight.Conditions time){
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
        shader.bind();
        shader.setUniformf("u_zenith",time.skyRed(),time.skyGreen(),time.skyBlue());
        shader.setUniformf("u_horizon",time.hazeRed(),time.hazeGreen(),time.hazeBlue());
        shader.setUniformf("u_daylight",(float)time.daylight());
        shader.setUniformf("u_hour",(float)time.hour());
        quad.render(shader,GL20.GL_TRIANGLES);
        Gdx.gl.glDepthMask(true);
    }
    @Override public void close(){quad.dispose();shader.dispose();}
}
