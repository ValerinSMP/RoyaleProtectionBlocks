package company.pluginName.Modules.ProtectionsPckg.Listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.DoubleChestInventory;

import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Features.PvPPckg.Utils.PvPUtilities;
import company.pluginName.Modules.ProtectionPermissionsPckg.ProtectionPermissionsService;
import company.pluginName.Modules.ProtectionsPckg.Utils.CrystalPvpGuard;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaListener;

@PandaListener
public class ProtectionPermissionListener implements Listener {

	private static final List<String> CONTROLLED_BLOCK_INVENTORIES = Arrays.asList("CHEST", "DISPENSER", "DROPPER",
			"FURNACE", "BREWING", "SHULKER_BOX", "BARREL");

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionsByLocation(e.getBlock().getLocation().add(0.5D, 0.5D, 0.5D))
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !prot.canPlace(e.getPlayer())).filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
			e.setBuild(true);
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		if (RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionsByLocation(e.getBlock().getLocation().add(0.5D, 0.5D, 0.5D))
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !prot.canBreak(e.getPlayer())).filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractBlock(PlayerInteractEvent e) {
		if (e.getClickedBlock() == null) return;
		boolean inventoryAccess = company.pluginName.Modules.ProtectionsPckg.Utils.InteractionPermissionRoute
				.opensInventory(e.getAction(), e.getClickedBlock());
		if (e.getClickedBlock().getType().isInteractable() && RoyaleProtectionBlocksAPIImpl.getInstance()
				.getProtectionsService()
				.findProtectionsByLocation(e.getClickedBlock().getLocation().add(0.5D, 0.5D, 0.5D))
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !company.pluginName.Modules.ProtectionsPckg.Utils.InteractionPermissionRoute
						.allowed(prot, e.getPlayer(), inventoryAccess)).filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		if (RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionsByLocation(e.getRightClicked().getLocation())
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !prot.canInteract(e.getPlayer())).filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
		if (RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionsByLocation(e.getRightClicked().getLocation())
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !prot.canInteract(e.getPlayer())).filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (isInteractionDenied(e.getPlayer(), e.getRightClicked().getLocation())) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onProtectedEntityDamage(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player
				&& (e.getEntity() instanceof ArmorStand || e.getEntity() instanceof Hanging)
				&& isInteractionDenied((Player) e.getDamager(), e.getEntity().getLocation())) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHangingBreak(HangingBreakByEntityEvent e) {
		if (e.getRemover() instanceof Player
				&& isInteractionDenied((Player) e.getRemover(), e.getEntity().getLocation())) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent e) {
		if (e.getPlayer() != null && RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionsByLocation(e.getEntity().getLocation())
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !prot.canPlace(e.getPlayer())).filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
			e.setCancelled(true);
		}
	}

	private boolean isInteractionDenied(Player player, org.bukkit.Location location) {
		return RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionsByLocation(location)
				.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
				.map(prot -> !prot.canInteract(player)).filter(Boolean.TRUE::equals).findFirst().orElse(false);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockInventoryOpen(InventoryOpenEvent e) {
		if (e.getPlayer() instanceof Player && e.getInventory() != null && e.getInventory().getHolder() != null) {
			List<Block> blocks = new ArrayList<>();

			if (e.getInventory().getHolder() instanceof BlockInventoryHolder) {
				blocks.add(((BlockInventoryHolder) e.getInventory().getHolder()).getBlock());
			} else if (e.getInventory().getHolder() instanceof DoubleChest) {
				blocks.add(
						((Chest) ((DoubleChest) e.getInventory().getHolder()).getLeftSide()).getLocation().getBlock());
				blocks.add(
						((Chest) ((DoubleChest) e.getInventory().getHolder()).getRightSide()).getLocation().getBlock());
			}

			if (!blocks.isEmpty() && blocks.stream()
					.map(block -> RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
							.findProtectionsByLocation(block.getLocation().add(0.5D, 0.5D, 0.5D))
							.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
							.map(prot -> !prot.canOpenInventories((Player) e.getPlayer())).findFirst().orElse(false))
					.filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryMoveItem(InventoryMoveItemEvent e) {
		boolean mustCheck = e.getSource() != null
				&& CONTROLLED_BLOCK_INVENTORIES.contains(e.getSource().getType().name());
		boolean isHopper = e.getDestination() != null && e.getDestination().getType() == InventoryType.HOPPER;

		if (mustCheck) {
			if (isHopper) {
				mustCheck = ProtectionPermissionsService.INVENTORYACCESS_PERMISSION
						.isPreventHoppersIfNonMembersAccessDenied();
			} else {
				mustCheck = false;
			}
		}

		if (mustCheck) {
			List<Block> blocks = new ArrayList<>();

			if (e.getSource() instanceof DoubleChestInventory) {
				blocks.add(((Chest) ((DoubleChestInventory) e.getSource()).getLeftSide()).getLocation().getBlock());
				blocks.add(((Chest) ((DoubleChestInventory) e.getSource()).getRightSide()).getLocation().getBlock());
			} else {
				blocks.add(e.getSource().getLocation().getBlock());
			}

			if (!blocks.isEmpty() && blocks.stream()
					.map(block -> RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
							.findProtectionsByLocation(block.getLocation().add(0.5D, 0.5D, 0.5D))
							.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
							.map(prot -> !prot.canOpenInventories(null)).findFirst().orElse(false))
					.filter(Boolean.TRUE::equals).findFirst().orElse(false)) {
				e.setCancelled(true);

				if (isHopper && ProtectionPermissionsService.INVENTORYACCESS_PERMISSION
						.isDropHoppersIfNonMembersAccessDenied()) {
					((Hopper) e.getDestination().getHolder()).getBlock().breakNaturally();
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCrystalPvP(EntityDamageByEntityEvent e) {
		if (CrystalPvpGuard.shouldCancel(e, (location, player) -> Boolean.TRUE.equals(
				RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
						.findProtectionsByLocation(location)
						.sorted((first, second) -> Integer.compare(second.getPriority(), first.getPriority()))
						.map(protection -> protection.canPvP(player)).findFirst().orElse(true)))) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPvP(EntityDamageByEntityEvent e) {
		Player attacker = e.getDamager() instanceof Player player ? player
				: e.getDamager() instanceof org.bukkit.entity.Projectile projectile
						&& projectile.getShooter() instanceof Player shooter ? shooter : null;
		if (e.getEntity() instanceof Player && attacker != null) {
			boolean damagerAllowed = Boolean.TRUE.equals(RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
							.findProtectionsByLocation(attacker.getLocation())
							.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
							.map(prot -> prot.canPvP(attacker)).findFirst().orElse(true));
			boolean damagedAllowed = Boolean.TRUE.equals(RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
							.findProtectionsByLocation(e.getEntity().getLocation())
							.sorted((prot1, prot2) -> prot2.getPriority() - prot1.getPriority())
							.map(prot -> prot.canPvP((Player) e.getEntity())).findFirst().orElse(true));
			if (!damagerAllowed || !damagedAllowed) {
				e.setCancelled(true);
			}
		}
	}

}
