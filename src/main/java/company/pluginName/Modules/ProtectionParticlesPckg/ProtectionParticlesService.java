package company.pluginName.Modules.ProtectionParticlesPckg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import company.pluginName.MainPluginClass;
import org.bukkit.scheduler.BukkitTask;

import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Hooks.ProtocolLib.ProtocolLibAPI;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Message;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.ReloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import darkpanda73.PandaUtils.PandaUtilities.NMS.Packets.PacketPlayOutParticle;
import darkpanda73.PandaUtils.PandaUtilities.NMS.Packets.PacketPlayOutParticle.DustParticleOptions;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaDoubleField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaIntegerField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import darkpanda73.PandaUtils.Utilities.Java.Objects.Pair;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.SimpleLocation;

@PandaService
public class ProtectionParticlesService implements Listener {

	@PandaInject
	private MainPluginClass plugin;

	private static final Color GREEN_COLOR = Color.fromRGB(50, 255, 55);
	private static final Color GRAY_COLOR = Color.fromRGB(180, 180, 180);
	private static final Color YELLOW_COLOR = Color.fromRGB(225, 230, 0);
	private static final Color BROWN_COLOR = Color.fromRGB(200, 130, 0);
	private static final Color PURPLE_COLOR = Color.fromRGB(195, 70, 255);

	@RegisteredPandaField("config")
	public static final PandaIntegerField SETTINGS_PROTECTION_VIEW_RADIUS = new PandaIntegerField(
			"Settings.Protection.View.Radius", 16);

	@RegisteredPandaField("config")
	public static final PandaDoubleField SETTINGS_PROTECTION_VIEW_SPEED = new PandaDoubleField(
			"Settings.Protection.View.Speed", 1.5D);

	@RegisteredPandaField("config")
	public static final PandaIntegerField SETTINGS_PROTECTION_VIEW_DURATIONINMILLIS = new PandaIntegerField(
			"Settings.Protection.View.Duration-in-seconds", 60);

	@RegisteredPandaField("lang")
	public static final PandaStringField MESSAGE_PROTECTION_VIEW_VIEWACTIVEACTIONBAR = new PandaStringField(
			"Message.Protection.View.View-active-action-bar", "&eProtection view: &aActive");

	@PandaInject
	private PlayerDataService playerDataService;

	@PandaInject
	private ProtocolLibAPI protocolLibApi;

	private HashMap<UUID, ProtectionParticlePlayerContainer> containers = new HashMap<>();
	private BukkitTask checkerTask;

