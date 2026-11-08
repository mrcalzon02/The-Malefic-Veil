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
        // Use a real seeded ocean parcel, not a free-floating synthetic wave.
        World coastalWorld=new World(887766L);
        ContinentVista coast=new ContinentVista(coastalWorld,'C');
        VeilOcean ocean=new VeilOcean(coastalWorld,coast);
        VeilOcean.SeaParcel sea=null;
        for(ContinentVista.Patch patch:coast.patches()){
            var parcels=ocean.sourceSeaParcels(patch.left(),patch.top(),
                patch.left()+patch.width(),patch.top()+patch.depth());
            if(!parcels.isEmpty()){sea=parcels.get(0);break;}
        }
        check(sea!=null,"Weather/ocean socket test has no real coastal sea parcel");
        double seaX=sea.west()+sea.size()*.5;
        double seaZ=sea.north()+sea.size()*.5;
        check(ocean.wet(seaX,seaZ),"Weather/ocean sample must be actual source water");
        try(LanServer host=new LanServer(0,20261021L);
            NetLink one=new NetLink("127.0.0.1",host.port());
            NetLink two=new NetLink("127.0.0.1",host.port())){
            until(()->one.weather()!=null&&two.weather()!=null);
            check(one.weather().preset().equals(two.weather().preset()),
                "Initial weather must be identical for players in the same world");
            double day=one.weather().dayFraction();
            check(day>=0&&day<1,"Server sends valid initial daylight time");
            // The client-side water setting must derive from received host weather.
            host.setWeatherPreset("CLEAR");
            until(()->one.weather().preset().equals("CLEAR")&&
                two.weather().preset().equals("CLEAR"));
            double calmEnvelope=ocean.effects(one.weather().preset()).envelope();
            check(Double.isFinite(ocean.height(seaX,seaZ,2.5,one.weather().preset())),
                "Host CLEAR weather must drive finite waves on real coastline");
            String answer=host.setWeatherPreset("HEAVY_RAIN");
            check(answer.startsWith("HEAVY_RAIN"),"Only host controls heavy rain");
            until(()->one.weather().preset().equals("HEAVY_RAIN")&&
                two.weather().preset().equals("HEAVY_RAIN"));
            check(Math.abs(one.weather().dayFraction()-two.weather().dayFraction())<.005,
                "Two player daylight clocks follow one host");
            double stormEnvelope=ocean.effects(one.weather().preset()).envelope();
            check(stormEnvelope>calmEnvelope&&
                Math.abs(stormEnvelope-ocean.effects(two.weather().preset()).envelope())<1e-9,
                "Host HEAVY_RAIN must increase identical ocean waves for both clients");
            check(Double.isFinite(ocean.height(seaX,seaZ,2.5,one.weather().preset())),
                "Authoritative storm must retain an actual finite ocean surface");
            try(NetLink late=new NetLink("127.0.0.1",host.port())){
                until(()->late.weather()!=null);
                check(late.weather().preset().equals("HEAVY_RAIN"),
                    "Late joining player receives live weather, not starting-clear");
                host.setWeatherPreset("SNOW");
                try {
                    until(()->one.weather().preset().equals("SNOW")&&
                        two.weather().preset().equals("SNOW")&&late.weather().preset().equals("SNOW"));
                } catch (AssertionError timeout) {
                    throw new AssertionError("Late-join SNOW update: host="+host.weatherStatus()+
                        " one="+one.weather()+" connected="+one.connected()+
                        " two="+two.weather()+" connected="+two.connected()+
                        " late="+late.weather()+" connected="+late.connected(),timeout);
                }
                double snowEnvelope=ocean.effects(late.weather().preset()).envelope();
                check(snowEnvelope>=calmEnvelope&&snowEnvelope<stormEnvelope&&
                    Math.abs(snowEnvelope-ocean.effects(one.weather().preset()).envelope())<1e-9,
                    "Host SNOW must reduce ocean storm waves for existing and late clients");
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
        System.out.println("PASS WeatherLanTest: host-owned weather, two peers, late join, CLEAR/HEAVY_RAIN/SNOW ocean coupling, manual/auto, client sync");
    }
}
