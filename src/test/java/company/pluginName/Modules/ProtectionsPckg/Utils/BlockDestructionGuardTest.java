package company.pluginName.Modules.ProtectionsPckg.Utils;

public final class BlockDestructionGuardTest {
    private static int assertions;
    public static void main(String[] args) {
        for(String type : new String[]{"WITHER","ENDER_DRAGON","ENDERMAN","SILVERFISH","RAVAGER","RABBIT"}) {
            check(true,BlockDestructionGuard.protectMobChange(type,true,false),type + " inside");
            check(false,BlockDestructionGuard.protectMobChange(type,false,false),type + " outside");
        }
        for(String type : new String[]{"VILLAGER","SHEEP","FALLING_BLOCK","PLAYER","SNOW_GOLEM","SNIFFER"}) {
            check(false,BlockDestructionGuard.protectMobChange(type,true,false),type + " normal behavior preserved");
            check(true,BlockDestructionGuard.protectMobChange(type,true,true),type + " cannot alter PB block");
        }
        for(String type : new String[]{"TNT","TNT_MINECART","PRIMED_TNT","MINECART_TNT"})
            check(true,BlockDestructionGuard.isTnt(type),type + " classified as TNT");
        for(String type : new String[]{"WITHER","WITHER_SKULL","CREEPER","END_CRYSTAL","FIREBALL","ENDER_DRAGON"}) {
            check(false,BlockDestructionGuard.isTnt(type),type + " not TNT");
            check(true,BlockDestructionGuard.protectExplosion(true,false,false,true,false),type + " protected even with TNT allowed");
            check(false,BlockDestructionGuard.protectExplosion(false,false,false,true,true),type + " outside unaffected");
        }
        for(boolean protectedArea : new boolean[]{false,true})
            for(boolean source : new boolean[]{false,true})
                for(boolean tnt : new boolean[]{false,true})
                    for(boolean enabled : new boolean[]{false,true})
                        for(boolean denied : new boolean[]{false,true}) {
                            boolean expected=source || protectedArea && (!tnt || enabled && denied);
                            check(expected,BlockDestructionGuard.protectExplosion(protectedArea,source,tnt,enabled,denied),"explosion matrix");
                        }
        check(false,BlockDestructionGuard.protectExplosion(true,false,true,true,false),"TNT allowed preserved");
        check(true,BlockDestructionGuard.protectExplosion(true,false,true,true,true),"TNT denied");
        System.out.println("Block destruction: " + assertions + " cases passed");
    }
    private static void check(boolean expected,boolean actual,String label) {
        if(expected!=actual) throw new AssertionError(label);
        assertions++;
    }
}
