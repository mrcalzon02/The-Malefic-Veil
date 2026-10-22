package veil;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Offline updater integrity, extraction and traversal protection regression. */
public final class DesktopUpdaterTest {
    private static void yes(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static byte[] zip(String name,byte[] value)throws IOException{
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(ZipOutputStream output=new ZipOutputStream(buffer)){
            output.putNextEntry(new ZipEntry(name));
            output.write(value);
            output.closeEntry();
        }
        return buffer.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        Path target=Files.createTempDirectory("malefic-update-test-");
        try{
            byte[] contents="known verified build".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String expected=DesktopUpdater.sha256(contents);
            yes(expected.length()==64,"SHA-256 output size");
            DesktopUpdater.installZip(zip("MaleficVeil.jar",contents),target);
            yes(java.util.Arrays.equals(contents,Files.readAllBytes(
                target.resolve("MaleficVeil.jar"))),"Payload not extracted intact");
            boolean denied=false;
            try{DesktopUpdater.installZip(zip("../outside.dat",contents),target);}
            catch(IOException rejected){denied=true;}
            yes(denied,"Archive traversal accepted");
            yes(!Files.exists(target.resolve("../outside.dat").normalize()),
                "Archive traversal wrote outside installation");
            System.out.println("PASS DesktopUpdaterTest");
        }finally{
            try(var children=Files.list(target)){
                children.forEach(p->{try{Files.deleteIfExists(p);}
                    catch(IOException e){throw new UncheckedIOException(e);}});
            }
            Files.deleteIfExists(target);
        }
    }
}
