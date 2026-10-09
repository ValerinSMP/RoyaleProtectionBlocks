package company.pluginName.Modules.ProtectionsPckg.Utils;
import java.util.Arrays;
import java.util.zip.ZipFile;
public final class VerifyBlockDestructionArtifact {
    public static void main(String[] args) throws Exception {
        String listener="royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionsPckg/Listeners/ProtectionsListener";
        String guard="royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionsPckg/Utils/BlockDestructionGuard.class";
        int preserved=0, changed=0, added=0;
        try(ZipFile base=new ZipFile(args[0]); ZipFile candidate=new ZipFile(args[1])) {
            var entries=base.entries();
            while(entries.hasMoreElements()) {
                var old=entries.nextElement(); var current=candidate.getEntry(old.getName());
                if(current==null) throw new AssertionError("Removed " + old.getName());
                if(!Arrays.equals(base.getInputStream(old).readAllBytes(),candidate.getInputStream(current).readAllBytes())) {
                    if(!old.getName().equals(listener+".class") && !old.getName().equals(listener+"$1.class"))
                        throw new AssertionError("Unrelated change " + old.getName());
                    changed++;
                } else preserved++;
            }
            entries=candidate.entries();
            while(entries.hasMoreElements()) {
                var entry=entries.nextElement();
                if(!entry.isDirectory() && base.getEntry(entry.getName())==null) {
                    if(!entry.getName().equals(guard)) throw new AssertionError("Unexpected addition " + entry.getName());
                    added++;
                }
            }
        }
        if(changed<1 || changed>2 || added!=1) throw new AssertionError("Incomplete patch");
        System.out.println("Artifact: " + preserved + " entries preserved, " + changed + " listener classes updated; guard added");
    }
}
