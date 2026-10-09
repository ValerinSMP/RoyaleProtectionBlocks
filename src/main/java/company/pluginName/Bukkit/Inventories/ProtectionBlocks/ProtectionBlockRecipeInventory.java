package company.pluginName.Bukkit.Inventories.ProtectionBlocks;

import java.util.Collections;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
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
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item.ClickResult;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Utils.ItemUtilities;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Listeners.PandaMessageListener.Callback;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;
import darkpanda73.PandaUtils.Utilities.Java.Methods.MethodFinder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Inventory("protectionblocks_recipe")
public class ProtectionBlockRecipeInventory extends ChestInventoryObject {

	private static final Item RECIPE_ITEM = new Item("Recipe-item", Collections.emptyMap())
			.setOnClickItemMethod(MethodFinder.forClass(ProtectionBlockRecipeInventory.class).setName("onEntityClick")
					.findSafely().getMethod())
			.setDefaultClickResult(ClickResult.IGNORE);
	public static final String RECIPESLOTS_PATH = "Recipe-slots";
	public static final String MESSAGES_PERMISSIONSPECIFYINFO_PATH = "Messages.Permission-specify-info";
	public static final String PERMISSION_NOTSETITEM_PATH = "Not-set-item";

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	public int[] recipeSlots;

	private ProtectionBlock protectionBlock;
	private SimpleRecipe newRecipe;
	private Consumer<SimpleRecipe> onRecipeUpdate;

	public ProtectionBlockRecipeInventory(Player pl, ProtectionBlock protectionBlock, SimpleRecipe recipe,
			Consumer<SimpleRecipe> onRecipeUpdate) {
		super(pl);

		this.protectionBlock = protectionBlock;
		this.newRecipe = new SimpleRecipe();
		if (recipe != null) {
			for (int i = 0; i < 9; i++) {
				this.newRecipe.getRecipe()[i] = recipe.getRecipe()[i] != null ? recipe.getRecipe()[i].clone() : null;
			}
			this.newRecipe.setPermission(recipe.getPermission());
		}
		this.onRecipeUpdate = onRecipeUpdate;

		setReplacements(new Replacement[] {
				new Replacement("{block}", () -> protectionBlock.getId() != null ? protectionBlock.getId() : "???") });
		setTitleReplacements(getReplacements());
	}

	@Override
	protected void onPreUpdate() {
		this.recipeSlots = ItemUtilities
				.stringToSlots(this.getChestInventoryData().getCustomFields().get(RECIPESLOTS_PATH).toString());
	}

	@Override
	protected void onPostUpdate() {
		for (int i = 0; i < 9 && i < this.recipeSlots.length; i++) {
			this.setSlot(this.recipeSlots[i],
					new GeneratedItem(RECIPE_ITEM, newRecipe.getRecipe()[i], (ItemBuilder) null));
		}
	}

	@ItemGenerator("Permission-button")
	private ItemStack generatePermissionButton(Item item) {
		return ItemBuilder.inst()
				.fromMap(item.getData(),
						this.newRecipe.getPermission() != null ? Item.DISPLAYITEM_KEY : PERMISSION_NOTSETITEM_PATH)
				.setReplacements(new Replacement("{block_permission}",
						() -> newRecipe.getPermission() != null ? newRecipe.getPermission() : ""))
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
	private void executeConfirmButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		for (int i = 0; i < 9 && i < recipeSlots.length; i++) {
			newRecipe.getRecipe()[i] = e.getInventory().getItem(recipeSlots[i]);
		}

		onRecipeUpdate.accept(newRecipe);

		goToPreviousInventory();
	}

	@ItemExecutor("Permission-button")
	private void executePermissionButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		for (int i = 0; i < 9 && i < recipeSlots.length; i++) {
			newRecipe.getRecipe()[i] = e.getInventory().getItem(recipeSlots[i]);
		}

		if (e.getClick() == ClickType.LEFT) {
			messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

				@Override
				public boolean message(String message) {
					newRecipe.setPermission(!message.isEmpty() ? message : null);
					return true;
				}

				public void cancel() {
					openInventory();
				}

			});
			closeInventory();
			MessageTemplate.inst(PandaPrefixedStringField.applyPrefix(
					this.getChestInventoryData().getCustomFields().get(MESSAGES_PERMISSIONSPECIFYINFO_PATH).toString()))
					.process().sendMessage(getPlayer());
		} else if (e.getClick() == ClickType.RIGHT && newRecipe.getPermission() != null) {
			newRecipe.setPermission(null);
			updateInventory();
		}
	}

	@NoArgsConstructor
	@Getter
	public static class SimpleRecipe {

		private ItemStack[] recipe = new ItemStack[9];
		private @Setter String permission = null;

	}

}
