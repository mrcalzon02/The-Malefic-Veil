package veil;

/** Independent JVM client for trusted-loopback legacy game acceptance only. */
public final class LanSocketProbe {
    public static void main(String[] args)throws Exception{
        try(NetLink client=new NetLink("127.0.0.1",Integer.parseInt(args[0]))){
            long deadline=System.nanoTime()+8_000_000_000L;
            while(client.accountKey()==null&&System.nanoTime()<deadline)Thread.sleep(20);
            if(client.accountKey()==null)throw new AssertionError("Missing local identity admission");
            client.selectFaction(CharacterRoster.Faction.THE_UNRAVELED);
            client.state(1.5,1.5,0,1);
            while(client.others.values().stream().noneMatch(p->p.opacity<.5)&&System.nanoTime()<deadline)Thread.sleep(20);
            if(client.others.values().stream().noneMatch(p->p.opacity<.5))throw new AssertionError("Independent peer stance did not replicate");
            Thread.sleep(400);System.out.println("PASS independent veil client: identity admission and peer stance replication");
        }
    }
}
