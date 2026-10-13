package company.pluginName.Tests;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import company.pluginName.Modules.PlayerInteractionsPckg.PlayerInteractionsServiceImpl;
import company.pluginName.Modules.PlayerInteractionsPckg.ReadOnlyFeedback;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;

public final class PresentationRegressionTest {
    static int checks;static final List<String> chat=new ArrayList<>(),bars=new ArrayList<>();
    static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},h);}
    static Object fallback(Object p,Method m,Object[] a){
        if(m.getName().equals("equals"))return p==a[0];
        if(m.getName().equals("hashCode"))return System.identityHashCode(p);
        if(m.getName().equals("toString"))return "fake";
        if(m.getReturnType()==boolean.class)return false;
        if(m.getReturnType()==int.class)return 0;
        return null;
    }
    static class Spigot extends Player.Spigot {
        @Override public void sendMessage(ChatMessageType type,BaseComponent component){
            (type==ChatMessageType.ACTION_BAR?bars:chat).add(component.toPlainText());
        }
        @Override public void sendMessage(ChatMessageType type,BaseComponent... components){
            (type==ChatMessageType.ACTION_BAR?bars:chat).add(BaseComponent.toPlainText(components));
        }
    }
    public static void main(String[] args)throws Exception {
        String version=args[0];
        field(Bukkit.class,"server").set(null,proxy(Server.class,(p,m,a)->switch(m.getName()){
            case "getBukkitVersion"->version+"-R0.1-SNAPSHOT";
            case "getVersion"->"Leaf (MC: "+version+")";
            case "getLogger"->Logger.getLogger("presentation-test");
            default->fallback(p,m,a);
        }));
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()){
            case "spigot"->new Spigot();
            case "sendActionBar"->{bars.add(PlainTextComponentSerializer.plainText().serialize((Component)a[0]));yield null;}
            case "sendMessage"->{chat.add(a[0].toString());yield null;}
            default->fallback(p,m,a);
        });
        String builder=MessageTemplate.getMessageBuilder().getClass().getName();
        require(builder.contains(version.startsWith("26.")?"NMS.Modern":"NMS.v1_11_R1"),"correct builder "+version);
        MessageTemplate.inst("&#00FB9AMensaje temporal").process().sendActionBar(player);
        require(bars.equals(List.of("Mensaje temporal"))&&chat.isEmpty(),"action bar stays out of chat");
        MessageTemplate.inst("&aResultado", "&7Detalle").process().sendMessage(player);
        require(chat.equals(List.of("Resultado","Detalle"))&&bars.size()==1,"chat multiline preserved");
        MessageTemplate.inst("&7Hola {player}").setReplacements(new Replacement("{player}",()->"<red>Nombre")).process().sendMessage(player);
        require(chat.get(2).equals("Hola <red>Nombre"),"player text is literal, not MiniMessage");
        CommandSender console=proxy(CommandSender.class,(p,m,a)->{
            if(m.getName().equals("sendMessage")){chat.add(a[0].toString());return null;}
            return fallback(p,m,a);
        });
        MessageTemplate.inst("Aviso de consola").process().sendActionBar(console);
        require(chat.get(3).equals("Aviso de consola"),"console fallback remains legible");
        String paged="Mis protecciones ({current_page}/{max_page})";
        require(PagedChestInventoryObject.formatPageTitle(paged,1).equals("Mis protecciones"),"hide one-page counter");
        require(PagedChestInventoryObject.formatPageTitle(paged,0).equals("Mis protecciones"),"hide empty-page counter");
        require(PagedChestInventoryObject.formatPageTitle(paged,2).equals(paged),"retain multiple-page counter");
        require(PagedChestInventoryObject.formatPageTitle("Protección | {protection_name}",1).equals("Protección | {protection_name}"),"nonpaged title preserved");
        String literal="Selección (1/1) ({current_page}/{max_page})";
        require(PagedChestInventoryObject.formatPageTitle(literal,1).equals("Selección (1/1)"),"literal name untouched");
        PlayerInteractionsServiceImpl service=new PlayerInteractionsServiceImpl();
        Method writable=service.getClass().getDeclaredMethod("checkIfWritable");writable.setAccessible(true);
        Settings.SETTINGS_NETWORK_VIEWONLY.setObjectContent(false);writable.invoke(service);
        require(true,"writable node still allowed");
        ReadOnlyFeedback.READ_ONLY.setObjectContent("Este servidor es de solo lectura.");
        Settings.SETTINGS_NETWORK_VIEWONLY.setObjectContent(true);
        try {writable.invoke(service);throw new AssertionError("read-only not blocked");}
        catch(InvocationTargetException ex){
            require(ex.getCause() instanceof RoyaleProtectionBlocksExceptionImpl,"exception contract retained");
            RoyaleProtectionBlocksExceptionImpl error=(RoyaleProtectionBlocksExceptionImpl)ex.getCause();
            require(error.getMessage().equals("Este servidor es de solo lectura."),"read-only has its own configurable message");
            require(error.getType()==royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException.Type.PROTECTIONS_PERMISSIONDENIED,"public exception type preserved");
        }
        Exceptions.Protections.PERMISSIONDENIED.setObjectContent("No tienes permiso para esta acción.");
        require(Exceptions.Protections.PERMISSIONDENIED.generateException().getMessage().equals("No tienes permiso para esta acción."),"permission denial remains distinct");
        Settings.SETTINGS_NETWORK_VIEWONLY.setObjectContent(false);
        System.out.println("PresentationRegressionTest "+version+": "+checks+" checks passed; no live server/DB");
    }
}
