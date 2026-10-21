package veil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/** Immutable-by-interface continental blueprint. Coordinates are world-generation parcel cells,
 *  not the current 132-tile arena: runtime chunk streaming is a separate release gate. */
public final class ContinentLayout {
    public static final int SIZE = 224;
    public enum Biome { WATER, BEACH, PLAINS, FOREST, HILLS, MOUNTAINS, MARSH }
    public enum SiteShape { L_WEST, SPIRE, SQUARE, L_SOUTH, IRREGULAR, COMPACT, HORSESHOE }
    public enum Zone { HAZARD, RESOURCE, SAFE, OPEN }
    public record City(char id, int x, int y, int radiusX, int radiusY, SiteShape shape) {}
    public record Lot(char city, int x, int y, int width, int height, Zone zone, boolean developed) {}
    public record Route(char from, char to, int length) {}
    private record Node(int x, int y, int cost) {}
    private record Edge(int a, int b, double weight) {}
    private static final int[] DX = {1,-1,0,0}, DY = {0,0,1,-1};
    private static final int[][] CITY_ANCHORS = {
        {25,28,19,18}, {52,21,12,17}, {76,34,13,13},
        {29,72,19,15}, {53,49,18,15}, {50,67,8,7}, {74,75,17,12}
    };
    private final long seed;
    private final double[][] elevation = new double[SIZE][SIZE];
    private final Biome[][] biomes = new Biome[SIZE][SIZE];
    private final byte[][] cityIds = new byte[SIZE][SIZE];
    private final boolean[][] roads = new boolean[SIZE][SIZE];
    private final boolean[][] immutableStructures = new boolean[SIZE][SIZE];
    private final List<City> cities = new ArrayList<>();
    private final List<Lot> lots = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();

