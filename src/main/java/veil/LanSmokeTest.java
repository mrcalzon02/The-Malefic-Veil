package veil;

/** Headless localhost test; validates transport, shared seed, presence and first PvP hit. */
public final class LanSmokeTest {
    private static void require(boolean ok,String s) {if(!ok)throw new AssertionError(s);}
    public static void main(String[] unused)throws Exception{
        try(LanServer server=new LanServer(0,887766L);
            NetLink shooter=new NetLink("127.0.0.1",server.port());
            NetLink victim=new NetLink("127.0.0.1",server.port())){
            require(shooter.seed==887766L&&victim.seed==887766L,"map seed mismatch");
            require(shooter.id!=victim.id,"duplicate player ID");
            shooter.state(1.5,1.5,0);
            victim.state(4.5,1.5,0);
            long until=System.nanoTime()+3_000_000_000L;
            while(System.nanoTime()<until){
                NetLink.Remote a=shooter.others.get(victim.id);
                NetLink.Remote b=victim.others.get(shooter.id);
                if(a!=null&&b!=null&&Math.abs(a.x-4.5)<.01)break;
                Thread.sleep(20);
            }
            require(shooter.others.containsKey(victim.id),"presence not broadcast");
            require(Math.abs(shooter.others.get(victim.id).x-4.5)<.01,"state update not broadcast");
            shooter.fire(0);
            int dealt=0;until=System.nanoTime()+3_000_000_000L;
            while(System.nanoTime()<until&&dealt==0){
                dealt=victim.drainDamage();
                if(dealt==0)Thread.sleep(20);
            }
            require(dealt==25,"expected authoritative handgun hit, actual="+dealt);
            System.out.println("PASS LAN localhost: seed, peer state, line-of-sight hit");
        }
    }
}
