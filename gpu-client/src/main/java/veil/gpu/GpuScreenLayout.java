package veil.gpu;

/** PSX-height GPU world with an aspect-correct expanding field of view.
 * UI occupies the fixed 640x400 left panel regardless of display shape. */
public final class GpuScreenLayout {
    public static final int BASE_WIDTH=640, BASE_HEIGHT=400;
    private GpuScreenLayout(){}
    public static int width(int windowWidth,int windowHeight){
        if(windowWidth<=0||windowHeight<=0)return BASE_WIDTH;
        return Math.max(BASE_WIDTH,Math.min(2048,
            Math.round((float)BASE_HEIGHT*windowWidth/windowHeight)));
    }
    public static float pointerX(float screenX,int windowWidth,int frameWidth){
        return windowWidth<=0?0:screenX*frameWidth/windowWidth;
    }
    public static float pointerY(float screenY,int windowHeight){
        return windowHeight<=0?0:BASE_HEIGHT-screenY*BASE_HEIGHT/windowHeight;
    }
    public static boolean leftMenuHit(float x,float y,int row,int firstY,int step){
        int bottom=firstY-row*step;
        return x>=37&&x<426&&y>=bottom&&y<bottom+22;
    }
}
