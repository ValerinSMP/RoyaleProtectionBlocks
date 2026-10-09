package company.pluginName.Modules.ProtectionBlocksPckg;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import company.pluginName.Modules.SQLPckg.SQLService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.ReloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaBooleanField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import lombok.Getter;

@PandaService(priority = 998)
public class ProtectionBlocksService {
	@RegisteredPandaField("config")
	private static final PandaBooleanField PRICE_LORE_ENABLED = new PandaBooleanField(
			"Protection-block-presentation.Price-lore.Enabled", true);
	@RegisteredPandaField("config")
	private static final PandaStringField PRICE_LORE_PLACEHOLDER = new PandaStringField(
			"Protection-block-presentation.Price-lore.Placeholder", "{price}");
	@RegisteredPandaField("config")
	private static final PandaStringField PRICE_LORE_LEGACY_PREFIX = new PandaStringField(
			"Protection-block-presentation.Price-lore.Legacy-prefix", "");
	@RegisteredPandaField("config")
	private static final PandaStringField PRICE_LORE_LEGACY_SUFFIX = new PandaStringField(
			"Protection-block-presentation.Price-lore.Legacy-suffix", "");

	@PandaInject
	private SQLService sqlService;

	private @Getter HashMap<String, ProtectionBlock> protectionBlocks = new HashMap<>();

	@LoadMethod
	private void load() {
		// Validate before replacing the current definitions; reload failure preserves the cache.
		PriceLore renderer = new PriceLore(PRICE_LORE_ENABLED.getContent(), PRICE_LORE_PLACEHOLDER.getContent(),
				PRICE_LORE_LEGACY_PREFIX.getContent(), PRICE_LORE_LEGACY_SUFFIX.getContent());
		HashMap<String, ProtectionBlock> loaded = new HashMap<>();
		sqlService.getProtectionBlocks().stream()
				.filter(block -> block.getItem() != null && block.getItem().getType() != Material.AIR)
				.forEach(block -> {
					ItemMeta meta = block.getItem().getItemMeta();
					if (meta != null && meta.hasLore()) {
						var previous = meta.getLore();
						var updated = renderer.render(block.getPrice(), previous);
						if (!updated.equals(previous)) {
							meta.setLore(updated);
							block.getItem().setItemMeta(meta);
						}
					}
					loaded.put(block.getId().toLowerCase(), block);
				});
		protectionBlocks.clear();
		protectionBlocks.putAll(loaded);
	}

	@UnloadMethod
	private void unload() {
		this.protectionBlocks.clear();
	}

	@ReloadMethod
	private void reload() {
		load();
	}

	/*
	 * Register/Unregister methods
	 */

	public synchronized void registerProtectionBlock(ProtectionBlock protectionBlock) {
		protectionBlocks.putIfAbsent(protectionBlock.getId().toLowerCase(), protectionBlock);
	}

	public synchronized void unregisterProtectionBlock(ProtectionBlock protectionBlock) {
		protectionBlocks.remove(protectionBlock.getId());
	}

	/*
	 * Search methods
	 */

	public ProtectionBlock getProtectionBlockById(String id) {
		return protectionBlocks.get(id.toLowerCase());
	}

}
