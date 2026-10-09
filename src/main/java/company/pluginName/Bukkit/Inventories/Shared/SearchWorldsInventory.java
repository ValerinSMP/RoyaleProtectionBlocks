package company.pluginName.Bukkit.Inventories.Shared;

import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Defaults.DefaultSearchWorldInventory;

@Inventory("searchworld")
public class SearchWorldsInventory extends DefaultSearchWorldInventory {

	public SearchWorldsInventory(Player player, Consumer<World> action) {
		super(player, action);
	}

	@ItemExecutor("Back-button")
	private void onClickBack() {
		goToPreviousInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Close-button")
	private void onClickClose() {
		closeInventory();
	}

}
