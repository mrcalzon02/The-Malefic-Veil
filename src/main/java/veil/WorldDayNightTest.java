package veil;

public final class WorldDayNightTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        var midnight=WorldDayNight.atHour(0);
        var dawn=WorldDayNight.atHour(6.4);
        var noon=WorldDayNight.atHour(12);
        var dusk=WorldDayNight.atHour(18.4);
        check(noon.daylight()>.98&&midnight.daylight()<.02,"Day night schedule");
        check(noon.ambient()>dawn.ambient()&&dawn.ambient()>midnight.ambient(),"Ambient curve");
        check(midnight.lampStrength()>noon.lampStrength(),"Night lights must be stronger");
        check(dawn.dusk()>.25&&dusk.dusk()>.25,"Dawn and dusk palette transitions");
        check(noon.skyBlue()>midnight.skyBlue(),"Sky must brighten");
        check(WorldDayNight.atHour(24).equals(midnight),"Clock wraps to midnight");
        check(WorldDayNight.atElapsedMs(WorldDayNight.DAY_MS).equals(midnight),"Elapsed clock wraps");
        check(WorldDayNight.atHour(-12).equals(noon),"Negative hour wraps");
        check(WorldDayNight.clock(18.5).equals("18:30"),"Clock formatting");
        for(int n=0;n<2400;n++){
            var c=WorldDayNight.atHour(n/100.0);
            check(c.ambient()>=0&&c.ambient()<=1,"Invalid light level");
            check(c.skyRed()>=0&&c.hazeBlue()<=1,"Invalid sky");
            check(c.fogDensity()>=0&&c.lampStrength()>=0,"Invalid night");
        }
        try{WorldDayNight.atHour(Double.NaN);throw new AssertionError("NaN accepted");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS WorldDayNightTest: repeatable day/night and safe light bounds");
    }
}