package veil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Seeded road graph and parcel grammar. A cell is six collision tiles; 2x2 is the standard lot. */
public final class CityLayout {
    public static final int CELL = 6;
    public static final int GRID = World.SIZE / CELL;
    public enum Category { HAZARD, RESOURCE, SAFE, OPEN }
    public record Footprint(int width, int height) {
        public Footprint { if (width < 1 || height < 1) throw new IllegalArgumentException("Invalid plot size"); }
    }
    public record Template(String id, Footprint footprint, Category category, int variant) {}
    public record Plot(int x, int y, Template template) {
        public int right() { return x + template.footprint().width(); }
        public int bottom() { return y + template.footprint().height(); }
    }
    public record Road(int x, int y, int connections) { // N=1 E=2 S=4 W=8
        public String piece(){
            return switch(Integer.bitCount(connections)){
                case 0 -> "isolated";
                case 1 -> "dead-end";
                case 2 -> (connections==5||connections==10)?"straight":"corner";
                case 3 -> "t-junction";
                default -> "crossroads";
            };
        }
    }

    private static final Footprint STANDARD = new Footprint(2, 2);
    private static final Footprint[] SHAPES = { STANDARD, new Footprint(1,2), new Footprint(2,1),
        new Footprint(1,1), new Footprint(2,3), new Footprint(3,2), new Footprint(3,3) };
    private static final String[][] NAMES = {
        {"infested-tenement", "electrical-substation", "chemical-store", "collapsed-flats",
         "burned-clinic", "cursed-chapel", "flooded-tunnel", "contaminated-lab"},
        {"salvage-yard", "munitions-cache", "abandoned-market", "supply-depot",
         "workshop", "field-pharmacy", "vehicle-scrapyard", "freight-terminal"},
        {"guard-post", "sheltered-residence", "field-hospital", "reinforced-garage",
         "safehouse", "ranger-station", "barricaded-shop", "operations-center"},
        {"public-square", "parking-court", "empty-lot", "municipal-park",
         "bus-terminus", "demolition-site", "pedestrian-plaza", "vacant-playground"}
    };
    private static final Map<Footprint, EnumMap<Category, List<Template>>> LIBRARY = library();
    private final long seed;
    private final boolean[][] roads = new boolean[GRID][GRID];
    private final boolean[][] protectedParcels = new boolean[GRID][GRID];
    private final byte[][] walls = new byte[World.SIZE][World.SIZE];
    private final Category[][] zones = new Category[GRID][GRID];
    private final List<Plot> plots = new ArrayList<>();
    private final List<Road> roadTiles = new ArrayList<>();

