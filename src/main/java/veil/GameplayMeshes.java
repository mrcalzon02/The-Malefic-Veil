package veil;

import java.util.*;
import java.awt.geom.*;

/**
 * Authored, redistributable low-poly *interim* gameplay geometry.
 * These are original project meshes, NOT copies/conversions of the
 * Characters_psx_1.2 FBX archive (license + conversion pending).
 * All world-space shapes enter the very same shaded triangle renderer as CC0 urban GLBs.
 */
public final class GameplayMeshes {
    public static final String[] HERO={"hero-warden","hero-medic","hero-shade","hero-occultist"};
    public static final String[] PICKUP={"pickup-health","pickup-sanity","pickup-pistol-ammo",
        "pickup-shells","pickup-cash","pickup-supplies","pickup-medkit"};
    public static final String[] DEVICE={"device-turret","device-healing","device-revival","device-barricade","device-field-camp","device-field-fire","device-field-workbench","device-field-storage",
        "facility-unraveled-scrap-outpost","facility-penitent-votive-altar",
        "facility-begotten-brood-nest",
        "facility-claim-heart","facility-hand-workshop","facility-dry-store",
        "facility-cooking-hearth","facility-rain-catcher","facility-cargo-laydown",
        "facility-forager-garden","facility-compost-yard","facility-herb-plot",
        "facility-charcoal-kiln","facility-stone-furnace","facility-forge",
        "facility-freight-bay","facility-materials-recycler",
        "facility-unraveled-machinist-bay","facility-penitent-reliquary",
        "facility-begotten-spore-basin",
        "facility-steam-boiler","facility-mechanical-shaft","facility-metal-press",
        "facility-unraveled-pressure-shop","facility-penitent-cantor-dais",
        "facility-begotten-chitin-vat",
        "facility-generator","facility-electric-bus","facility-precision-lathe",
        "facility-water-works","facility-unraveled-sensor-lab",
        "facility-penitent-choir-relay","facility-begotten-membrane-lab",
        "facility-conveyor-line","facility-automatic-sorter",
        "facility-industrial-workshop","facility-assembly-line",
        "facility-command-hall","facility-research-archive",
        "facility-signal-relay","facility-security-grid",
        "facility-fuel-refinery","facility-terror-collector",
        "facility-nutrient-digestor","facility-unraveled-defense-works",
        "facility-penitent-ward-sanctum","facility-begotten-organ-lattice",
        "facility-unraveled-resilience-grid"};
    public static final String[] WEAPON={"weapon-handgun","weapon-shotgun"};
    private final Map<String,GlbMesh> meshes=new LinkedHashMap<>();
    private static final class Model {
        final List<GlbMesh.Triangle> tris=new ArrayList<>();
        void tri(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,int c){
            tris.add(new GlbMesh.Triangle(ax,ay,az,bx,by,bz,cx,cy,cz,c));
        }
        void rect(float[] a,float[] b,float[] c,float[] d,int color){
            tri(a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],color);
            tri(a[0],a[1],a[2],c[0],c[1],c[2],d[0],d[1],d[2],color);
        }
        void box(double x,double y,double z,double sx,double sy,double sz,int c){
            float left=(float)(x-sx/2),right=(float)(x+sx/2);
            float low=(float)(y-sy/2),high=(float)(y+sy/2);
            float front=(float)(z-sz/2),back=(float)(z+sz/2);
            float[] lbf={left,low,front},rbf={right,low,front},rtf={right,high,front},ltf={left,high,front};
            float[] lbb={left,low,back},rbb={right,low,back},rtb={right,high,back},ltb={left,high,back};
            rect(lbf,rbf,rtf,ltf,dim(c,.88));rect(rbb,lbb,ltb,rtb,dim(c,.68));
            rect(lbb,lbf,ltf,ltb,dim(c,.73));rect(rbf,rbb,rtb,rtf,dim(c,.83));
            rect(ltf,rtf,rtb,ltb,c);rect(lbb,rbb,rbf,lbf,dim(c,.50));
        }
        void diamond(double x,double y,double z,double radius,double height,int color){
            float X=(float)x,Y=(float)y,Z=(float)z,r=(float)radius,h=(float)(height/2);
            float[][] ring={{X-r,Y,Z},{X,Y,Z-r},{X+r,Y,Z},{X,Y,Z+r}};
            for(int i=0;i<4;i++){
                float[] a=ring[i],b=ring[(i+1)%4];
                tri(X,Y+h,Z,a[0],a[1],a[2],b[0],b[1],b[2],dim(color,1-.08*i));
                tri(X,Y-h,Z,b[0],b[1],b[2],a[0],a[1],a[2],dim(color,.65));
            }
        }
        void barrel(double x,double y,double z,double radius,double length,int c){
            // Eight-sided low-poly cylinder, its axis points along world Z.
            float near=(float)(z-length/2),far=(float)(z+length/2);
            for(int i=0;i<8;i++){
                double angle=i*Math.PI/4,next=(i+1)*Math.PI/4;
                float xa=(float)(x+radius*Math.cos(angle)),xb=(float)(x+radius*Math.cos(next));
                float ya=(float)(y+radius*Math.sin(angle)),yb=(float)(y+radius*Math.sin(next));
                rect(new float[]{xa,ya,near},new float[]{xb,yb,near},
                    new float[]{xb,yb,far},new float[]{xa,ya,far},dim(c,.64+.035*i));
                tri((float)x,(float)y,near,xb,yb,near,xa,ya,near,dim(c,.32));
            }
        }
        GlbMesh done(){return GlbMesh.authored(tris);}
    }
    private static int dim(int rgb,double f){
        return (Math.min(255,(int)(((rgb>>>16)&255)*f))<<16)|
            (Math.min(255,(int)(((rgb>>>8)&255)*f))<<8)|
            Math.min(255,(int)((rgb&255)*f));
    }
    public GameplayMeshes(){
        int[] coats={0x536D8C,0xD0C5AE,0x343B55,0x5B3D73};
        int[] trims={0xC2AD59,0x5DA993,0x806BA8,0xA95F9A};
        for(int i=0;i<4;i++){
            Model p=new Model();int coat=coats[i],trim=trims[i];
            p.box(-.15,.35,0,.20,.70,.25,dim(coat,.55));
            p.box(.15,.35,0,.20,.70,.25,dim(coat,.52));
            p.box(0,1.08,0,.60,.81,.32,coat);
            p.box(-.40,1.05,0,.17,.70,.22,dim(coat,.78));
            p.box(.40,1.05,0,.17,.70,.22,dim(coat,.78));
            p.box(0,1.66,0,.39,.37,.36,0x9C8170);
            p.box(0,1.86,0,.42,.12,.38,dim(coat,.57));
            p.box(0,1.37,-.17,.40,.13,.03,trim);
            p.box(0,.72,.16,.45,.15,.12,dim(trim,.68));
            if(i==0){ // Warden: heavy shoulder armor and hard helmet
                p.box(-.40,1.46,0,.30,.23,.35,0x737D87);
                p.box(.40,1.46,0,.30,.23,.35,0x737D87);
                p.box(0,1.84,-.20,.48,.08,.11,0xD0BC6F);
            }else if(i==1){ // Medic: coat, cross, satchel
                p.box(0,.92,-.19,.22,.22,.035,0xBC4D52);
                p.box(0,.92,-.21,.07,.36,.035,0xBC4D52);
                p.box(.40,.87,.15,.19,.34,.27,0x576A54);
            }else if(i==2){ // Shade: dark hood and pale narrow visor
                p.box(0,1.68,-.23,.43,.27,.15,0x27273B);
                p.box(0,1.67,-.31,.26,.045,.025,0x8392A9);
            }else{ // Occultist: mantle and occult gem
                p.box(0,1.36,.18,.72,.19,.10,0x281A40);
                p.diamond(0,1.19,-.21,.11,.23,0xA858D2);
            }
            meshes.put(HERO[i],p.done());
        }
        Model enemy=new Model();
        enemy.box(0,.91,0,.55,.94,.34,0x765154);
        enemy.box(-.16,.27,0,.16,.54,.20,0x4C3D42);
        enemy.box(.16,.27,0,.16,.54,.20,0x4C3D42);
        enemy.box(-.40,1.08,-.03,.16,.65,.18,0x7B5351);
        enemy.box(.40,1.08,-.03,.16,.65,.18,0x7B5351);
        enemy.box(0,1.62,-.035,.38,.40,.39,0xA58C7F);
        enemy.box(-.10,1.67,-.238,.08,.05,.02,0xEA3B42);
        enemy.box(.10,1.67,-.238,.08,.05,.02,0xEA3B42);
        meshes.put("enemy-walker",enemy.done());

        Model handgun=new Model();
        handgun.box(0,.13,0,.23,.17,.40,0x55575C);
        handgun.box(0,.20,-.22,.20,.16,.35,0x777D80);
        handgun.box(0,-.04,.14,.14,.35,.15,0x373137);
        handgun.box(0,.13,-.42,.08,.05,.10,0xA9A7A0);
        meshes.put(WEAPON[0],handgun.done());

        Model shotgun=new Model();
        shotgun.box(0,.06,.20,.17,.22,.75,0x6A4533);
        shotgun.box(0,.18,-.17,.22,.15,.55,0x4F5559);
        shotgun.barrel(0,.23,-.60,.087,.73,0x636970);
        shotgun.box(0,-.07,.37,.14,.21,.22,0x45382D);
        meshes.put(WEAPON[1],shotgun.done());

        Model hp=new Model();hp.box(0,.25,0,.46,.47,.28,0xCFD2C5);
        hp.box(0,.26,-.155,.12,.35,.024,0xB54947);
        hp.box(0,.26,-.18,.34,.12,.024,0xB54947);
        hp.box(0,.53,0,.30,.10,.13,0x6B7471);
        meshes.put(PICKUP[0],hp.done());
        Model sanity=new Model();sanity.diamond(0,.34,0,.31,.68,0x8496E7);
        sanity.box(0,.06,0,.33,.09,.33,0x3B3B65);meshes.put(PICKUP[1],sanity.done());
        Model ammo=new Model();ammo.box(0,.18,0,.46,.28,.32,0x777855);
        for(int i=0;i<4;i++)ammo.barrel(-.15+i*.10,.35,-.04,.039,.15,0xB99A53);
        meshes.put(PICKUP[2],ammo.done());
        Model shells=new Model();shells.box(0,.11,0,.48,.21,.35,0x876442);
        for(int i=0;i<4;i++)shells.barrel(-.15+i*.10,.25,-.03,.045,.13,0xC33C33);
        meshes.put(PICKUP[3],shells.done());
        Model cash=new Model();cash.box(0,.12,0,.58,.16,.38,0xA09253);
        cash.box(0,.23,0,.51,.07,.32,0xD8C578);meshes.put(PICKUP[4],cash.done());
        Model supplies=new Model();supplies.box(0,.22,0,.48,.43,.45,0x936F48);
        supplies.box(0,.22,-.235,.38,.07,.025,0xD7B770);
        supplies.box(0,.22,.235,.38,.07,.025,0xD7B770);
        meshes.put(PICKUP[5],supplies.done());
        Model kit=new Model();kit.box(0,.25,0,.45,.38,.30,0xADAD9E);
        kit.box(0,.30,-.17,.36,.10,.023,0xDC4C54);
        kit.box(0,.30,-.19,.10,.28,.023,0xDC4C54);
        kit.box(0,.48,0,.22,.10,.14,0x696D64);
        meshes.put(PICKUP[6],kit.done());

        Model turret=new Model();
        turret.box(0,.22,0,.60,.45,.60,0x53636E);
        turret.box(0,.52,0,.47,.21,.42,0x7F8589);
        turret.barrel(-.11,.56,-.37,.068,.49,0xA0A1A2);
        turret.barrel(.11,.56,-.37,.068,.49,0xA0A1A2);
        meshes.put(DEVICE[0],turret.done());
        Model healing=new Model();
        healing.box(0,.30,0,.62,.58,.50,0x728D83);
        healing.box(0,.64,0,.38,.13,.39,0xBED7B7);
        healing.box(0,.35,-.27,.10,.31,.02,0xDDEDE0);
        healing.box(0,.35,-.29,.31,.10,.02,0xDDEDE0);
        meshes.put(DEVICE[1],healing.done());
        Model revive=new Model();
        revive.box(0,.15,0,.80,.28,.72,0x604F83);
        revive.box(-.29,.55,0,.14,.78,.16,0x8C72B8);
        revive.box(.29,.55,0,.14,.78,.16,0x8C72B8);
        revive.diamond(0,1.02,0,.30,.35,0xB68AEE);
        meshes.put(DEVICE[2],revive.done());
        Model barricade=new Model();
        barricade.box(0,.53,0,1.35,1.04,.18,0x847E70);
        barricade.box(0,.53,-.11,1.17,.18,.04,0xC6A963);
        barricade.box(0,.18,.12,1.44,.12,.24,0x5A6068);
        meshes.put(DEVICE[3],barricade.done());
        // Crude, constructed field camp / claim stake. Actual 3D canvas tarp,
        // timber supports, ground stakes and a supply crate—not a UI marker.
        Model camp=new Model();
        camp.box(0,.04,0,2.30,.08,1.80,0x564934);
        for(int side=-1;side<=1;side+=2){
            camp.box(side*.97,.48,.60,.10,.95,.12,0x6B5238);
            camp.box(side*.97,.48,-.60,.10,.95,.12,0x6B5238);
            camp.box(side*.97,1.00,0,.11,.09,1.54,0x8A714B);
            for(int sign=-1;sign<=1;sign+=2)
                camp.tri(0,1.48f,0,side*1.04f,.07f,sign*.89f,
                    0,.09f,sign*.90f,sign>0?0x7B6950:0x625545);
        }
        camp.box(0,1.49,0,.13,.13,1.97,0x635039);
        camp.box(.81,.22,-.48,.48,.37,.44,0x876443);
        camp.box(.81,.27,-.71,.40,.09,.06,0xB19668);
        camp.box(-1.24,.75,0,.14,1.50,.14,0x725A3F);
        camp.box(-1.24,1.42,.15,.39,.19,.08,0x957349);
        meshes.put(DEVICE[4],camp.done());
        Model fieldFire=new Model();
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;
            fieldFire.box(Math.cos(a)*.40,.115,Math.sin(a)*.40,.23,.23,.20,0x77716B);
        }
        fieldFire.box(-.12,.20,0,.63,.14,.15,0x62432D);
        fieldFire.box(.12,.21,0,.16,.14,.60,0x715038);
        fieldFire.diamond(0,.46,0,.20,.67,0xE9A23C);
        fieldFire.diamond(.07,.42,-.04,.11,.43,0xF3CB65);
        meshes.put(DEVICE[5],fieldFire.done());
        // Real deployable E0 workstation; one original low-poly authored
        // solid mesh with tabletop, braced legs, tool roll and clamp.
        Model bench=new Model();
        bench.box(0,.70,0,1.55,.16,.75,0x8C6C49);
        bench.box(0,.80,0,1.62,.055,.80,0xB49266);
        for(int side=-1;side<=1;side+=2)
            for(int back=-1;back<=1;back+=2)
                bench.box(side*.61,.37,back*.24,.13,.72,.15,0x614632);
        bench.box(0,.27,0,1.30,.12,.52,0x564832);
        bench.box(-.41,.90,-.08,.22,.18,.27,0x54636C);
        bench.box(.33,.90,.10,.38,.06,.16,0x857F72);
        bench.box(.55,.91,-.18,.13,.20,.09,0xB9AB89);
        meshes.put(DEVICE[6],bench.done());
        Model storage=new Model();
        storage.box(0,.12,0,1.4,.2,1.05,0x59422F);
        storage.box(0,.62,0,1.4,.8,1.05,0x816040);
        storage.box(0,1.08,0,1.5,.14,1.14,0xA5855E);
        for(int side=-1;side<=1;side+=2){
            storage.box(side*.63,.62,0,.10,.85,1.12,0x4D4D49);
            storage.box(0,.62,side*.47,1.48,.85,.08,0x66645A);
        }
        storage.box(0,1.18,0,1.1,.05,.08,0x483928);
        meshes.put(DEVICE[7],storage.done());

        // Permanent faction industrial entry sites, not thrown kits.
        // Unraveled: mechanical sorting/ram/rails and engine enclosure.
        Model scrapSite=new Model();
        scrapSite.box(0,.16,0,2.35,.32,1.76,0x565C60);
        scrapSite.box(-.65,.73,.12,.85,.95,.99,0x566879);
        scrapSite.box(.46,.60,-.15,.85,.70,.84,0x786D5A);
        scrapSite.box(0,1.05,-.55,2.10,.13,.27,0xD29D51);
        scrapSite.barrel(-.76,1.29,.14,.17,.67,0x6B767D);
        scrapSite.box(.45,1.30,0,.21,.67,.21,0xB99C4C);
        scrapSite.box(.45,1.60,0,.91,.18,.78,0x4B4F58);
        meshes.put(DEVICE[8],scrapSite.done());

        // Penitent: standing votive stone, scroll plinth, suspended censer
        // and distinct ritual pillars; no industrial conveyor meshes.
        Model altar=new Model();
        altar.box(0,.18,0,2.32,.36,1.95,0x4A434A);
        altar.box(0,.75,0,1.23,.67,.93,0x76646D);
        altar.box(0,1.13,0,1.46,.12,1.10,0xB99A76);
        altar.diamond(0,1.30,0,.21,.48,0xAC6B78);
        for(int side=-1;side<=1;side+=2){
            altar.box(side*.98,1.08,.21,.28,1.73,.28,0x70616B);
            altar.box(side*.98,2.01,.21,.47,.15,.46,0xC2A477);
            altar.diamond(side*.98,2.18,.21,.16,.31,0xCFAD67);
        }
        meshes.put(DEVICE[9],altar.done());

        // Begotten: raised wet organic nest, spore chamber and living ribs;
        // organic pulsing tissue is not a reskinned mechanical bench.
        Model brood=new Model();
        brood.box(0,.20,0,2.34,.32,1.89,0x4C393D);
        brood.box(0,.65,0,1.78,.67,1.24,0x75444B);
        brood.diamond(0,1.14,0,.61,.83,0xA75E68);
        for(int i=0;i<7;i++){
            double t=i*Math.PI*2/7;
            brood.barrel(Math.cos(t)*.74,.66,Math.sin(t)*.47,.12,.61,0xB08079);
        }
        brood.diamond(-.56,.93,-.29,.22,.44,0x8B707B);
        brood.diamond(.49,.99,.25,.19,.50,0x9D7476);
        meshes.put(DEVICE[10],brood.done());


        // Permanent common wilderness infrastructure. These are actual
        // triangulated PSX world models with differing physical silhouettes,
        // never field throwables or emoji/sprite substitutes.
        for(int index=11;index<=24;index++){
            Model site=new Model();
            int timber=0x765b3c,stone=0x706f69,metal=0x5c6670;
            site.box(0,.14,0,2.18,.24,1.78,index>=20?stone:timber);
            switch(index){
                case 11 -> { // claim heart, ownership hub with 3 standing posts
                    site.box(0,.90,0,.29,1.55,.29,timber);
                    site.box(0,1.68,0,.85,.19,.78,0x99845A);
                    for(int side:new int[]{-1,1})
                        site.box(side*.87,.65,0,.17,1.17,.19,0x877653);
                    site.diamond(0,1.91,0,.29,.49,0xBEA673);
                }
                case 12 -> { // hand workshop, framing table, saw and tool rack
                    site.box(0,.82,0,1.72,.17,.92,0xA58B59);
                    for(int sign:new int[]{-1,1})
                        site.box(sign*.73,.43,0,.12,.78,.81,timber);
                    site.box(-.51,1.00,0,.15,.17,.39,metal);
                    site.box(.41,1.03,.06,.57,.06,.09,0xBBB2A0);
                }
                case 13 -> { // storage building: back wall, high shelf, boxes
                    site.box(0,.80,.60,2.02,1.32,.19,0x746344);
                    for(int sign:new int[]{-1,1})
                        site.box(sign*.83,.73,0,.16,1.34,1.65,timber);
                    for(int y=0;y<2;y++)
                        site.box(0,.44+y*.55,-.25,1.75,.13,.70,0x9B774E);
                }
                case 14 -> { // cooking hearth chimney and firebox
                    site.box(0,.46,0,1.50,.65,1.25,stone);
                    site.box(.46,1.34,.29,.37,1.55,.39,0x584D43);
                    site.box(-.26,.77,-.53,.55,.24,.18,0xAD533B);
                    site.diamond(-.27,.94,-.49,.20,.41,0xE6A855);
                }
                case 15 -> { // rain catcher, tilted open trough and gutter
                    site.box(0,.96,0,1.79,.18,1.62,metal);
                    for(int side:new int[]{-1,1})
                        site.box(side*.79,.65,0,.14,1.05,1.54,0x9B8467);
                    site.box(0,1.14,.68,2.03,.15,.19,0x939FB1);
                    site.box(0,1.13,-.62,1.98,.13,.17,0x697F9E);
                }
                case 16 -> { // cargo laydown, pallets and lifting rail
                    site.box(-.42,.41,0,.96,.39,1.14,timber);
                    site.box(.52,.48,0,.82,.65,.79,0x736344);
                    site.box(0,1.16,.70,2.10,.16,.19,metal);
                    site.box(-.86,.76,.70,.15,1.16,.18,metal);
                }
                case 17 -> { // living forager garden raised beds
                    for(int row=-1;row<=1;row++){
                        site.box(row*.58,.37,0,.37,.22,1.53,0x76543E);
                        for(int i=-1;i<=1;i++)
                            site.diamond(row*.58,.56,i*.42,.14,.36,0x487852);
                    }
                }
                case 18 -> { // compost yard mounds and tool frame
                    for(int sign:new int[]{-1,1})
                        site.diamond(sign*.52,.60,0,.52,.89,0x61573B);
                    site.box(0,1.11,.66,2.01,.15,.18,timber);
                }
                case 19 -> { // herb bed and trellis
                    for(int side:new int[]{-1,1}){
                        site.box(side*.50,.41,0,.55,.20,1.40,0x6E5945);
                        for(int j=-1;j<=1;j++)
                            site.diamond(side*.50,.67,j*.42,.13,.46,0x507C66);
                    }
                    site.box(0,1.40,.65,1.96,.13,.13,0xA39876);
                }
                case 20 -> { // charcoal kiln: sealed drum with exhaust
                    site.barrel(0,.85,0,.64,1.28,0x494743);
                    site.box(.56,1.53,0,.29,.99,.32,metal);
                    site.box(0,1.38,-.57,1.22,.17,.23,0x5E5147);
                }
                case 21 -> { // stone furnace, chimney and orange forge mouth
                    site.box(0,.87,0,1.65,1.43,1.46,stone);
                    site.box(.57,1.83,.37,.48,.95,.48,0x585852);
                    site.box(0,.65,-.76,.71,.58,.15,0xD9824A);
                    site.box(0,1.18,-.76,.86,.13,.24,0x45474B);
                }
                case 22 -> { // shared salvage forge, vent and anvil
                    site.box(-.51,.77,.12,.92,1.25,1.22,0x625B55);
                    site.box(.55,.75,0,.78,.38,1.04,metal);
                    site.box(.55,1.01,0,1.08,.19,.36,0x969391);
                    site.box(-.64,1.75,.48,.31,.80,.31,0x3E4448);
                }
                case 23 -> { // freight bay, raised platform + hoist beam
                    site.box(0,.48,.21,2.13,.42,1.22,stone);
                    for(int side:new int[]{-1,1})
                        site.box(side*.88,1.38,.61,.14,2.00,.18,metal);
                    site.box(0,2.29,.61,2.08,.15,.20,0x96917D);
                    site.box(.31,1.65,.60,.10,.97,.12,0xE2AE63);
                }
                case 24 -> { // industrial material sorter dual jaws
                    site.box(-.65,.83,0,.71,1.18,1.04,metal);
                    site.box(.55,.93,.10,.72,1.26,.78,0x9A875E);
                    site.box(0,1.72,.17,1.92,.14,.34,0xAF8757);
                    site.barrel(0,1.02,-.45,.23,.65,0x7F898C);
                }
                default -> throw new IllegalStateException("Missing infrastructure mesh");
            }
            meshes.put(DEVICE[index],site.done());
        }

        // Explicitly faction-owned second-era geometry, not one cosmetic
        // universal machine with a different icon.
        Model machinist=new Model();
        machinist.box(0,.25,0,2.45,.39,1.95,0x454F5B);
        machinist.box(-.67,.93,.20,.88,.97,1.01,0x687480);
        machinist.box(.58,.73,0,1.13,.54,1.33,0xA2804F);
        machinist.barrel(-.55,1.53,-.16,.32,.83,0xB1A063);
        machinist.box(.50,1.51,-.12,.31,1.23,.32,0x445561);
        machinist.box(0,2.03,.18,1.94,.16,.30,0xC18A4A);
        meshes.put(DEVICE[25],machinist.done());

        Model reliquary=new Model();
        reliquary.box(0,.21,0,2.46,.35,2.01,0x4F4650);
        reliquary.box(0,.72,0,1.54,.70,1.14,0x84707D);
        reliquary.box(0,1.12,0,1.77,.15,1.39,0xA18B80);
        for(int side:new int[]{-1,1}){
            reliquary.box(side*.99,1.28,.32,.26,1.98,.30,0x605463);
            reliquary.diamond(side*.99,2.31,.32,.21,.39,0xC7A06F);
        }
        reliquary.diamond(0,1.51,0,.45,.82,0xA96E8C);
        reliquary.box(0,1.92,.15,.78,.11,.13,0xB9A16B);
        meshes.put(DEVICE[26],reliquary.done());

        Model sporeBasin=new Model();
        sporeBasin.box(0,.15,0,2.65,.27,2.14,0x4E3A44);
        sporeBasin.box(0,.51,0,2.12,.52,1.83,0x754D5B);
        sporeBasin.diamond(0,1.10,0,.86,1.01,0xB16B79);
        for(int limb=0;limb<6;limb++){
            double rad=limb*Math.PI/3;
            sporeBasin.diamond(Math.cos(rad)*.91,.77,Math.sin(rad)*.75,
                .26,.75,0x9B6072);
        }
        sporeBasin.diamond(0,1.73,0,.31,.67,0xC6A18B);
        meshes.put(DEVICE[27],sporeBasin.done());


        // Steam-era fixed buildings. The boiler and driveshaft are common
        // utilities; each faction retains a different E3 manufacturing plant.
        Model boiler=new Model();
        boiler.box(0,.2,0,2.62,.3,1.98,0x56565A);
        boiler.barrel(0,1.10,0,.72,1.40,0x667077);
        boiler.box(-.91,1.42,.23,.24,1.42,.22,0x40464B);
        boiler.box(.86,1.50,.23,.34,1.72,.32,0x4A4F52);
        boiler.box(0,.67,-.80,1.32,.34,.18,0xC66B33);
        boiler.box(0,1.97,0,1.17,.13,1.10,0x928C80);
        meshes.put(DEVICE[28],boiler.done());

        Model drive=new Model();
        drive.box(0,.22,0,2.36,.26,1.72,0x60636B);
        for(int side=-1;side<=1;side+=2){
            drive.box(side*.88,.75,0,.32,.89,.75,0x55585D);
            drive.barrel(side*.81,1.25,0,.33,.32,0xB1A47D);
        }
        drive.box(0,1.19,0,2.1,.25,.31,0xBDA273);
        drive.box(0,1.35,-.48,1.25,.17,.19,0x737E87);
        meshes.put(DEVICE[29],drive.done());

        Model press=new Model();
        press.box(0,.24,0,2.45,.35,1.88,0x56616B);
        for(int side=-1;side<=1;side+=2)
            press.box(side*.87,1.31,0,.28,2.19,.49,0x76828A);
        press.box(0,2.44,0,2.14,.28,.73,0x60676D);
        press.box(0,1.36,0,.64,1.88,.58,0xBA9958);
        press.box(0,.84,0,1.18,.27,1.0,0x95938A);
        meshes.put(DEVICE[30],press.done());

        Model unraveledPressure=new Model();
        unraveledPressure.box(0,.20,0,2.68,.31,2.18,0x4A545C);
        for(int side=-1;side<=1;side+=2){
            unraveledPressure.barrel(side*.62,.93,0,.42,1.16,0x72838D);
            unraveledPressure.box(side*.64,1.62,0,.87,.12,.75,0xCEAB61);
        }
        unraveledPressure.box(0,1.9,.34,1.72,.23,.29,0x545A5D);
        unraveledPressure.box(0,1.41,-.84,.25,.97,.22,0xA98C54);
        meshes.put(DEVICE[31],unraveledPressure.done());

        Model penitentDais=new Model();
        penitentDais.box(0,.22,0,2.48,.32,2.14,0x594C54);
        penitentDais.box(0,.79,0,1.65,.82,1.48,0x807080);
        penitentDais.box(0,1.29,0,1.89,.14,1.65,0xB3A18E);
        for(int side=-1;side<=1;side+=2){
            penitentDais.box(side*.91,1.47,.35,.24,2.32,.23,0x655B6A);
            penitentDais.diamond(side*.91,2.55,.35,.19,.39,0xCF9C65);
        }
        penitentDais.diamond(0,1.89,0,.44,.76,0xAF738A);
        penitentDais.box(0,2.21,.1,.19,.65,.17,0xC4A77C);
        meshes.put(DEVICE[32],penitentDais.done());

        Model chitinVat=new Model();
        chitinVat.box(0,.19,0,2.78,.31,2.20,0x4C393D);
        chitinVat.box(0,.75,0,2.15,.83,1.81,0x8A5868);
        chitinVat.diamond(0,1.35,0,.86,1.49,0xBC7C72);
        for(int i=0;i<8;i++){
            double t=i*Math.PI*2/8;
            chitinVat.diamond(Math.cos(t)*1.08,.93,
                Math.sin(t)*.77,.20,.68,0xA37B6C);
        }
        chitinVat.diamond(0,2.21,0,.32,.62,0xC69E7E);
        meshes.put(DEVICE[33],chitinVat.done());


        // E4: individually modeled physical electrical utility and faction
        // plants. Utilities are shared, but their factory outputs are NOT.
        Model generator=new Model();
        generator.box(0,.21,0,2.61,.30,2.13,0x4C5662);
        generator.barrel(-.48,1.04,.10,.55,1.15,0x7B7E73);
        generator.barrel(.64,.91,.10,.41,.87,0xA48B64);
        generator.box(0,1.84,.22,2.24,.15,.26,0xB9A05B);
        generator.box(-.74,1.40,-.61,.34,.98,.24,0x566271);
        generator.box(.84,1.52,-.59,.21,1.17,.28,0xA0AAB2);
        meshes.put(DEVICE[34],generator.done());

        Model bus=new Model();
        bus.box(0,.20,0,2.30,.31,1.75,0x52575E);
        for(int side=-1;side<=1;side+=2){
            bus.box(side*.85,.92,0,.29,1.40,.36,0x6C7881);
            bus.box(side*.85,1.72,0,.42,.17,.47,0xD9B15C);
        }
        bus.box(0,1.72,0,2.23,.25,.25,0x92876A);
        for(int j=-1;j<=1;j++)
            bus.box(j*.48,1.93,.13,.11,.25,.20,0xD8BD76);
        meshes.put(DEVICE[35],bus.done());

        Model lathe=new Model();
        lathe.box(0,.22,0,2.72,.31,1.88,0x515C67);
        lathe.box(-.83,.87,0,.86,1.18,.98,0x728390);
        lathe.box(.88,.65,0,.56,.72,.83,0x576875);
        lathe.barrel(-.08,1.03,0,.28,.83,0xB9A270);
        lathe.box(0,1.45,-.34,2.25,.20,.29,0xADB0A6);
        lathe.box(.52,1.88,-.37,.17,.76,.17,0xC8AC65);
        meshes.put(DEVICE[36],lathe.done());

        Model waterworks=new Model();
        waterworks.box(0,.21,0,2.65,.32,2.25,0x596770);
        for(int side=-1;side<=1;side+=2){
            waterworks.barrel(side*.67,.84,.19,.44,1.09,0x79929B);
            waterworks.box(side*.70,1.57,.15,.66,.18,.68,0xACB8AF);
        }
        waterworks.box(0,1.56,-.53,2.00,.19,.24,0x4E859E);
        waterworks.box(0,.83,-.92,1.83,.27,.19,0xA2B0AA);
        meshes.put(DEVICE[37],waterworks.done());

        Model sensor=new Model();
        sensor.box(0,.21,0,2.61,.31,2.02,0x4C5562);
        sensor.box(-.64,.91,.10,.89,1.10,.96,0x687D91);
        sensor.box(.61,.81,.16,.76,.77,.91,0xA68F55);
        sensor.barrel(0,1.73,.04,.28,1.17,0x748C95);
        sensor.box(0,2.39,.04,1.51,.12,.24,0xCAA35D);
        sensor.diamond(.56,1.50,-.43,.26,.43,0x7CB3C3);
        meshes.put(DEVICE[38],sensor.done());

        Model choir=new Model();
        choir.box(0,.19,0,2.74,.32,2.26,0x54464F);
        choir.box(0,.73,0,1.86,.73,1.64,0x827482);
        for(int j=-1;j<=1;j++){
            choir.box(j*.81,1.57,.57,.24,2.07,.22,0x64566D);
            choir.diamond(j*.81,2.64,.57,.23,.52,0xC4A874);
        }
        choir.diamond(0,1.74,-.19,.49,.90,0x997187);
        choir.box(0,2.31,.06,1.26,.12,.22,0xAAB4BC);
        meshes.put(DEVICE[39],choir.done());

        Model membrane=new Model();
        membrane.box(0,.20,0,2.80,.32,2.38,0x584551);
        membrane.box(0,.65,0,2.35,.60,1.94,0x906576);
        membrane.diamond(0,1.37,0,.90,1.38,0xC3879B);
        for(int j=0;j<8;j++){
            double angle=j*Math.PI/4;
            membrane.diamond(Math.cos(angle)*.98,.83,
                Math.sin(angle)*.80,.20,.66,0xB18E8D);
        }
        membrane.diamond(0,2.24,0,.37,.61,0x9BC2AD);
        meshes.put(DEVICE[40],membrane.done());


        // E5 has actual static, collidable, faction-distinct PSX 3D machinery,
        // not a common factory reskin or thrown deployment.
        for(int index=41;index<=55;index++){
            Model machine=new Model();
            int frame=index==53?0x635363:index==54?0x76565E:0x535D64;
            machine.box(0,.18,0,2.62,.27,2.10,frame);
            switch(index){
                case 41 -> { // Conveyor belt on raised driven rollers
                    for(int side:new int[]{-1,1}){
                        machine.box(side*1.04,.68,0,.19,.62,1.95,0x687279);
                        machine.barrel(side*.72,.87,0,.18,.44,0xA69A72);
                    }
                    machine.box(0,1.02,0,2.48,.16,1.58,0x494A43);
                    for(int n=-2;n<=2;n++)
                        machine.box(n*.43,1.13,0,.07,.05,1.62,0xA18C56);
                }
                case 42 -> { // Sorting bin array with lift arm
                    for(int i=-1;i<=1;i++){
                        machine.box(i*.73,.73,.15,.64,.89,.91,0x68767D);
                        machine.box(i*.73,1.23,.16,.69,.13,.92,0xC9B06C);
                    }
                    machine.box(0,1.82,-.66,2.12,.18,.24,0x8B9B9E);
                    machine.box(.70,1.64,-.67,.13,.51,.17,0xE4B75E);
                }
                case 43 -> { // Industrial fabrication bench / motor crane
                    machine.box(-.45,.83,0,1.32,.74,1.48,0x7E7E70);
                    machine.box(.81,1.28,-.21,.47,2.13,.47,0x687987);
                    machine.box(0,2.29,-.23,2.10,.18,.25,0xB8A36D);
                    machine.box(-.11,1.43,-.31,.31,1.02,.33,0x4E6977);
                }
                case 44 -> { // Actual multi-station production assembly line
                    for(int j=-1;j<=1;j++){
                        machine.box(j*.77,.80,0,.64,.87,1.24,0x576978);
                        machine.box(j*.77,1.38,0,.71,.13,.92,0xBE985D);
                    }
                    machine.box(0,1.82,-.61,2.52,.21,.26,0xA8AEB0);
                    machine.box(0,.32,.74,2.35,.21,.32,0x303942);
                }
                case 45 -> { // Command hall, strategic monitor canopy
                    machine.box(0,.83,.39,2.39,1.24,.62,0x74716F);
                    machine.box(0,1.52,.31,2.14,.57,.16,0x334E56);
                    machine.box(0,1.81,.29,1.86,.31,.07,0x5C8791);
                    machine.box(0,.73,-.56,1.94,.31,.58,0xA69479);
                }
                case 46 -> { // Research archive with tall racks and scanners
                    for(int side:new int[]{-1,1}){
                        machine.box(side*.83,1.23,.35,.55,2.01,.54,0x817A6C);
                        for(int h=0;h<3;h++)
                            machine.box(side*.83,.54+h*.59,.04,.59,.13,.88,0xB0A27A);
                    }
                    machine.diamond(0,1.35,0,.37,.71,0x85A7B0);
                    machine.box(0,2.06,0,1.38,.20,.34,0xABBAB1);
                }
                case 47 -> { // Signal mast and transmitter
                    machine.box(0,.64,0,.81,.92,.81,0x687B87);
                    machine.box(0,1.90,0,.21,2.12,.20,0xA8A19C);
                    machine.box(0,2.65,0,1.43,.12,.13,0xD1AC68);
                    machine.diamond(0,3.12,0,.32,.56,0x98BFCA);
                }
                case 48 -> { // Security grid, layered armored relay segments
                    for(int side:new int[]{-1,1}){
                        machine.box(side*.78,.93,0,.83,1.14,.69,0x555D6C);
                        machine.box(side*.78,1.61,0,.88,.20,.88,0xB3A36B);
                    }
                    machine.box(0,1.13,0,.74,.74,1.31,0x7395A8);
                    machine.box(0,1.95,0,2.40,.13,.31,0xCCA46F);
                }
                case 49 -> { // Liquid fuel refinery and pipe manifold
                    for(int side:new int[]{-1,1}){
                        machine.barrel(side*.67,.86,0,.47,1.11,0x7A878A);
                        machine.box(side*.67,1.53,.09,.77,.11,.75,0xAEA075);
                    }
                    machine.box(0,1.78,-.26,2.30,.18,.29,0xB2B0A0);
                    machine.box(0,.87,.81,.27,1.06,.23,0x9B783D);
                }
                case 50 -> { // Penitent terror collector / suspended censers
                    for(int side:new int[]{-1,1}){
                        machine.box(side*.85,1.22,.52,.26,2.03,.26,0x625669);
                        machine.diamond(side*.85,2.35,.52,.20,.39,0xC9A974);
                    }
                    machine.box(0,.82,0,1.32,.90,1.26,0x7F667A);
                    machine.diamond(0,1.72,0,.46,.79,0xAA7187);
                    machine.box(0,2.36,0,1.87,.13,.15,0xC1B19E);
                }
                case 51 -> { // Begotten digestive bioreactor and living ribs
                    machine.box(0,.68,0,2.20,.75,1.69,0x67434C);
                    machine.diamond(0,1.31,0,.77,1.27,0xA47576);
                    for(int n=0;n<7;n++){
                        double t=n*Math.PI*2/7;
                        machine.diamond(Math.cos(t)*.93,.89,Math.sin(t)*.73,
                            .21,.66,0xB18A6D);
                    }
                    machine.diamond(0,2.02,0,.35,.52,0xCFAB83);
                }
                case 52 -> { // Unraveled armored defense production bay
                    machine.box(0,.76,.12,2.24,.91,1.40,0x53697A);
                    machine.box(-.78,1.52,-.25,.51,.83,.66,0xA78B59);
                    machine.barrel(.60,1.75,.17,.38,.93,0x8399A4);
                    machine.box(0,2.12,.66,2.26,.19,.25,0xB99D61);
                    machine.box(.57,1.24,-.73,.16,.96,.17,0x6CB0BA);
                }
                case 53 -> { // Penitent consecration sanctum, tall colonnade
                    machine.box(0,.74,0,1.53,.76,1.38,0x80717A);
                    for(int side:new int[]{-1,1}){
                        machine.box(side*.98,1.50,.55,.26,2.21,.29,0x77677A);
                        machine.diamond(side*.98,2.68,.55,.21,.37,0xD8B777);
                    }
                    machine.diamond(0,1.69,0,.55,1.03,0xB47992);
                    machine.box(0,2.38,0,1.63,.18,.48,0xB7A884);
                }
                case 54 -> { // Begotten organ lattice, living vertical skeleton
                    machine.box(0,.68,0,2.34,.67,1.96,0x704B58);
                    machine.diamond(0,1.53,0,.90,1.59,0xBB7A84);
                    for(int n=0;n<10;n++){
                        double t=n*Math.PI/5;
                        machine.diamond(Math.cos(t)*1.0,1.06,
                            Math.sin(t)*.79,.19,.83,0xB29486);
                    }
                    machine.diamond(0,2.48,0,.32,.64,0xDAB4A2);
                }
                case 55 -> { // Unraveled resilience grid: four armored pylons and a reinforced lattice
                    machine.box(0,.72,0,2.36,.35,1.86,0x556F79);
                    for(int side:new int[]{-1,1})for(int end:new int[]{-1,1}){
                        machine.box(side*.90,1.28,end*.66,.30,1.64,.30,0x8B9EA6);
                        machine.diamond(side*.90,2.12,end*.66,.22,.39,0xA4C8D1);
                    }
                    machine.box(0,1.86,0,2.16,.16,.18,0xA7C2C9);
                    machine.box(0,1.46,0,.17,.86,1.65,0xB5A76E);
                    machine.diamond(0,1.75,0,.43,.53,0x82C9DB);
                }
                default -> throw new IllegalStateException("Missing E5/Incursion industrial geometry");
            }
            meshes.put(DEVICE[index],machine.done());
        }

        // Reuse selected CC0 Beckenburg models, not the full source library.
        try {
            for(String name:new String[]{"broadleaf","conifer"}){
                byte[] source=java.nio.file.Files.readAllBytes(java.nio.file.Path.of(
                    "assets","game","beckenburg","level_tree_"+name+".glb"));
                GlbMesh tree=GlbMesh.read(source);
                if(tree.triangleCount()<50)throw new java.io.IOException("Empty imported tree");
                meshes.put("resource-tree-"+name,tree);
            }
        }catch(java.io.IOException exception){
            throw new IllegalStateException("Required Beckenburg CC0 resource trees missing",exception);
        }
        Model stone=new Model();
        stone.diamond(-.19,.27,0,.31,.50,0x74767A);
        stone.diamond(.22,.20,.13,.23,.37,0x9C9B91);
        stone.diamond(.02,.13,-.23,.19,.27,0x505C5F);
        meshes.put("resource-stone",stone.done());
        Model ore=new Model();
        ore.diamond(-.12,.32,0,.35,.64,0x444A54);
        ore.diamond(.23,.22,.12,.24,.42,0x5B6877);
        ore.diamond(-.13,.41,-.20,.10,.18,0xC2A04B);
        ore.diamond(.20,.35,-.08,.09,.16,0xCFAE65);
        meshes.put("resource-ore",ore.done());
        Model fiber=new Model();
        for(int i=0;i<7;i++){
            double a=i*Math.PI*2/7;
            fiber.box(Math.cos(a)*.16,.27,Math.sin(a)*.16,
                .044,.54+(i%3)*.14,.043,i%2==0?0x758657:0x9D9363);
        }
        fiber.box(0,.17,0,.23,.13,.19,0x6E583D);
        meshes.put("resource-fiber",fiber.done());
        Model forage=new Model();
        forage.box(0,.18,0,.10,.35,.09,0x5D6D38);
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3;
            forage.diamond(Math.cos(a)*.23,.31+((i%3)*.04),Math.sin(a)*.23,
                .18,.17,0x436D43);
            forage.diamond(Math.cos(a)*.22,.23,Math.sin(a)*.22,.055,.10,
                i%2==0?0xB24D3F:0xBE8B45);
        }
        meshes.put("resource-edibles",forage.done());
        Model herbs=new Model();
        herbs.box(0,.18,0,.09,.35,.09,0x5E6641);
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3;
            herbs.diamond(Math.cos(a)*.15,.32,Math.sin(a)*.15,.15,.30,0x4B8261);
            herbs.diamond(Math.cos(a)*.17,.52,Math.sin(a)*.17,.075,.13,0xAC758B);
        }
        meshes.put("resource-herbs",herbs.done());
        Model water=new Model();
        water.box(0,.045,0,.85,.075,.72,0x4D6561);
        water.box(.08,.091,.04,.49,.011,.45,0x557F86);
        water.diamond(-.17,.12,.02,.09,.06,0x7A9C9B);
        meshes.put("resource-water",water.done());

        meshes.putAll(ThrownDeployableMeshes.build());
        // Nine real, authored static GLBs are immediately available to the
        // common shaded-triangle renderer under stable airframe IDs. There are
        // deliberately no invented flight actors or aircraft-spawn shortcuts.
        try {
            meshes.putAll(AirPowerMeshes.load(java.nio.file.Path.of(".")));
            meshes.putAll(VeilWeather.loadMeshes(java.nio.file.Path.of(".")));
        }catch(java.io.IOException error){
            throw new IllegalStateException("Required PSX faction aircraft invalid or missing",error);
        }
    }
    public Map<String,GlbMesh> meshes(){return Collections.unmodifiableMap(meshes);}
}
