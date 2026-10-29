package veil;
import java.util.Locale;

/** Faction finishes preserve authored triangle shading and moving geometry. */
public final class FactionWeaponMaterials {
    private FactionWeaponMaterials(){}
    public static int recolor(int weapon,String part,int group,int original){
        int faction=ArsenalCatalog.get(weapon).faction();
        if(faction<0)return original&0xffffff;
        String role=part==null?"":part.toLowerCase(Locale.ROOT);
        boolean glow=role.matches(".*(core|coil|emiss|energy|orb|reactor|cell|beam|crystal|glow|prong|ring).*");
        boolean grip=role.matches(".*(grip|stock|handle|trigger|magazine|belt|strap|guard|rubber).*");
        boolean trim=role.matches(".*(vent|muzzle|sight|rail|barrel|ornament|sigil|plate|hinge|bolt).*");
        int[][] palette={
            {0x515A62,0x438F8A,0xBE985C,0x282D35},
            {0x443E43,0xB59A60,0xA94750,0xB9ADA0},
            {0x574456,0x8C6BAA,0x65C8A9,0x342B37}
        };
        int[] set=palette[faction];
        int base=grip?set[3]:glow?set[2]:trim||group==1?set[1]:set[0];
        int r=(original>>16)&255,g=(original>>8)&255,b=original&255;
        double lum=(.2126*r+.7152*g+.0722*b)/255.0;
        double shade=.51+lum*.90;
        return (channel(base>>16,shade,r)<<16)|
               (channel(base>>8,shade,g)<<8)|
                channel(base,shade,b);
    }
    private static int channel(int tint,double shade,int source){
        int value=(int)Math.round(((tint&255)*.84+source*.16)*shade);
        return Math.max(0,Math.min(255,value));
    }
}