    private CityLayout(long seed) { this.seed = seed; }
    public long seed() { return seed; }
    public List<Plot> plots() { return Collections.unmodifiableList(plots); }
    public List<Road> roadTiles() { return Collections.unmodifiableList(roadTiles); }
    public boolean road(int x, int y) { return inSite(x,y) && roads[x][y]; }
    /** All city parcels, including vacant plots, are immutable; validate entire placement footprint. */
    public boolean worldStructureAt(double x,double y,double radius) {
        int x0=(int)Math.floor(x-radius),x1=(int)Math.floor(x+radius);
        int y0=(int)Math.floor(y-radius),y1=(int)Math.floor(y+radius);
        for(int xx=x0;xx<=x1;xx++)for(int yy=y0;yy<=y1;yy++) {
            if(xx<1||yy<1||xx>=World.SIZE-1||yy>=World.SIZE-1)return true;
            if(tile(xx,yy)!=0||protectedParcels[xx/CELL][yy/CELL])return true;
        }
        return false;
    }
    public Category categoryAt(int tileX,int tileY) {
        if(tileX<0 || tileY<0 || tileX>=World.SIZE || tileY>=World.SIZE)return null;
        return zones[tileX/CELL][tileY/CELL];
    }
    public int tile(int x,int y) {
        if(x<0 || y<0 || x>=World.SIZE || y>=World.SIZE)return 1;
        return walls[x][y];
    }
    public void copyInto(byte[][] target) {
        for(int x=0;x<World.SIZE;x++)System.arraycopy(walls[x],0,target[x],0,World.SIZE);
    }
    public static List<Template> templates(Footprint size,Category category) {
        EnumMap<Category,List<Template>> variants=LIBRARY.get(size);
        return variants==null?List.of():variants.getOrDefault(category,List.of());
    }
    public static int templateCount() {
        int total=0;
        for(var categories:LIBRARY.values())for(var variants:categories.values())total+=variants.size();
        return total;
    }
    private static Map<Footprint,EnumMap<Category,List<Template>>> library() {
        Map<Footprint,EnumMap<Category,List<Template>>> catalog=new HashMap<>();
        for(Footprint size:SHAPES){
            EnumMap<Category,List<Template>> variants=new EnumMap<>(Category.class);
            for(Category category:Category.values()){
                List<Template> items=new ArrayList<>();
                int count=size.equals(STANDARD)?8:3;
                for(int i=0;i<count;i++) {
                    String name=NAMES[category.ordinal()][i];
                    items.add(new Template(size.width()+"x"+size.height()+"_"+category.name().toLowerCase()+"_"+name,
                        size,category,i));
                }
                variants.put(category,List.copyOf(items));
            }
            catalog.put(size,variants);
        }
        return Collections.unmodifiableMap(catalog);
    }
    private static boolean inSite(int x,int y) { return x>=0&&y>=0&&x<GRID&&y<GRID; }
    private void segment(int x1,int y1,int x2,int y2){
        int dx=Integer.compare(x2,x1),dy=Integer.compare(y2,y1);
        for(int x=x1,y=y1;;x+=dx,y+=dy){ roads[x][y]=true; if(x==x2&&y==y2)break; }
    }
    private record Edge(int a,int b) {}
    private static int root(int[] parent,int i) {
        while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}
        return i;
    }
    private static void join(int[] parent,int a,int b){parent[root(parent,a)]=root(parent,b);}
    private void carveRoadNetwork(Random r){
        // 5x5 junction lattice. A randomized spanning tree guarantees connected roads;
        // additional loops and branching dead ends prevent a purely rectangular street grid.
        final int n=5;
        List<Edge> edges=new ArrayList<>();
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            int i=x+n*y;
            if(x<n-1)edges.add(new Edge(i,i+1));
            if(y<n-1)edges.add(new Edge(i,i+n));
        }
        Collections.shuffle(edges,r);
        int[] parent=new int[n*n];for(int i=0;i<parent.length;i++)parent[i]=i;
        // Keep all three legacy spawn sectors on connected public streets.
        int[] spine={0,1,2,7,12,13,14,19,24};
        for(int i=1;i<spine.length;i++){
            int a=spine[i-1],b=spine[i];
            segment((a%n)*5,(a/n)*5,(b%n)*5,(b/n)*5);
            join(parent,a,b);
        }
        for(Edge e:edges){
            boolean required=root(parent,e.a)!=root(parent,e.b);
            if(required || r.nextDouble()<.34){
                segment((e.a%n)*5,(e.a/n)*5,(e.b%n)*5,(e.b/n)*5);
                if(required)join(parent,e.a,e.b);
            }
        }
        // Connected alleys branch off streets, sometimes reconnecting as pedestrian loops.
        for(int i=0;i<22;i++){
            List<int[]> street=new ArrayList<>();
            for(int x=0;x<GRID;x++)for(int y=0;y<GRID;y++)if(roads[x][y])street.add(new int[]{x,y});
            int[] p=street.get(r.nextInt(street.size()));
            int direction=r.nextInt(4),dx=new int[]{1,0,-1,0}[direction],dy=new int[]{0,1,0,-1}[direction];
            int length=1+r.nextInt(3);
            for(int step=1;step<=length;step++){
                int x=p[0]+dx*step,y=p[1]+dy*step;
                if(x<1||y<1||x>=GRID-1||y>=GRID-1)break;
                roads[x][y]=true;
            }
        }
        for(int x=0;x<GRID;x++)for(int y=0;y<GRID;y++)if(roads[x][y]){
            int bits=(road(x,y-1)?1:0)|(road(x+1,y)?2:0)|(road(x,y+1)?4:0)|(road(x-1,y)?8:0);
            roadTiles.add(new Road(x,y,bits));
        }
    }
    private boolean fits(int x,int y,Footprint size,boolean[][] used){
        if(x+size.width()>GRID||y+size.height()>GRID)return false;
        for(int dx=0;dx<size.width();dx++)for(int dy=0;dy<size.height();dy++)
            if(used[x+dx][y+dy])return false;
        return true;
    }
    private void place(int x,int y,Footprint shape,boolean[][] used,Random r,int[] standardCount){
        Category[] types=Category.values();
        Category category=shape.equals(STANDARD)&&standardCount[0]<4
            ?types[standardCount[0]++]:types[r.nextInt(types.length)];
        List<Template> options=templates(shape,category);
        Template template=options.get(r.nextInt(options.size()));
        plots.add(new Plot(x,y,template));
        for(int dx=0;dx<shape.width();dx++)for(int dy=0;dy<shape.height();dy++){
            used[x+dx][y+dy]=true;
            zones[x+dx][y+dy]=category;
        }
    }
    private void choosePlots(Random r){
        boolean[][] used=new boolean[GRID][GRID];
        List<int[]> candidates=new ArrayList<>();
        for(int x=0;x<GRID;x++)for(int y=0;y<GRID;y++){
            used[x][y]=roads[x][y];
            if(!roads[x][y])candidates.add(new int[]{x,y});
        }
        Collections.shuffle(candidates,r);
        int[] standardCount={0};
        // Reserve the standard footprint first; the second pass packs irregular remnants.
        for(int[] p:candidates){
            int x=p[0],y=p[1];
            if(fits(x,y,STANDARD,used)&&r.nextDouble()<.92)
                place(x,y,STANDARD,used,r,standardCount);
        }
        Collections.shuffle(candidates,r);
        for(int[] p:candidates){
            int x=p[0],y=p[1];if(used[x][y])continue;
            List<Footprint> sizes=new ArrayList<>(List.of(SHAPES));
            Collections.shuffle(sizes,r);
            Footprint selected=null;
            for(Footprint candidate:sizes)if(fits(x,y,candidate,used)){selected=candidate;break;}
            if(selected==null)throw new IllegalStateException("Unfilled parcel at "+x+","+y);
            place(x,y,selected,used,r,standardCount);
        }
        for(int x=0;x<GRID;x++)for(int y=0;y<GRID;y++)
            if(!used[x][y])throw new IllegalStateException("World contains unassigned land");
        // The ENTIRE generated city is immutable world-owned territory.
        // Empty lots, parks, streets, destroyed buildings, rooftops and plazas
        // are not player construction space; wilderness exists beyond the city.
        for(int x=0;x<GRID;x++)for(int y=0;y<GRID;y++)
            protectedParcels[x][y]=true;
    }
    private void carveRect(int left,int top,int right,int bottom){
        for(int x=Math.max(1,left);x<Math.min(World.SIZE-1,right);x++)
            for(int y=Math.max(1,top);y<Math.min(World.SIZE-1,bottom);y++)walls[x][y]=0;
    }
    private void wall(int x,int y){if(x>0&&y>0&&x<World.SIZE-1&&y<World.SIZE-1)walls[x][y]=1;}
    private void buildPlot(Plot plot){
        Footprint shape=plot.template().footprint();
        int x0=plot.x()*CELL,y0=plot.y()*CELL;
        int x1=plot.right()*CELL,y1=plot.bottom()*CELL;
        int w=x1-x0,h=y1-y0;
        Category category=plot.template().category();
        int variant=plot.template().variant();
        carveRect(x0,y0,x1,y1);
        // A vacant lot must actually be bare land, unlike a paved square or existing park.
        if(category==Category.OPEN && variant==2)return;
        if(category!=Category.OPEN){
            // Four outward entrances keep buildings permeable to alleys and neighboring plots.
            for(int x=x0;x<x1;x++){
                if(Math.abs(x-(x0+w/2))>1){wall(x,y0);wall(x,y1-1);}
            }
            for(int y=y0;y<y1;y++){
                if(Math.abs(y-(y0+h/2))>1){wall(x0,y);wall(x1-1,y);}
            }
        }
        if(w<5||h<5)return;
        int cx=x0+w/2,cy=y0+h/2;
        switch(variant){
            case 0 -> { // Rooms with open doorways, not unbroken internal dividers.
                if(category!=Category.OPEN)for(int x=x0+2;x<x1-2;x++)if(Math.abs(x-cx)>1)wall(x,cy);
            }
            case 1 -> {
                for(int y=y0+2;y<y1-2;y++)if(Math.abs(y-cy)>1)wall(cx,y);
            }
            case 2 -> {
                if(w>=8&&h>=8){ wall(x0+3,y0+3);wall(x0+4,y0+3);wall(x1-4,y1-4);wall(x1-4,y1-3); }
            }
            case 3 -> {
                if(w>=10&&h>=10)for(int x=x0+3;x<x1-3;x++)if(Math.abs(x-cx)>2)wall(x,cy-1);
            }
            case 4 -> {
                wall(cx-2,cy-2);wall(cx+2,cy+2);
                if(category==Category.HAZARD){wall(cx-2,cy-1);wall(cx+2,cy+1);}
            }
            case 5 -> {
                if(category==Category.RESOURCE||category==Category.HAZARD)
                    for(int y=y0+3;y<y1-3;y+=3)wall(cx-1,y);
            }
            case 6 -> { // Offset sheltered recess and counter-cover.
                for(int x=x0+2;x<cx-1;x++)wall(x,y0+3);
                for(int y=cy+2;y<y1-2;y++)wall(x1-4,y);
            }
            case 7 -> { // Two offset lanes and central crossfire cover.
                if(w>=10&&h>=10){
                    for(int y=y0+2;y<cy-1;y++)wall(x0+4,y);
                    for(int y=cy+2;y<y1-2;y++)wall(x1-5,y);
                    wall(cx,cy);
                }
            }
        }
        // Open sites remain generally open; hazard areas carry more hard cover.
        if(category==Category.HAZARD&&w>=10&&h>=10){ wall(x0+3,y1-4);wall(x1-4,y0+3); }
    }
    private void carveGeometry(){
        for(int x=0;x<World.SIZE;x++)Arrays.fill(walls[x],(byte)1);
        for(Road street:roadTiles)carveRect(street.x()*CELL,street.y()*CELL,
            (street.x()+1)*CELL,(street.y()+1)*CELL);
        for(Plot plot:plots)buildPlot(plot);
    }
    private boolean[][] connected(){
        boolean[][] visited=new boolean[World.SIZE][World.SIZE];
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        int sx=1,sy=1;
        if(walls[sx][sy]!=0)throw new IllegalStateException("Spawn road blocked");
        visited[sx][sy]=true;queue.add(sx*World.SIZE+sy);
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        while(!queue.isEmpty()){
            int point=queue.removeFirst(),x=point/World.SIZE,y=point%World.SIZE;
            for(int k=0;k<4;k++){
                int nx=x+dx[k],ny=y+dy[k];
                if(nx<1||ny<1||nx>=World.SIZE-1||ny>=World.SIZE-1||visited[nx][ny]||walls[nx][ny]!=0)continue;
                visited[nx][ny]=true;queue.addLast(nx*World.SIZE+ny);
            }
        }
        return visited;
    }
    private void joinDisconnected(){
        // Minimal wall cuts connect all legal floor tiles: no sealed loot or spawn islands.
        final int n=World.SIZE,total=n*n;
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        for(int repair=0;repair<total;repair++){
            boolean[][] visited=connected();
            int destination=-1;
            for(int x=1;x<n-1&&destination<0;x++)for(int y=1;y<n-1;y++)
                if(walls[x][y]==0&&!visited[x][y]){destination=x*n+y;break;}
            if(destination<0)return;
            int[] distance=new int[total],previous=new int[total];
            Arrays.fill(distance,Integer.MAX_VALUE);Arrays.fill(previous,-1);
            ArrayDeque<Integer> deque=new ArrayDeque<>();
            for(int x=1;x<n-1;x++)for(int y=1;y<n-1;y++)if(visited[x][y]){
                int at=x*n+y;distance[at]=0;deque.addLast(at);
            }
            while(!deque.isEmpty()){
                int at=deque.removeFirst(),x=at/n,y=at%n;
                if(at==destination)break;
                for(int k=0;k<4;k++){
                    int nx=x+dx[k],ny=y+dy[k];
                    if(nx<1||ny<1||nx>=n-1||ny>=n-1)continue;
                    int next=nx*n+ny,delta=walls[nx][ny]==0?0:1;
                    if(distance[at]+delta>=distance[next])continue;
                    distance[next]=distance[at]+delta;previous[next]=at;
                    if(delta==0)deque.addFirst(next);else deque.addLast(next);
                }
            }
            if(previous[destination]<0)throw new IllegalStateException("Cannot connect city parcels");
            for(int at=destination;at>=0;at=previous[at])walls[at/n][at%n]=0;
        }
        throw new IllegalStateException("Accessibility repair exceeded world tile budget");
    }
    public static CityLayout generate(long seed){
        if(World.SIZE%CELL!=0)throw new IllegalStateException("World must align to lot grid");
        CityLayout city=new CityLayout(seed);
        Random streets=new Random(seed^0x726F61644E45544CL);
        Random parcels=new Random(seed^0x504C4F544C49424CL);
        city.carveRoadNetwork(streets);
        city.choosePlots(parcels);
        city.carveGeometry();
        city.joinDisconnected();
        return city;
    }
}
