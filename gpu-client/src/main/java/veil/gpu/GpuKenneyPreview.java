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
    private World world;
    private UrbanScene scene;
    private ShaderProgram shader;
    private PerspectiveCamera camera;
    private FrameBuffer framebuffer;
    private SpriteBatch sprites;
    private BitmapFont font;
    private GpuSkybox skybox;
    private GpuContinentTerrain terrain;
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
    public GpuKenneyPreview(long seed,boolean editorSmoke){
        this(seed,false,false,'A',null,false,true,editorSmoke);
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
                        tri.ax(),tri.ay(),tri.az(),surface.au(),surface.av(),
                        tri.bx(),tri.by(),tri.bz(),surface.bu(),surface.bv(),
                        tri.cx(),tri.cy(),tri.cz(),surface.cu(),surface.cv()
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
        camera=new PerspectiveCamera(72,WIDTH,HEIGHT);
        camera.near=0.08f;camera.far=300f;
        framebuffer=new FrameBuffer(Pixmap.Format.RGBA8888,WIDTH,HEIGHT,true);
        sprites=new SpriteBatch();
        font=new BitmapFont();
        font.getData().setScale(1.16f);
        skybox=new GpuSkybox();
        menu=new PsxHorrorMenu(smoke||gallery||gameplay!=null||editorMode);
        font.dispose();
        font=menu.font();
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        galleryViews=focusCity=='A'?GALLERY:generatedCityViews();
        if(gallery)setGalleryView(galleryViews[0]);
        if(gameplay!=null){
            buildGameplayMaps();
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
            scene.instances().size()+" sourceMeshes="+models.size()+" resolution="+WIDTH+"x"+HEIGHT);
    }

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
        {Input.Keys.L,KeyEvent.VK_L},{Input.Keys.X,KeyEvent.VK_X},
        {Input.Keys.I,KeyEvent.VK_I},{Input.Keys.K,KeyEvent.VK_K},
        {Input.Keys.J,KeyEvent.VK_J},{Input.Keys.O,KeyEvent.VK_O},
        {Input.Keys.P,KeyEvent.VK_P},{Input.Keys.U,KeyEvent.VK_U},
        {Input.Keys.M,KeyEvent.VK_M},{Input.Keys.N,KeyEvent.VK_N},
        {Input.Keys.F9,KeyEvent.VK_F9},
        {Input.Keys.NUM_1,KeyEvent.VK_1},{Input.Keys.NUM_2,KeyEvent.VK_2},
        {Input.Keys.NUM_3,KeyEvent.VK_3},{Input.Keys.ENTER,KeyEvent.VK_ENTER}
    };
    /** Only input adapters live here: movement, shooting, skills, factions,
     * cargo, crafting and host transactions remain in MaleficVeil. */
    private void liveControls(){
        PsxHorrorMenu.Settings settings=new PsxHorrorMenu.Settings(){
            public void changeView(int delta){
                visualDistance=Math.max(45,Math.min(240,visualDistance+delta*3));
            }
            public void changeLights(int delta){
                maxLights=Math.max(0,Math.min(8,maxLights+delta));
            }
            public void changeTimeSpeed(int ignored){} // server owns world time
        };
        boolean active=menu.input(settings);
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
        if(Gdx.input.isKeyJustPressed(Input.Keys.F11)){
            Gdx.input.setCursorCatched(false);
            gameplay.gpuManagement();
        }
        if(!Gdx.input.isCursorCatched()){
            Gdx.input.setCursorCatched(true);
            Gdx.input.getDeltaX();Gdx.input.getDeltaY();
        }else{
            gameplay.gpuLook(Gdx.input.getDeltaX(),Gdx.input.getDeltaY());
        }
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))gameplay.gpuFire();
        MaleficVeil.GpuFrame frame=gameplay.gpuFrame();
        if(frame!=null){
            x=frame.x();y=frame.z();heading=frame.yaw();
            cameraPitch=frame.pitch();cameraHeight=1.65;
            worldTimeMs=(long)(WorldDayNight.DAY_MS*frame.hour()/24.0);
        }
    }
    private void drawLiveHud(MaleficVeil.GpuFrame frame){
        String weapon=frame.weapon()==0?"PISTOL":"SHOTGUN";
        String rounds=frame.weapon()==0?
            frame.pistolClip()+"/"+frame.pistolReserve():
            frame.shellClip()+"/"+frame.shellReserve();
        font.draw(sprites,"HP "+frame.health()+"  SAN "+frame.sanity()+
            "  "+weapon+" "+rounds+"  MED "+frame.medkits(),9,391);
        font.draw(sprites,"CASH "+frame.cash()+"  SCRAP "+frame.scrap()+
            "  KILLS "+frame.frags()+"  GPU / LIVE WORLD",9,369);
        font.draw(sprites,"+",WIDTH/2f-4,HEIGHT/2f+5);
        if(!frame.notice().isBlank())
            font.draw(sprites,frame.notice(),12,HEIGHT/2f+50);
        if(!frame.connected())
            font.draw(sprites,"HOST CONNECTION LOST",WIDTH/2f-112,HEIGHT/2f+28);
        if(frame.dead())
            font.draw(sprites,"YOU HAVE FALLEN  1/2/3 RESPAWN LOCATION  ENTER DEPLOY",
                17,HEIGHT/2f);
        if(frame.inventory()){
            font.draw(sprites,"INVENTORY",WIDTH-205,HEIGHT-35);
            int row=0;
            for(var item:frame.inventoryItems().entrySet()){
                if(row>=12)break;
                font.draw(sprites,item.getKey()+" x"+item.getValue(),
                    WIDTH-210,HEIGHT-57-row*18);
                row++;
            }
            if(row==0)font.draw(sprites,"EMPTY",WIDTH-210,HEIGHT-57);
        }else if(frame.skills()){
            font.draw(sprites,"CHARACTER / SKILLS  K CLOSE",WIDTH-265,HEIGHT-35);
            int row=0;
            for(String line:frame.skillStats()){
                if(row>=11)break;
                font.draw(sprites,line,WIDTH-267,HEIGHT-56-row*18);
                row++;
            }
        }
        if(frame.map()||frame.continent()){
            Texture source=frame.map()?cityMapTexture:continentMapTexture;
            if(source==null)throw new IllegalStateException("Missing authoritative GPU map");
            float left=WIDTH-280,base=HEIGHT-325;
            sprites.draw(source,left,base,255,255,0,0,
                source.getWidth(),source.getHeight(),false,true);
            font.draw(sprites,frame.map()?"CITY (M CLOSE)":"CONTINENT (N CLOSE)",
                left,base+278);
            float markerX=frame.map()?
                (float)(frame.x()/World.SIZE*255):
                (float)((world.parcelX(frame.x())+.5)/ContinentLayout.SIZE*255);
            float markerY=frame.map()?
                (float)((World.SIZE-frame.z())/World.SIZE*255):
                (float)((ContinentLayout.SIZE-world.parcelY(frame.z())-.5)/
                    ContinentLayout.SIZE*255);
            if(markerX>=0&&markerX<=255&&markerY>=0&&markerY<=255)
                font.draw(sprites,"+",left+markerX-5,base+markerY+7);
        }
        font.draw(sprites,"WASD MOVE  MOUSE AIM/FIRE  E USE  R RELOAD  I INVENTORY  "+
            "K SKILLS  F11 MORE  ESC MENU",9,20);
    }
    /** Foreground sourced weapon mesh, drawn as a camera-relative 3D object. */
    private void renderLiveWeapon(MaleficVeil.GpuFrame frame){
        if(frame.dead()||!menu.running())return;
        String key=GameplayMeshes.WEAPON[frame.weapon()==0?0:1];
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
        float mouseX=Gdx.input.getX()*(WIDTH/(float)Gdx.graphics.getWidth());
        float mouseY=(Gdx.graphics.getHeight()-Gdx.input.getY())*
            (HEIGHT/(float)Gdx.graphics.getHeight());
        MaleficEditorProject.Placement best=null;
        float bestPixels=22f*22f;
        for(MaleficEditorProject.Placement item:editorProject.placements()){
            Vector3 point=camera.project(new Vector3((float)item.x(),
                (float)(item.elevation()+.7),(float)item.z()),0,0,WIDTH,HEIGHT);
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
        if(gameplay!=null){
            MaleficVeil.GpuFrame live=gameplay.gpuFrame();
            if(live!=null)renderLiveWeapon(live);
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        sprites.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,WIDTH,HEIGHT));
        sprites.begin();
        if(menu.running()&&editorMode){
            font.draw(sprites,"LEVEL EDITOR KIT / CITY A  F11 DOCK  CTRL+CLICK PICK",9,391);
            font.draw(sprites,"SEED "+seed+"  LAYER "+
                editorProject.placements().size()+"  MESHES "+visibleInstances,9,370);
            font.draw(sprites,"WASD FLY  MOUSE LOOK  SPACE UP  CTRL DOWN  F11 PALETTE",9,20);
        }else if(menu.running()&&gameplay!=null){
            MaleficVeil.GpuFrame live=gameplay.gpuFrame();
            if(live!=null)drawLiveHud(live);
        }else if(menu.running()){
            font.draw(sprites,"THE MALEFIC VEIL  "+WorldDayNight.clock(environment.hour()),9,391);
            font.draw(sprites,"TILES "+visibleInstances+"  LAND "+visibleTerrain+"  DRAWS "+drawCalls+
                "  LAMPS "+activeLights+"  VIEW "+(int)visualDistance,9,367);
            font.draw(sprites,"WASD MOVE  MOUSE LOOK  PGUP/DN PITCH  [ ] TIME  F10 GRAPHICS  ESC MENU",9,20);
        }else{
            menu.draw(sprites,font,visualDistance,maxLights,timeSpeed);
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
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,WIDTH,HEIGHT);
        Pixmap flipped=new Pixmap(WIDTH,HEIGHT,Pixmap.Format.RGBA8888);
        for(int row=0;row<HEIGHT;row++)for(int col=0;col<WIDTH;col++)
            flipped.drawPixel(col,HEIGHT-1-row,pixels.getPixel(col,row));
        FileHandle destination=Gdx.files.absolute(
            new java.io.File("build/test-visuals/"+filename).getAbsolutePath());
        destination.parent().mkdirs();
        PixmapIO.writePNG(destination,flipped);
        Set<Integer> colors=new HashSet<>();
        for(int yy=24;yy<170;yy+=4)for(int xx=8;xx<WIDTH-8;xx+=4)
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

    @Override public void render() {
        if(editorMode&&!editorSmoke)editorControls();
        else if(gameplay!=null&&!playSmoke)liveControls();
        else if(gameplay==null&&!smoke&&!gallery&&!editorMode)control();
        framebuffer.begin();
        drawWorld();
        if(gallery){
            frames++;
            if(frames%5==0){
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
        sprites.draw(framebuffer.getColorBufferTexture(),0,0,
            Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),0,1,1,0);
        sprites.end();
    }

    @Override public void dispose() {
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
        if(editorDock!=null)editorDock.close();
        if(cityMapTexture!=null)cityMapTexture.dispose();
        if(continentMapTexture!=null)continentMapTexture.dispose();
        if(gameplay!=null)gameplay.gpuShutdown();
    }
}