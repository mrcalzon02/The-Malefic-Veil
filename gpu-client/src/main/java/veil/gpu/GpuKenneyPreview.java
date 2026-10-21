package veil.gpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import veil.*;

import java.util.*;

/**
 * Hardware-instanced PSX resolution rendering from the REAL, seed-generated
 * Kenney city, not a second world generator or decorative cube substitutes.
 *
 * This is a GPU environment/walkthrough preview, NOT a replacement for the
 * networked gameplay client until character/HUD input and replication are ported.
 */
public final class GpuKenneyPreview extends ApplicationAdapter {
    static final int WIDTH=320, HEIGHT=200;
    static final int MAX_VISIBLE_MODELS=10000;
    static final String VERTEX_SHADER="""
        #version 330 core
        in vec3 a_position;
        in vec4 a_color;
        in vec4 a_instance;
        in vec2 a_extra;
        uniform mat4 u_projView;
        uniform vec3 u_eye;
        out vec4 v_color;
        out float v_distance;
        void main() {
            float cs=cos(a_instance.z),sn=sin(a_instance.z);
            vec3 scaled=a_position*a_instance.w;
            vec3 world=vec3(
                a_instance.x+scaled.x*cs-scaled.z*sn,
                scaled.y+a_extra.y,
                a_instance.y+scaled.x*sn+scaled.z*cs);
            gl_Position=u_projView*vec4(world,1.0);
            v_color=vec4(a_color.rgb,a_color.a*a_extra.x);
            v_distance=length(world-u_eye);
        }
        """;
    static final String FRAGMENT_SHADER="""
        #version 330 core
        in vec4 v_color;
        in float v_distance;
        uniform float u_fogFar;
        out vec4 fragColor;
        void main() {
            float fog=smoothstep(u_fogFar*0.32,u_fogFar,v_distance);
            vec3 shaded=v_color.rgb*max(0.35,1.0-v_distance/(u_fogFar*1.5));
            vec3 fogColor=vec3(0.055,0.061,0.080);
            fragColor=vec4(mix(shaded,fogColor,fog),v_color.a);
        }
        """;

    private static final class GpuModel {
        final Mesh mesh;
        final int vertexCount;
        final int capacity;
        GpuModel(Mesh mesh,int vertexCount,int capacity){
            this.mesh=mesh;this.vertexCount=vertexCount;this.capacity=capacity;
        }
    }
    private static final class FloatList {
        float[] values=new float[96];
        int size;
        void append(UrbanScene.ModelInstance instance){
            if(size+6>values.length)values=Arrays.copyOf(values,values.length*2);
            values[size++]=(float)instance.x();
            values[size++]=(float)instance.y();
            values[size++]=(float)instance.heading();
            values[size++]=(float)instance.scale();
            values[size++]=(float)instance.opacity();
            values[size++]=(float)instance.heightOffset();
        }
    }

    private final long seed;
    private final boolean smoke;
    private World world;
    private UrbanScene scene;
    private ShaderProgram shader;
    private PerspectiveCamera camera;
    private FrameBuffer framebuffer;
    private SpriteBatch sprites;
    private BitmapFont font;
    private final Map<String,GpuModel> models=new LinkedHashMap<>();
    private final Map<String,FloatList> visible=new LinkedHashMap<>();
    private double x=1.5,y=1.5,heading=0.2;
    private float visualDistance=30f;
    private int frames,visibleInstances,drawCalls;
    private boolean shownFailure;

    public GpuKenneyPreview(long seed,boolean smoke){
        this.seed=seed;this.smoke=smoke;
    }

