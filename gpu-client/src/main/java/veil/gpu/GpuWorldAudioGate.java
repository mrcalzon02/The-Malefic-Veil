package veil.gpu;

import java.util.*;

/**
 * Host-snapshot-driven non-combat feedback. No input is treated as proof of
 * success: inventory, cash, injury and notices must actually change.
 * Entirely client-side: cannot mint items or mutate gameplay rules.
 */
public final class GpuWorldAudioGate {
    public enum Cue { INVENTORY, PICKUP, MONEY, HURT, HEAL, DEATH, DENIED, CRAFTED, DEPLOYED }
    private boolean initialized;
    private int health,cash,frames,lastHurtFrame=-10000;
    private boolean dead,inventory;
    private Map<String,Integer> items=Map.of();
    private String notice="";
    public EnumSet<Cue> accept(int nowHealth,int nowCash,boolean nowDead,
            boolean nowInventory,Map<String,Integer> nowItems,String nowNotice){
        Objects.requireNonNull(nowItems);
        nowNotice=nowNotice==null?"":nowNotice;
        EnumSet<Cue> out=EnumSet.noneOf(Cue.class);
        if(initialized){
            if(nowDead&&!dead)out.add(Cue.DEATH);
            else if(!nowDead&&nowHealth<health&&frames-lastHurtFrame>=12){
                out.add(Cue.HURT);lastHurtFrame=frames;
            }else if(!nowDead&&nowHealth>health&&!dead)out.add(Cue.HEAL);
            if(nowInventory&&!inventory)out.add(Cue.INVENTORY);
            if(nowCash>cash)out.add(Cue.MONEY);
            if(hasAddedItems(nowItems,items))out.add(Cue.PICKUP);
            if(!nowNotice.equals(notice)&&!nowNotice.isBlank()){
                String message=nowNotice.toUpperCase(Locale.ROOT);
                if(message.contains("DENIED")||message.contains("REJECTED")||
                   message.contains("FAILED")||message.contains("NOT OWNED")||
                   message.startsWith("NO ")||message.startsWith("CANNOT "))
                    out.add(Cue.DENIED);
                else if(message.startsWith("THROWN FIELD")||
                        message.contains("DEPLOYED")||message.contains("FIELD PLACED"))
                    out.add(Cue.DEPLOYED);
                else if(message.startsWith("CRAFTED ")||
                        message.contains("CONSTRUCTION ACCEPTED")||
                        message.contains("FACILITY PLACED"))
                    out.add(Cue.CRAFTED);
            }
        }
        health=nowHealth;cash=nowCash;dead=nowDead;inventory=nowInventory;
        items=Map.copyOf(nowItems);notice=nowNotice;frames++;initialized=true;
        return out;
    }
    private static boolean hasAddedItems(Map<String,Integer> current,Map<String,Integer> previous){
        for(var entry:current.entrySet())
            if(entry.getValue()!=null&&entry.getValue()>previous.getOrDefault(entry.getKey(),0))
                return true;
        return false;
    }
    public void reset(){initialized=false;frames=0;lastHurtFrame=-10000;items=Map.of();notice="";}
    public static void main(String[] args){
        GpuWorldAudioGate gate=new GpuWorldAudioGate();
        assertCues(gate.accept(100,10,false,false,Map.of("WOOD",2),""),new Cue[]{});
        assertCues(gate.accept(100,10,false,false,Map.of("WOOD",2),""),new Cue[]{});
        assertCues(gate.accept(90,10,false,true,Map.of("WOOD",2),""),Cue.HURT,Cue.INVENTORY);
        assertCues(gate.accept(90,25,false,true,Map.of("WOOD",3),"CRAFTED TOOL"),Cue.MONEY,Cue.PICKUP,Cue.CRAFTED);
        assertCues(gate.accept(90,25,false,true,Map.of("WOOD",3),"CRAFTED TOOL"),new Cue[]{});
        assertCues(gate.accept(100,25,false,false,Map.of("WOOD",3),"CANNOT CRAFT"),Cue.HEAL,Cue.DENIED);
        assertCues(gate.accept(0,25,true,false,Map.of("WOOD",3),"YOU HAVE FALLEN"),Cue.DEATH);
        assertCues(gate.accept(100,25,false,false,Map.of("WOOD",3),""),new Cue[]{});
        assertCues(gate.accept(100,25,false,false,Map.of("WOOD",3),"THROWN FIELD WALL DEPLOYED"),Cue.DEPLOYED);
        gate.reset();
        assertCues(gate.accept(12,500,true,true,Map.of("WOOD",30),"CRAFTED TOOL"),new Cue[]{});
        System.out.println("PASS GPU_WORLD_AUDIO_GATE authoritative health,inventory,cash,notice");
    }
    private static void assertCues(EnumSet<Cue> actual,Cue... expected) {
        EnumSet<Cue> want=EnumSet.noneOf(Cue.class);
        for(Cue cue:expected)want.add(cue);
        if(!actual.equals(want))throw new AssertionError("Expected "+want+" got "+actual);
    }
}
