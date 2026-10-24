package veil;

import java.util.*;

/** Twenty-one authored silhouette-distinct low-poly meshes: role AND faction drive topology. */
public final class ThrownDeployableMeshes {
    private ThrownDeployableMeshes(){}
    private static final class Model {
        final List<GlbMesh.Triangle> tris=new ArrayList<>();
        static float f(double v){return (float)v;}
        void tri(double ax,double ay,double az,double bx,double by,double bz,
                 double cx,double cy,double cz,int c){
            tris.add(new GlbMesh.Triangle(f(ax),f(ay),f(az),f(bx),f(by),f(bz),f(cx),f(cy),f(cz),c));
        }
        void quad(double[] a,double[] b,double[] c,double[] d,int rgb){
            tri(a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],rgb);
            tri(a[0],a[1],a[2],c[0],c[1],c[2],d[0],d[1],d[2],rgb);
        }
        void box(double x,double y,double z,double w,double h,double d,int c){
            double a=x-w/2,b=x+w/2,lo=y-h/2,hi=y+h/2,p=z-d/2,q=z+d/2;
            quad(new double[]{a,lo,p},new double[]{b,lo,p},new double[]{b,hi,p},new double[]{a,hi,p},c);
            quad(new double[]{b,lo,q},new double[]{a,lo,q},new double[]{a,hi,q},new double[]{b,hi,q},shade(c,.67));
            quad(new double[]{a,lo,q},new double[]{a,lo,p},new double[]{a,hi,p},new double[]{a,hi,q},shade(c,.72));
            quad(new double[]{b,lo,p},new double[]{b,lo,q},new double[]{b,hi,q},new double[]{b,hi,p},shade(c,.83));
            quad(new double[]{a,hi,p},new double[]{b,hi,p},new double[]{b,hi,q},new double[]{a,hi,q},shade(c,1.18));
            quad(new double[]{a,lo,q},new double[]{b,lo,q},new double[]{b,lo,p},new double[]{a,lo,p},shade(c,.49));
        }
        void spike(double x,double y,double z,double radius,double height,int c){
            double[][] base={{x-radius,y,z-radius},{x+radius,y,z-radius},
                             {x+radius,y,z+radius},{x-radius,y,z+radius}};
            for(int i=0;i<4;i++){
                double[] a=base[i],b=base[(i+1)%4];
                tri(a[0],a[1],a[2],b[0],b[1],b[2],x,y+height,z,shade(c,.75+i*.08));
            }
            quad(base[0],base[1],base[2],base[3],shade(c,.50));
        }
        void orb(double x,double y,double z,double radius,int c){
            // A faceted octahedral core, not a flat billboard.
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2,b=(i+1)*Math.PI/2;
                tri(x,y+radius,z,x+Math.cos(a)*radius,y,z+Math.sin(a)*radius,
                    x+Math.cos(b)*radius,y,z+Math.sin(b)*radius,shade(c,.81+i*.07));
                tri(x,y-radius,z,x+Math.cos(b)*radius,y,z+Math.sin(b)*radius,
                    x+Math.cos(a)*radius,y,z+Math.sin(a)*radius,shade(c,.55+i*.06));
            }
        }
        void cylinder(double x,double y,double z,double r,double h,int c){
            for(int i=0;i<8;i++){
                double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
                quad(new double[]{x+Math.cos(a)*r,y,z+Math.sin(a)*r},
                     new double[]{x+Math.cos(b)*r,y,z+Math.sin(b)*r},
                     new double[]{x+Math.cos(b)*r,y+h,z+Math.sin(b)*r},
                     new double[]{x+Math.cos(a)*r,y+h,z+Math.sin(a)*r},shade(c,.72+i*.045));
                tri(x,y+h,z,x+Math.cos(a)*r,y+h,z+Math.sin(a)*r,
                    x+Math.cos(b)*r,y+h,z+Math.sin(b)*r,shade(c,1.16));
            }
        }
        void beam(double ax,double ay,double az,double bx,double by,double bz,double width,int c){
            // Slanted strut using a proper 3D eight-corner prism.
            double dx=bx-ax,dy=by-ay,dz=bz-az,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(len<1e-5)return;
            double vx=dz/len,vy=0,vz=-dx/len,n=Math.hypot(vx,vz);
            if(n<.02){vx=1;vz=0;}else{vx/=n;vz/=n;}
            double wx=vy*dz-vz*dy/len,wy=vz*dx/len-vx*dz/len,wz=vx*dy/len-vy*dx/len;
            double[][] A=new double[4][3],B=new double[4][3];
            for(int i=0;i<4;i++){
                double s=(i==0||i==3?-1:1)*width/2,t=(i<2?-1:1)*width/2;
                A[i]=new double[]{ax+vx*s+wx*t,ay+vy*s+wy*t,az+vz*s+wz*t};
                B[i]=new double[]{bx+vx*s+wx*t,by+vy*s+wy*t,bz+vz*s+wz*t};
            }
            for(int i=0;i<4;i++)quad(A[i],A[(i+1)%4],B[(i+1)%4],B[i],shade(c,.74+i*.10));
        }
        GlbMesh done(){return GlbMesh.authored(tris);}
    }
    private static int shade(int rgb,double k){
        return Math.min(255,(int)(((rgb>>16)&255)*k))<<16|
               Math.min(255,(int)(((rgb>>8)&255)*k))<<8|
               Math.min(255,(int)((rgb&255)*k));
    }
    private static void foundation(Model m,ThrownDeployables.Faction f,int shell,int trim,int glow){
        switch(f){
            case UNRAVELED -> {
                m.box(0,.12,0,1.00,.20,.75,shell);
                m.box(0,.24,0,.76,.09,.60,trim);
                for(int s=-1;s<=1;s+=2){
                    m.box(s*.42,.13,.27,.11,.20,.12,0x1E282F);
                    m.box(s*.42,.13,-.27,.11,.20,.12,0x1E282F);
                }
                m.box(0,.26,-.32,.34,.045,.035,glow);
            }
            case PENITENT -> {
                m.box(0,.12,0,.90,.21,.80,shell);
                m.box(0,.26,0,.78,.07,.69,trim);
                for(int s=-1;s<=1;s+=2){
                    m.spike(s*.33,.29,.23,.08,.22,trim);
                    m.spike(s*.33,.29,-.23,.08,.22,trim);
                }
                m.orb(0,.34,0,.12,glow);
            }
            case BEGOTTEN -> {
                m.orb(0,.24,0,.48,shell);
                m.orb(-.18,.30,.14,.31,trim);
                m.orb(.21,.22,-.18,.28,trim);
                for(int i=0;i<6;i++){
                    double a=i*Math.PI/3;
                    m.beam(Math.cos(a)*.14,.20,Math.sin(a)*.15,
                        Math.cos(a)*.72,.02,Math.sin(a)*.63,.10,shell);
                }
                m.orb(0,.39,-.18,.13,glow);
            }
        }
    }
    private static void shape(Model m,ThrownDeployables.Role role,
                              ThrownDeployables.Faction f,int shell,int trim,int glow){
        switch(role){
            case WALL -> {
                switch(f){
                    case UNRAVELED -> {
                        for(int i=-2;i<=2;i++){
                            m.box(i*.33,.92,0,.32,1.25,.20,i%2==0?shell:trim);
                            m.box(i*.33,1.52,0,.29,.085,.29,glow);
                        }
                        m.box(0,.40,.20,1.92,.14,.32,shell);
                        for(int i=-2;i<=2;i++)m.beam(i*.33,.31,.25,i*.33,1.50,.06,.07,0x202B31);
                    }
                    case PENITENT -> {
                        m.box(0,.88,0,2.0,1.36,.26,shell);
                        for(int i=-3;i<=3;i++){
                            m.spike(i*.30,1.53,0,.16,.40,trim);
                            m.box(i*.30,.94,-.145,.08,.93,.045,trim);
                        }
                        m.orb(0,1.17,-.20,.24,glow);
                    }
                    case BEGOTTEN -> {
                        for(int i=-4;i<=4;i++){
                            double x=i*.24,y=1.10+(.35*Math.cos(i*1.4));
                            m.beam(x,.13,0,x*.90,y,Math.sin(i)*.18,.18,shell);
                            m.spike(x*.90,y-.16,Math.sin(i)*.18,.18,.63,trim);
                        }
                        for(int i=-2;i<=2;i++)m.orb(i*.42,.72,.02,.28,glow);
                    }
                }
            }
            case BARRICADE -> {
                switch(f){
                    case UNRAVELED -> {
                        for(int i=-2;i<=2;i++){
                            m.beam(i*.36,.15,.35,i*.36,.95,-.23,.12,shell);
                            m.beam(i*.36,.15,-.35,i*.36,.95,.23,.12,trim);
                        }
                        m.box(0,.85,0,2.1,.14,.15,0xAAB1A4);
                        m.box(0,.35,0,1.95,.10,.15,shell);
                    }
                    case PENITENT -> {
                        for(int s=-1;s<=1;s+=2){
                            m.box(s*.84,.72,0,.15,1.20,.15,shell);
                            m.spike(s*.84,1.30,0,.24,.38,trim);
                            m.orb(s*.84,1.20,-.18,.14,glow);
                        }
                        m.beam(-.84,.45,0,.84,1.08,0,.13,trim);
                        m.beam(-.84,1.08,0,.84,.45,0,.13,trim);
                        m.orb(0,.80,-.06,.20,glow);
                    }
                    case BEGOTTEN -> {
                        for(int s=-2;s<=2;s++){
                            double x=s*.43;
                            m.beam(x,.10,.30,x*.88,.84,-.17,.20,shell);
                            m.beam(x,.10,-.35,x*.82,.90,.22,.12,trim);
                            m.spike(x*.88,.65,-.17,.16,.52,glow);
                        }
                        m.orb(0,.48,0,.39,trim);
                    }
                }
            }
            case TURRET -> {
                switch(f){
                    case UNRAVELED -> {
                        m.cylinder(0,.33,0,.32,.49,shell);
                        m.box(0,.90,0,.69,.32,.54,trim);
                        for(int i=-1;i<=1;i+=2){
                            m.beam(i*.18,.89,-.04,i*.18,.89,-.96,.15,0xBBC2BE);
                            m.box(i*.18,.89,-.91,.13,.10,.15,glow);
                        }
                        m.box(0,1.11,.16,.29,.07,.25,glow);
                    }
                    case PENITENT -> {
                        m.cylinder(0,.30,0,.22,.75,shell);
                        for(int i=0;i<6;i++){
                            double a=i*Math.PI/3;
                            m.beam(Math.cos(a)*.26,.50,Math.sin(a)*.26,
                                Math.cos(a)*.48,1.19,Math.sin(a)*.48,.08,trim);
                        }
                        m.orb(0,1.31,0,.38,glow);
                        m.spike(0,.83,-.46,.19,.39,trim);
                    }
                    case BEGOTTEN -> {
                        m.orb(0,.78,0,.54,shell);
                        m.orb(0,.92,-.41,.33,glow);
                        for(int i=-2;i<=2;i++)
                            m.beam(i*.19,.85,-.26,i*.22,1.06,-.86,.11,trim);
                        m.spike(0,1.31,.12,.22,.64,shell);
                    }
                }
            }
            case HEALTH -> {
                switch(f){
                    case UNRAVELED -> {
                        m.box(0,.73,0,.75,.96,.55,shell);
                        m.box(0,.87,-.30,.50,.46,.04,0xCBDDD7);
                        m.box(0,.87,-.35,.10,.34,.02,glow);
                        m.box(0,.87,-.37,.34,.10,.02,glow);
                        for(int s=-1;s<=1;s+=2)m.cylinder(s*.24,.32,.18,.08,.35,trim);
                    }
                    case PENITENT -> {
                        m.box(0,.73,0,.87,.92,.63,shell);
                        m.box(0,1.20,0,1.05,.11,.72,trim);
                        for(int s=-1;s<=1;s+=2){
                            m.cylinder(s*.36,1.19,-.21,.075,.31,trim);
                            m.spike(s*.36,1.48,-.21,.12,.26,glow);
                        }
                        m.orb(0,1.03,-.36,.25,glow);
                    }
                    case BEGOTTEN -> {
                        m.orb(0,.81,0,.60,shell);
                        for(int i=0;i<5;i++){
                            double a=i*Math.PI*2/5;
                            m.spike(Math.cos(a)*.35,.77,Math.sin(a)*.30,.23,.67,trim);
                        }
                        m.orb(0,1.10,-.37,.38,glow);
                        m.orb(-.19,.50,-.39,.19,trim);
                    }
                }
            }
            case AMMO -> {
                switch(f){
                    case UNRAVELED -> {
                        m.box(0,.62,0,.92,.71,.64,shell);
                        m.box(0,.98,0,.91,.10,.64,trim);
                        for(int i=-2;i<=2;i++){
                            m.cylinder(i*.15,.98,-.17,.055,.29,0xC5A46C);
                            m.box(i*.15,1.28,-.17,.11,.09,.13,glow);
                        }
                    }
                    case PENITENT -> {
                        m.cylinder(0,.30,0,.34,.77,shell);
                        for(int i=0;i<8;i++){
                            double a=i*Math.PI/4;
                            m.orb(Math.cos(a)*.48,1.12,Math.sin(a)*.43,.16,trim);
                            m.beam(0,.77,0,Math.cos(a)*.48,1.12,Math.sin(a)*.43,.05,glow);
                        }
                        m.orb(0,1.15,0,.24,glow);
                    }
                    case BEGOTTEN -> {
                        m.orb(0,.69,0,.49,shell);
                        for(int i=0;i<6;i++){
                            double a=i*Math.PI/3;
                            m.orb(Math.cos(a)*.42,.74,Math.sin(a)*.35,.25,trim);
                            m.spike(Math.cos(a)*.46,.91,Math.sin(a)*.35,.13,.30,glow);
                        }
                        m.orb(0,1.23,0,.23,glow);
                    }
                }
            }
            case MIASMA -> {
                switch(f){
                    case UNRAVELED -> {
                        m.cylinder(0,.30,0,.37,.83,shell);
                        m.cylinder(0,1.12,0,.22,.25,trim);
                        for(int s=-1;s<=1;s+=2){
                            m.beam(s*.23,1.02,0,s*.55,1.53,0,.14,trim);
                            m.orb(s*.55,1.55,0,.15,glow);
                        }
                        m.box(0,.75,-.39,.45,.08,.05,glow);
                    }
                    case PENITENT -> {
                        m.cylinder(0,.31,0,.27,.65,shell);
                        for(int i=0;i<6;i++){
                            double a=i*Math.PI/3;
                            m.beam(Math.cos(a)*.30,.56,Math.sin(a)*.30,
                                Math.cos(a)*.44,1.35,Math.sin(a)*.44,.11,trim);
                            m.spike(Math.cos(a)*.44,1.23,Math.sin(a)*.44,.15,.40,glow);
                        }
                        m.orb(0,.93,0,.38,glow);
                    }
                    case BEGOTTEN -> {
                        m.orb(0,.64,0,.59,shell);
                        m.orb(-.29,.91,-.12,.38,trim);
                        m.orb(.26,.95,.16,.42,trim);
                        for(int i=0;i<7;i++){
                            double a=i*Math.PI*2/7;
                            m.spike(Math.cos(a)*.50,1.09,Math.sin(a)*.45,.21,.59,glow);
                        }
                        m.orb(0,1.31,-.20,.34,glow);
                    }
                }
            }
            case SHIELD -> {
                switch(f){
                    case UNRAVELED -> {
                        for(int i=0;i<4;i++){
                            double a=i*Math.PI/2;
                            m.beam(Math.cos(a)*.38,.28,Math.sin(a)*.38,
                                Math.cos(a)*.78,1.05,Math.sin(a)*.78,.17,trim);
                            m.orb(Math.cos(a)*.78,1.05,Math.sin(a)*.78,.18,glow);
                        }
                        m.cylinder(0,.35,0,.24,.56,shell);
                        m.orb(0,1.16,0,.37,glow);
                    }
                    case PENITENT -> {
                        for(int i=0;i<4;i++){
                            double a=i*Math.PI/2;
                            m.box(Math.cos(a)*.54,.84,Math.sin(a)*.54,.14,1.24,.15,shell);
                            m.spike(Math.cos(a)*.54,1.46,Math.sin(a)*.54,.16,.40,trim);
                            m.beam(0,1.58,0,Math.cos(a)*.54,1.45,Math.sin(a)*.54,.07,trim);
                        }
                        m.orb(0,1.72,0,.43,glow);
                    }
                    case BEGOTTEN -> {
                        m.orb(0,.80,0,.48,shell);
                        for(int i=0;i<8;i++){
                            double a=i*Math.PI/4;
                            m.beam(0,.83,0,Math.cos(a)*.93,1.32,Math.sin(a)*.80,.13,trim);
                            m.orb(Math.cos(a)*.93,1.32,Math.sin(a)*.80,.18,glow);
                        }
                        m.orb(0,1.10,0,.37,glow);
                    }
                }
                // Open wireframe hemispheres make the protective bubble visible
                // without an opaque mesh hiding players or their bullets.
                int segments=16;
                for(int i=0;i<segments;i++){
                    double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
                    for(int ring=0;ring<3;ring++){
                        double height=.22+ring*.73;
                        double radius=ring==2?1.50:ring==1?2.20:1.95;
                        m.beam(Math.cos(a)*radius,height,Math.sin(a)*radius,
                            Math.cos(b)*radius,height,Math.sin(b)*radius,.034,
                            ring==1?glow:trim);
                    }
                }
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;
                    m.beam(Math.cos(a)*1.95,.22,Math.sin(a)*1.95,
                        Math.cos(a)*2.20,.95,Math.sin(a)*2.20,.035,glow);
                    m.beam(Math.cos(a)*2.20,.95,Math.sin(a)*2.20,
                        Math.cos(a)*1.50,1.68,Math.sin(a)*1.50,.035,trim);
                }
            }
        }
    }
    public static Map<String,GlbMesh> build(){
        Map<String,GlbMesh> result=new LinkedHashMap<>();
        for(ThrownDeployables.Blueprint bp:ThrownDeployables.catalog()){
            var f=bp.faction();Model model=new Model();
            int shell=switch(f){case UNRAVELED->0x4B5D67;case PENITENT->0x55464B;case BEGOTTEN->0x5C4558;};
            int trim=switch(f){case UNRAVELED->0x9BA4A0;case PENITENT->0xB7A06E;case BEGOTTEN->0x9B6BA3;};
            int glow=switch(f){case UNRAVELED->0x56C3C4;case PENITENT->0xE7AD6A;case BEGOTTEN->0x63DEAC;};
            foundation(model,f,shell,trim,glow);
            shape(model,bp.role(),f,shell,trim,glow);
            result.put(bp.mesh(),model.done());
        }
        Model burst=new Model();
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6;
            burst.spike(Math.cos(a)*.45,.05,Math.sin(a)*.45,.16,.65,0xBFB8BC);
        }
        burst.orb(0,.55,0,.55,0x8A75C5);
        result.put("thrown-harmless-burst",burst.done());
        return Collections.unmodifiableMap(result);
    }
}
