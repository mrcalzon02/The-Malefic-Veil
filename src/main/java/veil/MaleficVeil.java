package veil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** PSX-resolution Java2D raycast arena. No external jars, assets or native libraries. */
public final class MaleficVeil extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int RW=320,RH=200;
    private static final double FOV=1.18;
    private final BufferedImage buffer=new BufferedImage(RW,RH,BufferedImage.TYPE_INT_RGB);
    private final double[] depth=new double[RW];
    private final Set<Integer> held=new HashSet<>();
    private final World world;
    private final UrbanScene urban;
    private final PropRenderer urbanRenderer=new PropRenderer();
    private final NetLink net;
    private final int archetype;
    private final javax.swing.Timer timer;
    private double px,py,angle=0.2,abilitySeconds=0,abilityCooldown=0,fireCooldown=0,attackFlash=0;
    private int health=100,sanity=100,cash=150,scrap=4,medkits=2,pistolReserve=48,shellReserve=18,pistolClip=12,shellClip=6;
    private int gun=0,spawn=0,frags=0,previousMouse=-1;
    private boolean mapVisible=false,help=true,dead=false;
    private long ticks=0;
    private final String[] names={"WARDEN","MEDIC","SHADE","OCCULTIST"};
    private String notice="THE VEIL HAS THINNED";
    private int noticeTicks=120;
    private final JFrame window;

    private MaleficVeil(JFrame window,long seed,int archetype,NetLink net) {
        this.window=window;this.net=net;this.archetype=archetype;
        world=new World(seed);
        try{urban=new UrbanScene(world);}catch(IOException e){throw new IllegalStateException("Curated urban meshes unavailable",e);}
        px=world.spawns[0][0];py=world.spawns[0][1];
        if(net==null){
            world.npcs.add(new World.Npc(7.5,1.5));
            world.npcs.add(new World.Npc(1.5,8.5));
            world.pickups.add(new World.Pickup(4.5,1.5,3));
            world.pickups.add(new World.Pickup(1.5,4.5,4));
        }else net.choose(archetype);
        setBackground(Color.BLACK);setPreferredSize(new Dimension(1024,640));
        setFocusable(true);
        addKeyListener(this);addMouseListener(this);addMouseMotionListener(this);
        timer=new javax.swing.Timer(33,this);timer.start();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            String[] modes={"SOLO","HOST LAN","JOIN LAN"};
            int mode=JOptionPane.showOptionDialog(null,"THE MALEFIC VEIL — PROTOTYPE\nSelect play mode",
                "The Malefic Veil",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,modes,modes[0]);
            if(mode<0)return;
            String[] classes={"WARDEN — turret specialist","MEDIC — field recovery","SHADE — invisibility","OCCULTIST — sanity control"};
            Object selected=JOptionPane.showInputDialog(null,"Select character ability","Character Select",
                JOptionPane.PLAIN_MESSAGE,null,classes,classes[0]);
            if(selected==null)return;
            int character=0;for(int i=0;i<classes.length;i++)if(classes[i].equals(selected))character=i;
            long seed=Long.getLong("veil.seed",20261020L);
            NetLink link=null;
            try {
                if(mode==1){
                    new LanServer(7777,seed);
                    link=new NetLink("127.0.0.1",7777);
                } else if(mode==2){
                    String host=JOptionPane.showInputDialog(null,"Host IP / hostname","127.0.0.1");
                    if(host==null||host.isBlank())return;
                    link=new NetLink(host.trim(),7777);
                }
            } catch(IOException ex) {
                JOptionPane.showMessageDialog(null,"LAN connection failed: "+ex.getMessage(),"Network error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(link!=null)seed=link.seed;
            JFrame frame=new JFrame("THE MALEFIC VEIL — "+(link==null?"SOLO":"LAN"));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MaleficVeil game=new MaleficVeil(frame,seed,character,link);
            frame.setContentPane(game);frame.pack();frame.setLocationRelativeTo(null);frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
    private boolean down(int key){return held.contains(key);}
    private void toast(String msg){notice=msg;noticeTicks=85;}
    private double visibility(){
        double v=.36;
        int tileX=(int)px,tileY=(int)py;
        boolean avenue=tileX%12<3||tileY%12<3;
        v+=avenue?.25:-.14;
        if(down(KeyEvent.VK_SHIFT))v+=.29;
        if(down(KeyEvent.VK_C))v-=.28;
        if(attackFlash>0)v+=.38;
        if(archetype==2&&abilitySeconds>0)v-=.72;
        return Math.max(.02,Math.min(1,v));
    }
    private void slide(double dx,double dy){
        if(world.canWalk(px+dx,py))px+=dx;
        if(world.canWalk(px,py+dy))py+=dy;
    }
    @Override public void actionPerformed(ActionEvent unused){
        ticks++;
        double step=.033;
        abilityCooldown=Math.max(0,abilityCooldown-step);
        abilitySeconds=Math.max(0,abilitySeconds-step);
        fireCooldown=Math.max(0,fireCooldown-step);
        attackFlash=Math.max(0,attackFlash-step);
        if(noticeTicks>0)noticeTicks--;
        if(net!=null) {
            NetLink.Self s=net.self();
            if(s!=null){
                if(s.dead()&&!dead)toast("YOU HAVE FALLEN — CHOOSE RESPAWN 1/2/3");
                if(dead&&!s.dead()){px=s.x();py=s.y();toast("RETURNED FROM THE VEIL");}
                dead=s.dead();
                health=s.health();sanity=s.sanity();cash=s.cash();scrap=s.supplies();
                medkits=s.medkits();pistolReserve=s.pistolReserve();shellReserve=s.shellReserve();
                pistolClip=s.pistolClip();shellClip=s.shellClip();frags=s.frags();
                if(Math.hypot(px-s.x(),py-s.y())>1.0){px=s.x();py=s.y();}
            }
            Integer pickupIndex;
            while((pickupIndex=net.pickedUp.poll())!=null){
                if(pickupIndex>=0&&pickupIndex<world.pickups.size())world.pickups.get(pickupIndex).taken=true;
            }
            NetLink.DeviceEvent built;
            while((built=net.structures.poll())!=null){
                if(built.index()==world.devices.size()){
                    World.Device device=new World.Device(built.x(),built.y(),built.kind());
                    device.hp=built.hp();world.devices.add(device);
                }
            }
            NetLink.NpcEvent npc;
            while((npc=net.npcs.poll())!=null){
                if(npc.index()>=0&&npc.index()<world.npcs.size()){
                    World.Npc n=world.npcs.get(npc.index());
                    n.x=npc.x();n.y=npc.y();n.hp=npc.hp();
                }
            }
            if(ticks%3==0)net.state(px,py,angle,(down(KeyEvent.VK_C)?1:0)|(down(KeyEvent.VK_SHIFT)?2:0));
        }
        if(!dead){
            if(down(KeyEvent.VK_LEFT))angle-=step*2.1;
            if(down(KeyEvent.VK_RIGHT))angle+=step*2.1;
            double forward=(down(KeyEvent.VK_W)?1:0)-(down(KeyEvent.VK_S)?1:0);
            double sideways=(down(KeyEvent.VK_D)?1:0)-(down(KeyEvent.VK_A)?1:0);
            double m=Math.hypot(forward,sideways);
            if(m>0){
                forward/=m;sideways/=m;
                double speed=down(KeyEvent.VK_C)?1.1:down(KeyEvent.VK_SHIFT)?4.2:2.8;
                slide((Math.cos(angle)*forward-Math.sin(angle)*sideways)*speed*step,
                    (Math.sin(angle)*forward+Math.cos(angle)*sideways)*speed*step);
            }
            if(net!=null){
                if(ticks%8==0)for(int i=0;i<world.pickups.size();i++){
                    World.Pickup item=world.pickups.get(i);
                    if(!item.taken&&Math.hypot(px-item.x,py-item.y)<.57)net.pickup(i);
                }
            }else{
            for(World.Pickup item:world.pickups){
                if(item.taken||Math.hypot(px-item.x,py-item.y)>.55)continue;
                item.taken=true;
                switch(item.kind){
                    case 0 -> {health=Math.min(100,health+30);toast("+30 HEALTH");}
                    case 1 -> {sanity=Math.min(100,sanity+30);toast("+30 SANITY");}
                    case 2 -> {pistolReserve+=24;toast("+24 ROUNDS");}
                    case 3 -> {shellReserve+=10;toast("+10 SHELLS");}
                    case 4 -> {cash+=50;toast("+$50");}
                    case 5 -> {scrap+=2;toast("+2 SUPPLIES");}
                    case 6 -> {medkits++;toast("+1 MEDKIT");}
                }
            }
            simulateNpc(step);
            for(World.Device d:world.devices) {
                if(d.hp<=0)continue;
                d.cooldown=Math.max(0,d.cooldown-step);
                if(d.kind==1&&Math.hypot(d.x-px,d.y-py)<1.8&&ticks%20==0){
                    health=Math.min(100,health+2);sanity=Math.min(100,sanity+1);
                }
                if(d.kind==0&&d.cooldown==0){
                    World.Npc target=null;double best=7;
                    for(World.Npc n:world.npcs){
                        double dist=Math.hypot(n.x-d.x,n.y-d.y);
                        if(n.hp>0&&dist<best&&world.lineClear(d.x,d.y,n.x,n.y)){target=n;best=dist;}
                    }
                    if(target!=null){target.hp-=18;d.cooldown=.45;}
                }
            }
            if(sanity<25&&ticks%33==0)health=Math.max(1,health-1);
            }
        }
        repaint();
    }
    private void simulateNpc(double dt){
        for(World.Npc n:world.npcs) {
            if(n.hp<=0)continue;
            double dx=px-n.x,dy=py-n.y,d=Math.hypot(dx,dy);
            if(d<11&&visibility()>.13&&world.lineClear(n.x,n.y,px,py)&&d>.7){
                double speed=.72*dt;
                double nx=n.x+dx/d*speed,ny=n.y+dy/d*speed;
                if(world.canWalk(nx,n.y))n.x=nx;
                if(world.canWalk(n.x,ny))n.y=ny;
            } else if(ticks%8==0){
                double dir=(World.mix(world.seed+ticks/8+n.hashCode())&1023)*Math.PI/512;
                double nx=n.x+Math.cos(dir)*.09,ny=n.y+Math.sin(dir)*.09;
                if(world.canWalk(nx,ny)){n.x=nx;n.y=ny;}
            }
            n.cooldown=Math.max(0,n.cooldown-dt);
            if(d<1.05&&n.cooldown<=0){
                hurt(8,9);n.cooldown=1.15;
            }else if(d<3&&ticks%45==0)sanity=Math.max(0,sanity-1);
        }
    }
    private void hurt(int h,int s){
        health=Math.max(0,health-h);sanity=Math.max(0,sanity-s);
        if(health==0){dead=true;toast("YOU HAVE FALLEN — 1/2/3 CHOOSE RESPAWN");}
    }
    private void shoot(){
        if(dead||fireCooldown>0)return;
        int clip=gun==0?pistolClip:shellClip;
        if(clip<=0){toast("EMPTY — R TO RELOAD");return;}
        if(gun==0)pistolClip--;else shellClip--;
        fireCooldown=gun==0?.22:.88;
        attackFlash=.18;
        if(net!=null){net.fire(gun);return;}
        World.Npc hit=null;double best=gun==0?13:4.5;
        double spread=gun==0?.065:.20;
        for(World.Npc n:world.npcs){
            if(n.hp<=0)continue;
            double dx=n.x-px,dy=n.y-py,dist=Math.hypot(dx,dy);
            if(dist>best||dist<.01||!world.lineClear(px,py,n.x,n.y))continue;
            double da=Math.atan2(Math.sin(Math.atan2(dy,dx)-angle),Math.cos(Math.atan2(dy,dx)-angle));
            if(Math.abs(da)<Math.atan2(.3,dist)+spread&&dist<best){best=dist;hit=n;}
        }
        if(hit!=null){
            hit.hp-=gun==0?27:Math.max(25,85-(int)(best*8));
            if(hit.hp<=0){cash+=25;frags++;toast("HOSTILE DOWN  +$25");}
        }
    }
    private void reload(){
        if(dead)return;
        if(net!=null){net.reload(gun);return;}
        if(gun==0){
            int used=Math.min(12-pistolClip,pistolReserve);
            pistolReserve-=used;pistolClip+=used;
        }else{
            int used=Math.min(6-shellClip,shellReserve);
            shellReserve-=used;shellClip+=used;
        }
    }
    private void deploy(int kind){
        if(dead)return;
        if(net!=null){net.build(kind);toast("CONSTRUCTION REQUEST SENT");return;}
        int price=kind==0?(archetype==0?60:100):kind==1?75:120;
        int cost=kind==0?3:kind==1?1:2;
        if(cash<price||scrap<cost){toast("NEED $"+price+" AND "+cost+" SUPPLIES");return;}
        double x=px+Math.cos(angle)*1.1,y=py+Math.sin(angle)*1.1;
        if(!world.canWalk(x,y)){toast("BUILD SPACE BLOCKED");return;}
        for(World.Device d:world.devices)if(Math.hypot(x-d.x,y-d.y)<1.2){toast("TOO CLOSE TO ANOTHER DEVICE");return;}
        cash-=price;scrap-=cost;
        world.devices.add(new World.Device(x,y,kind));
        toast(kind==0?"TURRET DEPLOYED":kind==1?"HEAL STATION DEPLOYED":"REVIVE STATION DEPLOYED");
    }
    private void ability(){
        if(dead||abilityCooldown>0)return;
        abilityCooldown=18;
        if(net!=null){net.ability();return;}
        switch(archetype){
            case 0 -> {cash+=35;scrap++;toast("SALVAGE PROTOCOL +$35 +1 SUPPLY");}
            case 1 -> {health=Math.min(100,health+55);sanity=Math.min(100,sanity+20);toast("FIELD RECOVERY");}
            case 2 -> {abilitySeconds=7;toast("SHADE ACTIVE — LOW VISIBILITY");}
            case 3 -> {
                sanity=Math.min(100,sanity+45);
                for(World.Npc n:world.npcs)if(n.hp>0&&Math.hypot(n.x-px,n.y-py)<4)n.hp-=38;
                toast("PSYCHIC REPULSE");
            }
        }
    }
    private void respawnAt(int index){
        if(!dead)return;
        if(net!=null){net.respawn(index);toast("RESPAWN REQUESTED");return;}
        spawn=index;px=world.spawns[index][0];py=world.spawns[index][1];
        health=100;sanity=75;dead=false;pistolClip=12;shellClip=6;
        pistolReserve=Math.max(24,pistolReserve);shellReserve=Math.max(12,shellReserve);
        if(net!=null)net.respawn(index);
        toast("RESPAWN: SECTOR "+(index+1));
    }
    private void renderScene(Graphics2D g){
        g.setColor(new Color(14,14,27));g.fillRect(0,0,RW,RH/2);
        g.setColor(new Color(29,28,29));g.fillRect(0,RH/2,RW,RH/2);
        for(int i=0;i<RW;i++){
            double offset=Math.atan(((i+.5)/RW-.5)*2*Math.tan(FOV/2));
            double ray=angle+offset,rx=Math.cos(ray),ry=Math.sin(ray);
            double distance=.04,hitX=px,hitY=py;
            while(distance<35){
                hitX=px+rx*distance;hitY=py+ry*distance;
                if(world.solid(hitX,hitY))break;
                distance+=.055;
            }
            depth[i]=distance;
            double corrected=Math.max(.14,distance*Math.cos(offset));
            int h=(int)Math.min(1000,140/corrected);
            int top=RH/2-h/2;
            int tx=(int)Math.floor(hitX),ty=(int)Math.floor(hitY);
            int shade=Math.max(13,165-(int)(corrected*6));
            boolean concrete=(tx/12+ty/12)%3==0;
            int r=(int)(shade*(concrete?.75:.53));
            int gg=(int)(shade*(concrete?.76:.50));
            int b=(int)(shade*(concrete?.76:.54));
            g.setColor(new Color(r,gg,b));g.drawLine(i,Math.max(0,top),i,Math.min(RH,top+h));
            if((tx+ty)%5==0){g.setColor(new Color(r/2,gg/2,b/2));g.drawLine(i,Math.max(0,top+h/2),i,Math.min(RH,top+h/2));}
        }
        urbanRenderer.draw(g,urban,px,py,angle,depth,RW,RH);
        for(World.Pickup p:world.pickups)if(!p.taken) sprite(g,p.x,p.y,p.kind==0?0x7ad36c:p.kind==1?0x87a3fd:p.kind==4?0xF2D36B:0xE6AD54,16,false);
        for(World.Device d:world.devices)if(d.hp>0)sprite(g,d.x,d.y,d.kind==0?0x57CCD6:d.kind==1?0x62E696:0xB88FF5,56,true);
        for(World.Npc n:world.npcs)if(n.hp>0)sprite(g,n.x,n.y,0xD85569,74,true);
        if(net!=null)for(NetLink.Remote p:net.others.values())
            if(p.hp>0)sprite(g,p.x,p.y,0x77D2F5,80,true,p.opacity);
        g.setColor(new Color(187,176,149));
        g.fillRect(RW/2-5,RH/2,11,1);g.fillRect(RW/2,RH/2-5,1,11);
        int bob=(int)(Math.sin(ticks*.15)*(down(KeyEvent.VK_W)?2:0));
        g.setColor(gun==0?new Color(66,67,75):new Color(70,51,45));
        int ww=gun==0?35:66;
        g.fillPolygon(new int[]{RW/2-ww/2,RW/2+ww/2,RW/2+ww/3,RW/2-ww/3},
            new int[]{RH-3+bob,RH-3+bob,RH-45+bob,RH-45+bob},4);
        if(attackFlash>0){
            g.setColor(new Color(255,207,85));
            g.fillOval(RW/2-12,RH-64+bob,24,22);
        }
        if(sanity<35){
            int a=(int)((35-sanity)*1.9);
            g.setColor(new Color(65,30,91,Math.min(100,a)));
            g.fillRect(0,0,RW,RH);
        }
    }
    private void sprite(Graphics2D g,double x,double y,int rgb,int scale,boolean humanoid){
        sprite(g,x,y,rgb,scale,humanoid,1.0);
    }
    private void sprite(Graphics2D g,double x,double y,int rgb,int scale,boolean humanoid,double opacity){
        opacity=ArenaSystems.opacity(opacity);
        if(opacity==0)return;
        double dx=x-px,dy=y-py,d=Math.hypot(dx,dy);
        if(d<.15||d>27)return;
        double theta=Math.atan2(Math.sin(Math.atan2(dy,dx)-angle),Math.cos(Math.atan2(dy,dx)-angle));
        if(Math.abs(theta)>FOV*.68)return;
        double sx=RW/2+Math.tan(theta)/Math.tan(FOV/2)*RW/2;
        int col=(int)Math.round(sx);
        if(col<0||col>=RW||d>=depth[col])return;
        int size=(int)Math.min(RH*2,scale/d);
        int width=humanoid?Math.max(3,size/3):Math.max(2,size/2);
        int top=humanoid?RH/2-size/2:RH/2+Math.max(1,32/(int)Math.max(1,d))-size/2;
        int left=col-width/2;
        Composite old=g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)opacity));
        g.setColor(new Color(rgb));
        g.fillRect(left,top,width,size);
        if(humanoid){
            g.setColor(new Color(15,16,25));
            g.fillRect(left+width/5,top+size/4,Math.max(1,width/5),Math.max(2,size/9));
            g.fillRect(left+width*3/5,top+size/4,Math.max(1,width/5),Math.max(2,size/9));
        }
        g.setComposite(old);
    }
    @Override protected void paintComponent(Graphics raw){
        super.paintComponent(raw);
        Graphics2D g=(Graphics2D)raw.create();
        Graphics2D low=buffer.createGraphics();
        renderScene(low);low.dispose();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(buffer,0,0,getWidth(),getHeight(),null);
        int h=getHeight(),w=getWidth();
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
        g.setColor(new Color(10,11,16,225));g.fillRect(0,h-70,w,70);
        g.setColor(new Color(215,215,207));
        g.drawString("HP "+health+"   SAN "+sanity+"   CASH $"+cash+"   SUP "+scrap+"   MED "+medkits+"   KILLS "+frags,105,h-44);
        g.drawString((gun==0?"HANDGUN ":"SHOTGUN ")+(gun==0?pistolClip:shellClip)+"/"+(gun==0?pistolReserve:shellReserve)+
            "     "+names[archetype]+"  F "+(abilityCooldown==0?"READY":String.format("%.0fs",abilityCooldown)),105,h-17);
        double gem=net!=null&&net.self()!=null?net.self().visibility():visibility();
        drawGem(g,gem,31,h-35);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
        g.setColor(new Color(236,234,228));
        g.drawString("THE MALEFIC VEIL",15,22);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        g.drawString("SEED "+world.seed+"  "+(net==null?"SOLO":"LAN PLAYERS "+(net.others.size()+1)),w-218,22);
        if(noticeTicks>0){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));g.setColor(new Color(234,202,132));g.drawString(notice,15,52);}
        if(mapVisible)drawMap(g,w,h);
        if(help&&!dead){g.setColor(new Color(9,10,17,220));g.fillRect(12,64,440,100);
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            g.drawString("WASD move / Mouse or arrows turn / Shift sprint / C crouch",23,87);
            g.drawString("CLICK/SPACE fire  1/2 weapons  R reload  F ability",23,107);
            g.drawString("T turret  H heal  V revive  B barricade  E medkit",23,127);
            g.drawString("M city map  TAB help    Esc toggle help",23,147);}
        if(dead){
            g.setColor(new Color(0,0,0,210));g.fillRect(0,h/3,w,h/3);
            g.setColor(new Color(238,114,128));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,26));
            g.drawString("YOU HAVE FALLEN",w/2-150,h/2-30);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));g.setColor(Color.WHITE);
            g.drawString("SELECT RESPAWN:  [1] NORTH  [2] CENTER  [3] SOUTH",w/2-285,h/2+10);
        }
        g.dispose();
    }
    private void drawGem(Graphics2D g,double v,int cx,int cy){
        int red=(int)(35+v*205),green=(int)(75+(1-v)*165);
        Polygon jewel=new Polygon(new int[]{cx,cx+22,cx,cx-22},new int[]{cy-26,cy,cy+26,cy},4);
        g.setColor(new Color(red,green,110));g.fillPolygon(jewel);
        g.setColor(Color.WHITE);g.drawPolygon(jewel);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,10));
        g.setColor(Color.BLACK);g.drawString(""+(int)(v*100),cx-9,cy+4);
    }
    private void drawMap(Graphics2D g,int w,int h){
        int tile=3,left=w-World.SIZE*tile-16,top=35;
        g.setColor(new Color(8,9,13,225));g.fillRect(left-6,top-6,World.SIZE*tile+12,World.SIZE*tile+12);
        for(int x=0;x<World.SIZE;x++)for(int y=0;y<World.SIZE;y++) {
            g.setColor(world.tile(x,y)==0?new Color(37,39,49):new Color(103,107,114));
            g.fillRect(left+x*tile,top+y*tile,tile,tile);
        }
        for(double[] s:world.spawns){g.setColor(Color.YELLOW);g.fillRect(left+(int)(s[0]*tile)-2,top+(int)(s[1]*tile)-2,5,5);}
        g.setColor(Color.CYAN);g.fillOval(left+(int)(px*tile)-3,top+(int)(py*tile)-3,7,7);
    }
    @Override public void keyPressed(KeyEvent e){
        int k=e.getKeyCode();
        boolean fresh=held.add(k);
        if(!fresh)return;
        if(dead){
            if(k>=KeyEvent.VK_1&&k<=KeyEvent.VK_3)respawnAt(k-KeyEvent.VK_1);
            return;
        }
        switch(k){
            case KeyEvent.VK_1 -> gun=0;
            case KeyEvent.VK_2 -> gun=1;
            case KeyEvent.VK_R -> reload();
            case KeyEvent.VK_F -> ability();
            case KeyEvent.VK_T -> deploy(0);
            case KeyEvent.VK_H -> deploy(1);
            case KeyEvent.VK_V -> deploy(2);
            case KeyEvent.VK_E -> {
                if(net!=null)net.useMedkit();
                else if(medkits>0&&health<100){medkits--;health=Math.min(100,health+55);toast("MEDKIT USED");}
            }
            case KeyEvent.VK_B -> deploy(3);
            case KeyEvent.VK_SPACE -> shoot();
            case KeyEvent.VK_M -> mapVisible=!mapVisible;
            case KeyEvent.VK_TAB,KeyEvent.VK_ESCAPE -> help=!help;
        }
    }
    @Override public void keyReleased(KeyEvent e){held.remove(e.getKeyCode());}
    @Override public void keyTyped(KeyEvent e){}
    @Override public void mousePressed(MouseEvent e){requestFocusInWindow();if(e.getButton()==MouseEvent.BUTTON1)shoot();}
    @Override public void mouseReleased(MouseEvent e){}
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){previousMouse=-1;}
    @Override public void mouseExited(MouseEvent e){previousMouse=-1;}
    @Override public void mouseDragged(MouseEvent e){mouseMoved(e);}
    @Override public void mouseMoved(MouseEvent e){
        if(previousMouse>=0&&!dead)angle+=Math.max(-60,Math.min(60,e.getX()-previousMouse))*.004;
        previousMouse=e.getX();
    }
}
