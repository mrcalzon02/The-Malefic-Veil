package veil;
import java.util.*;

/** Bounded additional messages; compatibility commands remain in the existing protocol. */
public final class ArsenalWire {
    static String ints(int[] a){StringJoiner j=new StringJoiner(",");for(int n:a)j.add(""+n);return j.toString();}
    static int[] ints(String text,int count){String[] a=text.split(",");if(a.length!=count)throw new IllegalArgumentException("Arsenal vector size");int[] out=new int[count];for(int i=0;i<count;i++){out[i]=Integer.parseInt(a[i]);if(out[i]<0||out[i]>1_000_000_000)throw new IllegalArgumentException("Arsenal vector range");}return out;}
    public static String state(ArsenalSystems.View v,long now){return "ARS_STATE|"+v.owned()+"|"+v.selected()+"|"+v.faction()+"|"+v.factionLocked()+"|"+ints(v.clip())+"|"+ints(v.reserve())+"|"+ints(v.materials())+"|"+v.action()+"|"+Math.max(0,now-v.actionAt())+"|"+v.reloadRemaining()+"|"+v.chargeRemaining()+"|"+v.heat()+"|"+v.benchTier()+"|"+v.researched();}
    public static ArsenalSystems.View state(String[] a){
        if(a.length!=15)throw new IllegalArgumentException("Arsenal state length");int selected=Integer.parseInt(a[2]);ArsenalCatalog.get(selected);
        return new ArsenalSystems.View(Long.parseLong(a[1]),selected,Integer.parseInt(a[3]),Boolean.parseBoolean(a[4]),ints(a[5],32),ints(a[6],32),ints(a[7],9),Integer.parseInt(a[8]),System.nanoTime()/1_000_000-Long.parseLong(a[9]),Long.parseLong(a[10]),Long.parseLong(a[11]),Double.parseDouble(a[12]),Integer.parseInt(a[13]),Long.parseLong(a[14]));
    }
    public static String effects(List<ArsenalSystems.Effect> effects){StringJoiner j=new StringJoiner(";");for(var e:effects)j.add(e.id()+","+e.kind()+","+e.x()+","+e.y()+","+e.tx()+","+e.ty()+","+e.size()+","+e.color());return "ARS_FX|"+j;}
    public static List<ArsenalSystems.Effect> effects(String text){
        if(text.isEmpty())return List.of();String[] entries=text.split(";");if(entries.length>512)throw new IllegalArgumentException("Effect cap");List<ArsenalSystems.Effect> out=new ArrayList<>();
        for(String e:entries){String[] f=e.split(",");if(f.length!=8)throw new IllegalArgumentException("Effect format");out.add(new ArsenalSystems.Effect(Integer.parseInt(f[0]),f[1],Double.parseDouble(f[2]),Double.parseDouble(f[3]),Double.parseDouble(f[4]),Double.parseDouble(f[5]),Double.parseDouble(f[6]),Integer.parseInt(f[7]),System.nanoTime()/1_000_000+200));}return List.copyOf(out);
    }
}
