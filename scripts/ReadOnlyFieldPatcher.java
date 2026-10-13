import java.nio.file.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
/** Change one diagnostic field in the verified installed method; preserve all other methods. */
public final class ReadOnlyFieldPatcher {
    public static void main(String[] args)throws Exception {
        String path="royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/PlayerInteractionsServiceImpl.class";
        byte[] original;
        try(ZipFile zip=new ZipFile(args[0])){original=zip.getInputStream(zip.getEntry(path)).readAllBytes();}
        ClassReader reader=new ClassReader(original);
        ClassWriter writer=new ClassWriter(reader,0);
        int[] changes={0};
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                MethodVisitor mv=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(!name.equals("checkIfWritable")||!descriptor.equals("()V"))return mv;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    @Override public void visitFieldInsn(int opcode,String owner,String field,String type){
                        if(opcode==Opcodes.GETSTATIC&&owner.equals("royale/RoyaleProtectionBlocks/Plugin/Exceptions/Exceptions$Protections")&&field.equals("PERMISSIONDENIED")){
                            if(!type.equals("Lroyale/RoyaleProtectionBlocks/Plugin/Exceptions/Exceptions$ThrowableField;"))throw new AssertionError(type);
                            super.visitFieldInsn(opcode,"royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/ReadOnlyFeedback","READ_ONLY",type);
                            changes[0]++;
                        }else super.visitFieldInsn(opcode,owner,field,type);
                    }
                };
            }
        },0);
        if(changes[0]!=1)throw new AssertionError("Expected exactly one diagnostic field change, got "+changes[0]);
        Path output=Path.of(args[1],path);Files.createDirectories(output.getParent());Files.write(output,writer.toByteArray());
        System.out.println("Read-only patch: one field reference in checkIfWritable; original installed class preserved.");
    }
}
