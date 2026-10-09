package company.pluginName.Modules.ProtectionBlocksPckg.Objects;

import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.PermissionsPckg.PermissionsService;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.Components.ProtectionBlockAllowedWorlds;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.Components.ProtectionBlockInformation;
import company.pluginName.Modules.SQLPckg.SQLService;
import company.pluginName.Utils.EconomyUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.PandaPluginClass;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemStackUtilities;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.ItemType;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.RemovalCause;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.ProtectionBlocks.IProtectionBlock;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionRemovalData;

@Data
@Accessors(chain = true)
@Setter(lombok.AccessLevel.NONE)
@RequiredArgsConstructor
public class ProtectionBlock implements IProtectionBlock {

	@RegisteredPandaField("lang")
	private static final PandaPrefixedStringField MESSAGE_PROTECTION_BLOCKS_CHARGEDSUCCESSFULLY = new PandaPrefixedStringField(
			"Message.Protection.Blocks.Charged-successfully", "&aSe descontaron &e${amount} &ade tu saldo.");

	@PandaInject
	private static PandaPluginClass plugin;

	@PandaInject
	private static SQLService sqlService;

	@PandaInject
	private static ProtectionBlocksService protectionBlocksService;

	public static final String PROTECTION_BLOCK_ID_KEY = "ProtectionBlockId";

	private @NonNull ProtectionBlockInformation blockInformation;
	private @NonNull ProtectionBlockAllowedWorlds blockAllowedWorlds;

	public ProtectionBlock() {
		this(new ProtectionBlockInformation());
	}

	public ProtectionBlock(ProtectionBlockInformation information) {
		this(information, new ProtectionBlockAllowedWorlds());
	}

	public ProtectionBlock(ProtectionBlock protectionBlock) {
		this.blockInformation = new ProtectionBlockInformation();
		this.blockAllowedWorlds = new ProtectionBlockAllowedWorlds();

		this.blockInformation.setId(protectionBlock.getId());
		this.copy(protectionBlock);
	}

	public void purchase(Player pl) {
		try {
			if (RoyaleProtectionBlocksAPIImpl.getInstance().getPlayerInteractionsService()
					.getProtectionBlocksStoreEconomyService() == null) {
				throw Exceptions.Protections.STOREUNAVAILABLE.generateException();
			}

			ItemStack item = getBlockInformation().generateItem();

			if (ItemStackUtilities.hasAvailableSpace(pl, item)) {
				if (PermissionsService.ECONOMY_BYPASS.hasPermission(pl)) {
					pl.getInventory().addItem(item);
				} else {
					if (EconomyUtilities.withdraw(RoyaleProtectionBlocksAPIImpl.getInstance()
							.getPlayerInteractionsService().getProtectionBlocksStoreEconomyService(), pl, getPrice())) {
						pl.getInventory().addItem(item);
						MessageTemplate.inst(MESSAGE_PROTECTION_BLOCKS_CHARGEDSUCCESSFULLY.applyPrefix())
								.setReplacements(new Replacement("{amount}", () -> String.valueOf(getPrice())))
								.process().sendMessage(pl);
					} else {
						MessageTemplate.inst(Messages.ERROR_INSUFFICIENTBALANCE.applyPrefix()).process()
								.sendMessage(pl);
					}
				}
			} else {
				MessageTemplate.inst(Messages.ERROR_INVENTORYFULL.applyPrefix()).process().sendMessage(pl);
			}
		} catch (RoyaleProtectionBlocksExceptionImpl e) {
			e.sendError(pl);
		}
	}

	public void save() throws RoyaleProtectionBlocksExceptionImpl {
		this.save(null);
	}

	public void save(Player player) throws RoyaleProtectionBlocksExceptionImpl {
		if (player != null) {
			if (!PermissionsService.BLOCKS_CREATE.hasPermission(player)) {
				throw Exceptions.Protections.Blocks.Save.PERMISSIONDENIED.generateException();
			}
		}

		if (this.getId() == null) {
			throw Exceptions.Protections.Blocks.Save.IDNULL.generateException();
		}

		if (this.getItem() == null) {
			throw Exceptions.Protections.Blocks.Save.ITEMNULL.generateException();
		}

		ProtectionBlock registeredProtectionBlock = protectionBlocksService.getProtectionBlockById(this.getId());

		if (registeredProtectionBlock != null && registeredProtectionBlock != this) {
			throw Exceptions.Protections.Blocks.Save.IDINUSE.generateException();
		}

		sqlService.saveProtectionBlock(this);
		protectionBlocksService.registerProtectionBlock(this);
	}

	public void delete() throws RoyaleProtectionBlocksExceptionImpl {
		this.delete(null);
	}

	public void delete(Player player) throws RoyaleProtectionBlocksExceptionImpl {
		if (player != null) {
			if (!PermissionsService.BLOCKS_DELETE.hasPermission(player)) {
				throw Exceptions.Protections.Blocks.Delete.PERMISSIONDENIED.generateException();
			}
		}

		RoyaleProtectionBlocksAPI.getInstance().getProtectionsService().findProtectionsByProtectionBlock(this)
				.collect(Collectors.toList()).forEach(protection -> {
					try {
						RoyaleProtectionBlocksAPI.getInstance().getProtectionsService().delete(ProtectionRemovalData
								.inst(player, protection.getProtectionId(), RemovalCause.PROTECTION_BLOCK_REMOVAL));
					} catch (RoyaleProtectionBlocksException e1) {
						throw new RuntimeException(e1);
					}
				});

		sqlService.deleteProtectionBlock(this);
		protectionBlocksService.unregisterProtectionBlock(this);
	}

	public ProtectionBlock copy(ProtectionBlock protectionBlock) {
		this.blockInformation.copy(protectionBlock.getBlockInformation());
		this.blockAllowedWorlds.copy(protectionBlock.getBlockAllowedWorlds());
		return this;
	}

	/*
	 * API methods
	 */

	@Override
	public String getId() {
		return this.blockInformation.getId();
	}

	@Override
	public int getBlocksX() {
		return this.blockInformation.getBlocksX();
	}

	@Override
	public int getBlocksY() {
		return this.blockInformation.getBlocksY();
	}

	@Override
	public int getBlocksZ() {
		return this.blockInformation.getBlocksZ();
	}

	@Override
	public ItemStack getItem() {
		return this.blockInformation.getItem();
	}

	@Override
	public ItemType getItemType() {
		return this.blockInformation.getItemType();
	}

	@Override
	public ItemStack generateItem() {
		try {
			return this.blockInformation.generateItem();
		} catch (RoyaleProtectionBlocksExceptionImpl e) {
			e.sendError(Bukkit.getConsoleSender());
			return null;
		}
	}

	@Override
	public boolean isSameType(Block block) {
		return this.blockInformation.isSameType(block);
	}

	@Override
	public boolean isSameType(ItemStack item) {
		return this.blockInformation.isSameType(item);
	}

	@Override
	public String getPermission() {
		return this.blockInformation.getPermission();
	}

	@Override
	public Double getPrice() {
		return getBlockInformation().getPrice();
	}

	@Override
	public boolean isForSale() {
		return getBlockInformation().isForSale();
	}

	@Override
	public Set<String> getAllowedWorlds() {
		return this.blockAllowedWorlds.get();
	}

}