	@LoadMethod
	private void load() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		checkerTask = TasksUtils.executeWithTimer(() -> {
			containers.entrySet().stream().filter(entry -> {
				if ((entry.getValue().getTaskStart()
						+ (SETTINGS_PROTECTION_VIEW_DURATIONINMILLIS.getContent() * 1000L)) < System
								.currentTimeMillis()) {
					entry.getValue().stop();
					return true;
				}
				return false;
			}).map(entry -> entry.getKey()).collect(Collectors.toList()).forEach(uuid -> deactivateView(uuid));
		}, 20, 20);
	}

	@UnloadMethod
	private void unload() {
		HandlerList.unregisterAll(this);
		if (checkerTask != null) {
			checkerTask.cancel();
			checkerTask = null;
		}
		containers.values().forEach(ProtectionParticlePlayerContainer::stop);
		containers.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		deactivateView(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		ProtectionParticlePlayerContainer container = containers.get(event.getPlayer().getUniqueId());
		if (container != null) {
			container.currentTick = 0;
			container.updateLocation(event.getPlayer(), event.getPlayer().getLocation().getBlock().getLocation());
		}
	}

	@ReloadMethod
	private void reload() {
		unload();
		load();
	}

	public boolean isActive(Player player) {
		return this.containers.containsKey(player.getUniqueId());
	}

	public void toggleView(Player player, boolean showAll) {
		if (this.containers.containsKey(player.getUniqueId())) {
			deactivateView(player.getUniqueId());
		} else {
			activateView(player, showAll);
		}
	}

	private void activateView(Player player, boolean showAll) {
		ProtectionParticlePlayerContainer playerContainer = new ProtectionParticlePlayerContainer(this, player,
				showAll);

		containers.put(player.getUniqueId(), playerContainer);
		playerContainer.start();
	}

	private void deactivateView(UUID playerUuid) {
		ProtectionParticlePlayerContainer container = containers.remove(playerUuid);

		if (container != null) {
			container.stop();
		}
	}

	private static class ProtectionParticlePlayerContainer {

		private ProtectionParticlesService service;
		private UUID playerUuid;
		private final Player playerSession;
		private boolean showAll;

		private long radius = SETTINGS_PROTECTION_VIEW_RADIUS.getContent();
		private double speed = 1D / SETTINGS_PROTECTION_VIEW_SPEED.getContent();

		private long length = (radius * 2) + 1;
		private long diagonalLength = (length * 2) - 1;
		private double diagonalPortion = ((double) diagonalLength * 0.05D);

		private Location currentLocation;
		private Location currentMinLocation;
		private Location currentMaxLocation;
		private List<Protection> currentProtections;
		private int currentTick;

		private Message actionBarMessage;

		private long taskStart = System.currentTimeMillis();
		private BukkitTask task;

		public long getTaskStart() { return taskStart; }

		public ProtectionParticlePlayerContainer(ProtectionParticlesService service, Player player, boolean showAll) {
			this.service = service;
			this.playerUuid = player.getUniqueId();
			this.playerSession = player;
			this.showAll = showAll;
			this.currentTick = 0;
			this.actionBarMessage = MessageTemplate.inst(MESSAGE_PROTECTION_VIEW_VIEWACTIVEACTIONBAR.getContent())
					.process();

			updateLocation(player, player.getLocation().getBlock().getLocation());
		}

		public void start() {
			this.taskStart = System.currentTimeMillis();
			if (this.task != null) { return; }
			this.task = TasksUtils.executeWithTimer(() -> processTick(), 1, 1);
			if (this.task != null) {
				// Keep the existing configurable text, but announce only the transition.
				actionBarMessage.sendMessage(playerSession);
			} else {
				service.deactivateView(playerUuid);
			}
		}

		public void stop() {
			if (this.task != null) {
				this.task.cancel();
				this.task = null;
			}

		}

		private void processTick() {
			Player pl = Bukkit.getPlayer(playerUuid);

			if (service.containers.get(playerUuid) != this) { return; }

			if (pl != playerSession || !pl.isOnline()) {
				service.deactivateView(playerUuid);
				return;
			}

			if (!pl.getWorld().equals(currentLocation.getWorld())) {
				currentTick = 0;
				updateLocation(pl, pl.getLocation().getBlock().getLocation());
			}

			currentTick++;

			if (!currentProtections.isEmpty()) {
				long min = (int) ((diagonalPortion * (currentTick - 1)) / speed);
				long max = (int) ((diagonalPortion * currentTick) / speed);

				if (currentTick >= (20 * speed)) {
					max = diagonalLength + 1;
				}

				if (currentTick > (20 * speed)) {
					min = diagonalLength;
				}

				Location diagonalLocation = currentMinLocation.clone().add(min, 0, 0);
				try {
					for (long i = min; i <= max; i++) {
						for (long j = i; j >= 0; j--) {
							if (diagonalLocation.getY() > currentMaxLocation.getY()) {
								break;
							}

							if (j <= length) {
								for (long k = 0; k <= length; k++) {
									SimpleLocation simpleDiagonalLocation = SimpleLocation.of(diagonalLocation);
									if (currentProtections.stream().anyMatch(
											prot -> !prot.isDeleted() && prot.isInside(simpleDiagonalLocation, true))) {
										Optional<Pair<Protection, Long>> coincidences = currentProtections.stream()
												.filter(prot -> !prot.isDeleted()
														&& prot.isInside(simpleDiagonalLocation, true))
												.map(prot -> Pair.of(prot, Arrays
														.asList(((diagonalLocation.getY() == currentLocation.getY()
																|| diagonalLocation.getY() == prot.getBukkitLocation()
																		.getY())
																|| (diagonalLocation.getY() == prot.getProtectionArea()
																		.getMinLocation().getY()
																		|| diagonalLocation
																				.getY() == (prot.getProtectionArea()
																						.getMaxLocation().getY()))),
																(diagonalLocation.getX() == prot.getProtectionArea()
																		.getMinLocation().getX()),
																(diagonalLocation.getZ() == prot.getProtectionArea()
																		.getMinLocation().getZ()),
																(diagonalLocation.getX() == (prot.getProtectionArea()
																		.getMaxLocation().getX())),
																(diagonalLocation.getZ() == (prot.getProtectionArea()
																		.getMaxLocation().getZ())))
														.stream().filter(Boolean.TRUE::equals).count()))
												.filter(pair -> pair.getSecond() >= 2)
												.collect(Collectors.maxBy((pair1, pair2) -> {
													if (pair1.getSecond() == pair2.getSecond()) {
														return Integer.compare(pair1.getFirst().getPriority(),
																pair2.getFirst().getPriority());
													}
													return Long.compare(pair1.getSecond(), pair2.getSecond());
												}));

										if (coincidences.isPresent() && coincidences.get().getSecond() >= 2L) {
											if (coincidences.get().getSecond() >= 3L
													|| (diagonalLocation.getY() != currentLocation.getY()
															&& diagonalLocation.getY() != coincidences.get().getFirst()
																	.getBukkitLocation().getY())) {
												if (coincidences.get().getFirst().isMember(playerUuid)
														|| coincidences.get().getFirst().isOwner(playerUuid)) {
													if (currentProtections.stream().filter(
															prot -> prot.isInside(simpleDiagonalLocation, false))
															.count() == 0) {
														sendParticle(pl, diagonalLocation, GREEN_COLOR);
													}
												} else {
													sendParticle(pl, diagonalLocation, GRAY_COLOR);
												}
											}

											if (diagonalLocation.getY() == coincidences.get().getFirst()
													.getBukkitLocation().getY()) {
												sendParticle(pl, diagonalLocation.clone().add(0, 0.5D, 0),
														YELLOW_COLOR);
											} else if (diagonalLocation.getY() == currentLocation.getY()) {
												if (currentProtections.stream()
														.filter(prot -> prot.isInside(simpleDiagonalLocation, false))
														.count() == 0) {
													sendParticle(pl, diagonalLocation.clone().add(0, 0.5D, 0),
															BROWN_COLOR);
												}
											}
										}

										SimpleLocation diagonalExtraLocation = SimpleLocation.of(diagonalLocation)
												.add(0.5D, 0.5D, 0.5D);
										coincidences = currentProtections.stream()
												.filter(prot -> !prot.isDeleted()
														&& prot.isInside(diagonalExtraLocation, true))
												.map(prot -> Pair.of(prot, Arrays.asList(
														(diagonalLocation.getX() == prot.getBukkitLocation().getX()),
														(diagonalLocation.getY() == prot.getBukkitLocation().getY()),
														(diagonalLocation.getZ() == prot.getBukkitLocation().getZ()))
														.stream().filter(Boolean.TRUE::equals).count()))
												.filter(pair -> pair.getSecond() >= 2)
												.collect(Collectors.maxBy((pair1, pair2) -> {
													if (pair1.getSecond() == pair2.getSecond()) {
														return Integer.compare(pair1.getFirst().getPriority(),
																pair2.getFirst().getPriority());
													}
													return Long.compare(pair1.getSecond(), pair2.getSecond());
												}));

										if (coincidences.isPresent()) {
											if (coincidences.get().getSecond() == 2) {
												sendParticle(pl, diagonalExtraLocation.toLocation(), YELLOW_COLOR);
											} else {
												sendParticle(pl, diagonalExtraLocation.toLocation(), PURPLE_COLOR);
											}
										}
									}
									diagonalLocation.add(0, 0, 1);
								}
								diagonalLocation.setZ(currentMinLocation.getZ());
								diagonalLocation.add(-1, 1, 0);
							} else {
								diagonalLocation.add(-1, 1, 0);
							}
						}
						diagonalLocation.setX(currentMinLocation.getX() + i);
						diagonalLocation.setY(currentMinLocation.getY());
						diagonalLocation.setZ(currentMinLocation.getZ());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (currentTick >= (20 * speed)) {
				currentTick = 0;

				updateLocation(pl, pl.getLocation().getBlock().getLocation());
			}

		}

		private void updateLocation(Player player, Location location) {
			if (this.currentLocation == null || !this.currentLocation.equals(location)) {
				currentLocation = location;
				currentMinLocation = currentLocation.clone().subtract(radius, radius, radius);
				currentMaxLocation = currentLocation.clone().add(radius + 1, radius + 1, radius + 1);
				updateProtections(player, location);
			}
		}

		private void updateProtections(Player player, Location location) {
			List<Protection> newProtections = RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
					.findProtectionsByArea(currentMinLocation, currentMaxLocation).filter(prot -> showAll
							|| prot.isMember(playerUuid) || prot.isOwner(playerUuid) || prot.isMainOwner(playerUuid))
					.collect(Collectors.toList());

			this.currentProtections = newProtections;
		}

		private void sendParticle(Player player, Location location, Color color) throws Exception {
			if (this.service.protocolLibApi.isHooked()) {
				this.service.protocolLibApi.getHook().spawnParticle(player, location, color);
			} else {
				new PacketPlayOutParticle(location.getX(), location.getY(), location.getZ(), 0, 0, 0, 0.5F, 5, false,
						new DustParticleOptions(color, 1.0F)).send(player);
			}
		}

	}

}
