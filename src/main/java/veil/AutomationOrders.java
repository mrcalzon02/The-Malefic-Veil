package veil;

/**
 * The durable E5 one-slot manufacturing instruction. The V11 device record
 * already persists the queued recipe string, so a tagged repeat instruction
 * extends it without changing any binary field positions or old save data.
 * A repeat tag NEVER represents paid inventory or a pre-authorized cycle.
 */
public final class AutomationOrders {
    public static final String REPEAT_PREFIX="REPEAT:";
    private AutomationOrders(){}
    public static boolean repeating(String instruction){
        return instruction!=null&&instruction.startsWith(REPEAT_PREFIX);
    }
    public static String recipe(String instruction){
        if(instruction==null)return "";
        return repeating(instruction)?
            instruction.substring(REPEAT_PREFIX.length()):instruction;
    }
    public static String repeat(String recipe){return REPEAT_PREFIX+recipe;}
}
