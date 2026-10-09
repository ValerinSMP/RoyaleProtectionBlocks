package company.pluginName.Bukkit.Inventories.ProtectionBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.ProtectionBlocks.ProtectionBlockRecipeInventory.SimpleRecipe;
import company.pluginName.Bukkit.Inventories.ProtectionBlocks.BannedWorlds.ProtectionBlockAllowedWorldsInventory;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Hooks.WorldGuard.WorldGuardAPI;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.Reference.ReferencedProtectionBlock;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.RecipesPckg.RecipesService;
import company.pluginName.Modules.RecipesPckg.Objects.Recipe;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.ChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Listeners.PandaMessageListener.Callback;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;
import darkpanda73.PandaUtils.Utilities.Java.String.StringUtilities;

@Inventory("protectionblocks_manage")
public class ProtectionBlockManageInventory extends ChestInventoryObject {

	private static final String PROTECTIONBLOCKITEM_NOTSETITEM_PATH = "Not-set-item";
	private static final String PROTECTIONBLOCKITEM_REPLACEITEMLORELINE_PATH = "Replace-item-lore-line";
	private static final String PROTECTIONBLOCKITEM_TAKEITEMLORELINE_PATH = "Take-item-lore-line";
	private static final String IDBUTTON_NOTSETITEM_PATH = "Not-set-item";
	private static final String IDBUTTON_NOTMODIFIABLEITEM_PATH = "Not-modifiable-item";
	private static final String PERMISSIONBUTTON_NOTSETITEM_PATH = "Not-set-item";
	private static final String PRICEBUTTON_NOTSETITEM_PATH = "Not-set-item";

	private static final String MESSAGES_IDSPECIFYINFO_PATH = "Messages.Id-specify-info";
	private static final String MESSAGES_PERMISSIONSPECIFYINFO_PATH = "Messages.Permission-specify-info";
	private static final String MESSAGES_BLOCKSSPECIFYINFO_PATH = "Messages.Blocks-specify-info";
	private static final String MESSAGES_PRICESPECIFYINFO_PATH = "Messages.Price-specify-info";

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	@PandaInject
	private static ProtectionsServiceImpl protectionsServiceImpl;

	@PandaInject
	private static RecipesService recipesService;

	@PandaInject
	private static WorldGuardAPI worldGuardApi;

	private ProtectionBlock originalProtectionBlock;
	private ProtectionBlock copyOriginalProtectionBlock;
	private ProtectionBlock newProtectionBlock;

	private SimpleRecipe originalRecipe;
	private SimpleRecipe newRecipe;

	public ProtectionBlockManageInventory(Player player) {
		this(player, null);
	}

	public ProtectionBlockManageInventory(Player player, ProtectionBlock protectionBlock) {
		super(player);
		this.originalProtectionBlock = protectionBlock;
		this.copyOriginalProtectionBlock = protectionBlock != null ? new ProtectionBlock(originalProtectionBlock)
				: null;
		this.newProtectionBlock = protectionBlock != null ? new ProtectionBlock(originalProtectionBlock)
				: new ProtectionBlock();

		Recipe recipe = originalProtectionBlock != null
				? recipesService.findRecipeByProtectionBlock(originalProtectionBlock)
				: null;
		if (recipe != null) {
			this.originalRecipe = new SimpleRecipe();
			for (int i = 0; i < 9; i++) {
				this.originalRecipe.getRecipe()[i] = recipe.getRecipe()[i] != null ? recipe.getRecipe()[i].clone()
						: null;
			}
			this.originalRecipe.setPermission(recipe.getPermission());
		}

		setReplacements(new Replacement[] { new Replacement("{block}",
				() -> newProtectionBlock.getId() != null ? newProtectionBlock.getId() : "???") });
		setTitleReplacements(getReplacements());
	}

	@ItemGenerator("Protection-block-item")
	private ItemStack generateProtectionBlockItem(Item item) {
		if (newProtectionBlock.getItem() != null) {
			ItemBuilder builder = ItemBuilder.inst().fromItem(newProtectionBlock.getItem());

			List<String> lore = builder.getLore();
			lore.add("&0");
			lore.add(item.getData().get(PROTECTIONBLOCKITEM_REPLACEITEMLORELINE_PATH).toString());
			lore.add(item.getData().get(PROTECTIONBLOCKITEM_TAKEITEMLORELINE_PATH).toString());
			builder.setLore(lore);

			return builder.apply(newProtectionBlock.getItem().clone());
		} else {
			return ItemBuilder.inst().fromMap(item.getData(), PROTECTIONBLOCKITEM_NOTSETITEM_PATH).build();
		}
	}

