package company.pluginName.Modules.ProtectionsPckg.Utils;
import java.util.Arrays;
import java.util.zip.ZipFile;
public final class VerifyPvpToggleArtifact {
 public static void main(String[] args) throws Exception {
  String root="royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionsPckg/";
  int preserved=0, changed=0, added=0;
  try(ZipFile base=new ZipFile(args[0]); ZipFile candidate=new ZipFile(args[1])) {
   var entries=base.entries();
   while(entries.hasMoreElements()) {
    var old=entries.nextElement(); var current=candidate.getEntry(old.getName());
    if(current==null)throw new AssertionError("Removed "+old.getName());
    if(!Arrays.equals(base.getInputStream(old).readAllBytes(),candidate.getInputStream(current).readAllBytes())) {
     if(!old.getName().startsWith(root+"Objects/Components/Permissions/ProtectionPermissionManager")
       && !old.getName().equals(root+"Listeners/ProtectionPermissionListener.class"))throw new AssertionError("Unrelated change "+old.getName());
     changed++;
    } else preserved++;
   }
   entries=candidate.entries();
   while(entries.hasMoreElements()) {
    var entry=entries.nextElement();
    if(!entry.isDirectory() && base.getEntry(entry.getName())==null) {
     if(!entry.getName().equals(root+"Utils/ProtectionPvpBridge.class")
       && !entry.getName().equals(root+"Utils/InteractionPermissionRoute.class")
       && !entry.getName().equals(root+"Objects/Components/Permissions/ProtectionPermissionManager$1.class"))throw new AssertionError("Unexpected addition "+entry.getName());
     added++;
    }
   }
  }
  if(changed<2 || added<2 || added>3)throw new AssertionError("Incomplete patch");
  System.out.println("Artifact verified: "+preserved+" entries unchanged; "+changed+" changed; bridge added");
 }
}
