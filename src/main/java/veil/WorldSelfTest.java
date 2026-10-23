package veil;

import java.util.ArrayDeque;

/** Run with: java -cp out veil.WorldSelfTest */
public final class WorldSelfTest {
    private static void check(boolean yes,String message) {
        if(!yes)throw new AssertionError(message);
    }
    public static void main(String[] args){
        long[] seeds={0,1,5,42,20261020L,-1,Long.MAX_VALUE};
        for(long seed:seeds){
            World a=new World(seed),b=new World(seed);
            int open=0;
            for(int x=0;x<World.SIZE;x++)for(int y=0;y<World.SIZE;y++){
                check(a.tile(x,y)==b.tile(x,y),"non-deterministic geometry");
                if(a.tile(x,y)==0)open++;
            }
            check(open>World.SIZE*World.SIZE/2,"too few playable tiles");
            check(a.npcs.size()==45&&a.pickups.size()==170,"incorrect spawn populations");
            boolean[][] seen=new boolean[World.SIZE][World.SIZE];
            ArrayDeque<int[]> q=new ArrayDeque<>();
            int sx=(int)a.spawns[0][0],sy=(int)a.spawns[0][1];
            q.add(new int[]{sx,sy});seen[sx][sy]=true;
            int[][] dir={{0,1},{0,-1},{1,0},{-1,0}};
            while(!q.isEmpty()){
                int[] t=q.removeFirst();
                for(int[] d:dir){
                    int x=t[0]+d[0],y=t[1]+d[1];
                    if(x<0||y<0||x>=World.SIZE||y>=World.SIZE||seen[x][y]||a.tile(x,y)!=0)continue;
                    seen[x][y]=true;q.addLast(new int[]{x,y});
                }
            }
            for(double[] s:a.spawns) {
                int x=(int)s[0],y=(int)s[1];
                check(a.canWalk(s[0],s[1]),"spawn blocked");
                check(seen[x][y],"spawn inaccessible on seed "+seed);
            }
            for(World.Pickup item:a.pickups){
                check(a.canWalk(item.x,item.y),"pickup inside wall or urban collider");
                CityLayout.Category zone=a.city.categoryAt((int)item.x,(int)item.y);
                if(item.kind==0||item.kind==6)check(zone==CityLayout.Category.SAFE,"medical pickup outside safe zone");
                if(item.kind>=2&&item.kind<=5)check(zone==CityLayout.Category.RESOURCE,"supplies outside resource plot");
            }
            for(World.Npc npc:a.npcs){
                check(a.canWalk(npc.x,npc.y),"npc inside wall or urban collider");
                CityLayout.Category zone=a.city.categoryAt((int)npc.x,(int)npc.y);
                check(zone==CityLayout.Category.HAZARD||zone==CityLayout.Category.OPEN,"hostile spawned in protected zone");
            }
            check(!a.lineClear(-4,-4,10,10),"line-of-sight crossed outside city");
            System.out.println("PASS seed "+seed+": "+open+" open tiles, spawns connected");
        }
        System.out.println("ALL WORLD INVARIANTS PASS");
    }
}
