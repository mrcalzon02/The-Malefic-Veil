package veil.gpu;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Read-only, source-GLB-derived inventory and menu artwork; never gameplay authority. */
public final class GpuItemPortraits implements AutoCloseable {
    private final Map<String,Texture> loaded=new HashMap<>();
    private final Set<String> missing=new HashSet<>();

    private static String path(String id){
        if(id==null)return null;
        if(id.matches("w(0[1-9]|[12][0-9]|3[0-2])-[a-z0-9-]+"))
            return "/portraits/weapons/"+id+".png";
        if(id.matches("(unraveled|penitent|begotten)_[a-z0-9_]+"))
            return "/portraits/aircraft/"+id+".png";
        return null;
    }
    public boolean contains(String id){
        String resource=path(id);
        return resource!=null&&GpuItemPortraits.class.getResource(resource)!=null;
    }
    private synchronized Texture icon(String id){
        Texture found=loaded.get(id);
        if(found!=null)return found;
        if(missing.contains(id))return null;
        String resource=path(id);
        if(resource==null){if(id!=null)missing.add(id);return null;}
        try(InputStream stream=GpuItemPortraits.class.getResourceAsStream(resource)){
            if(stream==null){missing.add(id);return null;}
            ByteArrayOutputStream png=new ByteArrayOutputStream();
            stream.transferTo(png);
            byte[] bytes=png.toByteArray();
            if(bytes.length>1000000)throw new IOException("Oversize portrait "+id);
            Pixmap image=new Pixmap(bytes,0,bytes.length);
            try{
                if(image.getWidth()!=256||image.getHeight()!=256)
                    throw new IOException("Unexpected portrait size "+id);
                Texture texture=new Texture(image);
                texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
                loaded.put(id,texture);
                return texture;
            }finally{image.dispose();}
        }catch(IOException failure){
            throw new IllegalStateException("Invalid model portrait "+id,failure);
        }
    }
    public boolean draw(SpriteBatch sprites,String id,float x,float y,float size){
        Texture texture=icon(id);
        if(texture==null)return false;
        sprites.draw(texture,x,y,size,size);
        return true;
    }
    @Override public synchronized void close(){
        for(Texture texture:loaded.values())texture.dispose();
        loaded.clear();missing.clear();
    }
}
