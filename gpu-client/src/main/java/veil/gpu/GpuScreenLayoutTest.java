package veil.gpu;

/** Deterministic, display-free widescreen coordinates and left hitbox test. */
public final class GpuScreenLayoutTest {
    private static void check(boolean yes,String msg){
        if(!yes)throw new AssertionError(msg);
    }
    public static void main(String[] args){
        check(GpuScreenLayout.width(960,600)==640,"16:10");
        check(GpuScreenLayout.width(1280,720)==711,"16:9 720p");
        check(GpuScreenLayout.width(1920,1080)==711,"16:9 1080p");
        check(GpuScreenLayout.width(2560,1440)==711,"16:9 1440p");
        check(GpuScreenLayout.width(2560,1080)==948,"21:9");
        check(GpuScreenLayout.width(3440,1440)==956,"ultrawide");
        check(GpuScreenLayout.width(0,0)==640,"minimized window");
        int[] widths={960,1280,1920,2560,3440};
        int[] heights={600,720,1080,1080,1440};
        for(int i=0;i<widths.length;i++){
            int width=GpuScreenLayout.width(widths[i],heights[i]);
            float px=GpuScreenLayout.pointerX(80f*widths[i]/width,widths[i],width);
            float py=GpuScreenLayout.pointerY(90f*heights[i]/400f,heights[i]);
            check(GpuScreenLayout.leftMenuHit(px,py,0,307,24),"left row "+i);
            check(!GpuScreenLayout.leftMenuHit(
                GpuScreenLayout.pointerX(widths[i]*.96f,widths[i],width),
                py,0,307,24),"right side must not click menu "+i);
        }
        System.out.println("PASS GPU_WIDESCREEN_LAYOUT aspect=16:10,16:9,21:9,ultrawide left=anchored");
    }
}
