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
import org.beckenburg.editor.EditorHistory;
import java.awt.event.KeyEvent;

/**
 * Hardware-instanced PSX resolution rendering from the REAL, seed-generated
 * Kenney city, not a second world generator or decorative cube substitutes.
 *
 * Gallery/smoke remain deterministic captures, while the production path
 * receives authoritative snapshots from MaleficVeil's original game loop.
 */
public final class GpuKenneyPreview extends ApplicationAdapter {
    static final int WIDTH=640, HEIGHT=400;
    static final int MAX_VISIBLE_MODELS=45000; // No circular cutoff through the 30k+ authored city
    static final String VERTEX_SHADER="""
        #version 330 core
        in vec3 a_position;
        in vec4 a_color;
        in vec2 a_texcoord;
        in vec4 a_instance;
        in vec2 a_extra;
        uniform mat4 u_projView;
        uniform vec3 u_eye;
        out vec4 v_color;
        out float v_distance;
        out vec3 v_world;
        out vec2 v_texcoord;
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
            v_world=world;
            v_texcoord=a_texcoord;
        }
        """;
    static final String FRAGMENT_SHADER="""
        #version 330 core
        in vec4 v_color;
        in float v_distance;
        in vec3 v_world;
        in vec2 v_texcoord;
        uniform sampler2D u_diffuse;
        uniform int u_textured;
        uniform float u_fogFar;
        uniform float u_ambient;
        uniform float u_lampStrength;
        uniform int u_lightCount;
        uniform vec4 u_pointLights[8];
        uniform vec3 u_haze;
        uniform vec3 u_dayTint;
        out vec4 fragColor;
        void main() {
            float warm=0.0;
            for(int i=0;i<8;i++){
                if(i>=u_lightCount)break;
                vec4 lamp=u_pointLights[i];
                float distance=length(v_world.xz-lamp.xy);
                float falloff=max(0.0,1.0-distance/lamp.z);
                warm+=falloff*falloff*lamp.w*u_lampStrength;
            }
            // Ambient daylight and real sourced streetlight pools: no fake
            // emissive cubes, and at most eight lights tested per fragment.
            vec3 brightness=u_dayTint*u_ambient+vec3(1.00,0.73,0.46)*warm;
            vec3 sourceColor=v_color.rgb;
            if(u_textured!=0)sourceColor*=texture(u_diffuse,v_texcoord).rgb;
            vec3 shaded=sourceColor*min(brightness,vec3(1.65));
            float fog=smoothstep(u_fogFar*.35,u_fogFar,v_distance);
            fragColor=vec4(mix(shaded,u_haze,fog),v_color.a);
        }
        """;

