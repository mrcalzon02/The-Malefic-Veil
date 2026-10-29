package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Native libGDX audio mixer. Original source music retains exact SHA-256
 * verification. Sourced Drive samples take precedence over synthesized safety
 * cues; 32 separately rendered weapon profiles never collapse to one gunshot.
 * Audio failure is isolated from authoritative multiplayer gameplay.
 */
public final class GpuAudioBus implements AutoCloseable {
    private static final int PARTS=8;
    public static final long ORIGINAL_BYTES=3558384;
    public static final String ORIGINAL_SHA="0e0b0e5bbad299d733938f907aa63fd62de9abad3dc61a00a8ee287fbe10a293";
    private Music music, ambience, combatMusic, campfireLoop;
    private final Music[] industryLoops=new Music[GpuIndustryAudioPlanner.Channel.values().length];
    private final float[] industryGain=new float[industryLoops.length];
    private final Sound[][] fieldSounds=new Sound[3][GpuAudioSynthesis.DeployablePhase.values().length];
    private float combatGain,fireGain,combatHold;
    private final Music[] weatherBeds=new Music[GpuAudioSynthesis.WeatherBed.values().length];
    private final float[] weatherGains=new float[GpuAudioSynthesis.WeatherBed.values().length];
    private float thunderCooldown,previousFlash;
    private final Sound[] cues=new Sound[GpuAudioSynthesis.Cue.values().length];
    private final Sound[] impacts=new Sound[GpuAudioSynthesis.Impact.values().length];
    private final Sound[][] surfaceSteps=
        new Sound[GpuFootstepSurface.Material.values().length][2];
    private final Sound[] weapon=new Sound[32];
    private final Sound[] alternate=new Sound[32];
    private final Sound[] handling=new Sound[32];
    private Sound thunder,sourceReload,sourceCoins,sourceDrawer,sourceCloth,sourceHurt,sourceField;
    private final Sound[] sourceFamily=new Sound[8];
    private Sound menuClick,menuConfirm,menuReject,panelOpen;
    private final List<Path> temporary=new ArrayList<>();
    private GpuClientSettings settings;
    private boolean ingame,available;
    private int footstep;
    public GpuAudioBus(GpuClientSettings settings){this.settings=Objects.requireNonNull(settings);}

    public static byte[] verifiedMusic()throws IOException{
        ByteArrayOutputStream output=new ByteArrayOutputStream((int)ORIGINAL_BYTES);
        for(int i=0;i<PARTS;i++){
            String file="/audio/cold-unresolved-drone.mp3.part"+String.format(Locale.ROOT,"%02d",i);
            try(InputStream stream=GpuAudioBus.class.getResourceAsStream(file)){
                if(stream==null)throw new IOException("Missing user-authorized music part "+file);
                stream.transferTo(output);
            }
        }
        byte[] bytes=output.toByteArray();
        if(bytes.length!=ORIGINAL_BYTES)throw new IOException("Original music byte count mismatch");
        try{
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder actual=new StringBuilder();
            for(byte b:hash)actual.append(String.format(Locale.ROOT,"%02x",b&0xff));
            if(!ORIGINAL_SHA.equals(actual.toString()))
                throw new IOException("Music must match original approved Google Drive source");
        }catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        return bytes;
    }

