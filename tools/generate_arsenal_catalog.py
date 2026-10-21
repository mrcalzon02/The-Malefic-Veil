import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
ws=json.loads((R/'data/weapons.json').read_text())['weapons']
# kind, damage, pellets, cooldown ms, speed (m/s), blast radius, bounces, charge ms
specs=[
('HIT',24,1,250,0,0,0,0),('HIT',68,1,830,0,0,0,0),('HIT',7,1,65,0,0,0,0),('BOLT',32,1,200,26,0,0,0),
('PELLET',11,10,1100,0,0,0,0),('PELLET',10,8,600,0,0,0,0),('PELLET',9,12,1000,0,0,0,0),('HIT',8,1,70,0,0,0,650),
('HIT',9,2,85,0,0,0,650),('BEAM',22,1,180,0,0,0,0),('BEAM',22,3,400,0,0,0,0),('RAIL',125,1,1400,0,0,0,0),
('BOLT',38,1,200,16,0,0,0),('BEAM',30,1,200,0,0,0,350),('CHAIN',48,1,600,0,0,0,0),('CHAIN',28,2,450,0,0,0,0),
('ORB',75,1,800,7,2,0,0),('CHAIN',24,1,220,0,0,0,450),('ROCKET',105,1,900,14,2.7,0,0),('MISSILE',32,4,1400,11,1.4,0,0),
('ROCKET',150,1,1600,11,3.5,0,0),('GRENADE',100,1,900,10,2.8,3,0),('MORTAR',130,1,1800,10,3.5,0,0),('MORTAR',220,1,3200,9,5,0,700),
('FLAK',10,14,1100,0,0,0,0),('DISC',65,1,700,16,0,3,0),('BOUNCE',50,1,450,15,0,4,0),('SCRAP',8,18,1100,0,0,0,0),
('BIO',28,1,400,11,1.8,0,0),('CRYO',12,1,200,0,0,0,0),('GRAVITY',60,1,850,13,2,0,0),('SINGULARITY',240,1,2600,8,5,0,1500)]
alt=[
('PELLET',15,3,700,0,0,0,0),('RAIL',95,1,1200,0,0,0,700),('HIT',11,6,600,0,0,0,0),('BOLT',50,1,750,23,0,0,300),
('PELLET',9,20,1800,0,0,0,0),('PELLET',9,24,1800,0,0,0,0),('HIT',85,1,1100,0,0,0,0),('PELLET',9,12,1000,0,0,0,0),
('HIT',13,2,130,0,0,0,300),('BEAM',9,1,100,0,0,0,0),('BEAM',100,1,1300,0,0,0,800),('HIT',78,1,700,0,0,0,0),
('ORB',110,1,1400,6,3,0,600),('BEAM',200,1,2800,0,0,0,2000),('ORB',65,1,1100,6,3,0,300),('CHAIN',45,3,1000,0,0,0,300),
('DETONATE',110,1,800,0,3,0,0),('PULSE',80,1,1300,0,4,0,400),('ROCKET',90,3,1900,14,2.5,0,600),('MISSILE',40,4,2000,12,1.5,0,800),
('ROCKET',95,1,1900,11,4,0,350),('STICKY',80,1,900,9,2.8,0,0),('MORTAR',38,5,2300,9,1.9,0,300),('SMOKE',0,1,1800,8,4,0,0),
('GRENADE',90,1,1300,11,2.7,1,0),('RECALL',45,1,600,0,0,0,0),('BOUNCE',32,3,950,13,0,4,0),('RAIL',115,1,1700,0,0,0,700),
('BIO',15,1,1500,8,2.4,0,0),('BOLT',80,1,1100,22,0,0,350),('VORTEX',20,1,1800,7,3,0,600),('BEAM',180,1,2400,0,0,0,1800)]
prereq={8:7,10:9,11:9,12:9,13:12,14:9,15:14,16:14,17:15,19:18,20:18,21:18,22:21,23:22,24:21,26:25,27:24,29:28,30:26,31:30}
tiers=[0]*8+[2,1,2,2,2,3,2,2,2,3,1,2,2,2,2,3,2,1,2,2,1,2,3,3]
rows=[]
for i,w in enumerate(ws):
    faction=-1 if i<8 else 0 if i in(8,18,19,20,21,22,23,24,27) else 1 if 9<=i<=17 else 2
    cap=w['magazine'];cap=12 if i==0 else cap
    tier=tiers[i]
    cost=[4+i//4,1+tier,0,1+tier,1 if i>=8 else 0,1 if faction==1 else 0,1 if i in(14,15,16,17,30,31) else 0,1 if 18<=i<=24 else 0,1 if i in(30,31) else 0]
    if i<8:cost=[3+i,0,0,1,0,0,0,0,0]
    runtime=dict(index=i,id=w['id'],name=w['name'],map_spawn=i<8,faction=faction,tier=tier,prerequisite=prereq.get(i,-1),capacity=cap,reload_ms=int(w['reload_s']*1000),mechanism=w['mechanism'],primary=specs[i],alternate=alt[i],cost=cost)
    rows.append(runtime)
source=['package veil;','', 'import java.util.*;','', '/** Generated from the authored runtime table; indices 0..7 are the ONLY map weapon loot. */','public final class ArsenalCatalog {',
'    public enum Kind { HIT, PELLET, BEAM, RAIL, BOLT, CHAIN, ORB, ROCKET, MISSILE, GRENADE, MORTAR, FLAK, DISC, BOUNCE, SCRAP, BIO, CRYO, GRAVITY, SINGULARITY, DETONATE, PULSE, STICKY, RECALL, SMOKE, VORTEX }',
'    public record Mode(Kind kind,int damage,int pellets,int cooldown,double speed,double radius,int bounces,int charge) {}',
'    public record Weapon(int index,String id,String name,String mechanism,int capacity,int reloadMs,int tier,int faction,int prerequisite,Mode primary,Mode alternate,int[] cost) {',
'        public boolean mapSpawn(){return index<8;}','        public int[] cost(){return cost.clone();}','    }',
'    public static final String[] FACTIONS={"Foundry","Lumen","Veil"};',
'    public static final String[] MATERIALS={"Scrap","Electronics","Chemicals","Alloy","Machined frame","Power cell","Arc coil","Warhead","Anomaly core"};',
'    public static final int[][] COMPONENT_COSTS={{6,0,0,2,0,0,0,0,0},{0,4,2,0,0,0,0,0,0},{0,6,0,2,0,0,0,0,0},{3,0,5,0,0,0,0,0,0},{0,0,5,0,0,2,1,0,0}};',
'    public static final Weapon[] WEAPONS={']
for d in rows:
    modes=[]
    for key in('primary','alternate'):
        p=d[key];modes.append('new Mode(Kind.'+p[0]+','+','.join(str(x) for x in p[1:])+')')
    source.append('        new Weapon('+','.join([str(i) if False else str(d['index']),json.dumps(d['id']),json.dumps(d['name']),json.dumps(d['mechanism']),str(d['capacity']),str(d['reload_ms']),str(d['tier']),str(d['faction']),str(d['prerequisite']),*modes,'new int[]{'+','.join(map(str,d['cost']))+'}'])+'),')
source+=['    };','    public static Weapon get(int n){if(n<0||n>=32)throw new IllegalArgumentException("Unknown weapon");return WEAPONS[n];}','}']
(R/'src/main/java/veil/ArsenalCatalog.java').write_text('\n'.join(source)+'\n')
(R/'data/runtime-arsenal.json').write_text(json.dumps(rows,indent=2)+'\n')
print('Generated 32 functional weapon definitions')
