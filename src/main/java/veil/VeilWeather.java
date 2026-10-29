package veil;

import java.awt.Graphics2D;
import java.io.*;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Thin adapter from the one reusable Java 17 weather JAR to The Malefic Veil.
 * No weather simulation is copied or rebuilt per game. No alternative world
 * positions: all API calls use the SAME world-metre actor coordinates.
 * Uses a validated runtime JAR so existing javac *.java quality gates do not
 * require edits to every build script's compiler classpath.
 */
public final class VeilWeather implements AutoCloseable {
    public static final String VERSION="shared-psx-weather-java17-v1";
    private static final String JAR_SHA="4a666341fbebc83737f52772253bfe465e975106fb2ea0b1c672960f0204ce9b";
    private static final String[] PACKS={"cloud_to_ground","fork","sheet","horizontal_arc","atmosphere"};
    private static final String[] AUDIO_PACKS={"audio_00","audio_01","audio_02","audio_03","audio_04","audio_05"};
    private static final String[] PRESETS={"CLEAR","OVERCAST","DRIZZLE","RAIN","HEAVY_RAIN",
        "THUNDERSTORM","FOG","DENSE_FOG","SNOW","BLIZZARD","SLEET","HAIL",
        "DUST_STORM","ASHFALL","MIASMA"};
    private final URLClassLoader loader;
    private final Object weather;
    private final Class<?> typeClass,viewClass,sinkClass;
    private final Method advance,sky,overlay,setWeather,setClock,getClock,setAutoWeather,weatherState,surfaceRegister,surfaceShade,meshSink;
    private final Object surfaces,materialStone;
    private final Method currentWeather,skyState;
    private final Method sampleRgb;
    private final Map<String,GlbMesh> meshes;
    private final Map<Long,Integer> shadingCache=new HashMap<>();
    private Object audioPlayer;
    private Method audioUpdate;
    private final Object audio;
    private int selection=0;
    private long frame=0;
    private double rain,snow,flash;
    private double lastX,lastY,lastYaw;

