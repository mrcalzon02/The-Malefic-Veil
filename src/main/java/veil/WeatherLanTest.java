package veil;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Live socket regression: every player in ONE world sees one weather authority. */
public final class WeatherLanTest {
    private WeatherLanTest(){}
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    private static void until(BooleanSupplier predicate)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
        while(System.nanoTime()<end){
            if(predicate.getAsBoolean())return;
            Thread.sleep(15);
        }
        throw new AssertionError("Weather socket replication timeout");
    }
    public static void main(String[] args)throws Exception{
        try(LanServer host=new LanServer(0,20261021L);
            NetLink one=new NetLink("127.0.0.1",host.port());
            NetLink two=new NetLink("127.0.0.1",host.port())){
            until(()->one.weather()!=null&&two.weather()!=null);
            check(one.weather().preset().equals(two.weather().preset()),
                "Initial weather must be identical for players in the same world");
            double day=one.weather().dayFraction();
            check(day>=0&&day<1,"Server sends valid initial daylight time");
            String answer=host.setWeatherPreset("HEAVY_RAIN");
            check(answer.startsWith("HEAVY_RAIN"),"Only host controls heavy rain");
            until(()->one.weather().preset().equals("HEAVY_RAIN")&&
                two.weather().preset().equals("HEAVY_RAIN"));
            check(Math.abs(one.weather().dayFraction()-two.weather().dayFraction())<.005,
                "Two player daylight clocks follow one host");
            try(NetLink late=new NetLink("127.0.0.1",host.port())){
                until(()->late.weather()!=null);
                check(late.weather().preset().equals("HEAVY_RAIN"),
                    "Late joining player receives live weather, not starting-clear");
                host.setWeatherPreset("SNOW");
                until(()->one.weather().preset().equals("SNOW")&&
                    two.weather().preset().equals("SNOW")&&late.weather().preset().equals("SNOW"));
            }
            boolean rejected=false;
            try{host.setWeatherPreset("INVALID_ICE");}
            catch(IllegalArgumentException error){rejected=true;}
            check(rejected&&host.weatherStatus().startsWith("SNOW"),
                "Invalid admin preset rejected without changing active weather");
            host.resumeAutoWeather();
        }
        try(VeilWeather visual=new VeilWeather(20261021L,Path.of("."))){
            visual.followHost();
            visual.applyHostWeather("HEAVY_RAIN",.70);
            check(visual.current().equals("HEAVY_RAIN"),
                "Visual module applies host preset");
            check(Math.abs(visual.timeOfDay()-.70)<.001,
                "Visual module applies the host day fraction");
            visual.setPreset("SNOW",0);
            visual.applyHostWeather("HEAVY_RAIN",.72);
            check(visual.current().equals("HEAVY_RAIN"),
                "Client reconciles a changed weather type");
        }
        System.out.println("PASS WeatherLanTest: host-owned weather, two peers, late join, manual/auto, client sync");
    }
}
