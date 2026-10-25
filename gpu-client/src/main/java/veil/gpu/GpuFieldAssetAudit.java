package veil.gpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.*;
import veil.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Actual GL30 instanced-renderer evidence for staged GLBs; not a gameplay or art certificate. */
public final class GpuFieldAssetAudit extends ApplicationAdapter {
    private static final int W=640,H=480,BACKGROUND=0x14191fff;
    private final Path source,output;
    private final String id;
    private AnimatedGlb rig;
    private GpuKenneyPreview uploader;
    private ShaderProgram shader;
    private FrameBuffer framebuffer;
    private PerspectiveCamera camera;
    private int frame;
    private int[] rest;
    private final List<String> evidence=new ArrayList<>();
    private final String[] clips={"ACTIVE","PACKED","DEPLOYING","OPERATING"};
    private final double[] times={0,0,.75,.20};
    private GpuFieldAssetAudit(Path source,Path output){
        this.source=source.toAbsolutePath();this.output=output.toAbsolutePath();
        id=source.getFileName().toString().replaceFirst("\\.glb$","");
        if(!id.matches("[a-z0-9-]+"))throw new IllegalArgumentException("Invalid audit model name");
    }
    private static String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
    private static String digest(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    @Override public void create(){
        try{
            if(Gdx.gl30==null)throw new IllegalStateException("GL30 unavailable");
            Files.createDirectories(output);rig=new AnimatedGlb(Files.readAllBytes(source),"audit-"+id);
            if(!rig.hasFieldCoordinateContract())throw new IllegalStateException("Missing canonical field coordinate contract");
            uploader=new GpuKenneyPreview(20261024L,false);
            shader=new ShaderProgram(GpuKenneyPreview.VERTEX_SHADER,GpuKenneyPreview.FRAGMENT_SHADER);
            if(!shader.isCompiled())throw new IllegalStateException(shader.getLog());
            framebuffer=new FrameBuffer(Pixmap.Format.RGBA8888,W,H,true);
            camera=new PerspectiveCamera(42,W,H);camera.position.set(1.55f,1.15f,-2.15f);camera.lookAt(0,.48f,0);camera.near=.02f;camera.far=20;camera.update();
        }catch(Exception error){throw new IllegalStateException("GPU candidate audit setup failed",error);}
    }
    @Override public void render(){
        if(frame>=clips.length){Gdx.app.exit();return;}
        try{
            GlbMesh pose=rig.pose(clips[frame],times[frame],0,0);uploader.uploadAuditPose(pose);
            framebuffer.begin();Gdx.gl.glViewport(0,0,W,H);Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);Gdx.gl.glDisable(GL20.GL_CULL_FACE);
            Gdx.gl.glClearColor(20/255f,25/255f,31/255f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
            shader.bind();shader.setUniformMatrix("u_projView",camera.combined);shader.setUniformf("u_eye",camera.position);
            shader.setUniformf("u_fogFar",20);shader.setUniformf("u_ambient",1);shader.setUniformf("u_lampStrength",0);
            shader.setUniformf("u_dayTint",1,1,1);shader.setUniformf("u_haze",.08f,.10f,.12f);shader.setUniformi("u_lightCount",0);
            shader.setUniform4fv("u_pointLights[0]",new float[32],0,32);uploader.drawAuditPose(shader);Gdx.gl.glFinish();
            int glError=Gdx.gl.glGetError();if(glError!=GL20.GL_NO_ERROR)throw new IllegalStateException("GL error "+glError);
            Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,W,H),flipped=new Pixmap(W,H,Pixmap.Format.RGBA8888);
            int[] values=new int[W*H];Set<Integer> colors=new HashSet<>();int visible=0,changed=0;
            for(int y=0;y<H;y++)for(int x=0;x<W;x++){
                int pixel=pixels.getPixel(x,y),index=y*W+x;values[index]=pixel;flipped.drawPixel(x,H-1-y,pixel);
                if(pixel!=BACKGROUND){visible++;colors.add(pixel);}if(rest!=null&&pixel!=rest[index])changed++;
            }
            if(visible<1500||colors.size()<12)throw new IllegalStateException("Blank/untextured candidate: "+visible+" pixels, "+colors.size()+" colors");
            if(frame==0)rest=values;
            if(frame==1&&changed<500)throw new IllegalStateException("Packed geometry did not visibly change");
            if(frame==3&&changed<5)throw new IllegalStateException("Operating pose did not visibly change");
            Path image=output.resolve(clips[frame].toLowerCase(Locale.ROOT)+".png");PixmapIO.writePNG(new FileHandle(image.toFile()),flipped);
            pixels.dispose();flipped.dispose();framebuffer.end();
            evidence.add("{\"clip\":"+quote(clips[frame])+",\"time_s\":"+times[frame]+",\"visible_pixels\":"+visible+",\"colors\":"+colors.size()+",\"changed_from_rest\":"+changed+",\"sha256\":"+quote(digest(Files.readAllBytes(image)))+"}");
            frame++;
            if(frame==clips.length){
                String report="{\"id\":"+quote(id)+",\"asset_sha256\":"+quote(digest(Files.readAllBytes(source)))+",\"status\":\"PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER\",\"renderer\":"+quote(Gdx.gl.glGetString(GL20.GL_RENDERER))+",\"evidence\":["+String.join(",",evidence)+"],\"scope\":\"Production upload path and shaders with actual candidate TRS/morph poses; not gameplay or artistic acceptance\"}\n";
                Files.writeString(output.resolve("gpu-audit.json"),report);System.out.println("PASS GPU_FIELD_ASSET_AUDIT "+id);Gdx.app.exit();
            }
        }catch(Exception error){throw new IllegalStateException("GPU candidate audit failed",error);}
    }
    @Override public void dispose(){
        if(uploader!=null)uploader.dispose();if(shader!=null)shader.dispose();if(framebuffer!=null)framebuffer.dispose();
    }
    public static void main(String[] args){
        if(args.length!=2)throw new IllegalArgumentException("Expected GLB path and evidence directory");
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();config.setTitle("Malefic Veil asset audit");config.setWindowedMode(W,H);config.setInitialVisible(false);config.setForegroundFPS(30);
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,3);
        new Lwjgl3Application(new GpuFieldAssetAudit(Path.of(args[0]),Path.of(args[1])),config);
    }
}