    private static final class GpuModel {
        final Mesh mesh;
        final int vertexCount;
        final int capacity;
        final Texture texture;
        GpuModel(Mesh mesh,int vertexCount,int capacity,Texture texture){
            this.mesh=mesh;this.vertexCount=vertexCount;this.capacity=capacity;
            this.texture=texture;
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
    private final boolean gallery;
    private final char focusCity;
    private final MaleficVeil gameplay;
    private final boolean playSmoke;
    private final boolean editorMode,editorSmoke;
    private MaleficEditorProject editorProject;
    private MaleficEditorDock editorDock;
    private EditorHistory<MaleficEditorProject.Snapshot> editorTestHistory;
    private String smokeObject;
    private final Set<Integer> liveHeld=new HashSet<>();
    private long firstSmokeFrames;
    private int smokeStartingAmmo=-1;
    private GalleryView[] galleryViews;
    private int galleryIndex;
    private int visibleTerrainPatches;
    private int visibleTerrainTriangles;
    private long oceanBenchmarkPreviousNs;
    private final java.util.List<Double> oceanBenchmarkFrameMs=new java.util.ArrayList<>();
    private final java.util.List<Double> oceanBaselineFrameMs=new java.util.ArrayList<>();
    private int[] oceanBaselinePixels;
    private World world;
    private UrbanScene scene;
    private ShaderProgram shader;
    private PerspectiveCamera camera;
    private FrameBuffer framebuffer;
    private int renderWidth=WIDTH;
    private SpriteBatch sprites;
    private BitmapFont font;
    private GpuSkybox skybox;
    private GpuContinentTerrain terrain;
    private GpuOcean ocean;
    private Texture weatherPixel;
    private double oceanSeconds;
    private Texture cityMapTexture,continentMapTexture;
    private UrbanLighting lighting;
    private PsxHorrorMenu menu;
    private final Map<String,List<GpuModel>> models=new LinkedHashMap<>();
    private final Map<String,Texture> materialTextures=new HashMap<>();
    private final Map<String,FloatList> visible=new LinkedHashMap<>();
    private double x=1.5,y=1.5,heading=0.2;
    private float visualDistance=145f;
    private int frames,visibleInstances,drawCalls;
    private int activeLights,maxLights=8,timeSpeed=1;
    private long worldTimeMs=WorldDayNight.DAY_MS/2;
    private final float[] lightUniforms=new float[8*4];
    private WorldDayNight.Conditions environment=WorldDayNight.atHour(12);
    private boolean shownFailure;
    private boolean showGpuPerf;
    private boolean menuBackdrop;
    private Runnable launcherOverlay;
    /** Draw launcher text and city in one native framebuffer and orientation. */
    public void launcherOverlay(Runnable overlay){
        if(!menuBackdrop)throw new IllegalStateException("Launcher callback needs backdrop mode");
        launcherOverlay=overlay;
    }
    private GpuClientSettings clientSettings=GpuClientSettings.load();
    private GpuAudioBus audioBus;
    private double audioLastStepX=Double.NaN,audioLastStepZ=Double.NaN,audioStepDistance;
    private GpuOverlayMenu gameplayMenu;
    private Runnable onReturnMain;
    public void audioBus(GpuAudioBus bus){this.audioBus=bus;}
    public void onReturnMain(Runnable callback){this.onReturnMain=callback;}
    public void auditPause(boolean paused){
        if(gameplayMenu==null)throw new IllegalStateException("No live gameplay overlay");
        gameplayMenu.auditPause(paused);
    }
    public void auditPanel(String id){
        if(gameplayMenu==null)throw new IllegalStateException("Not in live gameplay");
        gameplayMenu.auditPanel(id);
    }
    public com.badlogic.gdx.InputProcessor worldInputProcessor(){
        return gameplayMenu==null?null:gameplayMenu.inputProcessor();
    }
    private float menuTime;

    private double cameraHeight=1.65, cameraPitch=-.04;
    private ContinentVista vista;
    public GpuKenneyPreview(long seed,boolean smoke){this(seed,smoke,false);}
    public GpuKenneyPreview(long seed,boolean smoke,boolean gallery){
        this(seed,smoke,gallery,'A');
    }
    public GpuKenneyPreview(long seed,boolean smoke,boolean gallery,char site){
        this(seed,smoke,gallery,site,null,false,false,false);
    }
    public GpuKenneyPreview(MaleficVeil gameplay,boolean playSmoke){
        this(gameplay.gpuWorld().seed,false,false,'A',gameplay,playSmoke,false,false);
    }
    /** Same licensed GPU scene, with project-independent editor library attached. */
    public static GpuKenneyPreview editableWorld(long seed,boolean smoke){
        return new GpuKenneyPreview(seed,false,false,'A',null,false,true,smoke);
    }
    public static GpuKenneyPreview menuBackdrop(long seed){
        GpuKenneyPreview scene=new GpuKenneyPreview(seed,false,false,'A');
        scene.menuBackdrop=true;return scene;
    }
    public void clientSettings(GpuClientSettings settings){
        this.clientSettings=Objects.requireNonNull(settings);
        visualDistance=settings.distance;
        maxLights=settings.lights;
        if(camera!=null)camera.fieldOfView=settings.fieldOfView;
    }
    private GpuKenneyPreview(long seed,boolean smoke,boolean gallery,char site,
            MaleficVeil gameplay,boolean playSmoke,boolean editorMode,boolean editorSmoke){
        if(site<'A'||site>'G')throw new IllegalArgumentException("Unknown city "+site);
        this.seed=seed;this.smoke=smoke;this.gallery=gallery;focusCity=site;
        this.gameplay=gameplay;this.playSmoke=playSmoke;
        this.editorMode=editorMode;this.editorSmoke=editorSmoke;
    }
    /** Deterministic photography poses: no fabricated scenery or composited backgrounds. */
    private record GalleryView(String name,double x,double z,double elevation,
        double heading,double pitch,float distance,double hour){}
    private static final GalleryView[] GALLERY={
        new GalleryView("01-city-street",61.5,61.5,1.55,1.10,-.05f,48,12),
        new GalleryView("02-central-streets",61.5,61.5,3.1,0.67,-.15f,78,16.5),
        new GalleryView("03-urban-rooftops",66,66,43,0.45,-.81f,100,18.2),
        // Aerial distance must exceed camera altitude or the actual world
        // vanishes into fog even though its geometry is correctly loaded.
        new GalleryView("04-city-overhead",66,66,115,0.30,-1.42f,230,12),
        new GalleryView("05-city-woodland-edge",125,66,38,0.12,-.64f,100,10),
        new GalleryView("06-urban-night",61.5,61.5,40,0.42,-.80f,145,0),
        new GalleryView("07-outside-toward-city",-22,66,2.1,0,-.10f,165,12),
        new GalleryView("08-outside-woodland",-22,66,2.1,Math.PI,-.08f,165,12),
        new GalleryView("09-southern-foundation",66,-12,2.6,Math.PI/2,-.36f,150,10),
        new GalleryView("10-east-embankment",World.SIZE+14,66,4.0,Math.PI,-.48f,150,16)
    };
    /** Generated-city audit cameras point at the real ContinentLayout cell mask. */
    private GalleryView[] generatedCityViews(){
        ContinentLayout.City city=world.continent().cities().get(focusCity-'A');
        double cx=vista.cityCenterX(focusCity),cz=vista.cityCenterZ(focusCity);
        double west=cx-city.radiusX()*World.PARCEL_METERS-23;
        double north=cz-city.radiusY()*World.PARCEL_METERS-23;
        double platform=vista.cityPlates().plateHeight(focusCity);
        return new GalleryView[]{
            new GalleryView("01-generated-plate-overhead",cx,cz,platform+150,
                0,-1.48f,260,12),
            new GalleryView("02-generated-west-edge",west,cz,
                5.0,0,-.32f,240,12),
            new GalleryView("03-generated-north-edge",cx,north,
                5.0,Math.PI/2,-.32f,240,16),
            new GalleryView("04-generated-diagonal",west,north,
                27.0,Math.PI/4,-.64f,260,10)
        };
    }
    private void setGalleryView(GalleryView view){
        x=view.x();y=view.z();cameraHeight=view.elevation();heading=view.heading();
        if(focusCity!='A'||view.name().contains("outside")||
           view.name().contains("foundation")||view.name().contains("embankment"))
            cameraHeight+=vista.sample(x,y).elevation();
        if(view.name().equals("02-central-streets")){
            for(CityLayout.Road road:world.city.roadTiles()){
                if(Integer.bitCount(road.connections())<3)continue;
                double cx=road.x()*CityLayout.CELL+3.5;
                double cz=road.y()*CityLayout.CELL+3.5;
                if(Math.hypot(cx-61.5,cz-61.5)<25||!world.canWalk(cx,cz))continue;
                x=cx;y=cz;break;
            }
        }
        cameraPitch=view.pitch();visualDistance=view.distance();
        worldTimeMs=(long)(WorldDayNight.DAY_MS*view.hour()/24.0);
        menu.showRunning();
        System.out.println("GALLERY_POSE seed="+seed+" view="+view.name()+" xyz="+
            x+","+cameraHeight+","+y+" angle="+heading+" pitch="+cameraPitch+
            " hour="+view.hour());
    }


    /** One real uploaded GPU mesh per original GLB material group. */
    private void uploadGeometry(String id,GlbMesh source,int initialCapacity){
        // Uploaded meshes also support editor-authored overlay instances.
        int capacity=Math.max(2048,initialCapacity+2048);

            Map<String,List<GlbMesh.Surface>> groups=new LinkedHashMap<>();
            for(GlbMesh.Surface surface:source.surfaces())
                groups.computeIfAbsent(surface.texture(),k->new ArrayList<>()).add(surface);
            List<GpuModel> geometry=new ArrayList<>();
            for(var group:groups.entrySet()){
                int count=group.getValue().size()*3;
                float[] vertexData=new float[count*9];
                int cursor=0;
                for(GlbMesh.Surface surface:group.getValue()){
                    GlbMesh.Triangle tri=surface.triangle();
                    int rgb=surface.texture()==null?tri.rgb():surface.tintRgb();
                    float red=((rgb>>>16)&255)/255f;
                    float green=((rgb>>>8)&255)/255f;
                    float blue=(rgb&255)/255f;
                    float[] verts={
                        tri.ax(),tri.ay(),tri.az(),surface.au(),
                        GlbMesh.gpuAtlasV(group.getKey(),surface.av()),
                        tri.bx(),tri.by(),tri.bz(),surface.bu(),
                        GlbMesh.gpuAtlasV(group.getKey(),surface.bv()),
                        tri.cx(),tri.cy(),tri.cz(),surface.cu(),
                        GlbMesh.gpuAtlasV(group.getKey(),surface.cv())
                    };
                    for(int i=0;i<15;i+=5){
                        vertexData[cursor++]=verts[i];
                        vertexData[cursor++]=verts[i+1];
                        vertexData[cursor++]=verts[i+2];
                        vertexData[cursor++]=red;
                        vertexData[cursor++]=green;
                        vertexData[cursor++]=blue;
                        vertexData[cursor++]=1;
                        vertexData[cursor++]=verts[i+3];
                        vertexData[cursor++]=verts[i+4];
                    }
                }
                Mesh mesh=new Mesh(Mesh.VertexDataType.VertexBufferObjectWithVAO,
                    true,count,0,VertexAttribute.Position(),VertexAttribute.ColorUnpacked(),
                    new VertexAttribute(VertexAttributes.Usage.TextureCoordinates,2,"a_texcoord"));
                mesh.setVertices(vertexData);
                mesh.enableInstancedRendering(false,capacity,
                    new VertexAttribute(VertexAttributes.Usage.Generic,4,"a_instance"),
                    new VertexAttribute(VertexAttributes.Usage.Generic,2,"a_extra"));
                Texture texture=null;
                if(group.getKey()!=null){
                    texture=materialTextures.get(group.getKey());
                    if(texture==null){
                        try{
                            // Original UV atlas for GGBot; Kenney remains a separate source kit.
                            byte[] data=source.textureBytes(group.getKey());
                            if(data==null)data=KenneyAssetPack.load().texture(group.getKey());
                            Pixmap image=new Pixmap(data,0,data.length);
                            texture=new Texture(image);
                            image.dispose();
                            texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
                            texture.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
                            materialTextures.put(group.getKey(),texture);
                        }catch(Exception ex){
                            throw new IllegalStateException("Missing licensed source texture "+
                                group.getKey()+" for "+id,ex);
                        }
                    }
                }
                geometry.add(new GpuModel(mesh,count,capacity,texture));
            }
            models.put(id,List.copyOf(geometry));
    }
    @Override public void create() {
        if(Gdx.gl30==null)throw new IllegalStateException("GPU client requires OpenGL 3.3 instancing");
        world=gameplay==null?new World(seed):gameplay.gpuWorld();
        // Same seed and continent city anchors as the authoritative world
        // blueprint. Outside-city surfaces are visible, not yet walkable.
        ContinentVista continental=new ContinentVista(world,focusCity);
        vista=continental;
        try{scene=new UrbanScene(world,continental.scenery());}
        catch(Exception ex){throw new IllegalStateException("Kenney source pack unavailable",ex);}
        if(editorMode||gameplay!=null){
            try{editorProject=new MaleficEditorProject(world);}
            catch(java.io.IOException failed){
                throw new IllegalStateException("Cannot load authored scene layer",failed);
            }
        }
        terrain=new GpuContinentTerrain(continental);
        veil.VeilOcean seaModel=new veil.VeilOcean(world,continental);
        ocean=new GpuOcean(seaModel,continental);
        lighting=new UrbanLighting(world);
        shader=new ShaderProgram(VERTEX_SHADER,FRAGMENT_SHADER);
        if(!shader.isCompiled())throw new IllegalStateException("PSX GPU shader failed: "+shader.getLog());
        // Mesh geometry is uploaded only once per source-material pair. A
        // typical Kenney wall has two textures; each material keeps one actual
        // GL30 instanced draw for all its visible wall placements.
        for(UrbanInstanceBatches.Batch batch:scene.batches().all())
            uploadGeometry(batch.modelId(),batch.mesh(),batch.count());
        if(gameplay!=null){
            for(var entry:gameplay.gpuDynamicMeshes().entrySet())
                if(!models.containsKey(entry.getKey()))
                    uploadGeometry(entry.getKey(),entry.getValue(),512);
        }
        renderWidth=GpuScreenLayout.width(
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        camera=new PerspectiveCamera(72,renderWidth,HEIGHT);
        camera.near=0.08f;camera.far=300f;
        framebuffer=new FrameBuffer(Pixmap.Format.RGBA8888,renderWidth,HEIGHT,true);
        sprites=new SpriteBatch();
        Pixmap weatherDot=new Pixmap(1,1,Pixmap.Format.RGBA8888);
        weatherDot.setColor(1,1,1,1);weatherDot.fill();
        weatherPixel=new Texture(weatherDot);weatherDot.dispose();
        font=new BitmapFont();
        font.getData().setScale(1.16f);
        skybox=new GpuSkybox();
        menu=new PsxHorrorMenu(smoke||gallery||gameplay!=null||editorMode);
        font.dispose();
        font=menu.font();
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        galleryViews=focusCity=='A'?GALLERY:generatedCityViews();
        clientSettings(clientSettings);
        if(menuBackdrop){
            menu.showRunning();
            x=68;y=68;heading=.3;cameraPitch=-.42;cameraHeight=30;
            visualDistance=Math.max(145,clientSettings.distance);
        }
        if(gallery)setGalleryView(galleryViews[0]);
        if(gallery&&Boolean.getBoolean("veil.gpu.oceanBenchmark")){
            // Use an actual seeded ocean parcel directly under the camera.
            // A city-overhead pose can submit water triangles that are wholly
            // out of view, concealing a broken water shader.
            double cx=continental.cityCenterX(focusCity);
            double cz=continental.cityCenterZ(focusCity);
            var parcels=seaModel.sourceSeaParcels(cx-180,cz-180,cx+180,cz+180);
            var nearest=parcels.stream().min(java.util.Comparator.comparingDouble(parcel->
                Math.hypot(parcel.west()+parcel.size()*.5-cx,
                           parcel.north()+parcel.size()*.5-cz)))
                .orElseThrow(()->new IllegalStateException(
                    "Coastal benchmark city has no real water parcel"));
            x=nearest.west()+nearest.size()*.5;
            y=nearest.north()+nearest.size()*.5;
            cameraHeight=26;cameraPitch=-1.52;heading=.32;
            visualDistance=Math.max(180,visualDistance);
            System.out.println("OCEAN_BENCHMARK_CAMERA world="+x+","+y+
                " source=authoritative-wet-parcel");
        }
        if(gameplay!=null){
            buildGameplayMaps();
            gameplayMenu=new GpuOverlayMenu(menu,gameplay,clientSettings,audioBus,
                ()->{
                    if(onReturnMain==null)Gdx.app.exit();
                    else Gdx.app.postRunnable(onReturnMain);
                },()->clientSettings(clientSettings));
            gameplay.gpuSceneReady();
        }
        if(editorMode){
            x=61.5;y=61.5;cameraHeight=13;heading=.45;cameraPitch=-.38;
            if(editorSmoke)editorTestHistory=new EditorHistory<>(editorProject,80);
            else{
                editorDock=new MaleficEditorDock(editorProject,
                    ()->new MaleficEditorDock.View(x,y,heading));
                editorDock.open();
            }
        }
        System.out.println("GPU_KENNEY_READY seed="+seed+" placedTiles="+
            scene.instances().size()+" sourceMeshes="+models.size()+" resolution="+renderWidth+"x"+HEIGHT);
    }

    /** Expand/crop only the inexpensive offscreen color target on resize.
     * World VBOs, materials, authoritative simulation and menu hitboxes stay. */
    @Override public void resize(int width,int height){
        int next=GpuScreenLayout.width(width,height);
        if(next==renderWidth&&framebuffer!=null)return;
        renderWidth=next;
        if(camera!=null){
            camera.viewportWidth=renderWidth;
            camera.viewportHeight=HEIGHT;
            camera.update();
        }
        if(framebuffer!=null){
            framebuffer.dispose();
            framebuffer=new FrameBuffer(Pixmap.Format.RGBA8888,renderWidth,HEIGHT,true);
        }
        System.out.println("GPU_WIDESCREEN_VIEWPORT screen="+width+"x"+height+
            " frame="+renderWidth+"x"+HEIGHT+" ui=left");
    }
    public int frameWidth(){return renderWidth;}

    private static int mapColor(ContinentLayout.Biome biome){
        return switch(biome){
            case WATER -> 0x25436Bff;
            case BEACH -> 0xAA956Cff;
            case PLAINS -> 0x638C4Eff;
            case FOREST -> 0x265A3Cff;
            case HILLS -> 0x79795Bff;
            case MOUNTAINS -> 0x88898Aff;
            case MARSH -> 0x516B5Bff;
        };
    }
    /** Both maps are actual generated cell masks from the same World as combat. */
    private void buildGameplayMaps(){
        Pixmap city=new Pixmap(World.SIZE,World.SIZE,Pixmap.Format.RGBA8888);
        for(int iz=0;iz<World.SIZE;iz++)for(int ix=0;ix<World.SIZE;ix++){
            CityLayout.Category category=world.city.categoryAt(ix,iz);
            int rgba=world.city.road(ix/CityLayout.CELL,iz/CityLayout.CELL)?
                0xC4BCA5ff:switch(category){
                    case HAZARD -> 0x76515Aff;
                    case RESOURCE -> 0x9A8A56ff;
                    case SAFE -> 0x567868ff;
                    case OPEN -> 0x77776Aff;
                };
            city.drawPixel(ix,World.SIZE-1-iz,rgba);
        }
        cityMapTexture=new Texture(city);city.dispose();
        cityMapTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        Pixmap continent=new Pixmap(ContinentLayout.SIZE,ContinentLayout.SIZE,
            Pixmap.Format.RGBA8888);
        ContinentLayout map=world.continent();
        for(int iz=0;iz<ContinentLayout.SIZE;iz++)
            for(int ix=0;ix<ContinentLayout.SIZE;ix++){
                int rgba=mapColor(map.biome(ix,iz));
                if(map.cityAt(ix,iz)!='\0')rgba=0x978B78ff;
                if(map.road(ix,iz))rgba=0xC5BCACff;
                continent.drawPixel(ix,ContinentLayout.SIZE-1-iz,rgba);
            }
        continentMapTexture=new Texture(continent);continent.dispose();
        continentMapTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
    }
    private static final int[][] LIVE_BINDINGS={
        {Input.Keys.W,KeyEvent.VK_W},{Input.Keys.S,KeyEvent.VK_S},
        {Input.Keys.A,KeyEvent.VK_A},{Input.Keys.D,KeyEvent.VK_D},
        {Input.Keys.UP,KeyEvent.VK_UP},{Input.Keys.DOWN,KeyEvent.VK_DOWN},
        {Input.Keys.LEFT,KeyEvent.VK_LEFT},{Input.Keys.RIGHT,KeyEvent.VK_RIGHT},
        {Input.Keys.SHIFT_LEFT,KeyEvent.VK_SHIFT},{Input.Keys.C,KeyEvent.VK_C},
        {Input.Keys.SPACE,KeyEvent.VK_SPACE},{Input.Keys.E,KeyEvent.VK_E},
        {Input.Keys.Q,KeyEvent.VK_Q},{Input.Keys.R,KeyEvent.VK_R},
        {Input.Keys.F,KeyEvent.VK_F},{Input.Keys.T,KeyEvent.VK_T},
        {Input.Keys.V,KeyEvent.VK_V},{Input.Keys.B,KeyEvent.VK_B},
        {Input.Keys.H,KeyEvent.VK_H},{Input.Keys.G,KeyEvent.VK_G},
        {Input.Keys.L,KeyEvent.VK_L},{Input.Keys.PERIOD,KeyEvent.VK_PERIOD},
                {Input.Keys.X,KeyEvent.VK_X},
        {Input.Keys.Z,KeyEvent.VK_Z},{Input.Keys.Y,KeyEvent.VK_Y},
        {Input.Keys.I,KeyEvent.VK_I},{Input.Keys.K,KeyEvent.VK_K},
        // J/O open in-world crafting/crew menus, never legacy Swing popups.
        {Input.Keys.P,KeyEvent.VK_P},{Input.Keys.U,KeyEvent.VK_U},
        {Input.Keys.M,KeyEvent.VK_M},{Input.Keys.N,KeyEvent.VK_N},
        {Input.Keys.F9,KeyEvent.VK_F9},
        {Input.Keys.NUM_1,KeyEvent.VK_1},{Input.Keys.NUM_2,KeyEvent.VK_2},
        {Input.Keys.NUM_3,KeyEvent.VK_3},{Input.Keys.ENTER,KeyEvent.VK_ENTER}
    };
    /** Only input adapters live here: movement, shooting, skills, factions,
     * cargo, crafting and host transactions remain in MaleficVeil. */
    private void liveControls(){
        if(gameplayMenu.active()&&Gdx.input.isKeyJustPressed(Input.Keys.J))
            gameplayMenu.openCraft();
        else if(gameplayMenu.active()&&Gdx.input.isKeyJustPressed(Input.Keys.F2))
            gameplayMenu.openBook();
        else if(gameplayMenu.active()&&Gdx.input.isKeyJustPressed(Input.Keys.O))
            gameplayMenu.openCrew();
        boolean active=gameplayMenu.input();
        for(int[] binding:LIVE_BINDINGS){
            boolean pressed=active&&Gdx.input.isKeyPressed(binding[0]);
            boolean wasDown=liveHeld.contains(binding[1]);
            if(pressed!=wasDown){
                if(pressed)liveHeld.add(binding[1]);else liveHeld.remove(binding[1]);
                gameplay.gpuKey(binding[1],pressed);
            }
        }
        if(!active){
            Gdx.input.setCursorCatched(false);
            return;
        }
        if(!Gdx.input.isCursorCatched()){
            Gdx.input.setCursorCatched(true);
            Gdx.input.getDeltaX();Gdx.input.getDeltaY();
        }else{
            gameplay.gpuLook(Gdx.input.getDeltaX()*
                clientSettings.sensitivity/100.0,
                Gdx.input.getDeltaY()*clientSettings.sensitivity/100.0*
                (clientSettings.invertY?-1:1));
        }
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
            MaleficVeil.GpuFrame before=gameplay.gpuFrame();
            gameplay.gpuFire(); // Only the authoritative host determines damage/ammunition.
            if(audioBus!=null&&before!=null){
                var state=before.arsenalState();
                if(state!=null&&state.clip()[before.weapon()]<=0)audioBus.empty();
                else audioBus.shoot(before.weapon());
            }
        }
        MaleficVeil.GpuFrame frame=gameplay.gpuFrame();
        if(frame!=null){
            if(!Double.isNaN(audioLastStepX)){
                double movement=Math.hypot(frame.x()-audioLastStepX,frame.z()-audioLastStepZ);
                if(movement>3)audioStepDistance=0; // teleport/network correction
                else if(movement>0){
                    audioStepDistance+=movement;
                    if(audioStepDistance>=1.6){
                        if(audioBus!=null)audioBus.step();
                        audioStepDistance%=1.6;
                    }
                }
            }
            audioLastStepX=frame.x();audioLastStepZ=frame.z();
            x=frame.x();y=frame.z();heading=frame.yaw();
            cameraPitch=frame.pitch();cameraHeight=1.65;
            worldTimeMs=(long)(WorldDayNight.DAY_MS*frame.hour()/24.0);
        }
    }
    /** Every gameplay panel uses original heyheythere fog-theme source widgets.
     * The data still comes only from the authoritative host snapshot. */
    private void drawLiveHud(MaleficVeil.GpuFrame frame){
        menu.panel(sprites,6,326,620,68);
        var equipped=ArsenalCatalog.get(frame.weapon());
        String weaponName=equipped.name().toUpperCase(java.util.Locale.ROOT);
        var arsenal=frame.arsenalState();
        String rounds=arsenal==null?"--/--":
            arsenal.clip()[frame.weapon()]+"/"+arsenal.reserve()[frame.weapon()];
        font.draw(sprites,"HP "+frame.health()+"  SAN "+frame.sanity()+
            "  "+weaponName+" "+rounds+"  MED "+frame.medkits(),14,383);
        menu.progress(sprites,15,355,91,frame.health()/100f);
        menu.progress(sprites,119,355,91,frame.sanity()/100f);
        menu.progress(sprites,223,355,91,(float)(frame.sprintEnergy()/100));
        font.draw(sprites,"STA "+(int)frame.sprintEnergy()+"%",225,343);
        font.draw(sprites,"CASH "+frame.cash()+"  SCRAP "+frame.scrap()+
            "  KILLS "+frame.frags(),330,343);
        font.draw(sprites,gameplay.gpuFieldStatus()+"   Z SELECT  Y THROW",14,315);
        // Source cursor sprite and source tooltip/dialogue are not impostor polygons.
        menu.marker(sprites,renderWidth/2f-4,HEIGHT/2f-4);
        if(!frame.interactPrompt().isBlank()){
            menu.tooltip(sprites,renderWidth/2f-118,HEIGHT/2f-39,236,27);
            font.draw(sprites,frame.interactPrompt(),renderWidth/2f-107,HEIGHT/2f-20);
        }
        if(!frame.notice().isBlank()){
            menu.tooltip(sprites,8,HEIGHT/2f+29,Math.min(renderWidth-16,450),30);
            font.draw(sprites,frame.notice(),16,HEIGHT/2f+49);
        }
        if(!frame.connected()){
            menu.frame(sprites,178,HEIGHT/2f+13,285,39);
            font.draw(sprites,"HOST CONNECTION LOST",194,HEIGHT/2f+38);
        }
        if(frame.dead()){
            menu.frame(sprites,90,HEIGHT/2f-36,460,87);
            font.draw(sprites,"YOU HAVE FALLEN",118,HEIGHT/2f+20);
            font.draw(sprites,"1/2/3 RESPAWN  ENTER DEPLOY",115,HEIGHT/2f-7);
        }
        if(frame.inventory()){
            menu.frame(sprites,renderWidth-248,61,241,285);
            font.draw(sprites,"INVENTORY  I CLOSE",renderWidth-231,HEIGHT-77);
            menu.separator(sprites,renderWidth-229,HEIGHT-88,201);
            int row=0;
            for(var item:frame.inventoryItems().entrySet()){
                if(row>=11)break;
                float top=HEIGHT-117-row*21;
                menu.slot(sprites,renderWidth-230,top-5,20,row==0);
                font.draw(sprites,item.getKey()+" x"+item.getValue(),
                    renderWidth-205,top+10);
                row++;
            }
            if(row==0)font.draw(sprites,"EMPTY",renderWidth-226,HEIGHT-115);
        }else if(frame.skills()){
            menu.frame(sprites,renderWidth-311,69,304,277);
            font.draw(sprites,"CHARACTER / SKILLS",renderWidth-294,HEIGHT-77);
            menu.separator(sprites,renderWidth-294,HEIGHT-88,275);
            int row=0;
            for(String line:frame.skillStats()){
                if(row>=11)break;
                font.draw(sprites,line,renderWidth-294,HEIGHT-111-row*20);
                row++;
            }
        }
        if(frame.arsenalMenu()){
            menu.frame(sprites,39,33,560,316);
            var player=frame.arsenalState();
            String faction=player==null||player.faction()<0?"UNENROLLED":
                ArsenalCatalog.FACTIONS[player.faction()];
            font.draw(sprites,"ARSENAL / "+faction+" / BENCH "+(player==null?0:player.benchTier()),
                53,325);
            int first=(frame.arsenalChoice()/9)*9;
            for(int i=first;i<Math.min(32,first+9);i++){
                var weapon=ArsenalCatalog.get(i);
                boolean owned=player!=null&&(player.owned()&(1L<<i))!=0;
                String label=(i==frame.arsenalChoice()?"> ":"  ")+
                    weapon.name()+" / "+(weapon.mapSpawn()?"SCAVENGED":
                        weapon.faction()==(player==null?-1:player.faction())?
                        "T"+weapon.tier()+" OWN FACTION":"FOREIGN")+
                    (owned?" [OWNED]":"");
                font.draw(sprites,label,57,294-(i-first)*25);
            }
            var current=ArsenalCatalog.get(frame.arsenalChoice());
            font.draw(sprites,"PRIMARY "+current.primary().kind()+" / ALT "+current.alternate().kind(),
                53,63);
            font.draw(sprites,"UP/DOWN CHOOSE  ENTER MAKE  E EQUIP  U BENCH  K CLOSE",53,47);
        }
        if(frame.map()||frame.continent()){
            Texture source=frame.map()?cityMapTexture:continentMapTexture;
            if(source==null)throw new IllegalStateException("Missing authoritative GPU map");
            float left=renderWidth-285,base=52;
            menu.frame(sprites,left-7,base-12,278,305);
            font.draw(sprites,frame.map()?"CITY MAP  M CLOSE":"CONTINENT  N CLOSE",
                left+3,base+281);
            // Seed-generated map images remain gameplay data, inside a sourced
            // nine-slice frame. The position uses the original mark_you sprite.
            sprites.draw(source,left,base,255,255,0,0,
                source.getWidth(),source.getHeight(),false,true);
            float markerX=frame.map()?
                (float)(frame.x()/World.SIZE*255):
                (float)((world.parcelX(frame.x())+.5)/ContinentLayout.SIZE*255);
            float markerY=frame.map()?
                (float)((World.SIZE-frame.z())/World.SIZE*255):
                (float)((ContinentLayout.SIZE-world.parcelY(frame.z())-.5)/
                    ContinentLayout.SIZE*255);
            if(markerX>=0&&markerX<=255&&markerY>=0&&markerY<=255)
                menu.marker(sprites,left+markerX-5,base+markerY-5);
        }
        menu.band(sprites,4,5,renderWidth-8,23);
        font.draw(sprites,"WASD MOVE  AIM/FIRE  Z FIELD  Y THROW  E USE  I INV  "+
            "K ARSENAL  F8 PERF  F11 MENU  ESC",12,20);
    }
    /** Foreground sourced weapon mesh, drawn as a camera-relative 3D object. */
    private void renderLiveWeapon(MaleficVeil.GpuFrame frame){
        if(frame.dead()||!menu.running())return;
        String key=ArsenalCatalog.get(frame.weapon()).id();
        List<GpuModel> weapons=models.get(key);
        if(weapons==null)throw new IllegalStateException("Missing GPU weapon mesh "+key);
        double facing=heading;
        UrbanScene.ModelInstance held=new UrbanScene.ModelInstance(key,
            x+Math.cos(facing)*.65-Math.sin(facing)*.30,
            y+Math.sin(facing)*.65+Math.cos(facing)*.30,
            facing-Math.PI/2,.82,1,cameraHeight-.66);
        FloatList one=new FloatList();
        one.append(held);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        shader.bind();
        shader.setUniformf("u_fogFar",280f);
        for(GpuModel weapon:weapons){
            weapon.mesh.setInstanceData(one.values,0,one.size);
            shader.setUniformi("u_textured",weapon.texture==null?0:1);
            if(weapon.texture!=null)weapon.texture.bind(0);
            weapon.mesh.render(shader,GL20.GL_TRIANGLES,0,weapon.vertexCount);
        }
        shader.setUniformf("u_fogFar",visualDistance);
    }
    /** Editor models are real licensed GLBs from the shared GPU source loader. */
    private void ensureAuthoringModel(String id){
        if(models.containsKey(id))return;
        if(!editorProject.modelIds().contains(id))
            throw new IllegalStateException("Unapproved editor source model "+id);
        try{
            GlbMesh mesh;
            if(KenneyAssetPack.load().modelNames().contains(id+".glb"))
                mesh=GlbMesh.asset(id);
            else mesh=GGBotCarPack.required().model(id);
            uploadGeometry(id,mesh,2048);
        }catch(Exception failure){
            throw new IllegalStateException("Cannot upload editor model "+id,failure);
        }
    }
    /** Ctrl+click projects real authored positions through THIS OpenGL camera. */
    private void editorPick(){
        if(editorDock==null||editorProject==null)return;
        if(!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)||
           !Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))return;
        float mouseX=GpuScreenLayout.pointerX(
            Gdx.input.getX(),Gdx.graphics.getWidth(),renderWidth);
        float mouseY=GpuScreenLayout.pointerY(Gdx.input.getY(),Gdx.graphics.getHeight());
        MaleficEditorProject.Placement best=null;
        float bestPixels=22f*22f;
        for(MaleficEditorProject.Placement item:editorProject.placements()){
            Vector3 point=camera.project(new Vector3((float)item.x(),
                (float)(item.elevation()+.7),(float)item.z()),0,0,renderWidth,HEIGHT);
            if(point.z<0||point.z>1)continue;
            float dx=point.x-mouseX,dy=point.y-mouseY;
            float score=dx*dx+dy*dy;
            if(score<bestPixels){best=item;bestPixels=score;}
        }
        if(best!=null)editorDock.select(best.id());
    }
    private void editorControls(){
        control(); // Same physical OpenGL camera used for every gallery view.
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE))
            cameraHeight=Math.min(150,cameraHeight+Gdx.graphics.getDeltaTime()*12);
        if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
            cameraHeight=Math.max(.3,cameraHeight-Gdx.graphics.getDeltaTime()*12);
        if(editorDock!=null&&Gdx.input.isKeyJustPressed(Input.Keys.F11))
            editorDock.focus();
        editorPick();
    }
    private void control() {
        PsxHorrorMenu.Settings actions=new PsxHorrorMenu.Settings(){
            public void changeView(int delta){
                visualDistance=Math.max(45,Math.min(240,visualDistance+delta*3));
            }
            public void changeLights(int delta){
                maxLights=Math.max(0,Math.min(8,maxLights+delta));
            }
            public void changeTimeSpeed(int direction){
                int[] speeds={0,1,2,4,8};
                int index=1;
                for(int i=0;i<speeds.length;i++)if(speeds[i]==timeSpeed)index=i;
                timeSpeed=speeds[Math.max(0,Math.min(speeds.length-1,index+direction))];
            }
        };
        if(!menu.input(actions)){
            Gdx.input.setCursorCatched(false);
            return;
        }
        if(!Gdx.input.isCursorCatched()){
            Gdx.input.setCursorCatched(true);
            Gdx.input.getDeltaX();Gdx.input.getDeltaY();
        }else{
            heading+=Math.max(-120,Math.min(120,Gdx.input.getDeltaX()))*.0036;
            cameraPitch=Math.max(-1.52,Math.min(1.52,
                cameraPitch-Math.max(-120,Math.min(120,Gdx.input.getDeltaY()))*.0036));
        }
        float dt=Math.min(.04f,Math.max(0,Gdx.graphics.getDeltaTime()));
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT))heading-=dt*1.8;
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT))heading+=dt*1.8;
        if(Gdx.input.isKeyPressed(Input.Keys.PAGE_UP))cameraPitch=Math.min(1.52,cameraPitch+dt*1.3);
        if(Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN))cameraPitch=Math.max(-1.52,cameraPitch-dt*1.3);
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
        if(Gdx.input.isKeyJustPressed(Input.Keys.MINUS))actions.changeView(-4);
        if(Gdx.input.isKeyJustPressed(Input.Keys.EQUALS))actions.changeView(4);
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET))
            worldTimeMs=Math.floorMod(worldTimeMs-WorldDayNight.DAY_MS/24,WorldDayNight.DAY_MS);
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET))
            worldTimeMs=(worldTimeMs+WorldDayNight.DAY_MS/24)%WorldDayNight.DAY_MS;
        worldTimeMs=(worldTimeMs+(long)(Gdx.graphics.getDeltaTime()*1000*timeSpeed))%WorldDayNight.DAY_MS;
    }

    private void drawWorld() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        if(gameplay!=null){
            MaleficVeil.GpuFrame f=gameplay.gpuFrame();
            if(f!=null){
                x=f.x();y=f.z();heading=f.yaw();cameraPitch=f.pitch();
                cameraHeight=1.65;
                worldTimeMs=(long)(WorldDayNight.DAY_MS*f.hour()/24.0);
            }
        }
        environment=WorldDayNight.atElapsedMs(worldTimeMs);
        Gdx.gl.glClearColor(environment.hazeRed(),environment.hazeGreen(),environment.hazeBlue(),1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        skybox.render(environment);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        camera.position.set((float)x,(float)cameraHeight,(float)y);
        double plane=Math.cos(cameraPitch);
        camera.direction.set((float)(Math.cos(heading)*plane),
            (float)Math.sin(cameraPitch),(float)(Math.sin(heading)*plane)).nor();
        camera.up.set(Vector3.Y);
        // At elevated inspection cameras the ground is farther away in 3D
        // than the horizontal view radius. The old far plane clipped entire
        // rooftops and produced near-empty fake-looking aerial screenshots.
        camera.far=(float)Math.max(visualDistance+12,
            Math.hypot(cameraHeight,visualDistance)+28);
        camera.update();
        int visibleTerrain=terrain.render(camera,environment,visualDistance+10);
        visibleTerrainPatches=visibleTerrain;
        visibleTerrainTriangles=terrain.visibleTriangles();

        visible.clear();
        visibleInstances=0;drawCalls=0;
        for(UrbanScene.ModelInstance placement:scene.nearby(x,y,visualDistance)){
            double dx=placement.x()-x,dy=placement.y()-y;
            double distance2=dx*dx+dy*dy;
            // Horizontal front/back rejection only applies to nearly level
            // ground cameras. It discarded HALF of the city when a pitched
            // overhead camera looked down at the streets behind its XY heading.
            boolean levelView=Math.abs(cameraPitch)<.55 && cameraHeight<20;
            if(levelView&&distance2>9 &&
                dx*Math.cos(heading)+dy*Math.sin(heading)<-2)continue;
            if(visibleInstances>=MAX_VISIBLE_MODELS)break;
            if(!models.containsKey(placement.model()))continue;
            visible.computeIfAbsent(placement.model(),ignore->new FloatList()).append(placement);
            visibleInstances++;
        }
        if(editorProject!=null)
            for(MaleficEditorProject.Placement item:editorProject.placements()){
                double dx=item.x()-x,dz=item.z()-y;
                if(dx*dx+dz*dz>visualDistance*visualDistance)continue;
                ensureAuthoringModel(item.model());
                visible.computeIfAbsent(item.model(),unused->new FloatList())
                    .append(item.instance());
                visibleInstances++;
            }
        if(gameplay!=null){
            MaleficVeil.GpuFrame frame=gameplay.gpuFrame();
            if(frame!=null)for(UrbanScene.ModelInstance entity:frame.dynamic()){
                if(Math.hypot(entity.x()-x,entity.y()-y)>visualDistance)continue;
                if(!models.containsKey(entity.model()))
                    throw new IllegalStateException("Missing live GPU actor model "+entity.model());
                visible.computeIfAbsent(entity.model(),unused->new FloatList()).append(entity);
            }
        }
        shader.bind();
        shader.setUniformMatrix("u_projView",camera.combined);
        shader.setUniformf("u_eye",camera.position);
        shader.setUniformf("u_fogFar",visualDistance);
        shader.setUniformf("u_ambient",(float)environment.ambient());
        shader.setUniformf("u_lampStrength",(float)environment.lampStrength());
        shader.setUniformf("u_dayTint",environment.sunRed(),environment.sunGreen(),environment.sunBlue());
        shader.setUniformf("u_haze",environment.hazeRed(),environment.hazeGreen(),environment.hazeBlue());
        java.util.Arrays.fill(lightUniforms,0);
        var nearest=lighting.nearby(x,y,maxLights);
        activeLights=nearest.size();
        for(int i=0;i<nearest.size();i++){
            UrbanLighting.Light light=nearest.get(i);
            lightUniforms[4*i]=(float)light.x();
            lightUniforms[4*i+1]=(float)light.z();
            lightUniforms[4*i+2]=(float)light.radius();
            lightUniforms[4*i+3]=(float)light.intensity();
        }
        shader.setUniformi("u_lightCount",activeLights);
        shader.setUniform4fv("u_pointLights[0]",lightUniforms,0,lightUniforms.length);
        for(var entry:visible.entrySet()){
            List<GpuModel> geometry=models.get(entry.getKey());
            FloatList transforms=entry.getValue();
            if(geometry==null||transforms.size==0)continue;
            for(GpuModel gpu:geometry){
                if(transforms.size/6>gpu.capacity)
                    throw new IllegalStateException("Instance upload exceeds capacity "+entry.getKey());
                gpu.mesh.setInstanceData(transforms.values,0,transforms.size);
                shader.bind();
                shader.setUniformi("u_textured",gpu.texture==null?0:1);
                shader.setUniformi("u_diffuse",0);
                if(gpu.texture!=null)gpu.texture.bind(0);
                gpu.mesh.render(shader,GL20.GL_TRIANGLES,0,gpu.vertexCount);
                drawCalls++;
            }
        }
        String weatherPreset=gameplay!=null&&gameplay.gpuFrame()!=null?
            gameplay.gpuFrame().weatherPreset():"CLEAR";
        oceanSeconds+=Math.max(0,Math.min(.05,Gdx.graphics.getDeltaTime()));
        // Paired diagnostic uses the SAME city, camera and seeded world;
        // stage one measures the scene without ocean draws, stage two with.
        // Neither mode changes host physics or climate.
        boolean baselineOceanOff=gallery&&
            Boolean.getBoolean("veil.gpu.oceanBenchmark")&&frames<180;
        if(!baselineOceanOff)
            ocean.render(camera,environment,oceanSeconds,weatherPreset,visualDistance+10);
        // These counters are measured in the actual OpenGL viewport; CPU
        // submission time is explicitly NOT presented as GPU execution time.
        if((smoke||gallery||playSmoke||editorSmoke)&&frames%15==0){
            var sea=ocean.stats();
            if(sea.vertexUploadBytes()!=0)
                throw new IllegalStateException("Ocean re-uploaded animated vertices every frame");
            System.out.println("OCEAN_GPU_PERF frame="+frames+
                " chunks="+sea.visibleChunks()+" triangles="+sea.drawnTriangles()+
                " calls="+sea.drawCalls()+" staticBytes="+sea.allocatedStaticBytes()+
                " uploadedFrameBytes="+sea.vertexUploadBytes()+
                " cpuSubmitMs="+String.format(java.util.Locale.ROOT,"%.3f",
                    sea.cpuSubmitNanos()/1_000_000.0)+
                " fps="+Gdx.graphics.getFramesPerSecond());
        }
        if(gameplay!=null){
            MaleficVeil.GpuFrame live=gameplay.gpuFrame();
            if(live!=null)renderLiveWeapon(live);
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        sprites.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,renderWidth,HEIGHT));
        sprites.begin();
        if(gameplay!=null&&gameplay.gpuFrame()!=null)
            renderGpuWeather(sprites,gameplay.gpuFrame());
        if(menuBackdrop){
            // Foreground launcher paints its own source widgets; this viewport
            // is purely the live seeded world, never a fake static wallpaper.
        }else if(menu.running()&&editorMode){
            menu.panel(sprites,5,351,480,43);
            font.draw(sprites,"LEVEL EDITOR KIT / CITY A  F11 DOCK  CTRL+CLICK PICK",14,381);
            font.draw(sprites,"SEED "+seed+"  LAYER "+
                editorProject.placements().size()+"  MESHES "+visibleInstances,14,364);
            menu.band(sprites,4,5,renderWidth-8,23);
            font.draw(sprites,"WASD FLY  MOUSE LOOK  SPACE UP  CTRL DOWN  F11 PALETTE",12,20);
        }else if(menu.running()&&gameplay!=null){
            // While configuring/crafting, avoid drawing a second HUD, map,
            // toast and controls underneath the real in-game menu.
            if(gameplayMenu!=null&&!gameplayMenu.active())
                gameplayMenu.draw(sprites);
            else{
                MaleficVeil.GpuFrame live=gameplay.gpuFrame();
                if(live!=null)drawLiveHud(live);
            }
        }else if(menu.running()){
            font.draw(sprites,"THE MALEFIC VEIL  "+WorldDayNight.clock(environment.hour()),9,391);
            font.draw(sprites,"TILES "+visibleInstances+"  LAND "+visibleTerrain+"  DRAWS "+drawCalls+
                "  LAMPS "+activeLights+"  VIEW "+(int)visualDistance,9,367);
            font.draw(sprites,"WASD MOVE  MOUSE LOOK  [ ] TIME  F8 PERF  F10 GRAPHICS  ESC MENU",9,20);
        }else{
            menu.draw(sprites,font,visualDistance,maxLights,timeSpeed);
        }
        if(showGpuPerf){
            var sea=ocean.stats();
            menu.band(sprites,3,HEIGHT-59,342,54);
            font.draw(sprites,"F8 PERFORMANCE  FPS "+Gdx.graphics.getFramesPerSecond()+
                "  CITY DRAW "+drawCalls,10,HEIGHT-13);
            font.draw(sprites,"LAND "+visibleTerrainPatches+" PATCHES / "+
                visibleTerrainTriangles+" TRIANGLES  OBJECTS "+visibleInstances,
                10,HEIGHT-27);
            font.draw(sprites,"WATER "+sea.visibleChunks()+" BATCHES / "+
                sea.drawnTriangles()+" TRIANGLES  STATIC "+sea.allocatedStaticBytes()/1024+" KiB",
                10,HEIGHT-40);
            font.draw(sprites,"WATER CPU SUBMIT "+
                String.format(java.util.Locale.ROOT,"%.2f",sea.cpuSubmitNanos()/1_000_000.0)+
                "ms  GPU TIME NOT MEASURED",10,HEIGHT-53);
        }
        sprites.end();
    }

    /** CI camera searches genuine seeded in-world props; never inserts showcase-only assets. */
    private void viewPlacedModel(String modelPrefix) {
        for(World.UrbanProp target:world.urbanProps){
            if(!target.model().startsWith(modelPrefix))continue;
            for(int step=3;step<=6;step++){
                for(int sector=0;sector<8;sector++){
                    double angle=sector*Math.PI/4;
                    double cx=target.x()+Math.cos(angle)*step;
                    double cy=target.y()+Math.sin(angle)*step;
                    if(!world.canWalk(cx,cy)||
                       !world.lineClear(cx,cy,target.x(),target.y()))continue;
                    x=cx;y=cy;
                    heading=Math.atan2(target.y()-cy,target.x()-cx);
                    visualDistance=48;
                    System.out.println("GPU_SCENE_PROOF model="+target.model()+
                        " world="+target.x()+","+target.y());
                    return;
                }
            }
        }
        throw new IllegalStateException("No visually reachable seeded "+modelPrefix+
            " in scene; cannot prove model integration");
    }

    private void saveFrame(String filename) {
        // Read back the *actual* GPU framebuffer, correcting GL's bottom-left origin.
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,renderWidth,HEIGHT);
        Pixmap flipped=new Pixmap(renderWidth,HEIGHT,Pixmap.Format.RGBA8888);
        for(int row=0;row<HEIGHT;row++)for(int col=0;col<renderWidth;col++)
            flipped.drawPixel(col,HEIGHT-1-row,pixels.getPixel(col,row));
        FileHandle destination=Gdx.files.absolute(
            new java.io.File("build/test-visuals/"+filename).getAbsolutePath());
        destination.parent().mkdirs();
        PixmapIO.writePNG(destination,flipped);
        Set<Integer> colors=new HashSet<>();
        for(int yy=24;yy<170;yy+=4)for(int xx=8;xx<renderWidth-8;xx+=4)
            colors.add(flipped.getPixel(xx,yy));
        pixels.dispose();flipped.dispose();
        // An exterior vista may legitimately contain only water/terrain, with no
        // city model in front of the camera. Reject blank terrain and missing
        // city meshes independently; never assert that the ocean has street props.
        boolean exterior=gallery && (focusCity!='A'||
            galleryViews[galleryIndex].name().contains("outside") ||
            galleryViews[galleryIndex].name().contains("embankment") ||
            galleryViews[galleryIndex].name().contains("foundation"));
        if(colors.size()<18||visibleTerrainPatches<1||
           (!exterior&&(visibleInstances<5||drawCalls<3)))
            throw new IllegalStateException("GPU frame lacks required scene geometry: colors="+
                colors.size()+" instances="+visibleInstances+" calls="+drawCalls+
                " patches="+visibleTerrainPatches+" exterior="+exterior);
        System.out.println("PASS GPU_KENNEY_SMOKE screenshot="+destination.path()+
            " visibleInstances="+visibleInstances+" instancedDrawCalls="+drawCalls+
            " variedColors="+colors.size());
    }

    /** Benchmark imagery has its OWN paired-pixel assertion below. Normal
     * smoke/gallery captures retain their distinct city-geometry color gate. */
    private void saveOceanBenchmarkFrame(String filename){
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,renderWidth,HEIGHT);
        Pixmap flip=new Pixmap(renderWidth,HEIGHT,Pixmap.Format.RGBA8888);
        try{
            for(int py=0;py<HEIGHT;py++)
                for(int px=0;px<renderWidth;px++)
                    flip.drawPixel(px,HEIGHT-1-py,pixels.getPixel(px,py));
            FileHandle destination=Gdx.files.absolute(
                new java.io.File("build/test-visuals/"+filename).getAbsolutePath());
            destination.parent().mkdirs();
            PixmapIO.writePNG(destination,flip);
        }finally{
            flip.dispose();pixels.dispose();
        }
    }
    /** A sparse paired RGB readback proves that OpenGL actually drew water
     * into the visible world. Merely submitting triangles is insufficient. */
    private int[] oceanSamplePixels(){
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,renderWidth,HEIGHT);
        try{
            int count=0;
            int[] sampled=new int[((HEIGHT-72+3)/4)*((renderWidth-16+3)/4)];
            for(int py=36;py<HEIGHT-36;py+=4)
                for(int px=8;px<renderWidth-8;px+=4)
                    sampled[count++]=pixels.getPixel(px,py);
            return java.util.Arrays.copyOf(sampled,count);
        }finally{pixels.dispose();}
    }
    @Override public void render() {
        // Purely visual diagnostics: F8 never enters host gameplay controls
        // and never affects world simulation, purchases, or permissions.
        if(Gdx.input!=null&&Gdx.input.isKeyJustPressed(Input.Keys.F8))
            showGpuPerf=!showGpuPerf;
        // Real frame-start cadence, including prior GPU presentation and
        // swap/vsync delays. First 30 frames are warmup, not benchmark data.
        if(gallery&&Boolean.getBoolean("veil.gpu.oceanBenchmark")){
            long now=System.nanoTime();
            if(oceanBenchmarkPreviousNs!=0){
                double cadenceMs=(now-oceanBenchmarkPreviousNs)/1_000_000.0;
                if(frames>=30&&frames<180)
                    oceanBaselineFrameMs.add(cadenceMs);
                else if(frames>=210&&frames<360)
                    oceanBenchmarkFrameMs.add(cadenceMs);
            }
            oceanBenchmarkPreviousNs=now;
        }
        if(menuBackdrop){
            menuTime+=Math.min(.1f,Gdx.graphics.getDeltaTime());
            x=67+Math.sin(menuTime*.10)*9;
            y=67+Math.cos(menuTime*.08)*11;
            heading=.35+Math.sin(menuTime*.04)*.47;
            cameraPitch=-.45+Math.cos(menuTime*.055)*.10;
            cameraHeight=31+Math.cos(menuTime*.065)*4;
        }else if(editorMode&&!editorSmoke)editorControls();
        else if(gameplay!=null&&!playSmoke)liveControls();
        else if(gameplay==null&&!smoke&&!gallery&&!editorMode)control();
        // Handle backend-dependent resize delivery before rendering the frame.
        if(framebuffer==null||renderWidth!=GpuScreenLayout.width(
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight()))
            resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        framebuffer.begin();
        drawWorld();
        if(menuBackdrop&&launcherOverlay!=null)launcherOverlay.run();
        if(gallery){
            frames++;
            if(Boolean.getBoolean("veil.gpu.oceanBenchmark")){
                if(frames==179){
                    oceanBaselinePixels=oceanSamplePixels();
                    saveOceanBenchmarkFrame("gpu-ocean-coastal-no-water.png");
                }
                if(frames==360){
                    var waterStats=ocean.stats();
                    int[] nowPixels=oceanSamplePixels();
                    if(oceanBaselinePixels==null||
                       nowPixels.length!=oceanBaselinePixels.length)
                        throw new IllegalStateException("Missing paired GPU water readback");
                    int changed=0;
                    for(int i=0;i<nowPixels.length;i++){
                        int a=nowPixels[i],b=oceanBaselinePixels[i];
                        int delta=Math.abs(((a>>>24)&255)-((b>>>24)&255))+
                            Math.abs(((a>>>16)&255)-((b>>>16)&255))+
                            Math.abs(((a>>>8)&255)-((b>>>8)&255));
                        if(delta>36)changed++;
                    }
                    if(changed<200)
                        throw new IllegalStateException("Ocean was not visibly rendered: changed="+
                            changed+"/"+nowPixels.length+" sampled pixels");
                    if(waterStats.visibleChunks()==0||waterStats.drawnTriangles()==0||
                       waterStats.vertexUploadBytes()!=0||
                       oceanBenchmarkFrameMs.size()<100||
                       oceanBaselineFrameMs.size()<100)
                        throw new IllegalStateException(
                            "Invalid paired coastal ocean benchmark: "+waterStats+
                            " samples="+oceanBenchmarkFrameMs.size()+
                            " baseline="+oceanBaselineFrameMs.size());
                    var withWater=new java.util.ArrayList<>(oceanBenchmarkFrameMs);
                    var withoutWater=new java.util.ArrayList<>(oceanBaselineFrameMs);
                    java.util.Collections.sort(withWater);
                    java.util.Collections.sort(withoutWater);
                    double seaAverage=withWater.stream().mapToDouble(Double::doubleValue)
                        .average().orElseThrow();
                    double baseAverage=withoutWater.stream().mapToDouble(Double::doubleValue)
                        .average().orElseThrow();
                    double seaP95=withWater.get((int)Math.ceil(withWater.size()*.95)-1);
                    double baseP95=withoutWater.get((int)Math.ceil(withoutWater.size()*.95)-1);
                    // Runner-speed-independent catastrophic regression gate:
                    // the same camera and world are measured in adjacent
                    // phases. Slack permits software Mesa/CI noise while
                    // catching runaway shaders or draw-call multipliers.
                    if(seaAverage>baseAverage*1.8+4.0||
                       seaP95>baseP95*2.0+6.0)
                        throw new IllegalStateException(String.format(
                            java.util.Locale.ROOT,
                            "GPU ocean frame-cost budget exceeded baselineAvg=%.3f oceanAvg=%.3f baselineP95=%.3f oceanP95=%.3f",
                            baseAverage,seaAverage,baseP95,seaP95));
                    System.out.printf(java.util.Locale.ROOT,
                        "PASS GPU_OCEAN_BENCHMARK samples=%d baselineAvgMs=%.3f oceanAvgMs=%.3f oceanDeltaMs=%.3f baselineP95Ms=%.3f oceanP95Ms=%.3f baselineFPS=%.2f oceanFPS=%.2f oceanChunks=%d triangles=%d calls=%d uploadedFrameBytes=%d staticBytes=%d changedPixels=%d%n",
                        withWater.size(),baseAverage,seaAverage,seaAverage-baseAverage,
                        baseP95,seaP95,1000.0/baseAverage,1000.0/seaAverage,
                        waterStats.visibleChunks(),waterStats.drawnTriangles(),
                        waterStats.drawCalls(),waterStats.vertexUploadBytes(),
                        waterStats.allocatedStaticBytes(),changed);
                    saveOceanBenchmarkFrame("gpu-ocean-coastal-benchmark.png");
                    framebuffer.end();Gdx.app.exit();return;
                }
            }else if(frames%5==0){
                GalleryView view=galleryViews[galleryIndex];
                if(focusCity=='A'&&view.name().equals("04-city-overhead")&&
                   visibleInstances<world.urbanProps.size())
                    throw new IllegalStateException("Overhead city rendered an incomplete radius: "+
                        visibleInstances+" of "+world.urbanProps.size()+" authored tiles");
                saveFrame("gallery-seed-"+seed+"-city-"+focusCity+"-"+view.name()+".png");
                String log="GALLERY_CAPTURE seed="+seed+" city="+focusCity+" view="+view.name()+
                    " camera="+x+","+cameraHeight+","+y+
                    " heading="+heading+" pitch="+cameraPitch+
                    " time="+WorldDayNight.clock(environment.hour())+
                    " tiles="+visibleInstances+" drawCalls="+drawCalls+
                    " terrainPatches="+visibleTerrainPatches+
                    " terrainTriangles="+visibleTerrainTriangles+
                    " groundElevation="+vista.sample(x,y).elevation();
                System.out.println(log);
                galleryIndex++;
                if(galleryIndex==galleryViews.length){
                    framebuffer.end();Gdx.app.exit();return;
                }
                setGalleryView(galleryViews[galleryIndex]);
            }
        }
        if(editorSmoke){
            frames++;
            if(frames==10){
                var library=editorProject.assets();
                if(library.isEmpty())throw new IllegalStateException("Shared editor palette empty");
                String model=library.stream().map(MaleficEditorProject::modelFromAsset)
                    .filter(id->id.equals("detail-bench")).findFirst()
                    .orElse(MaleficEditorProject.modelFromAsset(library.get(0)));
                editorTestHistory.execute(()->{
                    smokeObject=editorProject.add(model,65.0,62.0,0,1.25,0).id();
                });
                editorTestHistory.execute(()->editorProject.transform(
                    smokeObject,65.5,62.0,.25,1.25,0));
                if(!editorTestHistory.undo()||
                   Math.abs(editorProject.get(smokeObject).x()-65.0)>.0001)
                    throw new IllegalStateException("Portable editor undo failed");
                if(!editorTestHistory.redo()||
                   Math.abs(editorProject.get(smokeObject).x()-65.5)>.0001)
                    throw new IllegalStateException("Portable editor redo failed");
                try{
                    editorProject.save();
                    new MaleficEditorProject(world).get(smokeObject);
                }catch(java.io.IOException bad){
                    throw new IllegalStateException("Interchangeable editor save/reload failed",bad);
                }
            }
            if(frames==65){
                if(menu.sourcedDraws()<20)
                    throw new IllegalStateException("Editor GPU UI lacks original PSX skin");
                System.out.println("PASS PSX_HORROR_UI_EDITOR_RENDER source="+
                    PsxHorrorUiSource.SHA256+" sourcedDraws="+menu.sourcedDraws());
                if(!models.containsKey(editorProject.get(smokeObject).model()))
                    throw new IllegalStateException("Editor-authored mesh never reached OpenGL");
                saveFrame("gpu-standalone-editor-live-viewport.png");
                System.out.println("PASS EXTERNAL_EDITOR_KIT_GPU_IMPORT seed="+seed+
                    " adapter="+editorProject.id()+
                    " saved="+editorProject.documentPath()+
                    " nodes="+editorProject.hierarchy().size()+
                    " palette="+editorProject.assets().size()+
                    " activeModels="+visibleInstances);
                framebuffer.end();Gdx.app.exit();return;
            }
        }
        if(playSmoke){
            frames++;
            if(frames==4){
                MaleficVeil.GpuFrame f=gameplay.gpuFrame();
                if(f==null)throw new IllegalStateException("Game never published authoritative snapshot");
                smokeStartingAmmo=f.pistolClip();
                gameplay.gpuKey(KeyEvent.VK_W,true);
            }
            if(frames==26){
                gameplay.gpuKey(KeyEvent.VK_W,false);
                gameplay.gpuFire();
            }
            if(frames==60){
                if(menu.sourcedDraws()<20)
                    throw new IllegalStateException("Gameplay HUD did not render original UI skin");
                System.out.println("PASS PSX_HORROR_UI_GAMEPLAY_RENDER source="+
                    PsxHorrorUiSource.SHA256+" sourcedDraws="+menu.sourcedDraws());
                saveFrame("gpu-live-play-shipped-client.png");
                MaleficVeil.GpuFrame f=gameplay.gpuFrame();
                if(f==null||f.pistolClip()>=smokeStartingAmmo)
                    throw new IllegalStateException("GPU live input did not fire authoritative weapon");
                System.out.println("PASS GPU_LIVE_PLAY_REAL_GAME seed="+seed+
                    " hp="+f.health()+" ammo="+f.pistolClip()+
                    " initialAmmo="+smokeStartingAmmo+" pos="+f.x()+","+f.z()+
                    " entities="+f.dynamic().size());
                framebuffer.end();Gdx.app.exit();return;
            }
        }
        if(smoke){
            frames++;
            if(frames==12){
                saveFrame("gpu-kenney-city.png");
                worldTimeMs=0;
            }else if(frames==24){
                saveFrame("gpu-kenney-night.png");
                menu.showMain();
            }else if(frames==36){
                if(menu.sourcedDraws()<5)
                    throw new IllegalStateException("Source PSX Horror UI menu not drawn");
                System.out.println("PASS PSX_HORROR_UI_MENU_RENDER source="+
                    PsxHorrorUiSource.SHA256+" sourcedDraws="+menu.sourcedDraws());
                saveFrame("gpu-psx-menu.png");
                menu.showRunning();
                heading=Math.PI*1.25;
                visualDistance=65;
                worldTimeMs=WorldDayNight.DAY_MS/2;
            }else if(frames==48){
                saveFrame("gpu-continental-edge.png");
                viewPlacedModel("truck-");
            }else if(frames==60){
                saveFrame("gpu-kenney-vehicles.png");
                viewPlacedModel("ggbot-");
            }else if(frames==72){
                // Actual in-world GGBot car, not a disconnected showroom proxy.
                saveFrame("gpu-ggbot-city-cars.png");
                viewPlacedModel("tree-");
            }else if(frames==84){
                saveFrame("gpu-kenney-foliage.png");
                framebuffer.end();
                Gdx.app.exit();
                return;
            }
        }
        framebuffer.end();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sprites.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        sprites.begin();
        // A FrameBuffer color texture has OpenGL's bottom-left origin.
        // SpriteBatch's ordinary Texture draw uses v=1 at the bottom; that
        // vertically inverts the whole composited city AND in-FBO menus.
        // Present the offscreen pixels with bottom-left mapped to bottom-left.
        sprites.draw(framebuffer.getColorBufferTexture(),0,0,
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),0,0,1,1);
        sprites.end();
    }

    /**
     * Tests the actual default-framebuffer image, not just an offscreen FBO
     * screenshot. In particular catches a vertically inverted desktop launch
     * even when the source screenshot/gallery smoke tests all pass.
     */
    void assertScreenOrientation(){
        if(framebuffer==null)throw new IllegalStateException("No rendered framebuffer");
        int screenWidth=Gdx.graphics.getBackBufferWidth();
        int screenHeight=Gdx.graphics.getBackBufferHeight();
        Pixmap presented=Pixmap.createFromFrameBuffer(0,0,screenWidth,screenHeight);
        Pixmap source=null;
        try{
            framebuffer.begin();
            try{source=Pixmap.createFromFrameBuffer(0,0,renderWidth,HEIGHT);}
            finally{framebuffer.end();}
            long uprightDifference=0,upsideDownDifference=0;
            int sampled=0;
            for(int sy=22;sy<HEIGHT-22;sy+=29){
                for(int sx=22;sx<renderWidth-22;sx+=31){
                    int dx=Math.min(screenWidth-1,
                        (int)((sx+.5)*screenWidth/renderWidth));
                    int dy=Math.min(screenHeight-1,
                        (int)((sy+.5)*screenHeight/HEIGHT));
                    int actual=presented.getPixel(dx,dy);
                    uprightDifference+=presentationRgbDistance(actual,source.getPixel(sx,sy));
                    upsideDownDifference+=presentationRgbDistance(
                        actual,source.getPixel(sx,HEIGHT-1-sy));
                    sampled++;
                }
            }
            // The same composed pixels must match the visible screen much
            // more closely upright than when mirrored vertically.
            if(upsideDownDifference<=uprightDifference*1.4+500)
                throw new IllegalStateException("Desktop GPU presentation is upside down"+
                    " uprightDifference="+uprightDifference+
                    " invertedDifference="+upsideDownDifference+" samples="+sampled);
            System.out.println("PASS GPU_FRAME_PRESENTATION_ORIENTATION"+
                " screen="+screenWidth+"x"+screenHeight+
                " uprightDifference="+uprightDifference+
                " invertedDifference="+upsideDownDifference+" samples="+sampled);
        }finally{
            if(source!=null)source.dispose();
            presented.dispose();
        }
    }
    private static int presentationRgbDistance(int a,int b){
        return Math.abs(((a>>>24)&255)-((b>>>24)&255))+
            Math.abs(((a>>>16)&255)-((b>>>16)&255))+
            Math.abs(((a>>>8)&255)-((b>>>8)&255));
    }

    /** Post-opaque GPU weather uses the host-synchronized weather state,
     * not a second random climate system. UI/menu panels draw over it. */
    private void renderGpuWeather(SpriteBatch sprites,MaleficVeil.GpuFrame frame){
        if(weatherPixel==null)return;
        float rain=(float)Math.max(0,Math.min(1,frame.rain()));
        float snow=(float)Math.max(0,Math.min(1,frame.snow()));
        int rainCount=(int)(rain*170),snowCount=(int)(snow*115);
        for(int i=0;i<rainCount;i++){
            int h=i*198491317+(frames/2)*1103515245;
            float px=(h&0x7fffffff)%renderWidth;
            float py=((h>>>7)&0x7fffffff)%HEIGHT;
            sprites.setColor(.71f,.80f,.88f,.17f+rain*.42f);
            sprites.draw(weatherPixel,px,py,1f,6f+rain*7f);
        }
        for(int i=0;i<snowCount;i++){
            int h=i*160481183+(frames/5)*734287;
            float px=(h&0x7fffffff)%renderWidth;
            float py=((h>>>6)&0x7fffffff)%HEIGHT;
            sprites.setColor(.86f,.91f,.96f,.35f+snow*.35f);
            sprites.draw(weatherPixel,px,py,2f,2f);
        }
        float flash=(float)Math.max(0,Math.min(.58,frame.flash()*.58));
        if(flash>0){
            sprites.setColor(.90f,.94f,1f,flash);
            sprites.draw(weatherPixel,0,0,renderWidth,HEIGHT);
        }
        sprites.setColor(1,1,1,1);
    }
    @Override public void dispose() {
        if(gameplayMenu!=null)gameplayMenu.dispose();
        for(List<GpuModel> groups:models.values())for(GpuModel model:groups)model.mesh.dispose();
        models.clear();
        for(Texture texture:materialTextures.values())texture.dispose();
        materialTextures.clear();
        if(shader!=null)shader.dispose();
        if(framebuffer!=null)framebuffer.dispose();
        if(sprites!=null)sprites.dispose();
        if(menu!=null)menu.close();
        else if(font!=null)font.dispose();
        if(skybox!=null)skybox.close();
        if(Gdx.input!=null)Gdx.input.setCursorCatched(false);
        if(terrain!=null)terrain.close();
        if(ocean!=null)ocean.close();
        if(weatherPixel!=null)weatherPixel.dispose();
        if(editorDock!=null)editorDock.close();
        if(cityMapTexture!=null)cityMapTexture.dispose();
        if(continentMapTexture!=null)continentMapTexture.dispose();
        if(gameplay!=null)gameplay.gpuShutdown();
    }
}