    @Override public void create() {
        if(Gdx.gl30==null)throw new IllegalStateException("GPU client requires OpenGL 3.3 instancing");
        world=new World(seed);
        try{scene=new UrbanScene(world);}
        catch(Exception ex){throw new IllegalStateException("Kenney source pack unavailable",ex);}
        shader=new ShaderProgram(VERTEX_SHADER,FRAGMENT_SHADER);
        if(!shader.isCompiled())throw new IllegalStateException("PSX GPU shader failed: "+shader.getLog());
        for(UrbanInstanceBatches.Batch batch:scene.batches().all()){
            GlbMesh source=batch.mesh();
            int count=source.triangleCount()*3;
            float[] vertexData=new float[count*7];
            int cursor=0;
            for(GlbMesh.Triangle triangle:source.triangles()){
                int rgb=triangle.rgb();
                float r=((rgb>>>16)&255)/255f,g=((rgb>>>8)&255)/255f,b=(rgb&255)/255f;
                float[] coords={triangle.ax(),triangle.ay(),triangle.az(),
                    triangle.bx(),triangle.by(),triangle.bz(),
                    triangle.cx(),triangle.cy(),triangle.cz()};
                for(int offset=0;offset<9;offset+=3){
                    vertexData[cursor++]=coords[offset];
                    vertexData[cursor++]=coords[offset+1];
                    vertexData[cursor++]=coords[offset+2];
                    vertexData[cursor++]=r;vertexData[cursor++]=g;
                    vertexData[cursor++]=b;vertexData[cursor++]=1;
                }
            }
            Mesh mesh=new Mesh(Mesh.VertexDataType.VertexBufferObjectWithVAO,true,count,0,
                VertexAttribute.Position(),VertexAttribute.ColorUnpacked());
            mesh.setVertices(vertexData);
            mesh.enableInstancedRendering(false,batch.count(),
                new VertexAttribute(VertexAttributes.Usage.Generic,4,"a_instance"),
                new VertexAttribute(VertexAttributes.Usage.Generic,2,"a_extra"));
            models.put(batch.modelId(),new GpuModel(mesh,count,batch.count()));
        }
        camera=new PerspectiveCamera(72,WIDTH,HEIGHT);
        camera.near=0.08f;camera.far=140f;
        framebuffer=new FrameBuffer(Pixmap.Format.RGBA8888,WIDTH,HEIGHT,true);
        sprites=new SpriteBatch();
        font=new BitmapFont();
        font.getData().setScale(0.62f);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        System.out.println("GPU_KENNEY_READY seed="+seed+" placedTiles="+
            scene.instances().size()+" sourceMeshes="+models.size()+" resolution="+WIDTH+"x"+HEIGHT);
    }

