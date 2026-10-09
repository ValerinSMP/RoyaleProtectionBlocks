package company.pluginName.Modules.CommandsPckg.Commands.ProtectionBlocks;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import company.pluginName.Bukkit.Inventories.Store.ProtectionBlocksStoreInventory;
import company.pluginName.Modules.FilePckg.Messages;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Annotations.PandaCommandAnnotation;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Annotations.PandaCommandAnnotation.PandaSubCommandAnnotation;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Objects.PandaParameters;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Objects.PandaSubCommand;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Objects.Response.CommandResponse;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Objects.Response.CommandResponse.TrueResponse;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaBooleanField;

@PandaSubCommandAnnotation(parentCommand = ProtectionBlocksCommand.class)
@PandaCommandAnnotation(
		id = "store",
		pathName = "Store",
		defaultName = "store",
		defaultDescription = "Abre la tienda de bloques de protección")
@PandaCommandAnnotation.Customizable(
		cooldown = true,
		aliases = true,
		description = true,
		name = true,
		permission = true,
		usage = true)
public class StoreSubCommand extends PandaSubCommand {

	@RegisteredPandaField("config")
	private static final PandaBooleanField SETTINGS_STORE_ENABLEPROTECTIONBLOCKSSTORE = new PandaBooleanField(
			"Settings.Store.Enable-protection-blocks-store", true);

	public StoreSubCommand() throws InstantiationException {
		super();
	}

	@Override
	public boolean precondition() {
		return SETTINGS_STORE_ENABLEPROTECTIONBLOCKSSTORE.isTrue();
	}

	@Override
	public CommandResponse executeCommandProcess(CommandSender sender, PandaParameters parameters) {
		Player pl = sender instanceof Player ? (Player) sender : null;
		if (pl != null) {
			if (precondition()) {
				new ProtectionBlocksStoreInventory(pl).openInventory();
			} else {
				MessageTemplate.inst(Messages.ERROR_UNAVAILABLESTORE.applyPrefix()).process().sendMessage(sender);
			}
		} else {
			MessageTemplate.inst(Messages.ERROR_CONSOLEDENIED.applyPrefix()).process().sendMessage(sender);
		}
		return new TrueResponse();
	}
}
