package veil.gpu;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Proof that gameplay uses the original free heyheythere intake, not substitutes. */
public final class PsxHorrorUiSource {
    public static final String RESOURCE="/psx-ui/psx-horror-ui-free-assets.zip";
    public static final String FOG="psx-horror-ui-free/addons/psx_horror_ui/themes/fog/";
    public static final String CREDIT="PSX Horror UI Free by heyheythere - "+
        "https://heyheythere.itch.io/psx-horror-ui-free - CC BY 4.0";
    public static final String SHA256=
        "47193ece2bcc9b5f8193dfee6ff064e291cbbc0ae1582104a1d1fa8132535350";
    private PsxHorrorUiSource(){}
    public static Path verifiedCopy()throws IOException{
        Path archive=null;
        try(InputStream resource=PsxHorrorUiSource.class.getResourceAsStream(RESOURCE)){
            if(resource==null)throw new IOException("Missing original PSX Horror UI Free intake "+RESOURCE);
            archive=Files.createTempFile("veil-psx-horror-source-", ".zip");
            Files.copy(resource,archive,StandardCopyOption.REPLACE_EXISTING);
            byte[] actual;
            try(InputStream content=Files.newInputStream(archive)){
                actual=MessageDigest.getInstance("SHA-256").digest(content.readAllBytes());
            }catch(java.security.NoSuchAlgorithmException impossible){
                throw new IllegalStateException(impossible);
            }
            StringBuilder hex=new StringBuilder(64);
            for(byte b:actual)hex.append(String.format("%02x",b&255));
            if(!SHA256.equals(hex.toString()))
                throw new IOException("PSX Horror UI Free source checksum mismatch: "+hex);
            try(ZipFile zip=new ZipFile(archive.toFile())){
                int pngs=0;
                for(var entries=zip.entries();entries.hasMoreElements();){
                    var entry=entries.nextElement();
                    if(entry.getName().startsWith(FOG)&&entry.getName().endsWith(".png"))
                        pngs++;
                }
                if(pngs!=53)throw new IOException("Expected 53 licensed fog sprites, got "+pngs);
                var license=zip.getEntry("psx-horror-ui-free/LICENSE.txt");
                if(license==null)throw new IOException("Original heyheythere license missing");
                try(InputStream in=zip.getInputStream(license)){
                    String terms=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                    if(!terms.contains(CREDIT)||!terms.contains("Attribution 4.0"))
                        throw new IOException("PSX free-pack attribution/license does not match");
                }
                for(String item:new String[]{"window","panel","slot","slot_selected",
                        "progress_bg","progress_fill","map_room_current","mark_you",
                        "button_normal","button_hover","menu_band"}){
                    if(zip.getEntry(FOG+item+".png")==null)
                        throw new IOException("Missing original source sprite "+item);
                }
                if(zip.getEntry("psx-horror-ui-free/addons/psx_horror_ui/font/psx_horror_ui_font.fnt")==null &&
                   zip.stream().noneMatch(e->e.getName().endsWith("/font/psx_horror_ui_font.fnt")))
                    throw new IOException("Original bitmap font missing");
            }
            return archive;
        }catch(Exception problem){
            if(archive!=null)Files.deleteIfExists(archive);
            if(problem instanceof IOException io)throw io;
            throw new IOException("Corrupt original PSX Horror UI intake",problem);
        }
    }
    public static void main(String[] args)throws IOException{
        Path path=verifiedCopy();
        try{System.out.println("PASS PSX_HORROR_UI_SOURCE sha256="+SHA256+
            " fogSprites=53 source=heyheythere CC-BY-4.0");}
        finally{Files.deleteIfExists(path);}
    }
}
