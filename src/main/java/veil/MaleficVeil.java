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
    private final PerformanceSettings performance=new PerformanceSettings();
    private final GameplayMeshes actorModels=new GameplayMeshes();
    private final NetLink net;
    private final Vehicles localVehicles;
    private final ArenaSystems simulation;
    private final ArsenalMeshes arsenalMeshes=new ArsenalMeshes();
    private boolean primaryHeld,alternateHeld;
    private long arsenalSequence;
    private JDialog fabrication;
    private final int archetype;
    private final javax.swing.Timer timer;
    private double px,py,angle=0.2,abilitySeconds=0,abilityCooldown=0,fireCooldown=0,attackFlash=0;
    private int health=100,sanity=100,cash=150,scrap=4,medkits=2,pistolReserve=48,shellReserve=18,pistolClip=12,shellClip=6;
    private int gun=0,spawn=0,frags=0,previousMouse=-1;
    private boolean mapVisible=false,continentVisible=false,help=true,dead=false;
    private boolean respawnPending=false;
    private long ticks=0;
    private final String[] names={"WARDEN","MEDIC","SHADE","OCCULTIST"};
    private String notice="THE VEIL HAS THINNED";
    private int noticeTicks=120;
    private final JFrame window;

    private MaleficVeil(JFrame window,long seed,int archetype,NetLink net) {
        this.window=window;this.net=net;this.archetype=archetype;
        simulation=new ArenaSystems(seed);world=simulation.world;
        localVehicles=simulation.vehicles;
        if(net==null){simulation.join(1);simulation.choose(1,archetype);}
        try{urban=new UrbanScene(world);}catch(IOException e){throw new IllegalStateException("Curated urban meshes unavailable",e);}
        px=world.spawns[0][0];py=world.spawns[0][1];
        if(net==null){
            world.npcs.add(new World.Npc(7.5,1.5));
            world.npcs.add(new World.Npc(1.5,8.5));
            world.pickups.add(new World.Pickup(4.5,1.5,3));
            world.pickups.add(new World.Pickup(1.5,4.5,4));
        }else net.choose(archetype);
        urbanRenderer.setBudgets(performance);
        setBackground(Color.BLACK);setPreferredSize(new Dimension(1024,640));
        setFocusable(true);
        addKeyListener(this);addMouseListener(this);addMouseMotionListener(this);
        addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent e){held.clear();primaryHeld=alternateHeld=false;releaseTrigger();}});
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
        boolean avenue=world.city.road(tileX/CityLayout.CELL,tileY/CityLayout.CELL);
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
                if(s.dead()&&!dead){respawnPending=false;toast("YOU HAVE FALLEN — SELECT A RESPAWN POINT");}
                if(dead&&!s.dead()){respawnPending=false;px=s.x();py=s.y();toast("RETURNED FROM THE VEIL");}
                dead=s.dead();
                health=s.health();sanity=s.sanity();cash=s.cash();scrap=s.supplies();
                medkits=s.medkits();pistolReserve=s.pistolReserve();shellReserve=s.shellReserve();
                pistolClip=s.pistolClip();shellClip=s.shellClip();frags=s.frags();
                if(net.drivenCar()>=0||Math.hypot(px-s.x(),py-s.y())>1.0){
                    px=s.x();py=s.y();
                    if(net.drivenCar()>=0)angle=s.angle();
                }
            }
            String rideReply;
            while((rideReply=net.rideResults.poll())!=null)
                toast(rideReply.equals("OK")?"ENGINE ENGAGED":rideReply.equals("EXITED")?
                    "EXITED VEHICLE":"VEHICLE ACTION REJECTED");
            String respawnReply;
            while((respawnReply=net.respawnResults.poll())!=null)
                if(!respawnReply.equals("OK")){respawnPending=false;toast(respawnReply.equals("WAIT")?"RESPAWN COUNTDOWN ACTIVE":"RESPAWN LOCATION UNAVAILABLE");}
            NetLink.ShotEvent result;
            while((result=net.shots.poll())!=null){
                if(!result.fired())toast("SHOT REJECTED: "+result.outcome());
                else if(result.damage()>0)toast("CONFIRMED "+result.outcome()+" HIT "+result.damage());
            }
            String arsenalReply;while((arsenalReply=net.arsenalResults.poll())!=null){if(arsenalReply.equals("FIRED")||arsenalReply.startsWith("HIT_")){attackFlash=.12;if(arsenalReply.startsWith("HIT_"))toast(arsenalReply.replace('_',' '));}else toast(arsenalReply.replace('_',' '));}
            Integer arsenalIndex;while((arsenalIndex=net.arsenalPicked.poll())!=null)if(arsenalIndex>=0&&arsenalIndex<simulation.arsenal.loot.size())simulation.arsenal.loot.get(arsenalIndex).taken=true;
            for(var tier:net.benchTiers.entrySet())if(tier.getKey()>=0&&tier.getKey()<world.devices.size())world.devices.get(tier.getKey()).tier=tier.getValue();
            ArsenalSystems.View av=net.arsenal();if(av!=null){gun=av.selected();pistolClip=av.clip()[0];}
            NetLink.BuildEvent construction;
            while((construction=net.buildResults.poll())!=null){
                toast(construction.reason().equals("OK")?"CONSTRUCTION ACCEPTED":
                    "BUILD REJECTED: "+construction.reason());
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
                }else if(built.index()>=0&&built.index()<world.devices.size()){
                    World.Device device=world.devices.get(built.index());
                    if(device.kind==built.kind()&&device.x==built.x()&&device.y==built.y())
                        device.hp=built.hp();
                }
            }
            NetLink.NpcEvent npc;
            while((npc=net.npcs.poll())!=null){
                if(npc.index()>=0&&npc.index()<world.npcs.size()){
                    World.Npc n=world.npcs.get(npc.index());
                    n.x=npc.x();n.y=npc.y();n.hp=npc.hp();
                }
            }
            if(ticks%3==0){
                if(net.drivenCar()>=0)net.drive((down(KeyEvent.VK_W)?1:0)-(down(KeyEvent.VK_S)?1:0),
                    (down(KeyEvent.VK_D)?1:0)-(down(KeyEvent.VK_A)?1:0));
                else net.state(px,py,angle,(down(KeyEvent.VK_C)?1:0)|(down(KeyEvent.VK_SHIFT)?2:0));
            }
            if(ticks%90==0)net.ping();
        }
        if(net==null){simulation.advance(33);syncSolo(false);}
        if(!dead){
            int carId=net==null?localVehicles.carDrivenBy(1):net.drivenCar();
            if(carId>=0){
                if(net==null){
                    localVehicles.input(1,(down(KeyEvent.VK_W)?1:0)-(down(KeyEvent.VK_S)?1:0),
                        (down(KeyEvent.VK_D)?1:0)-(down(KeyEvent.VK_A)?1:0));
                    Vehicles.State car=localVehicles.state(carId);
                    if(car!=null){px=car.x();py=car.y();angle=car.heading();}
                }
            }else{
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
            }
            if(net!=null){
                if(carId<0&&ticks%8==0)for(int i=0;i<world.pickups.size();i++){
                    World.Pickup item=world.pickups.get(i);
                    if(!item.taken&&Math.hypot(px-item.x,py-item.y)<.57)net.pickup(i);
                }
            }else{
                simulation.move(1,px,py,angle,(down(KeyEvent.VK_C)?1:0)|(down(KeyEvent.VK_SHIFT)?2:0));
                if(carId<0)for(int i=0;i<world.pickups.size();i++)if(!world.pickups.get(i).taken&&Math.hypot(px-world.pickups.get(i).x,py-world.pickups.get(i).y)<.55)simulation.pickup(1,i);
                syncSolo(true);
            }
            if(carId<0&&(net==null||ticks%5==0))for(int i=0;i<simulation.arsenal.loot.size();i++){
                var l=simulation.arsenal.loot.get(i);if(!l.taken&&Math.hypot(px-l.x,py-l.y)<.57){if(net!=null)net.arsenalPickup(i);else{var r=simulation.arsenal.pickup(1,i);if(r.ok())toast(l.type==0?"ACQUIRED "+ArsenalCatalog.get(l.value).name():l.type==1?"SALVAGED "+ArsenalCatalog.MATERIALS[l.value]:"AMMUNITION");}}
            }
            if((fabrication==null||!fabrication.isVisible())&&(primaryHeld||alternateHeld||down(KeyEvent.VK_SPACE)))shoot(alternateHeld);
        }
        if(performance.frameCap>=30 || ticks%2==0)repaint();
    }
    private ArsenalSystems.View arsenalView(){return net==null?simulation.arsenal.view(1):net.arsenal();}
    private void syncSolo(boolean position){
        ArenaSystems.Player p=simulation.player(1);if(p==null)return;
        health=p.hp;sanity=p.sanity;cash=p.cash;scrap=p.scrap;medkits=p.medkits;frags=p.frags;dead=p.dead;
        var a=simulation.arsenal.view(1);gun=a.selected();pistolClip=a.clip()[0];pistolReserve=a.reserve()[0];
        if(position){px=p.x;py=p.y;if(localVehicles.carDrivenBy(1)>=0)angle=p.angle;}
    }
    private void shoot(){shoot(false);}
    private void shoot(boolean alternate){
        if(dead||fireCooldown>0)return;var v=arsenalView();if(v==null)return;
        if(v.clip()[v.selected()]<=0&&!alternate){if(!primaryHeld)toast("EMPTY — R TO RELOAD");return;}
        fireCooldown=.05;
        if(net!=null){net.arsenalFire(alternate);return;}
        simulation.move(1,px,py,angle,(down(KeyEvent.VK_C)?1:0)|(down(KeyEvent.VK_SHIFT)?2:0));
        var r=simulation.arsenal.fire(1,alternate,++arsenalSequence);if(r.fired()){attackFlash=.12;if(r.damage()>0)toast("HIT "+r.damage());}
        else if(!java.util.List.of("COOLDOWN","CHARGING","COOLING","NO_ACTIVE_PROJECTILE","NO_AMMO").contains(r.reason()))toast(r.reason().replace('_',' '));syncSolo(false);
    }
    private void reload(){if(dead)return;if(net!=null)net.arsenalReload();else{var r=simulation.arsenal.reload(1);toast(r.reason().replace('_',' '));}}
    private void selectWeapon(int w){if(net!=null)net.arsenalSelect(w);else{var r=simulation.arsenal.select(1,w);if(!r.ok())toast(r.reason().replace('_',' '));syncSolo(false);}}
    private void cycleWeapon(int step){var v=arsenalView();if(v==null)return;for(int i=1;i<=32;i++){int w=Math.floorMod(v.selected()+step*i,32);if((v.owned()&(1L<<w))!=0){selectWeapon(w);return;}}}
    private void releaseTrigger(){if(net!=null)net.arsenalRelease();else simulation.arsenal.release(1);}
    private void craftingMenu(){
        primaryHeld=alternateHeld=false;held.clear();releaseTrigger();
        if(fabrication!=null&&fabrication.isDisplayable()){fabrication.setVisible(!fabrication.isVisible());return;}
        fabrication=new JDialog(window,"Inventory / Fabrication",false);
        fabrication.setContentPane(new FabricationPanel(this::arsenalView,
            code->{if(net!=null)net.arsenalCraft(code);else toast(simulation.arsenal.craft(1,code).reason().replace('_',' '));},
            this::selectWeapon, f->{if(net!=null)net.arsenalFaction(f);else toast(simulation.arsenal.faction(1,f).reason().replace('_',' '));},
            ()->{if(net!=null)net.arsenalUpgrade();else toast(simulation.arsenal.upgrade(1).reason().replace('_',' '));},()->deploy(4)));
        fabrication.pack();fabrication.setLocationRelativeTo(window);fabrication.setVisible(true);
    }
    private void deploy(int kind){
        if(dead)return;
        if(net!=null){net.build(kind);return;}
        simulation.move(1,px,py,angle,0);var r=simulation.buildOutcome(1,kind);syncSolo(false);toast(r.built()?"CONSTRUCTION ACCEPTED":r.review().reason().replace('_',' '));
    }
    private void ability(){
        if(dead||abilityCooldown>0)return;
        abilityCooldown=18;
        if(net!=null){net.ability();return;}
        simulation.ability(1);syncSolo(false);if(archetype==2)abilitySeconds=7;
    }
    private void respawnAt(int index){
        if(!dead)return;
        spawn=index;
        if(net!=null){
            if(net.respawnRemainingMs()>0){toast("RESPAWN COUNTDOWN ACTIVE");return;}
            if(net.spawnScores()[index]>=1000){toast("SPAWN POINT BLOCKED");return;}
            if(respawnPending)return;
            respawnPending=true;net.respawn(index);toast("RESPAWN REQUESTED");return;
        }
        if(simulation.respawn(1,index)){syncSolo(true);toast("RESPAWN: SECTOR "+(index+1));}else toast("RESPAWN COUNTDOWN OR OBSTRUCTION");
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
        java.util.List<UrbanScene.ModelInstance> cars=new java.util.ArrayList<>();
        if(net!=null){
            for(NetLink.Car car:net.cars.values())if(car.hp()>0&&car.driverId()!=net.id)
                cars.add(new UrbanScene.ModelInstance("truck-green",car.x(),car.y(),car.heading(),1.9));
        }else{
            for(Vehicles.State car:localVehicles.states())if(car.hp()>0&&car.driverId()!=1)
                cars.add(new UrbanScene.ModelInstance("truck-green",car.x(),car.y(),car.heading(),1.9));
        }
        urbanRenderer.draw(g,urban.models(),cars,px,py,angle,depth,RW,RH);
        java.util.List<UrbanScene.ModelInstance> gameplay=new java.util.ArrayList<>();
        for(World.Pickup p:world.pickups)if(!p.taken&&p.kind>=0&&p.kind<GameplayMeshes.PICKUP.length)
            gameplay.add(new UrbanScene.ModelInstance(GameplayMeshes.PICKUP[p.kind],p.x,p.y,
                ticks*.018+p.kind,1.15,1.0,.86+.10*Math.sin(ticks*.11+p.kind)));
        for(World.Device d:world.devices)if(d.hp>0&&d.kind>=0&&d.kind<GameplayMeshes.DEVICE.length)
            gameplay.add(new UrbanScene.ModelInstance(GameplayMeshes.DEVICE[d.kind],d.x,d.y,0,1.0));
        for(World.Npc n:world.npcs)if(n.hp>0)
            gameplay.add(new UrbanScene.ModelInstance("enemy-walker",n.x,n.y,
                Math.atan2(py-n.y,px-n.x)-Math.PI/2,1.0));
        if(net!=null)for(java.util.Map.Entry<Integer,NetLink.Remote> entry:net.others.entrySet()){
            NetLink.Remote p=entry.getValue();
            if(p.hp>0&&net.cars.values().stream().noneMatch(car->car.driverId()==entry.getKey()))
                gameplay.add(new UrbanScene.ModelInstance(
                    GameplayMeshes.HERO[Math.max(0,Math.min(3,p.classId))],
                    p.x,p.y,p.angle-Math.PI/2,1.0,p.opacity));
        }
        urbanRenderer.draw(g,actorModels.meshes(),gameplay,px,py,angle,depth,RW,RH);
        java.util.List<UrbanScene.ModelInstance> arsenalLoot=new java.util.ArrayList<>();
        for(var l:simulation.arsenal.loot)if(!l.taken){
            if(l.type==0)arsenalLoot.add(new UrbanScene.ModelInstance(arsenalMeshes.worldId(l.value),l.x,l.y,ticks*.018,.9,1,.88));
        }
        // Resource/ammo meshes use the existing authored pickup shapes; weapon loot uses the working arsenal.
        java.util.List<UrbanScene.ModelInstance> resourceLoot=new java.util.ArrayList<>();for(var l:simulation.arsenal.loot)if(!l.taken&&l.type!=0)resourceLoot.add(new UrbanScene.ModelInstance(GameplayMeshes.PICKUP[l.type==1?5:2],l.x,l.y,ticks*.012,1,1,.7));
        urbanRenderer.draw(g,actorModels.meshes(),resourceLoot,px,py,angle,depth,RW,RH);
        urbanRenderer.draw(g,arsenalMeshes.worldModels(),arsenalLoot,px,py,angle,depth,RW,RH);
        ArsenalEffects.draw(g,net==null?simulation.arsenal.effects():net.arsenalEffects(),world,px,py,angle,RW,RH);
        g.setColor(new Color(187,176,149));
        g.fillRect(RW/2-5,RH/2,11,1);g.fillRect(RW/2,RH/2-5,1,11);
        int bob=(int)(Math.sin(ticks*.15)*(down(KeyEvent.VK_W)?2:0));
        var av=arsenalView();long age=av==null?0:(net==null?simulation.time():System.nanoTime()/1_000_000)-av.actionAt();
        arsenalMeshes.draw(g,av==null?0:av.selected(),av,age,RW,RH,bob);
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
    /** A disposed/reparented Swing view must not keep an event timer running. */
    @Override public void removeNotify(){
        timer.stop();
        super.removeNotify();
    }
    @Override public void addNotify(){
        super.addNotify();
        if(timer!=null)timer.start();
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
        var inventory=arsenalView();int equipped=inventory==null?0:inventory.selected();
        g.drawString(ArsenalCatalog.get(equipped).name()+" "+(inventory==null?0:inventory.clip()[equipped])+"/"+(inventory==null?0:inventory.reserve()[equipped])+
            "     "+names[archetype]+"  F "+(abilityCooldown==0?"READY":String.format("%.0fs",abilityCooldown)),105,h-17);
        if(inventory!=null&&(inventory.reloadRemaining()>0||inventory.action()==2)){g.setColor(new Color(232,184,103));g.drawString(inventory.reloadRemaining()>0?"RELOADING":"CHARGING",w-170,h-42);}
        double gem=net!=null&&net.self()!=null?net.self().visibility():visibility();
        drawGem(g,gem,31,h-35);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
        g.setColor(new Color(236,234,228));
        g.drawString("THE MALEFIC VEIL",15,22);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        g.drawString("SEED "+world.seed+"  "+(net==null?"SOLO":"LAN PLAYERS "+(net.others.size()+1)),w-218,22);
        if(net!=null){
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            String diagnostic=net.connected()?
                "HOST RTT "+(net.roundTripMs()<0?"--":net.roundTripMs()+"ms"):"HOST DISCONNECTED";
            g.setColor(net.connected()?new Color(181,196,177):new Color(247,107,105));
            g.drawString(diagnostic,w-218,39);
        }
        if(noticeTicks>0){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));g.setColor(new Color(234,202,132));g.drawString(notice,15,52);}
        if(continentVisible)drawContinentMap(g,w,h);
        else if(mapVisible)drawMap(g,w,h);
        int activeCar=net==null?localVehicles.carDrivenBy(1):net.drivenCar();
        if(activeCar>=0){
            double speed=0,fuel=0;
            if(net==null){Vehicles.State car=localVehicles.state(activeCar);
                if(car!=null){speed=car.speed();fuel=car.fuel();}}
            else{NetLink.Car car=net.cars.get(activeCar);
                if(car!=null){speed=car.speed();fuel=car.fuel();}}
            g.setColor(new Color(229,211,152));
            g.drawString(String.format("CAR  SPEED %.1f  FUEL %.0f%%  Q exit",speed,fuel),15,70);
        }
        if(help&&!dead){g.setColor(new Color(9,10,17,220));g.fillRect(12,64,440,100);
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            g.drawString("WASD move / Mouse or arrows turn / Shift sprint / C crouch",23,87);
            g.drawString("LMB/SPACE fire  RMB alt  1–8 / [ ] weapons  R reload",23,107);
            g.drawString("T turret  H heal  V revive  B barricade  E medkit",23,127);
            g.drawString("K inventory / craft  J bench  Q car  M/N maps  F ability",23,147);}
        if(net!=null&&!net.connected()){
            g.setColor(new Color(0,0,0,220));g.fillRect(0,h/3,w,h/3);
            g.setColor(new Color(255,126,126));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,23));
            g.drawString("HOST CONNECTION LOST",Math.max(15,w/2-170),h/2);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
            g.drawString("This match is no longer synchronized. Close to reconnect.",
                Math.max(15,w/2-235),h/2+27);
        }
        if(dead){
            int panelWidth=Math.min(590,w-30),left=(w-panelWidth)/2,top=Math.max(46,(h-282)/2);
            g.setColor(new Color(10,12,19,238));g.fillRoundRect(left,top,panelWidth,282,14,14);
            g.setColor(new Color(238,114,128));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,24));
            g.drawString("YOU HAVE FALLEN",left+20,top+37);
            long wait=net==null?simulation.respawnRemainingMs(1):net.respawnRemainingMs();
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
            g.setColor(wait>0?new Color(240,191,103):new Color(127,224,161));
            g.drawString(wait>0?"RESPAWN IN "+String.format("%.1f",wait/1000.0)+"s":"RESPAWN READY",
                left+20,top+64);
            int[] risks=net==null?new int[]{0,0,0}:net.spawnScores();
            String[] labels={"NORTH","CENTER","SOUTH"};
            for(int i=0;i<3;i++){
                int score=risks[i],rowTop=top+79+i*52;
                boolean blocked=score>=1000,selected=spawn==i;
                g.setColor(selected?new Color(54,65,80):new Color(28,32,41));
                g.fillRoundRect(left+16,rowTop,panelWidth-32,46,8,8);
                g.setColor(blocked?new Color(238,114,128):
                    score>=55?new Color(240,154,111):score>=20?new Color(244,211,134):
                    new Color(133,218,164));
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
                g.drawString("["+ (i+1) +"]  "+labels[i],left+30,rowTop+29);
                String status=blocked?"BLOCKED":score>=55?"DANGER":score>=20?"CAUTION":"SAFE";
                g.drawString(status,left+panelWidth-155,rowTop+29);
            }
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            g.drawString("1–3 SELECT  /  CLICK ROW  /  ENTER DEPLOY",left+20,top+258);
            if(net!=null&&!net.connected())g.drawString("CONNECTION LOST — RECONNECT REQUIRED",left+20,top+275);
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
            CityLayout.Category category=world.city.categoryAt(x,y);
            Color tint=category==null?new Color(43,45,54):switch(category){
                case HAZARD -> new Color(184,77,73);
                case RESOURCE -> new Color(95,152,81);
                case SAFE -> new Color(75,132,172);
                case OPEN -> new Color(169,144,104);
            };
            g.setColor(world.tile(x,y)==0?tint:tint.darker().darker());
            g.fillRect(left+x*tile,top+y*tile,tile,tile);
        }
        for(double[] s:world.spawns){g.setColor(Color.YELLOW);g.fillRect(left+(int)(s[0]*tile)-2,top+(int)(s[1]*tile)-2,5,5);}
        g.setColor(Color.CYAN);g.fillOval(left+(int)(px*tile)-3,top+(int)(py*tile)-3,7,7);
    }
    /** Continental overview is a generation preview; actual traversal is still city-sector only. */
    private void drawContinentMap(Graphics2D g,int w,int h){
        ContinentLayout map=world.continent();
        int tile=2,left=w-ContinentLayout.SIZE*tile-16,top=35;
        g.setColor(new Color(8,9,13,225));
        g.fillRect(left-6,top-6,ContinentLayout.SIZE*tile+12,ContinentLayout.SIZE*tile+12);
        for(int x=0;x<ContinentLayout.SIZE;x++)for(int y=0;y<ContinentLayout.SIZE;y++){
            Color tint=switch(map.biome(x,y)){
                case WATER -> new Color(37,67,107);
                case BEACH -> new Color(170,149,108);
                case PLAINS -> new Color(99,145,78);
                case FOREST -> new Color(38,91,60);
                case HILLS -> new Color(121,121,91);
                case MOUNTAINS -> new Color(131,136,141);
                case MARSH -> new Color(81,107,91);
            };
            if(map.cityAt(x,y)!='\0')tint=map.worldStructureAt(x,y)?new Color(108,104,109):new Color(135,128,98);
            if(map.road(x,y))tint=new Color(192,186,165);
            g.setColor(tint);g.fillRect(left+x*tile,top+y*tile,tile,tile);
        }
        for(ContinentLayout.City city:map.cities()){
            g.setColor(Color.BLACK);g.fillOval(left+city.x()*tile-8,top+city.y()*tile-8,16,16);
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.drawString(String.valueOf(city.id()),left+city.x()*tile-4,top+city.y()*tile+5);
        }
    }
    @Override public void keyPressed(KeyEvent e){
        int k=e.getKeyCode();
        boolean fresh=held.add(k);
        if(!fresh)return;
        if(dead){
            if(k>=KeyEvent.VK_1&&k<=KeyEvent.VK_3)spawn=k-KeyEvent.VK_1;
            if(k==KeyEvent.VK_ENTER)respawnAt(spawn);
            return;
        }
        switch(k){
            case KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8 -> selectWeapon(k-KeyEvent.VK_1);
            case KeyEvent.VK_OPEN_BRACKET -> cycleWeapon(-1);
            case KeyEvent.VK_CLOSE_BRACKET -> cycleWeapon(1);
            case KeyEvent.VK_K -> craftingMenu();
            case KeyEvent.VK_J -> deploy(4);
            case KeyEvent.VK_R -> reload();
            case KeyEvent.VK_F -> ability();
            case KeyEvent.VK_T -> deploy(0);
            case KeyEvent.VK_H -> deploy(1);
            case KeyEvent.VK_V -> deploy(2);
            case KeyEvent.VK_E -> {
                if(net!=null)net.useMedkit();
                else {simulation.useMedkit(1);syncSolo(false);}
            }
            case KeyEvent.VK_B -> deploy(3);
            case KeyEvent.VK_Q -> toggleCar();
            case KeyEvent.VK_SPACE -> shoot();
            case KeyEvent.VK_M -> {mapVisible=!mapVisible;continentVisible=false;}
            case KeyEvent.VK_N -> {continentVisible=!continentVisible;mapVisible=false;}
            case KeyEvent.VK_F10 -> performanceDialog();
            case KeyEvent.VK_TAB,KeyEvent.VK_ESCAPE -> help=!help;
        }
    }
    /** Compact graphics panel; local display preferences do not modify network simulation. */
    private void performanceDialog(){
        JSlider distance=new JSlider(4,64,performance.propDistance);
        JSlider detail=new JSlider(2,32,performance.highDetailDistance);
        JSlider models=new JSlider(8,500,performance.meshBudget);
        JSlider cap=new JSlider(15,30,performance.frameCap);
        JPanel panel=new JPanel(new GridLayout(4,2,8,3));
        panel.add(new JLabel("View distance"));panel.add(distance);
        panel.add(new JLabel("High detail range"));panel.add(detail);
        panel.add(new JLabel("Visible model budget"));panel.add(models);
        panel.add(new JLabel("Target FPS"));panel.add(cap);
        if(JOptionPane.showConfirmDialog(window,panel,"Graphics / LOD",JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        performance.apply(distance.getValue(),detail.getValue(),models.getValue(),cap.getValue());
        urbanRenderer.setBudgets(performance);
        // Simulation remains at 30 ticks/s regardless of the frame rendering cap.
        toast("GRAPHICS SETTINGS SAVED");
    }
    private void toggleCar(){
        if(net!=null){
            if(net.drivenCar()>=0){net.exitCar();return;}
            int chosen=-1;double best=2.0;
            for(NetLink.Car car:net.cars.values()){
                double dist=Math.hypot(px-car.x(),py-car.y());
                if(car.hp()>0&&car.driverId()<0&&dist<best){best=dist;chosen=car.id();}
            }
            if(chosen<0)toast("NO VEHICLE IN REACH");else net.ride(chosen);
        }else{
            if(localVehicles.carDrivenBy(1)>=0){
                if(!simulation.exitVehicle(1))toast("STOP AND CLEAR THE DOOR");else{syncSolo(true);toast("EXITED VEHICLE");}
                return;
            }
            int chosen=-1;double best=2.0;
            for(Vehicles.State car:localVehicles.states()){
                double dist=Math.hypot(px-car.x(),py-car.y());
                if(car.hp()>0&&car.driverId()<0&&dist<best){best=dist;chosen=car.id();}
            }
            if(chosen<0||!simulation.enterVehicle(1,chosen))toast("NO VEHICLE IN REACH");
            else toast("ENGINE ENGAGED");
        }
    }
    @Override public void keyReleased(KeyEvent e){held.remove(e.getKeyCode());if(e.getKeyCode()==KeyEvent.VK_SPACE)releaseTrigger();}
    @Override public void keyTyped(KeyEvent e){}
    @Override public void mousePressed(MouseEvent e){
        requestFocusInWindow();
        if(e.getButton()==MouseEvent.BUTTON3){alternateHeld=true;shoot(true);return;}
        if(e.getButton()!=MouseEvent.BUTTON1)return;
        if(dead){
            int top=Math.max(46,(getHeight()-282)/2);
            int left=(getWidth()-Math.min(590,getWidth()-30))/2;
            int right=getWidth()-left;
            for(int i=0;i<3;i++){
                int rowTop=top+79+i*52;
                if(e.getX()>=left+16&&e.getX()<right-16&&
                   e.getY()>=rowTop&&e.getY()<rowTop+46)spawn=i;
            }
            return;
        }
        primaryHeld=true;shoot();
    }
    @Override public void mouseReleased(MouseEvent e){if(e.getButton()==MouseEvent.BUTTON1)primaryHeld=false;if(e.getButton()==MouseEvent.BUTTON3)alternateHeld=false;releaseTrigger();}
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){previousMouse=-1;}
    @Override public void mouseExited(MouseEvent e){previousMouse=-1;}
    @Override public void mouseDragged(MouseEvent e){mouseMoved(e);}
    @Override public void mouseMoved(MouseEvent e){
        if(previousMouse>=0&&!dead)angle+=Math.max(-60,Math.min(60,e.getX()-previousMouse))*.004;
        previousMouse=e.getX();
    }
}
