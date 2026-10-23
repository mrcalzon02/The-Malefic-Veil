package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Native libGDX sound bus. User-owned source music is not re-encoded:
 * eight Git-backed binary resource pieces reconstruct exactly the approved
 * 3,558,384-byte original MP3, verified by SHA-256 before playback.
 */
public final class GpuAudioBus implements AutoCloseable {
    private static final int PARTS=8;
    public static final long ORIGINAL_BYTES=3558384;
    public static final String ORIGINAL_SHA="0e0b0e5bbad299d733938f907aa63fd62de9abad3dc61a00a8ee287fbe10a293";
    private Music music;
    private Sound menuTick,gunshot;
    private Path trackPath,uiPath,shotPath;
    private GpuClientSettings settings;
    private boolean ingame;
    private boolean available;
    public GpuAudioBus(GpuClientSettings settings){
        this.settings=Objects.requireNonNull(settings);
    }
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
    /** Small neutral UI click and gun report: real audible SFX routed by category. */
    private static byte[] synth(boolean shot){
        int rate=22050,samples=shot?rate/6:rate/20;
        ByteBuffer bytes=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(new byte[]{'R','I','F','F'});bytes.putInt(36+samples*2);
        bytes.put(new byte[]{'W','A','V','E','f','m','t',' '});bytes.putInt(16);
        bytes.putShort((short)1);bytes.putShort((short)1);
        bytes.putInt(rate);bytes.putInt(rate*2);bytes.putShort((short)2);bytes.putShort((short)16);
        bytes.put(new byte[]{'d','a','t','a'});bytes.putInt(samples*2);
        Random noise=new Random(shot?83:7);
        for(int i=0;i<samples;i++){
            double phase=i/(double)rate,envelope=Math.pow(1-i/(double)samples,shot?2:3);
            double signal=shot?(noise.nextDouble()*2-1)*envelope:
                Math.sin(Math.PI*2*(970-540*phase)*phase)*envelope;
            bytes.putShort((short)(signal*(shot?10000:4400)));
        }
        return bytes.array();
    }
    public void open(){
        if(available)return;
        try{
            byte[] original=verifiedMusic();
            trackPath=Files.createTempFile("malefic-menu-music-", ".mp3");
            Files.write(trackPath,original);
            music=Gdx.audio.newMusic(new FileHandle(trackPath.toFile()));
            music.setLooping(true);
            uiPath=Files.createTempFile("malefic-ui-click-", ".wav");
            shotPath=Files.createTempFile("malefic-gun-sfx-", ".wav");
            Files.write(uiPath,synth(false));Files.write(shotPath,synth(true));
            menuTick=Gdx.audio.newSound(new FileHandle(uiPath.toFile()));
            gunshot=Gdx.audio.newSound(new FileHandle(shotPath.toFile()));
            available=true;
            apply(settings);
            music.play();
            System.out.println("AUDIO_MENU_READY originalDriveMusicSha="+ORIGINAL_SHA+
                " bytes="+original.length+" categories=music,ui,effects,ambience");
        }catch(Exception failed){
            // Headless Mesa/CI may have no ALSA device. A missing physical
            // sound card never blocks game launch. Verified music test is separate.
            System.err.println("AUDIO_DEVICE_UNAVAILABLE "+failed);
            close();
        }
    }
    public void apply(GpuClientSettings fresh){
        settings=Objects.requireNonNull(fresh);
        if(music!=null)
            music.setVolume(ingame?settings.ambienceGain()*.17f:settings.musicGain());
    }
    public void inGame(){
        ingame=true;apply(settings);
        if(music!=null&&!music.isPlaying())music.play();
    }
    public void menu(){ingame=false;apply(settings);}
    public void click(){
        if(available&&menuTick!=null&&settings.uiGain()>0)
            menuTick.play(settings.uiGain());
    }
    public void shoot(){
        if(available&&gunshot!=null&&settings.effectsGain()>0)
            gunshot.play(settings.effectsGain());
    }
    @Override public void close(){
        if(music!=null){music.dispose();music=null;}
        if(menuTick!=null){menuTick.dispose();menuTick=null;}
        if(gunshot!=null){gunshot.dispose();gunshot=null;}
        available=false;
        for(Path path:new Path[]{trackPath,uiPath,shotPath}){
            if(path!=null)try{Files.deleteIfExists(path);}catch(IOException ignored){}
        }
        trackPath=uiPath=shotPath=null;
    }
    public static void main(String[] args)throws IOException{
        byte[] music=verifiedMusic();
        if(music.length!=ORIGINAL_BYTES||music[0]!='I'||music[1]!='D'||
           music[2]!='3')throw new IOException("Expected full original ID3 MP3 source");
        System.out.println("PASS GPU_DRIVE_MUSIC_INTEGRITY sha256="+ORIGINAL_SHA+
            " bytes="+music.length+" parts="+PARTS);
    }
}