    private Path temp(String stem,String ext,byte[] data)throws IOException {
        Path file=Files.createTempFile("malefic-"+stem+"-",ext);
        temporary.add(file);
        Files.write(file,data);
        return file;
    }
    private Sound synthesized(String name,byte[] wav)throws IOException {
        return Gdx.audio.newSound(new FileHandle(temp(name,".wav",wav).toFile()));
    }
    private Sound sourced(String filename) {
        String resource="audio/library/"+filename;
        if(GpuAudioBus.class.getResource("/"+resource)==null)return null;
        try{return Gdx.audio.newSound(Gdx.files.classpath(resource));}
        catch(RuntimeException unavailable){
            System.err.println("AUDIO_SOURCE_DECODER_UNAVAILABLE "+resource+" "+unavailable);
            return null;
        }
    }
    private Sound sourcedOrFallback(String filename, GpuAudioSynthesis.Cue cue)throws IOException {
        Sound original=sourced(filename);
        return original==null?synthesized("ui-"+cue.name().toLowerCase(Locale.ROOT),
            GpuAudioSynthesis.cue(cue)):original;
    }
    public void open(){
        if(available)return;
        try{
            byte[] original=verifiedMusic();
            music=Gdx.audio.newMusic(new FileHandle(temp("original-music",".mp3",original).toFile()));
            music.setLooping(true);
            ambience=Gdx.audio.newMusic(new FileHandle(temp("city-ambience",".wav",
                GpuAudioSynthesis.cityAmbience()).toFile()));
            ambience.setLooping(true);
            combatMusic=Gdx.audio.newMusic(new FileHandle(temp(
                "hostile-combat-pulse",".wav",GpuAudioSynthesis.combatPulse()).toFile()));
            combatMusic.setLooping(true);
            combatMusic.setVolume(0f);
            campfireLoop=Gdx.audio.newMusic(new FileHandle(temp(
                "field-fire-loop",".wav",GpuAudioSynthesis.campfire()).toFile()));
            campfireLoop.setLooping(true);
            campfireLoop.setVolume(0f);
            for(GpuAudioSynthesis.Impact type:GpuAudioSynthesis.Impact.values())
                impacts[type.ordinal()]=synthesized("impact-"+type.name().toLowerCase(Locale.ROOT),
                    GpuAudioSynthesis.impact(type));
            for(GpuIndustryAudioPlanner.Channel channel:GpuIndustryAudioPlanner.Channel.values()){
                Music source=Gdx.audio.newMusic(new FileHandle(temp(
                    "industry-"+channel.name().toLowerCase(Locale.ROOT),".wav",
                    GpuAudioSynthesis.industry(channel)).toFile()));
                source.setLooping(true);
                source.setVolume(0);
                industryLoops[channel.ordinal()]=source;
            }
            for(int faction=0;faction<3;faction++)
                for(GpuAudioSynthesis.DeployablePhase phase:GpuAudioSynthesis.DeployablePhase.values())
                    fieldSounds[faction][phase.ordinal()]=synthesized(
                        "field-"+faction+"-"+phase.name().toLowerCase(Locale.ROOT),
                        GpuAudioSynthesis.deployable(faction,phase));
            for(GpuFootstepSurface.Material material:GpuFootstepSurface.Material.values())
                for(int variant=0;variant<2;variant++)
                    surfaceSteps[material.ordinal()][variant]=synthesized(
                        "step-"+material.name().toLowerCase(Locale.ROOT)+"-"+variant,
                        GpuAudioSynthesis.footstep(material,variant));
            for(GpuAudioSynthesis.WeatherBed bed:GpuAudioSynthesis.WeatherBed.values()){
                Music layer=Gdx.audio.newMusic(new FileHandle(temp(
                    "weather-"+bed.name().toLowerCase(Locale.ROOT),".wav",
                    GpuAudioSynthesis.weatherBed(bed)).toFile()));
                layer.setLooping(true);
                layer.setVolume(0f);
                weatherBeds[bed.ordinal()]=layer;
            }
            thunder=synthesized("thunder",GpuAudioSynthesis.thunder());
            for(GpuAudioSynthesis.Cue cue:GpuAudioSynthesis.Cue.values())
                cues[cue.ordinal()]=synthesized(cue.name().toLowerCase(Locale.ROOT),
                    GpuAudioSynthesis.cue(cue));
            menuClick=sourcedOrFallback("Single_click.wav",GpuAudioSynthesis.Cue.CLICK);
            menuConfirm=sourcedOrFallback("Cathonk.wav",GpuAudioSynthesis.Cue.CONFIRM);
            menuReject=sourcedOrFallback("Metal_wonk.wav",GpuAudioSynthesis.Cue.REJECT);
            panelOpen=sourcedOrFallback("Book_snap.wav",GpuAudioSynthesis.Cue.NAVIGATE);
            for(int i=0;i<weapon.length;i++){
                weapon[i]=synthesized("weapon-"+(i+1),GpuAudioSynthesis.weapon(i));
                alternate[i]=synthesized("weapon-alt-"+(i+1),GpuAudioSynthesis.weapon(i,true));
                handling[i]=synthesized("weapon-handling-"+(i+1),GpuAudioSynthesis.handling(i));
            }
            sourceReload=sourced("RELOAD.mp3");
            sourceCoins=sourced("Coins.wav");
            sourceDrawer=sourced("Drawer.wav");
            sourceCloth=sourced("Shifting_cloth.wav");
            sourceHurt=sourced("Bash.mp3");
            sourceField=sourced("Granade.wav");
            sourceFamily[0]=sourced("Shot.mp3");
            sourceFamily[1]=sourced("LAS.mp3");
            sourceFamily[2]=sourced("Lightning-crackle.mp3");
            sourceFamily[3]=sourced("FLAME.mp3");
            sourceFamily[6]=sourced("Plas.mp3");
            available=true;
            apply(settings);
            music.play();
            System.out.println("AUDIO_BANK_READY originalDriveMusicSha="+ORIGINAL_SHA+
                " bytes="+original.length+" weapons=32 primary+alternate+handling"+
                " ui=positive,negative,navigate,click weather=host-reactive"+
                " categories=music,ambience,effects,ui,voice,combat,environment");
        }catch(Exception failed){
            // Missing ALSA/OpenAL in CI must never prevent a real game launch.
            System.err.println("AUDIO_DEVICE_UNAVAILABLE "+failed);
            close();
        }
    }
    public void apply(GpuClientSettings fresh){
        settings=Objects.requireNonNull(fresh);
        // Music never borrows the ambience slider. Both honor master gain.
        if(music!=null)music.setVolume(settings.musicGain()*(ingame?
            (.18f-.10f*combatGain):1f));
        if(combatMusic!=null)combatMusic.setVolume(ingame?
            settings.musicGain()*.46f*combatGain:0f);
        if(campfireLoop!=null)campfireLoop.setVolume(ingame?
            settings.ambienceGain()*.33f*fireGain:0f);
        if(ambience!=null)ambience.setVolume(ingame?settings.ambienceGain()*.5f:0f);
        for(int i=0;i<weatherBeds.length;i++)
            if(weatherBeds[i]!=null)weatherBeds[i].setVolume(
                ingame?weatherGains[i]*settings.ambienceGain():0f);
        for(int i=0;i<industryLoops.length;i++)
            if(industryLoops[i]!=null)industryLoops[i].setVolume(
                ingame?industryGain[i]*machineBusGain(i):0f);
    }
    public void inGame(){
        ingame=true;apply(settings);
        if(music!=null&&!music.isPlaying())music.play();
        if(ambience!=null&&!ambience.isPlaying())ambience.play();
        // Zero-volume sources must not occupy OpenAL voices at scene load.
        // They start only when the host presents an audible nearby source.
    }
    public void menu(){
        ingame=false;apply(settings);
        if(ambience!=null)ambience.stop();
        if(combatMusic!=null)combatMusic.stop();
        if(campfireLoop!=null)campfireLoop.stop();
        combatHold=combatGain=fireGain=0;
        for(Music bed:weatherBeds)if(bed!=null)bed.stop();
        for(Music bed:industryLoops)if(bed!=null)bed.stop();
        Arrays.fill(weatherGains,0f);
        Arrays.fill(industryGain,0f);
        previousFlash=0;thunderCooldown=0;
        if(music!=null&&!music.isPlaying())music.play();
    }
    private void play(Sound sound,float gain){
        if(available&&sound!=null&&gain>0) sound.play(Math.min(1,gain));
    }
    public void click(){play(menuClick,settings.uiGain()*2.0f);}
    public void navigate(){play(cues[GpuAudioSynthesis.Cue.NAVIGATE.ordinal()],settings.uiGain()*.45f);}
    public void confirm(){play(menuConfirm,settings.uiGain()*.9f);}
    public void reject(){play(menuReject,settings.uiGain()*.40f);}
    public void panel(){play(panelOpen,settings.uiGain()*.16f);}
    public void empty(){play(cues[GpuAudioSynthesis.Cue.EMPTY.ordinal()],settings.effectsGain()*.7f);}
    /** Called only for an authoritative successful reload start. */
    public void reload(int index){
        GpuAudioSynthesis.family(index);
        play(handling[index],settings.effectsGain()*.68f);
        play(sourceReload,settings.effectsGain()*.13f);
    }
    public void reload(){reload(0);}
    public void charge(int index){
        GpuAudioSynthesis.family(index);
        play(handling[index],settings.effectsGain()*.24f);
    }
    public void equip(int index){
        GpuAudioSynthesis.family(index);
        play(handling[index],settings.effectsGain()*.34f);
    }
    public void alternate(int index){
        int family=GpuAudioSynthesis.family(index);
        play(alternate[index],settings.effectsGain()*.93f);
        combatHold=Math.max(combatHold,4f);
        play(sourceFamily[family],settings.effectsGain()*.16f);
    }
    /**
     * Do not invent weather: blend the existing host-synchronized weather
     * snapshot. Music/sfx buses are independent; weather uses ambience gain.
     * Device update is render-thread-only and rate-limited by frame delta.
     */
    public void weather(String preset,double rain,double snow,double flash,float elapsed) {
        if(!available||!ingame)return;
        double r=Math.max(0,Math.min(1,Double.isFinite(rain)?rain:0));
        double s=Math.max(0,Math.min(1,Double.isFinite(snow)?snow:0));
        String kind=preset==null?"CLEAR":preset;
        double targetWind=(kind.equals("BLIZZARD")||kind.equals("DUST_STORM")||
            kind.equals("ASHFALL")||kind.equals("THUNDERSTORM"))?.34:
            kind.equals("HEAVY_RAIN")||kind.equals("SLEET")?.21:.09;
        double[] targets={r*.50,targetWind,s*.32};
        float dt=Math.max(0,Math.min(.1f,elapsed));
        float maxStep=dt*.42f;
        for(int i=0;i<weatherBeds.length;i++){
            float want=(float)targets[i],have=weatherGains[i];
            weatherGains[i]=have+Math.max(-maxStep,Math.min(maxStep,want-have));
            Music weatherLayer=weatherBeds[i];
            if(weatherLayer!=null){
                float gain=Math.min(1f,weatherGains[i]*settings.ambienceGain());
                if(gain>.003f){
                    weatherLayer.setVolume(gain);
                    if(!weatherLayer.isPlaying())weatherLayer.play();
                }else if(weatherLayer.isPlaying())weatherLayer.stop();
            }
        }
        thunderCooldown=Math.max(0f,thunderCooldown-dt);
        float now=(float)Math.max(0,Math.min(1,Double.isFinite(flash)?flash:0));
        if(kind.equals("THUNDERSTORM")&&now>.30f&&previousFlash<=.30f&&
            thunderCooldown<=0f){
            play(thunder,settings.ambienceGain()*.48f);
            thunderCooldown=4f;
        }
        previousFlash=now;
    }
    /**
     * A sound follows a unique host-published effect ID. Direction is relative
     * to camera heading, distance attenuates, and blocked lines are damped.
     * No client-side combat or projectile collisions are invented.
     */
    public void impact(GpuCombatAudioGate.Event event,double playerX,
            double playerY,double heading,boolean clearSight){
        if(!available)return;
        double dx=event.x()-playerX,dy=event.y()-playerY;
        double distance=Math.hypot(dx,dy);
        if(distance>36)return;
        GpuAudioSynthesis.Impact sound=switch(event.kind()){
            case HIT -> GpuAudioSynthesis.Impact.FLESH;
            case IMPACT -> GpuAudioSynthesis.Impact.SOLID;
            case BOUNCE -> GpuAudioSynthesis.Impact.RICOCHET;
            case ARC -> GpuAudioSynthesis.Impact.ELECTRIC;
            case BLAST -> GpuAudioSynthesis.Impact.EXPLOSION;
        };
        float falloff=(float)(1.0/(1.0+(distance/8.0)*(distance/8.0)));
        float occlusion=clearSight?1f:.43f;
        float magnitude=(float)(sound==GpuAudioSynthesis.Impact.EXPLOSION?
            Math.min(1.15, .5 + event.radius()*.12):.7);
        float volume=settings.effectsGain()*falloff*occlusion*magnitude;
        float pan=(float)Math.max(-1,Math.min(1,
            Math.sin(Math.atan2(dy,dx)-heading)*Math.min(1,distance/3.0)));
        Sound effect=impacts[sound.ordinal()];
        if(volume>.005f&&effect!=null)effect.play(Math.min(1f,volume),1f,pan);
        if(sound==GpuAudioSynthesis.Impact.EXPLOSION)combatHold=Math.max(combatHold,3.5f);
    }
    /**
     * Threat: nearby host-reported NPCs, recent host combat, or critical HP.
     * Campfire: the nearest *real* active fire prop from world snapshot.
     * This deliberately does not pretend idle industrial devices are running.
     */
    public void situation(boolean nearbyHostile,boolean criticalHealth,
            double nearestFireDistance,float elapsed) {
        if(!available||!ingame)return;
        float dt=Math.max(0f,Math.min(.1f,elapsed));
        combatHold=Math.max(0,combatHold-dt);
        float wanted=(nearbyHostile||combatHold>0||criticalHealth)?1f:0f;
        float fade=dt*(wanted>combatGain?.52f:.23f);
        combatGain+=Math.max(-fade,Math.min(fade,wanted-combatGain));
        float fireTarget=Double.isFinite(nearestFireDistance)?
            (float)(1.0/(1.0+Math.pow(nearestFireDistance/5.0,2))):0f;
        float fireStep=dt*.45f;
        fireGain+=Math.max(-fireStep,Math.min(fireStep,fireTarget-fireGain));
        // Start/stop streams only on actual demand, preserving SFX voices.
        if(combatMusic!=null) {
            if(combatGain>.01f){
                if(!combatMusic.isPlaying())combatMusic.play();
            }else if(combatMusic.isPlaying())combatMusic.stop();
        }
        if(campfireLoop!=null){
            if(fireGain>.01f){
                if(!campfireLoop.isPlaying())campfireLoop.play();
            }else if(campfireLoop.isPlaying())campfireLoop.stop();
        }
        apply(settings);
    }
    private float machineBusGain(int index){
        return index>=GpuIndustryAudioPlanner.Channel.ENGINE_IDLE.ordinal()?
            settings.effectsGain()*.39f:settings.ambienceGain()*.28f;
    }
    /** Apply one bounded, spatial mix per operating faction and vehicle lane. */
    public void industry(java.util.EnumMap<GpuIndustryAudioPlanner.Channel,
            GpuIndustryAudioPlanner.Mix> plan,float elapsed){
        if(!available||!ingame||plan==null)return;
        float dt=Math.max(0,Math.min(.10f,elapsed));
        for(GpuIndustryAudioPlanner.Channel lane:GpuIndustryAudioPlanner.Channel.values()){
            int id=lane.ordinal();
            var desired=plan.get(lane);
            float target=desired==null?0:desired.gain();
            float max=dt*(target>industryGain[id]?.65f:.42f);
            industryGain[id]+=Math.max(-max,Math.min(max,target-industryGain[id]));
            Music loop=industryLoops[id];
            if(loop!=null){
                float volume=industryGain[id]*machineBusGain(id);
                if(volume>.003f){
                    loop.setPan(desired==null?0:desired.pan(),volume);
                    if(!loop.isPlaying())loop.play();
                }else if(loop.isPlaying())loop.stop();
            }
        }
    }
    public void deployable(GpuDeployableAudioGate.Event event,
            double playerX,double playerY,double yaw,boolean clearLine){
        if(!available||event.faction()<0||event.faction()>2)return;
        double dx=event.x()-playerX,dy=event.y()-playerY;
        double distance=Math.hypot(dx,dy);
        if(distance>32)return;
        float attenuation=(float)(1/(1+Math.pow(distance/7,2)));
        float occlusion=clearLine?1f:.43f;
        float pan=(float)Math.max(-1,Math.min(1,
            Math.sin(Math.atan2(dy,dx)-yaw)*Math.min(1,distance/3)));
        var phase=switch(event.phase()){
            case LAUNCH -> GpuAudioSynthesis.DeployablePhase.LAUNCH;
            case LAND -> GpuAudioSynthesis.DeployablePhase.LAND;
            case EXPIRE -> GpuAudioSynthesis.DeployablePhase.REMOVE;
        };
        Sound effect=fieldSounds[event.faction()][phase.ordinal()];
        float volume=settings.effectsGain()*attenuation*occlusion*.52f;
        if(effect!=null&&volume>.005f)effect.play(Math.min(1,volume),1,pan);
    }
    public void step(GpuFootstepSurface.Material surface){
        if(surface==null)return;
        Sound sound=surfaceSteps[surface.ordinal()][footstep++%2];
        play(sound,settings.effectsGain()*.32f);
    }
    public void step(){step(GpuFootstepSurface.Material.ASPHALT);}
    /** Play only events derived from observable gameplay changes, never client guesses. */
    public void worldCue(GpuWorldAudioGate.Cue event){
        switch(event){
            case INVENTORY -> play(sourceDrawer,settings.uiGain()*.32f);
            case PICKUP -> play(sourceCloth,settings.effectsGain()*.22f);
            case MONEY -> play(sourceCoins,settings.effectsGain()*.47f);
            case HURT -> play(sourceHurt,settings.effectsGain()*.52f);
            case HEAL -> play(cues[GpuAudioSynthesis.Cue.CONFIRM.ordinal()],
                settings.effectsGain()*.36f);
            case DEATH -> {
                play(sourceHurt,settings.effectsGain()*.7f);
                play(thunder,settings.effectsGain()*.19f);
            }
            case DENIED -> play(menuReject,settings.uiGain()*.29f);
            case CRAFTED -> play(menuConfirm,settings.uiGain()*.56f);
            case DEPLOYED -> play(sourceField,settings.effectsGain()*.3f);
        }
    }
    /** Legacy entry point only; world code must supply its actual arsenal index. */
    public void shoot(){shoot(0);}
    public void shoot(int arsenalIndex){
        int family=GpuAudioSynthesis.family(arsenalIndex);
        play(weapon[arsenalIndex],settings.effectsGain()*.92f);
        combatHold=Math.max(combatHold,4f);
        play(sourceFamily[family],settings.effectsGain()*.22f);
    }
    @Override public void close(){
        if(music!=null){music.dispose();music=null;}
        if(ambience!=null){ambience.dispose();ambience=null;}
        if(combatMusic!=null){combatMusic.dispose();combatMusic=null;}
        if(campfireLoop!=null){campfireLoop.dispose();campfireLoop=null;}
        for(int i=0;i<weatherBeds.length;i++){
            if(weatherBeds[i]!=null){weatherBeds[i].dispose();weatherBeds[i]=null;}
            weatherGains[i]=0;
        }
        for(int i=0;i<industryLoops.length;i++){
            if(industryLoops[i]!=null){industryLoops[i].dispose();industryLoops[i]=null;}
            industryGain[i]=0;
        }
        Set<Sound> disposed=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Sound s:cues)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:impacts)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound[] bank:fieldSounds)
            for(Sound s:bank)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound[] group:surfaceSteps)
            for(Sound s:group)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:weapon)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:alternate)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:handling)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:new Sound[]{thunder,sourceReload,sourceCoins,sourceDrawer,
                sourceCloth,sourceHurt,sourceField})
            if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:sourceFamily)if(s!=null&&disposed.add(s))s.dispose();
        for(Sound s:new Sound[]{menuClick,menuConfirm,menuReject,panelOpen})
            if(s!=null&&disposed.add(s))s.dispose();
        Arrays.fill(cues,null);Arrays.fill(impacts,null);Arrays.fill(weapon,null);
        for(Sound[] group:surfaceSteps)Arrays.fill(group,null);
        for(Sound[] bank:fieldSounds)Arrays.fill(bank,null);
        Arrays.fill(alternate,null);
        Arrays.fill(handling,null);Arrays.fill(sourceFamily,null);
        thunder=sourceReload=sourceCoins=sourceDrawer=sourceCloth=sourceHurt=sourceField=null;
        menuClick=menuConfirm=menuReject=panelOpen=null;
        available=false;
        for(Path path:temporary)try{Files.deleteIfExists(path);}catch(IOException ignored){}
        temporary.clear();
    }
    public static void main(String[] args)throws IOException{
        byte[] music=verifiedMusic();
        if(music.length!=ORIGINAL_BYTES||music[0]!='I'||music[1]!='D'||
           music[2]!='3')throw new IOException("Expected full original ID3 MP3 source");
        System.out.println("PASS GPU_DRIVE_MUSIC_INTEGRITY sha256="+ORIGINAL_SHA+
            " bytes="+music.length+" parts="+PARTS);
    }

}