    private void control() {
        float dt=Math.min(.04f,Math.max(0,Gdx.graphics.getDeltaTime()));
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT))heading-=dt*1.8;
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT))heading+=dt*1.8;
        double forward=0,strafe=0;
        if(Gdx.input.isKeyPressed(Input.Keys.W)||Gdx.input.isKeyPressed(Input.Keys.UP))forward+=1;
        if(Gdx.input.isKeyPressed(Input.Keys.S)||Gdx.input.isKeyPressed(Input.Keys.DOWN))forward-=1;
        if(Gdx.input.isKeyPressed(Input.Keys.D))strafe+=1;
        if(Gdx.input.isKeyPressed(Input.Keys.A))strafe-=1;
        double length=Math.hypot(forward,strafe);
        if(length>0){
            forward/=length;strafe/=length;
            double velocity=(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)?6.8:3.5)*dt;
            double vx=(forward*Math.cos(heading)-strafe*Math.sin(heading))*velocity;
            double vy=(forward*Math.sin(heading)+strafe*Math.cos(heading))*velocity;
            if(world.canWalk(x+vx,y))x+=vx;
            if(world.canWalk(x,y+vy))y+=vy;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.MINUS))visualDistance=Math.max(8,visualDistance-4);
        if(Gdx.input.isKeyJustPressed(Input.Keys.EQUALS))visualDistance=Math.min(80,visualDistance+4);
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))Gdx.app.exit();
    }

    private void drawWorld() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glClearColor(.055f,.061f,.080f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        camera.position.set((float)x,1.55f,(float)y);
        camera.direction.set((float)Math.cos(heading),-.04f,(float)Math.sin(heading)).nor();
        camera.up.set(Vector3.Y);
        camera.far=visualDistance+12;
        camera.update();

        visible.clear();
        visibleInstances=0;drawCalls=0;
        for(UrbanScene.ModelInstance placement:scene.nearby(x,y,visualDistance)){
            double dx=placement.x()-x,dy=placement.y()-y;
            double distance2=dx*dx+dy*dy;
            if(distance2>9 && dx*Math.cos(heading)+dy*Math.sin(heading)<-2)continue;
            if(visibleInstances>=MAX_VISIBLE_MODELS)break;
            if(!models.containsKey(placement.model()))continue;
            visible.computeIfAbsent(placement.model(),ignore->new FloatList()).append(placement);
            visibleInstances++;
        }
        shader.bind();
        shader.setUniformMatrix("u_projView",camera.combined);
        shader.setUniformf("u_eye",camera.position);
        shader.setUniformf("u_fogFar",visualDistance);
        for(var entry:visible.entrySet()){
            GpuModel gpu=models.get(entry.getKey());
            FloatList transforms=entry.getValue();
            if(gpu==null||transforms.size==0)continue;
            if(transforms.size/6>gpu.capacity)
                throw new IllegalStateException("Instancing upload exceeds capacity for "+entry.getKey());
            gpu.mesh.setInstanceData(transforms.values,0,transforms.size);
            // Exactly ONE GPU glDrawArraysInstanced per visible Kenney mesh type.
            gpu.mesh.render(shader,GL20.GL_TRIANGLES,0,gpu.vertexCount);
            drawCalls++;
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        sprites.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,WIDTH,HEIGHT));
        sprites.begin();
        font.draw(sprites,"THE MALEFIC VEIL - KENNEY GPU",5,195);
        font.draw(sprites,"INST "+visibleInstances+"  DRAWS "+drawCalls+"  LOD "+(int)visualDistance,5,184);
        font.draw(sprites,"WASD MOVE  ARROWS TURN  +/- VIEW  ESC EXIT",5,10);
        sprites.end();
    }

    private void saveFrame() {
        // Read back the *actual* GPU framebuffer, correcting GL's bottom-left origin.
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,WIDTH,HEIGHT);
        Pixmap flipped=new Pixmap(WIDTH,HEIGHT,Pixmap.Format.RGBA8888);
        for(int row=0;row<HEIGHT;row++)for(int col=0;col<WIDTH;col++)
            flipped.drawPixel(col,HEIGHT-1-row,pixels.getPixel(col,row));
        FileHandle destination=Gdx.files.absolute(
            new java.io.File("build/test-visuals/gpu-kenney-city.png").getAbsolutePath());
        destination.parent().mkdirs();
        PixmapIO.writePNG(destination,flipped);
        Set<Integer> colors=new HashSet<>();
        for(int yy=24;yy<170;yy+=4)for(int xx=8;xx<WIDTH-8;xx+=4)
            colors.add(flipped.getPixel(xx,yy));
        pixels.dispose();flipped.dispose();
        if(colors.size()<10||visibleInstances<5||drawCalls<3)
            throw new IllegalStateException("GPU smoke frame lacks actual Kenney geometry: colors="+
                colors.size()+" instances="+visibleInstances+" calls="+drawCalls);
        System.out.println("PASS GPU_KENNEY_SMOKE screenshot="+destination.path()+
            " visibleInstances="+visibleInstances+" instancedDrawCalls="+drawCalls+
            " variedColors="+colors.size());
    }

    @Override public void render() {
        if(!smoke)control();
        framebuffer.begin();
        drawWorld();
        if(smoke&&++frames==12){
            saveFrame();
            framebuffer.end();
            Gdx.app.exit();
            return;
        }
        framebuffer.end();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sprites.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        sprites.begin();
        sprites.draw(framebuffer.getColorBufferTexture(),0,0,
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),0,1,1,0);
        sprites.end();
    }

    @Override public void dispose() {
        for(GpuModel model:models.values())model.mesh.dispose();
        models.clear();
        if(shader!=null)shader.dispose();
        if(framebuffer!=null)framebuffer.dispose();
        if(sprites!=null)sprites.dispose();
        if(font!=null)font.dispose();
    }
}