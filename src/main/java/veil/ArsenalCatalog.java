package veil;

import java.util.*;

/** Generated from the authored runtime table; indices 0..7 are the ONLY map weapon loot. */
public final class ArsenalCatalog {
    public enum Kind { HIT, PELLET, BEAM, RAIL, BOLT, CHAIN, ORB, ROCKET, MISSILE, GRENADE, MORTAR, FLAK, DISC, BOUNCE, SCRAP, BIO, CRYO, GRAVITY, SINGULARITY, DETONATE, PULSE, STICKY, RECALL, SMOKE, VORTEX }
    public record Mode(Kind kind,int damage,int pellets,int cooldown,double speed,double radius,int bounces,int charge) {}
    public record Weapon(int index,String id,String name,String mechanism,int capacity,int reloadMs,int tier,int faction,int prerequisite,Mode primary,Mode alternate,int[] cost) {
        public boolean mapSpawn(){return index<8;}
        public int[] cost(){return cost.clone();}
    }
    public static final String[] FACTIONS={"The Unraveled","The Penitent","The Begotten"};
    public static final String[] MATERIALS={"Scrap","Electronics","Chemicals","Alloy","Machined frame","Power cell","Arc coil","Warhead","Anomaly core"};
    public static final int[][] COMPONENT_COSTS={{6,0,0,2,0,0,0,0,0},{0,4,2,0,0,0,0,0,0},{0,6,0,2,0,0,0,0,0},{3,0,5,0,0,0,0,0,0},{0,0,5,0,0,2,1,0,0}};
    public static final Weapon[] WEAPONS={
        new Weapon(0,"w01-vesper","Vesper","slide",12,1400,0,-1,-1,new Mode(Kind.HIT,24,1,250,0,0,0,0),new Mode(Kind.PELLET,15,3,700,0,0,0,0),new int[]{3,0,0,1,0,0,0,0,0}),
        new Weapon(1,"w02-ossuary","Ossuary","cylinder",6,1400,0,-1,-1,new Mode(Kind.HIT,68,1,830,0,0,0,0),new Mode(Kind.RAIL,95,1,1200,0,0,0,700),new int[]{4,0,0,1,0,0,0,0,0}),
        new Weapon(2,"w03-needle-choir","Needle Choir","bolt",30,1400,0,-1,-1,new Mode(Kind.HIT,7,1,65,0,0,0,0),new Mode(Kind.HIT,11,6,600,0,0,0,0),new int[]{5,0,0,1,0,0,0,0,0}),
        new Weapon(3,"w04-rivet-saint","Rivet Saint","bolt",12,1400,0,-1,-1,new Mode(Kind.BOLT,32,1,200,26,0,0,0),new Mode(Kind.BOLT,50,1,750,23,0,0,300),new int[]{6,0,0,1,0,0,0,0,0}),
        new Weapon(4,"w05-grave-bell","Grave Bell","hinge",2,2200,0,-1,-1,new Mode(Kind.PELLET,11,10,1100,0,0,0,0),new Mode(Kind.PELLET,9,20,1800,0,0,0,0),new int[]{7,0,0,1,0,0,0,0,0}),
        new Weapon(5,"w06-cerberus","Cerberus","hinge",3,2200,0,-1,-1,new Mode(Kind.PELLET,10,8,600,0,0,0,0),new Mode(Kind.PELLET,9,24,1800,0,0,0,0),new int[]{8,0,0,1,0,0,0,0,0}),
        new Weapon(6,"w07-reliquary","Reliquary","pump",6,2200,0,-1,-1,new Mode(Kind.PELLET,9,12,1000,0,0,0,0),new Mode(Kind.HIT,85,1,1100,0,0,0,0),new int[]{9,0,0,1,0,0,0,0,0}),
        new Weapon(7,"w08-furnace-organ","Furnace Organ","rotor",160,2200,0,-1,-1,new Mode(Kind.HIT,8,1,70,0,0,0,650),new Mode(Kind.PELLET,9,12,1000,0,0,0,0),new int[]{10,0,0,1,0,0,0,0,0}),
        new Weapon(8,"w09-hailstorm","Hailstorm","rotor",160,2200,2,0,7,new Mode(Kind.HIT,9,2,85,0,0,0,650),new Mode(Kind.HIT,13,2,130,0,0,0,300),new int[]{6,3,0,3,1,0,0,0,0}),
        new Weapon(9,"w10-cautery","Cautery","capacitor",40,2200,1,1,-1,new Mode(Kind.BEAM,22,1,180,0,0,0,0),new Mode(Kind.BEAM,9,1,100,0,0,0,0),new int[]{6,2,0,2,1,1,0,0,0}),
        new Weapon(10,"w11-prism-writ","Prism Writ","prongs",40,2200,2,1,9,new Mode(Kind.BEAM,22,3,400,0,0,0,0),new Mode(Kind.BEAM,100,1,1300,0,0,0,800),new int[]{6,3,0,3,1,1,0,0,0}),
        new Weapon(11,"w12-longwake","Longwake","bolt",5,2200,2,1,9,new Mode(Kind.RAIL,125,1,1400,0,0,0,0),new Mode(Kind.HIT,78,1,700,0,0,0,0),new int[]{6,3,0,3,1,1,0,0,0}),
        new Weapon(12,"w13-ion-psalm","Ion Psalm","capacitor",40,2200,2,1,9,new Mode(Kind.BOLT,38,1,200,16,0,0,0),new Mode(Kind.ORB,110,1,1400,6,3,0,600),new int[]{7,3,0,3,1,1,0,0,0}),
        new Weapon(13,"w14-sun-eater","Sun Eater","vents",3,2200,3,1,12,new Mode(Kind.BEAM,30,1,200,0,0,0,350),new Mode(Kind.BEAM,200,1,2800,0,0,0,2000),new int[]{7,4,0,4,1,1,0,0,0}),
        new Weapon(14,"w15-storm-rosary","Storm Rosary","prongs",30,2200,2,1,9,new Mode(Kind.CHAIN,48,1,600,0,0,0,0),new Mode(Kind.ORB,65,1,1100,6,3,0,300),new int[]{7,3,0,3,1,1,1,0,0}),
        new Weapon(15,"w16-forked-benediction","Forked Benediction","prongs",30,2200,2,1,14,new Mode(Kind.CHAIN,28,2,450,0,0,0,0),new Mode(Kind.CHAIN,45,3,1000,0,0,0,300),new int[]{7,3,0,3,1,1,1,0,0}),
        new Weapon(16,"w17-ball-lightning","Ball Lightning","capacitor",30,2200,2,1,14,new Mode(Kind.ORB,75,1,800,7,2,0,0),new Mode(Kind.DETONATE,110,1,800,0,3,0,0),new int[]{8,3,0,3,1,1,1,0,0}),
        new Weapon(17,"w18-thunder-loom","Thunder Loom","rotor",30,2200,3,1,15,new Mode(Kind.CHAIN,24,1,220,0,0,0,450),new Mode(Kind.PULSE,80,1,1300,0,4,0,400),new int[]{8,4,0,4,1,1,1,0,0}),
        new Weapon(18,"w19-pilgrim","Pilgrim","rack",3,3000,1,0,-1,new Mode(Kind.ROCKET,105,1,900,14,2.7,0,0),new Mode(Kind.ROCKET,90,3,1900,14,2.5,0,600),new int[]{8,2,0,2,1,0,0,1,0}),
        new Weapon(19,"w20-widow-choir","Widow Choir","rack",12,3000,2,0,18,new Mode(Kind.MISSILE,32,4,1400,11,1.4,0,0),new Mode(Kind.MISSILE,40,4,2000,12,1.5,0,800),new int[]{8,3,0,3,1,0,0,1,0}),
        new Weapon(20,"w21-basilisk","Basilisk","bolt",3,3000,2,0,18,new Mode(Kind.ROCKET,150,1,1600,11,3.5,0,0),new Mode(Kind.ROCKET,95,1,1900,11,4,0,350),new int[]{9,3,0,3,1,0,0,1,0}),
        new Weapon(21,"w22-censer","Censer","drum",6,3000,2,0,18,new Mode(Kind.GRENADE,100,1,900,10,2.8,3,0),new Mode(Kind.STICKY,80,1,900,9,2.8,0,0),new int[]{9,3,0,3,1,0,0,1,0}),
        new Weapon(22,"w23-vigil-mortar","Vigil Mortar","hinge",4,3000,2,0,21,new Mode(Kind.MORTAR,130,1,1800,10,3.5,0,0),new Mode(Kind.MORTAR,38,5,2300,9,1.9,0,300),new int[]{9,3,0,3,1,0,0,1,0}),
        new Weapon(23,"w24-cathedral","Cathedral","braces",4,3000,3,0,22,new Mode(Kind.MORTAR,220,1,3200,9,5,0,700),new Mode(Kind.SMOKE,0,1,1800,8,4,0,0),new int[]{9,4,0,4,1,0,0,1,0}),
        new Weapon(24,"w25-shrapnel-mass","Shrapnel Mass","pump",8,3000,2,0,21,new Mode(Kind.FLAK,10,14,1100,0,0,0,0),new Mode(Kind.GRENADE,90,1,1300,11,2.7,1,0),new int[]{10,3,0,3,1,0,0,1,0}),
        new Weapon(25,"w26-ricochet-gospel","Ricochet Gospel","disc",8,3000,1,2,-1,new Mode(Kind.DISC,65,1,700,16,0,3,0),new Mode(Kind.RECALL,45,1,600,0,0,0,0),new int[]{10,2,0,2,1,0,0,0,0}),
        new Weapon(26,"w27-bankshot","Bankshot","capacitor",12,3000,2,2,25,new Mode(Kind.BOUNCE,50,1,450,15,0,4,0),new Mode(Kind.BOUNCE,32,3,950,13,0,4,0),new int[]{10,3,0,3,1,0,0,0,0}),
        new Weapon(27,"w28-scrap-cyclone","Scrap Cyclone","drum",40,3000,2,0,24,new Mode(Kind.SCRAP,8,18,1100,0,0,0,0),new Mode(Kind.RAIL,115,1,1700,0,0,0,700),new int[]{10,3,0,3,1,0,0,0,0}),
        new Weapon(28,"w29-miasma","Miasma","tank",60,3000,1,2,-1,new Mode(Kind.BIO,28,1,400,11,1.8,0,0),new Mode(Kind.BIO,15,1,1500,8,2.4,0,0),new int[]{11,2,0,2,1,0,0,0,0}),
        new Weapon(29,"w30-winter-verdict","Winter Verdict","tank",60,3000,2,2,28,new Mode(Kind.CRYO,12,1,200,0,0,0,0),new Mode(Kind.BOLT,80,1,1100,22,0,0,350),new int[]{11,3,0,3,1,0,0,0,0}),
        new Weapon(30,"w31-eventide","Eventide","rings",3,3000,3,2,26,new Mode(Kind.GRAVITY,60,1,850,13,2,0,0),new Mode(Kind.VORTEX,20,1,1800,7,3,0,600),new int[]{11,4,0,4,1,0,1,0,1}),
        new Weapon(31,"w32-veilbreaker","Veilbreaker","rings",1,3000,3,2,30,new Mode(Kind.SINGULARITY,240,1,2600,8,5,0,1500),new Mode(Kind.BEAM,180,1,2400,0,0,0,1800),new int[]{11,4,0,4,1,0,1,0,1}),
    };
    public static int factionIndex(CharacterRoster.Faction group){
        if(group==null)return -1;
        return switch(group){
            case THE_UNRAVELED -> 0;
            case THE_PENITENT -> 1;
            case THE_BEGOTTEN -> 2;
        };
    }
    public static Weapon get(int n){if(n<0||n>=32)throw new IllegalArgumentException("Unknown weapon");return WEAPONS[n];}
}