    private static String sha256(byte[] bytes)throws Exception{
        var digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        var out=new StringBuilder(digest.length*2);
        for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static Object call(Object target,String name,Class<?>[] signature,Object...args)throws Exception{
        return target.getClass().getMethod(name,signature).invoke(target,args);
    }
    private static byte[] readBounded(ZipFile zip,ZipEntry entry,int max)throws IOException{
        if(entry.isDirectory()||entry.getSize()>max)throw new IOException("Invalid weather ZIP entry");
        try(InputStream in=zip.getInputStream(entry)){
            byte[] bytes=in.readNBytes(max+1);
            if(bytes.length>max)throw new IOException("Oversized weather ZIP entry");
            return bytes;
        }
    }
    /** Loads exactly the shared library's registered real 3D meshes. */
    public static Map<String,GlbMesh> loadMeshes(Path root)throws IOException{
        Map<String,GlbMesh> out=new LinkedHashMap<>();
        Path directory=root.resolve("assets/game/weather");
        for(String pack:PACKS){
            Path path=directory.resolve(pack+".zip");
            if(!Files.isRegularFile(path))throw new IOException("Missing shared weather pack "+path);
            try(ZipFile zip=new ZipFile(path.toFile())){
                for(var entries=zip.entries();entries.hasMoreElements();){
                    ZipEntry item=entries.nextElement();
                    if(item.isDirectory()||!item.getName().matches("[a-z0-9_]{4,95}\\.glb"))
                        throw new IOException("Unexpected weather asset entry "+item.getName());
                    String id=item.getName().substring(0,item.getName().length()-4);
                    GlbMesh decoded=GlbMesh.read(readBounded(zip,item,500000));
                    if(decoded.triangleCount()<2||decoded.triangleCount()>6000||out.putIfAbsent(id,decoded)!=null)
                        throw new IOException("Invalid weather geometry "+id);
                }
            }
        }
        if(out.size()!=25)throw new IOException("Shared VFX pack must include 25 actual GLBs, got "+out.size());
        return Collections.unmodifiableMap(out);
    }
    private static Path unpackAudio(Path root)throws IOException{
        Path target=Path.of("build","weather-audio-runtime");
        Files.createDirectories(target);
        int total=0;
        for(String name:AUDIO_PACKS){
            Path archive=root.resolve("assets/game/weather").resolve(name+".zip");
            try(ZipFile zip=new ZipFile(archive.toFile())){
                for(var files=zip.entries();files.hasMoreElements();){
                    var entry=files.nextElement();
                    if(entry.isDirectory()||!entry.getName().matches("[a-z0-9_]{4,95}\\.wav"))
                        throw new IOException("Unexpected weather audio: "+entry.getName());
                    Files.write(target.resolve(entry.getName()),readBounded(zip,entry,500000));
                    total++;
                }
            }
        }
        if(total!=12)throw new IOException("Expected 12 portable weather sounds, got "+total);
        return target;
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    public VeilWeather(long seed,Path root)throws IOException{
        this(seed,root,true);
    }
    /** Host-only instances run the SAME shared simulation, without client GLBs/audio. */
    public VeilWeather(long seed,Path root,boolean graphicsAndAudio)throws IOException{
        try{
            Path jar=root.resolve("lib").resolve(VERSION+".jar");
            byte[] contents=Files.readAllBytes(jar);
            if(!sha256(contents).equals(JAR_SHA))throw new IOException("Shared weather module digest mismatch");
            loader=new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},VeilWeather.class.getClassLoader());
            Class<?> module=Class.forName("dev.mrcalzon.weather.WeatherModule",true,loader);
            typeClass=Class.forName("dev.mrcalzon.weather.WeatherType",true,loader);
            viewClass=Class.forName("dev.mrcalzon.weather.WeatherView",true,loader);
            sinkClass=Class.forName("dev.mrcalzon.weather.WeatherMeshes$Sink",true,loader);
            weather=module.getConstructor(long.class).newInstance(seed);
            advance=module.getMethod("advance",double.class,viewClass);
            sky=module.getMethod("renderSky",Graphics2D.class,int.class,int.class);
            overlay=module.getMethod("renderOverlay",Graphics2D.class,int.class,int.class);
            setWeather=module.getMethod("setWeather",typeClass,double.class);
            setClock=module.getMethod("setTimeOfDay",double.class);
            getClock=module.getMethod("timeOfDay");
            setAutoWeather=module.getMethod("setAutoWeather",boolean.class,double.class,double.class,double.class);
            weatherState=module.getMethod("weather");
            meshSink=module.getMethod("emitMeshes",sinkClass);
            currentWeather=Class.forName("dev.mrcalzon.weather.WeatherSystem",true,loader).getMethod("state");
            skyState=module.getMethod("sky");
            surfaces=module.getMethod("surfaces").invoke(weather);
            Class<?> material=Class.forName("dev.mrcalzon.weather.WeatherSurfaceGrid$Material",true,loader);
            materialStone=Enum.valueOf((Class)material,"STONE");
            surfaceRegister=surfaces.getClass().getMethod("register",int.class,material,double.class);
            surfaceShade=surfaces.getClass().getMethod("shade",int.class,int.class);
            Class<?> sample=Class.forName("dev.mrcalzon.weather.WeatherSurfaceGrid$Sample",true,loader);
            sampleRgb=sample.getMethod("rgb");
            audio=module.getMethod("audio").invoke(weather);
            meshes=graphicsAndAudio?loadMeshes(root):Collections.emptyMap();
            module.getMethod("setDayLength",double.class).invoke(weather,1500d);
            setAutoWeather.invoke(weather,true,240d,.55,65d);
            setPreset("OVERCAST",0);
            // Java Sound is genuinely optional; a dedicated server or headless
            // CI must NEVER require a physical audio device to start gameplay.
            if(graphicsAndAudio&&!java.awt.GraphicsEnvironment.isHeadless()&&
               !Boolean.getBoolean("veil.weather.mute")){
                Class<?> player=Class.forName("dev.mrcalzon.weather.WeatherAudioPlayer",true,loader);
                audioPlayer=player.getConstructor(Path.class).newInstance(unpackAudio(root));
                audioUpdate=player.getMethod("update",audio.getClass());
            }
        }catch(IOException ex){throw ex;}
        catch(Exception ex){throw new IOException("Cannot load pinned shared weather Java module",ex);}
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    public void setPreset(String name,double transitionSeconds){
        if(!Arrays.asList(PRESETS).contains(name))throw new IllegalArgumentException("Unknown weather "+name);
        try{
            Object value=Enum.valueOf((Class)typeClass,name);
            setWeather.invoke(weather,value,transitionSeconds);
            selection=Arrays.asList(PRESETS).indexOf(name);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Weather preset",e);}
    }
    public String cycle(){setPreset(PRESETS[(selection+1)%PRESETS.length],8);return PRESETS[selection];}
    public double timeOfDay(){
        try{return ((Number)getClock.invoke(weather)).doubleValue();}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Weather clock read",e);}
    }
    public void autoWeather(boolean enabled,double interval,double variance,double transition){
        try{setAutoWeather.invoke(weather,enabled,interval,variance,transition);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Weather automation",e);}
    }
    /** A network client's weather MUST follow the host, not run a rival RNG schedule. */
    public void followHost(){autoWeather(false,240,.55,65);}
    public void applyHostWeather(String preset,double dayFraction){
        if(!Arrays.asList(PRESETS).contains(preset)||!Double.isFinite(dayFraction)||
           dayFraction<0||dayFraction>=1)
            throw new IllegalArgumentException("Invalid authoritative weather packet");
        if(!current().equals(preset))setPreset(preset,3);
        double local=timeOfDay();
        double difference=Math.abs(local-dayFraction);
        difference=Math.min(difference,1-difference);
        if(difference>.003)setTimeOfDay(dayFraction);
    }
    public String current(){return PRESETS[selection];}
    public double rain(){return rain;}
    public double snow(){return snow;}
    public double flash(){return flash;}
    public void tick(double seconds,double x,double y,double heading,double exposed){
        try{
            lastX=x;lastY=y;lastYaw=heading;
            Object view=viewClass.getConstructor(double.class,double.class,double.class,double.class,double.class)
                .newInstance(x,y,1.55,heading,exposed);
            advance.invoke(weather,seconds,view);
            Object state=currentWeather.invoke(weatherState.invoke(weather));
            String actual=((Enum<?>)state.getClass().getMethod("type").invoke(state)).name();
            selection=Arrays.asList(PRESETS).indexOf(actual);
            rain=((Number)state.getClass().getMethod("rain").invoke(state)).doubleValue();
            snow=((Number)state.getClass().getMethod("snow").invoke(state)).doubleValue();
            flash=((Number)state.getClass().getMethod("flash").invoke(state)).doubleValue();
            shadingCache.clear();frame++;
            if(audioPlayer!=null){
                try{audioUpdate.invoke(audioPlayer,audio);}
                catch(ReflectiveOperationException | IllegalArgumentException ex){
                    System.err.println("Weather audio unavailable: "+ex.getMessage());
                    closeAudio();
                }
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Shared weather tick failure",e);}
    }
    public void paintSky(Graphics2D g,int width,int height){
        try{sky.invoke(weather,g,width,height);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public void paintOverlay(Graphics2D g,int width,int height){
        try{overlay.invoke(weather,g,width,height);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public Map<String,GlbMesh> meshes(){return meshes;}
    public void emitLightning(java.util.List<UrbanScene.ModelInstance> result){
        Objects.requireNonNull(result);
        try{
            Object callback=Proxy.newProxyInstance(loader,new Class[]{sinkClass},(obj,method,args)->{
                if(method.getName().equals("emit")&&args!=null&&args.length==1){
                    Object p=args[0];Class<?> k=p.getClass();
                    String id=(String)k.getMethod("meshId").invoke(p);
                    if(id.startsWith("lightning_")&&meshes.containsKey(id)){
                        double x=((Number)k.getMethod("x").invoke(p)).doubleValue();
                        double y=((Number)k.getMethod("y").invoke(p)).doubleValue();
                        double z=((Number)k.getMethod("z").invoke(p)).doubleValue();
                        double yaw=((Number)k.getMethod("yaw").invoke(p)).doubleValue();
                        double scale=((Number)k.getMethod("scale").invoke(p)).doubleValue();
                        double opacity=((Number)k.getMethod("opacity").invoke(p)).doubleValue();
                        if(Math.hypot(x-lastX,y-lastY)<35)
                            result.add(new UrbanScene.ModelInstance(id,x,y,yaw,scale,opacity,z));
                    }
                }
                return null;
            });
            meshSink.invoke(weather,callback);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Lightning mesh callback",e);}
    }
    /** Use the actual host/world cell classification for ground sky exposure. */
    public static double exposure(World world,double x,double y){
        if(world.detailedCity(x,y)){
            int tx=(int)Math.floor(x),ty=(int)Math.floor(y);
            if(world.city.road(tx/CityLayout.CELL,ty/CityLayout.CELL))return 1;
            if(world.city.categoryAt(tx,ty)==CityLayout.Category.OPEN)return 1;
            // Conservative indoor/roofed plot proxy until per-mesh upward ray
            // tests are available. Prevent rainfall passing through roofs.
            return 0;
        }
        return world.worldStructureAt(x,y,.1)?0:1;
    }
    private static int surfaceId(double x,double y){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        // Real current continent is bounded by roughly +/-1300 metres.
        if(ix< -1900||ix>=1900||iy< -1900||iy>=1900)return -1;
        return (ix+1900)*4000+(iy+1900);
    }
    /** Register actual visible ground and maintain persistent wet/snow state. */
    public void registerAround(World world,double x,double y){
        try{
            int cx=(int)Math.floor(x),cy=(int)Math.floor(y);
            for(int i=-10;i<=10;i++)for(int j=-10;j<=10;j++){
                double wx=cx+i+.5,wy=cy+j+.5;
                int id=surfaceId(wx,wy);
                if(id>=0&&world.continent().inBounds(world.parcelX(wx),world.parcelY(wy)))
                    surfaceRegister.invoke(surfaces,id,materialStone,exposure(world,wx,wy));
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Weather exposure update",e);}
    }
    /** Wet/snow tint uses shared module's real persistent per-surface simulation. */
    public int tint(double x,double y,int baseRgb){
        int id=surfaceId(x,y);
        if(id<0)return baseRgb;
        long key=(((long)id)<<24)|(baseRgb&0xffffffL);
        Integer cached=shadingCache.get(key);
        if(cached!=null)return cached;
        try{
            int rgb=((Number)sampleRgb.invoke(surfaceShade.invoke(surfaces,id,baseRgb))).intValue();
            if(shadingCache.size()<20000)shadingCache.put(key,rgb);
            return rgb;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Weather material shade",e);}
    }
    public void setTimeOfDay(double dayFraction){
        try{setClock.invoke(weather,dayFraction);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public double daylight(){
        try{
            Object state=skyState.invoke(weather);
            return ((Number)state.getClass().getMethod("daylight").invoke(state)).doubleValue();
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    /** GPU client owns its audio mixer; keep Java Sound for Swing-only play. */
    public void disableAudio(){closeAudio();}
    private void closeAudio(){
        if(audioPlayer!=null){
            try{audioPlayer.getClass().getMethod("close").invoke(audioPlayer);}
            catch(ReflectiveOperationException ignored){}
            audioPlayer=null;
        }
    }
    @Override public void close(){closeAudio();try{loader.close();}catch(IOException ignored){}}
}