	@ItemGenerator("Blocks-x-button")
	private ItemStack generateBlocksXButton(Item item) {
		return ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY)
				.setReplacements(
						new Replacement("{blocks_x}", () -> String.valueOf((newProtectionBlock.getBlocksX() * 2) + 1)))
				.build();
	}

	@ItemGenerator("Blocks-y-button")
	private ItemStack generateBlocksYButton(Item item) {
		return ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY)
				.setReplacements(new Replacement("{blocks_y}",
						() -> newProtectionBlock.getBlocksY() == -1 ? Messages.MESSAGE_GENERAL_NOLIMIT.toString()
								: String.valueOf((newProtectionBlock.getBlocksY() * 2) + 1)))
				.build();
	}

	@ItemGenerator("Blocks-z-button")
	private ItemStack generateBlocksZButton(Item item) {
		return ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY)
				.setReplacements(
						new Replacement("{blocks_z}", () -> String.valueOf((newProtectionBlock.getBlocksZ() * 2) + 1)))
				.build();
	}

	@ItemGenerator("Id-button")
	private ItemStack generateIdButton(Item item) {
		return ItemBuilder.inst()
				.fromMap(item.getData(), (this.originalProtectionBlock != null ? IDBUTTON_NOTMODIFIABLEITEM_PATH
						: (this.newProtectionBlock.getId() != null ? Item.DISPLAYITEM_KEY : IDBUTTON_NOTSETITEM_PATH)))
				.setReplacements(new Replacement("{block_id}",
						() -> newProtectionBlock.getId() != null ? newProtectionBlock.getId() : ""))
				.build();
	}

	@ItemGenerator("Permission-button")
	private ItemStack generatePermissionButton(Item item) {
		return ItemBuilder.inst()
				.fromMap(item.getData(),
						(this.newProtectionBlock.getPermission() != null ? Item.DISPLAYITEM_KEY
								: PERMISSIONBUTTON_NOTSETITEM_PATH))
				.setReplacements(new Replacement("{block_permission}",
						() -> newProtectionBlock.getPermission() != null ? newProtectionBlock.getPermission() : ""))
				.build();
	}

	@ItemGenerator("Price-button")
	private ItemStack generatePriceButton(Item item) {
		return ItemBuilder.inst()
				.fromMap(item.getData(),
						this.newProtectionBlock.isForSale() ? Item.DISPLAYITEM_KEY : PRICEBUTTON_NOTSETITEM_PATH)
				.setReplacements(new Replacement("{block_price}",
						() -> newProtectionBlock.isForSale() ? StringUtilities.toCurrency(newProtectionBlock.getPrice())
								: "---"))
				.build();
	}

	@ItemExecutor("Cancel-button")
	private void executeCancelButton() {
		goToPreviousInventory();
	}

	@ItemExecutor("Close-button")
	private void executeCloseButton() {
		closeInventory();
	}

	@ItemExecutor("Confirm-button")
	private void executeConfirmButton() {
		List<Protection> relatedProtections = new ArrayList<>();
		try {
			if (originalProtectionBlock != null) {
				protectionsServiceImpl.getProtectionsByWorld().values().forEach(protections -> protections.stream()
						.filter(protection -> protection.getProtectionBlockId().equals(originalProtectionBlock.getId())
								&& protection.getUtils().isProtectionBlockShown())
						.forEach(relatedProtections::add));

				relatedProtections.forEach(prot -> prot.getUtils().hideProtectionBlock());

				originalProtectionBlock.copy(newProtectionBlock);
				originalProtectionBlock.save(getPlayer());
			} else {
				newProtectionBlock.save(getPlayer());
			}

			if (newRecipe != null) {
				Recipe recipeToModify = recipesService.findRecipeByProtectionBlock(newProtectionBlock);
				if (recipeToModify == null) {
					recipeToModify = new Recipe(new ReferencedProtectionBlock(newProtectionBlock.getId()));
				}
				for (int i = 0; i < 9; i++) {
					recipeToModify.getRecipe()[i] = newRecipe.getRecipe()[i];
				}
				recipeToModify.setPermission(newRecipe.getPermission());
				recipeToModify.save(getPlayer());
			}

			if (originalProtectionBlock != null) {
				MessageTemplate.inst(Messages.MESSAGE_PROTECTIONS_BLOCKS_SAVEDSUCCESSFULLY.applyPrefix()).process()
						.sendMessage(getPlayer());
			} else {
				MessageTemplate.inst(Messages.MESSAGE_PROTECTIONS_BLOCKS_CREATEDSUCCESSFULLY.applyPrefix()).process()
						.sendMessage(getPlayer());
			}

			if (getPreviousInventory() != null
					&& getPreviousInventory().getHolder() instanceof ProtectionBlocksListInventory) {
				((ProtectionBlocksListInventory) getPreviousInventory().getHolder()).updateEntityList();
			}
			goToPreviousInventory();
		} catch (RoyaleProtectionBlocksExceptionImpl ex) {
			relatedProtections.clear();
			if (originalProtectionBlock != null) {
				originalProtectionBlock.copy(copyOriginalProtectionBlock);
				try {
					originalProtectionBlock.save();
				} catch (RoyaleProtectionBlocksExceptionImpl e) {
					e.sendError(Bukkit.getConsoleSender());
				}
			}
			ex.sendError(getPlayer());
		} finally {
			relatedProtections.forEach(prot -> {
				if (Bukkit.getWorld(prot.getWorldName()) != null) {
					prot.getUtils().showProtectionBlock();

					try {
						prot.getUtils().regenerateProtectionArea();
						if (prot.getProtectedRegion() == null || (!prot.getProtectedRegion().getMinimumPoint()
								.equals(worldGuardApi.getHook().getInternalWorldGuard().asBlockVector(
										prot.getUtils().getProtectionArea().getMinLocation().toLocation()))
								|| !prot.getProtectedRegion().getMaximumPoint()
										.equals(worldGuardApi.getHook().getInternalWorldGuard().asBlockVector(
												prot.getUtils().getProtectionArea().getMaxLocation().toLocation())))) {
							prot.regenerateProtectedRegion();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}

	}

	@ItemExecutor("Protection-block-item")
	private void executeProtectionBlockItem(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		try {
			if (e.getClick() == ClickType.LEFT) {
				if (e.getCursor() != null && e.getCursor().getType() != Material.AIR) {
					newProtectionBlock.getBlockInformation().setItem(e.getCursor().clone());
					e.getWhoClicked().getOpenInventory().setCursor(null);
					updateInventory();
				} else if (newProtectionBlock.getItem() != null) {
					e.getWhoClicked().getOpenInventory().setCursor(newProtectionBlock.getItem());
					newProtectionBlock.getBlockInformation().setItem(null);
					updateInventory();
				}
			}
		} catch (RoyaleProtectionBlocksExceptionImpl ex) {
			ex.sendError(getPlayer());
		}
	}

	@ItemExecutor("Blocks-x-button")
	private void executeBlocksXButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		setBlocks(e, () -> newProtectionBlock.getBlocksX(),
				(blocks) -> newProtectionBlock.getBlockInformation().setBlocksX(blocks));
	}

	@ItemExecutor("Blocks-y-button")
	private void executeBlocksYButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		setBlocks(e, () -> newProtectionBlock.getBlocksY(),
				(blocks) -> newProtectionBlock.getBlockInformation().setBlocksY(blocks), true);
	}

	@ItemExecutor("Blocks-z-button")
	private void executeBlocksZButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		setBlocks(e, () -> newProtectionBlock.getBlocksZ(),
				(blocks) -> newProtectionBlock.getBlockInformation().setBlocksZ(blocks));
	}

	@ItemExecutor("Allowed-worlds-button")
	private void executeAllowedWorldsButton() {
		new ProtectionBlockAllowedWorldsInventory(getPlayer(), newProtectionBlock).openInventory();
	}

	@ItemExecutor("Id-button")
	private void executeIdButton() {
		if (originalProtectionBlock == null) {
			messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

				@Override
				public boolean message(String message) {
					newProtectionBlock.getBlockInformation().setId(!message.isEmpty() ? message : null);
					return true;
				}

				public void cancel() {
					openInventory();
				}

			});
			closeInventory();
			MessageTemplate
					.inst(PandaPrefixedStringField.applyPrefix(
							getChestInventoryData().getCustomFields().get(MESSAGES_IDSPECIFYINFO_PATH).toString()))
					.process().sendMessage(getPlayer());
		}
	}

	@ItemExecutor("Recipe-button")
	private void executeRecipeButton() {
		new ProtectionBlockRecipeInventory(getPlayer(), newProtectionBlock,
				newRecipe != null ? newRecipe : originalRecipe, (recipe) -> newRecipe = recipe).openInventory();
	}

	@ItemExecutor("Permission-button")
	private void executePermissionButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.getClick() == ClickType.LEFT) {
			messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

				@Override
				public boolean message(String message) {
					newProtectionBlock.getBlockInformation().setPermission(!message.isEmpty() ? message : null);
					return true;
				}

				public void cancel() {
					openInventory();
				}

			});
			closeInventory();
			MessageTemplate
					.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
							.get(MESSAGES_PERMISSIONSPECIFYINFO_PATH).toString()))
					.process().sendMessage(e.getWhoClicked());
		} else if (e.getClick() == ClickType.RIGHT && newProtectionBlock.getPermission() != null) {
			newProtectionBlock.getBlockInformation().setPermission(null);
			updateInventory();
		}
	}

	@ItemExecutor("Price-button")
	private void executePriceButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.getClick() == ClickType.LEFT) {
			messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

				@Override
				public boolean message(String message) {
					try {
						double price = Double.parseDouble(message);

						if (price <= 0D) {
							newProtectionBlock.getBlockInformation().setPrice(null);
						} else {
							newProtectionBlock.getBlockInformation().setPrice(price);
						}
					} catch (NumberFormatException e1) {
						MessageTemplate.inst(Messages.ERROR_INVALIDNUMBER.applyPrefix()).process()
								.sendMessage(e.getWhoClicked());
					}
					return true;
				}

				public void cancel() {
					openInventory();
				}

			});
			closeInventory();
			MessageTemplate
					.inst(PandaPrefixedStringField.applyPrefix(
							getChestInventoryData().getCustomFields().get(MESSAGES_PRICESPECIFYINFO_PATH).toString()))
					.process().sendMessage(e.getWhoClicked());
		} else if (e.getClick() == ClickType.RIGHT && newProtectionBlock.getPrice() != null) {
			newProtectionBlock.getBlockInformation().setPrice(null);
			updateInventory();
		}
	}

	public void setBlocks(InventoryClickEvent e, IntSupplier getBlocks, IntConsumer setBlocks) {
		this.setBlocks(e, getBlocks, setBlocks, false);
	}

	public void setBlocks(InventoryClickEvent e, IntSupplier getBlocks, IntConsumer setBlocks, boolean allowUnlimited) {
		int cur = getBlocks.getAsInt();
		if (e.getClick() == ClickType.SHIFT_LEFT) {
			messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

				@Override
				public boolean message(String message) {
					try {
						int value = Integer.parseInt(message);

						if (value < 0 && (value != -1 || !allowUnlimited)) {
							MessageTemplate.inst(Messages.ERROR_NUMBERBELOWZERO.applyPrefix()).process()
									.sendMessage(e.getWhoClicked());
							return true;
						}

						setBlocks.accept(value == -1 ? value : value / 2);
					} catch (NumberFormatException e1) {
						MessageTemplate.inst(Messages.ERROR_INVALIDNUMBER.applyPrefix()).process()
								.sendMessage(e.getWhoClicked());
					}
					return true;
				}

				public void cancel() {
					openInventory();
				}

			});
			closeInventory();
			MessageTemplate
					.inst(PandaPrefixedStringField.applyPrefix(
							getChestInventoryData().getCustomFields().get(MESSAGES_BLOCKSSPECIFYINFO_PATH).toString()))
					.process().sendMessage(e.getWhoClicked());
		} else if (e.getClick() == ClickType.LEFT) {
			setBlocks.accept(++cur);
			updateInventory();
		} else if (e.getClick() == ClickType.RIGHT && (cur > (allowUnlimited ? -1 : 0))) {
			setBlocks.accept(--cur);
			updateInventory();
		}
	}

}