    private ContinentLayout(long seed) { this.seed = seed; }
    public long seed() { return seed; }
    public List<City> cities() { return Collections.unmodifiableList(cities); }
    public List<Lot> lots() { return Collections.unmodifiableList(lots); }
    public List<Route> routes() { return Collections.unmodifiableList(routes); }
    public boolean inBounds(int x,int y) { return x>=0 && y>=0 && x<SIZE && y<SIZE; }
    public double elevation(int x,int y) { return inBounds(x,y)?elevation[x][y]:-1; }
    public Biome biome(int x,int y) { return inBounds(x,y)?biomes[x][y]:Biome.WATER; }
    public boolean land(int x,int y) { return biome(x,y)!=Biome.WATER; }
    public char cityAt(int x,int y) { return inBounds(x,y)&&cityIds[x][y]!=0?(char)('A'+cityIds[x][y]-1):'\0'; }
    public boolean road(int x,int y) { return inBounds(x,y)&&roads[x][y]; }
    /** True for generated structures and roads; never treat simple natural terrain as occupied. */
    public boolean worldStructureAt(int x,int y) { return !inBounds(x,y)||immutableStructures[x][y]; }
    public boolean buildableGround(int x,int y) {
        return land(x,y) && !worldStructureAt(x,y) && biome(x,y)!=Biome.MOUNTAINS;
    }
    private static long hash(long value) {
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;
        return value^(value>>>31);
    }
    private double random(int x,int y,long salt) {
        return ((hash(seed^salt^((long)x*0x9e3779b97f4a7c15L)^((long)y*0x632be59bd9b4e019L))>>>11)*0x1.0p-53)*2-1;
    }
    private static double smooth(double t) { return t*t*(3-2*t); }
    private double noise(double x,double y,long salt) {
        int ix=(int)Math.floor(x), iy=(int)Math.floor(y);
        double sx=smooth(x-ix), sy=smooth(y-iy);
        double a=random(ix,iy,salt),b=random(ix+1,iy,salt),c=random(ix,iy+1,salt),d=random(ix+1,iy+1,salt);
        return (a+(b-a)*sx)*(1-sy)+(c+(d-c)*sx)*sy;
    }
    private double fractal(double x,double y,long salt) {
        return .58*noise(x/51,y/51,salt)+.27*noise(x/23,y/23,salt+1)+
               .15*noise(x/9,y/9,salt+2);
    }
    private void terrain() {
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            double px=(x-SIZE*.5)/(SIZE*.465), py=(y-SIZE*.49)/(SIZE*.445);
            double angle=Math.atan2(py,px);
            double radius=Math.hypot(px,py);
            double coastline=1+.07*Math.sin(5*angle+.8)+.05*Math.sin(9*angle-1.2)
                +.13*fractal(x,y,31);
            double landHeight=(coastline-radius)*1.7 + .055*fractal(x,y,71);
            elevation[x][y]=landHeight;
            if(landHeight<=0){biomes[x][y]=Biome.WATER;continue;}
            double ridge=fractal(x+79,y-33,91), wet=fractal(x-35,y+67,111);
            if(landHeight<.065)biomes[x][y]=wet>.22?Biome.MARSH:Biome.BEACH;
            else if(landHeight>.38 && ridge>.14)biomes[x][y]=Biome.MOUNTAINS;
            else if(landHeight>.26 && ridge>-.03)biomes[x][y]=Biome.HILLS;
            else biomes[x][y]=wet>-.07?Biome.FOREST:Biome.PLAINS;
            // Rocky outcrops and occasional trees are world features, not the whole biome.
            if(biomes[x][y]==Biome.MOUNTAINS && random(x,y,213)>.25 ||
               biomes[x][y]==Biome.FOREST && random(x,y,214)>.83)
                immutableStructures[x][y]=true;
        }
        // Remove detached terrain islands; roads and cities must belong to one continent.
        boolean[][] seen=new boolean[SIZE][SIZE];
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        int mid=SIZE/2;
        if(!land(mid,mid))throw new IllegalStateException("Continent center flooded");
        seen[mid][mid]=true;queue.add(mid*SIZE+mid);
        while(!queue.isEmpty()){
            int p=queue.removeFirst(),x=p/SIZE,y=p%SIZE;
            for(int i=0;i<4;i++){
                int xx=x+DX[i], yy=y+DY[i];
                if(inBounds(xx,yy)&&land(xx,yy)&&!seen[xx][yy]){
                    seen[xx][yy]=true;queue.addLast(xx*SIZE+yy);
                }
            }
        }
        for(int x=0;x<SIZE;x++)for(int y=0;y<SIZE;y++)if(!seen[x][y]){
            biomes[x][y]=Biome.WATER;immutableStructures[x][y]=false;elevation[x][y]=-1;
        }
    }
    private boolean shape(SiteShape shape,double dx,double dy) {
        double ax=Math.abs(dx),ay=Math.abs(dy);
        if(ax>1||ay>1)return false;
        return switch(shape){
            case SQUARE -> true;
            case L_WEST -> dx<.12 || dy<.12;
            case L_SOUTH -> dx>-.12 || dy>-.12;
            case SPIRE -> dx*dx+dy*dy*.75<.94 && !(dx>.25&&dy>.25);
            case IRREGULAR -> ax*.7+ay*.42<.92 && !(dx>.3 && dy<-.3);
            case COMPACT -> ax+ay*.50<1.2;
            case HORSESHOE -> !(ax<.52 && dy>.13);
        };
    }
    private void placeCities() {
        SiteShape[] shapes=SiteShape.values();
        for(int i=0;i<CITY_ANCHORS.length;i++){
            int[] cfg=CITY_ANCHORS[i];
            Random r=new Random(hash(seed+0x33331L+i*117));
            int cx=SIZE*cfg[0]/100+r.nextInt(7)-3,cy=SIZE*cfg[1]/100+r.nextInt(7)-3;
            int rx=cfg[2]+r.nextInt(5)-2,ry=cfg[3]+r.nextInt(5)-2;
            City city=new City((char)('A'+i),cx,cy,rx,ry,shapes[i]);
            if(!land(cx,cy))throw new IllegalStateException("City center at sea: "+city.id());
            cities.add(city);
            for(int x=Math.max(1,cx-rx-1);x<Math.min(SIZE-1,cx+rx+2);x++)
                for(int y=Math.max(1,cy-ry-1);y<Math.min(SIZE-1,cy+ry+2);y++){
                    double jitter=.055*random(x,y,510+i);
                    if(!shape(city.shape(),(x-cx)/(double)rx+jitter,(y-cy)/(double)ry+jitter)
                       || !land(x,y))continue;
                    if(cityIds[x][y]!=0)continue;
                    cityIds[x][y]=(byte)(i+1);
                    immutableStructures[x][y]=false; // City pass replaces natural scatter with plot-level occupancy.
                }
            if(cityIds[cx][cy]==0)throw new IllegalStateException("Unplaced city: "+city.id());
        }
    }
    private void pruneDisconnectedCityCells() {
        for(City city:cities) {
            int id=city.id()-'A'+1;
            boolean[][] seen=new boolean[SIZE][SIZE];
            ArrayDeque<int[]> queue=new ArrayDeque<>();
            seen[city.x()][city.y()]=true;queue.add(new int[]{city.x(),city.y()});
            while(!queue.isEmpty()){
                int[] cell=queue.removeFirst();
                for(int i=0;i<4;i++){
                    int x=cell[0]+DX[i],y=cell[1]+DY[i];
                    if(inBounds(x,y)&&!seen[x][y]&&cityIds[x][y]==id){
                        seen[x][y]=true;queue.addLast(new int[]{x,y});
                    }
                }
            }
            for(int x=0;x<SIZE;x++)for(int y=0;y<SIZE;y++)
                if(cityIds[x][y]==id&&!seen[x][y])cityIds[x][y]=0;
        }
    }
    private static int root(int[] parent,int i){
        while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}return i;
    }
    private static void union(int[] parent,int a,int b){parent[root(parent,a)]=root(parent,b);}
    private List<int[]> path(int sx,int sy,int tx,int ty,boolean withinCity) {
        int total=SIZE*SIZE;
        int[] cost=new int[total],previous=new int[total];
        Arrays.fill(cost,Integer.MAX_VALUE);Arrays.fill(previous,-1);
        int start=sx*SIZE+sy,target=tx*SIZE+ty;
        PriorityQueue<Node> queue=new PriorityQueue<>(Comparator.comparingInt(Node::cost));
        cost[start]=0;queue.add(new Node(sx,sy,0));
        while(!queue.isEmpty()){
            Node at=queue.poll();int point=at.x()*SIZE+at.y();
            if(at.cost()!=cost[point])continue;
            if(point==target)break;
            for(int i=0;i<4;i++){
                int x=at.x()+DX[i],y=at.y()+DY[i];
                if(!inBounds(x,y)||!land(x,y))continue;
                if(withinCity && cityIds[x][y]!=cityIds[sx][sy])continue;
                int step=withinCity?1:(roads[x][y]?2:biomes[x][y]==Biome.MOUNTAINS?35:
                    biomes[x][y]==Biome.HILLS?9:biomes[x][y]==Biome.MARSH?8:4);
                if(!withinCity && cityIds[x][y]!=0 && !(Math.abs(x-tx)+Math.abs(y-ty)<4) &&
                    !(Math.abs(x-sx)+Math.abs(y-sy)<4))step+=60;
                int next=x*SIZE+y,updated=cost[point]+step;
                if(updated<cost[next]){
                    cost[next]=updated;previous[next]=point;queue.add(new Node(x,y,updated));
                }
            }
        }
        if(cost[target]==Integer.MAX_VALUE)throw new IllegalStateException("Unreachable route "+sx+","+sy+" -> "+tx+","+ty);
        List<int[]> points=new ArrayList<>();
        for(int at=target;at!=start;at=previous[at])points.add(new int[]{at/SIZE,at%SIZE});
        points.add(new int[]{sx,sy});Collections.reverse(points);
        return points;
    }
    private void markRoad(List<int[]> points) {
        for(int[] p:points){roads[p[0]][p[1]]=true;immutableStructures[p[0]][p[1]]=true;}
    }
    private void intercityRoutes() {
        List<Edge> edges=new ArrayList<>();
        for(int a=0;a<cities.size();a++)for(int b=a+1;b<cities.size();b++){
            City x=cities.get(a),y=cities.get(b);
            double distance=Math.hypot(x.x()-y.x(),x.y()-y.y());
            edges.add(new Edge(a,b,distance+random(a,b,1200)*9));
        }
        edges.sort(Comparator.comparingDouble(Edge::weight));
        int[] parent=new int[cities.size()];for(int i=0;i<parent.length;i++)parent[i]=i;
        int extras=0;
        for(Edge e:edges){
            boolean essential=root(parent,e.a())!=root(parent,e.b());
            if(!essential && (extras>=2 || random(e.a(),e.b(),1300)<-.1))continue;
            City a=cities.get(e.a()),b=cities.get(e.b());
            List<int[]> route=path(a.x(),a.y(),b.x(),b.y(),false);
            markRoad(route);
            routes.add(new Route(a.id(),b.id(),route.size()));
            if(essential)union(parent,e.a(),e.b());else extras++;
        }
    }
    private void localStreets() {
        for(City city:cities){
            Random r=new Random(hash(seed^city.id()*76189L));
            // A dense seeded set of arterial endpoints creates a city-specific network.
            List<int[]> inside=new ArrayList<>();
            for(int x=Math.max(0,city.x()-city.radiusX());x<=Math.min(SIZE-1,city.x()+city.radiusX());x++)
                for(int y=Math.max(0,city.y()-city.radiusY());y<=Math.min(SIZE-1,city.y()+city.radiusY());y++)
                    if(cityAt(x,y)==city.id() && (x-city.x())%5==0 && (y-city.y())%5==0)
                        inside.add(new int[]{x,y});
            Collections.shuffle(inside,r);
            int count=Math.min(inside.size(),Math.max(8,inside.size()*2/3));
            for(int i=0;i<count;i++){
                int[] p=inside.get(i);
                markRoad(path(city.x(),city.y(),p[0],p[1],true));
            }
        }
    }
    private boolean fits(int x,int y,int w,int h,boolean[][] used,int id){
        for(int xx=x;xx<x+w;xx++)for(int yy=y;yy<y+h;yy++)
            if(!inBounds(xx,yy)||used[xx][yy]||cityIds[xx][yy]!=id)return false;
        return true;
    }
    private void lot(int x,int y,int w,int h,boolean[][] used,int id,Random r,int[] mandatory){
        Zone z=mandatory[0]<Zone.values().length && w==2&&h==2
            ?Zone.values()[mandatory[0]++]:Zone.values()[r.nextInt(Zone.values().length)];
        // Open lots are a mixture of empty/vacant ground and pre-existing public sites.
        boolean developed=z!=Zone.OPEN || r.nextInt(3)==0;
        lots.add(new Lot((char)('A'+id-1),x,y,w,h,z,developed));
        for(int xx=x;xx<x+w;xx++)for(int yy=y;yy<y+h;yy++){
            used[xx][yy]=true;
            if(developed)immutableStructures[xx][yy]=true;
        }
    }
    private void cityLots() {
        boolean[][] used=new boolean[SIZE][SIZE];
        for(int x=0;x<SIZE;x++)for(int y=0;y<SIZE;y++)used[x][y]=roads[x][y];
        int[][] shapes={{2,2},{2,3},{3,2},{1,2},{2,1},{3,3},{1,1}};
        for(City c:cities){
            int id=c.id()-'A'+1;
            Random r=new Random(hash(seed^c.id()*9317L));
            List<int[]> candidates=new ArrayList<>();
            for(int x=0;x<SIZE;x++)for(int y=0;y<SIZE;y++)if(cityIds[x][y]==id&&!used[x][y])
                candidates.add(new int[]{x,y});
            Collections.shuffle(candidates,r);
            int[] mandatory={0};
            for(int[] p:candidates)if(fits(p[0],p[1],2,2,used,id)&&r.nextDouble()<.85)
                lot(p[0],p[1],2,2,used,id,r,mandatory);
            Collections.shuffle(candidates,r);
            for(int[] p:candidates){
                if(used[p[0]][p[1]])continue;
                int[] order={0,1,2,3,4,5,6};
                for(int i=order.length-1;i>0;i--){int j=r.nextInt(i+1),tmp=order[j];order[j]=order[i];order[i]=tmp;}
                for(int k:order)if(fits(p[0],p[1],shapes[k][0],shapes[k][1],used,id)){
                    lot(p[0],p[1],shapes[k][0],shapes[k][1],used,id,r,mandatory);break;
                }
            }
            if(mandatory[0]<4)throw new IllegalStateException("Not enough 2x2 lots in city "+c.id());
        }
    }
    public static ContinentLayout generate(long seed) {
        ContinentLayout map=new ContinentLayout(seed);
        map.terrain();map.placeCities();map.pruneDisconnectedCityCells();
        map.intercityRoutes();map.localStreets();map.cityLots();
        return map;
    }
}
