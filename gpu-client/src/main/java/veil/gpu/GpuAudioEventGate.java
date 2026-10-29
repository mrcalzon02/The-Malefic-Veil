package veil.gpu;

/**
 * Reduces immutable authoritative arsenal snapshots to edge-triggered sounds.
 * Mouse input merely asks the host to act; it is NOT evidence a shot occurred.
 * The initial snapshot establishes a baseline and may not replay old actions.
 */
public final class GpuAudioEventGate {
    public enum Kind { NONE, PRIMARY, ALTERNATE, RELOAD, CHARGE, EQUIP }
    public record Event(Kind kind,int weapon) {}
    private boolean initialized;
    private long lastActionAt;
    private int lastAction,lastWeapon;

    public Event accept(int weapon,int action,long actionAt) {
        if(weapon<0||weapon>=32)throw new IllegalArgumentException("Invalid arsenal weapon "+weapon);
        if(!initialized){
            initialized=true;lastWeapon=weapon;lastAction=action;lastActionAt=actionAt;
            return new Event(Kind.NONE,weapon);
        }
        boolean moved=weapon!=lastWeapon;
        boolean changed=actionAt!=lastActionAt;
        // A new action timestamp is authoritative. A changed action with the
        // same timestamp can be a release/expiry and must not replay its sound.
        lastWeapon=weapon;lastAction=action;lastActionAt=actionAt;
        if(changed&&actionAt>0){
            return new Event(switch(action){
                case 1 -> Kind.PRIMARY;
                case 2 -> Kind.CHARGE;
                case 3 -> Kind.RELOAD;
                case 4 -> Kind.ALTERNATE;
                default -> Kind.NONE;
            },weapon);
        }
        return new Event(moved?Kind.EQUIP:Kind.NONE,weapon);
    }
    public void reset(){initialized=false;lastActionAt=0;lastAction=0;lastWeapon=0;}
    public static void main(String[] args){
        GpuAudioEventGate g=new GpuAudioEventGate();
        check(g.accept(0,0,0),Kind.NONE);
        check(g.accept(0,1,100),Kind.PRIMARY);
        check(g.accept(0,1,100),Kind.NONE); // 120fps render: same host event
        check(g.accept(0,0,100),Kind.NONE); // stale/expired action
        check(g.accept(0,3,200),Kind.RELOAD);
        check(g.accept(0,3,200),Kind.NONE);
        check(g.accept(0,4,300),Kind.ALTERNATE);
        check(g.accept(7,4,300),Kind.EQUIP);
        check(g.accept(7,2,400),Kind.CHARGE);
        check(g.accept(7,1,500),Kind.PRIMARY);
        check(g.accept(7,0,500),Kind.NONE);
        g.reset();
        check(g.accept(9,1,700),Kind.NONE); // never replay joining history
        check(g.accept(9,1,800),Kind.PRIMARY);
        try{g.accept(32,0,0);throw new AssertionError("Unvalidated weapon");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS GPU_AUTHORITATIVE_AUDIO_EVENTS fire,alt,reload,charge,equip,no-replay");
    }
    private static void check(Event event,Kind want){
        if(event.kind()!=want)throw new AssertionError("Wanted "+want+" got "+event);
    }
}
