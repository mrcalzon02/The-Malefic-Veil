package veil;

import java.util.prefs.Preferences;

/** Local render-only budgets. Never changes host collision, hits or authoritative simulation. */
public final class PerformanceSettings {
    private static final Preferences PREFS=Preferences.userNodeForPackage(PerformanceSettings.class).node("performance");
    public int propDistance=clamp(PREFS.getInt("propDistance",19),4,64);
    public int highDetailDistance=clamp(PREFS.getInt("highDetailDistance",9),2,32);
    public int meshBudget=clamp(PREFS.getInt("meshBudget",80),8,500);
    public int frameCap=clamp(PREFS.getInt("frameCap",30),15,30);
    private static int clamp(int x,int low,int high){return Math.max(low,Math.min(high,x));}
    public void apply(int distance,int near,int budget,int cap){
        propDistance=clamp(distance,4,64);
        highDetailDistance=Math.min(propDistance,clamp(near,2,32));
        meshBudget=clamp(budget,8,500);
        frameCap=clamp(cap,15,30);
        PREFS.putInt("propDistance",propDistance);PREFS.putInt("highDetailDistance",highDetailDistance);
        PREFS.putInt("meshBudget",meshBudget);PREFS.putInt("frameCap",frameCap);
    }
}
