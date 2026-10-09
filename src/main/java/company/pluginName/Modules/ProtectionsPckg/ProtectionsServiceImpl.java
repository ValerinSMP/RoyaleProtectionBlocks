package company.pluginName.Modules.ProtectionsPckg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import company.pluginName.MainPluginClass;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Hooks.CoreProtect.CoreProtectAPI;
import company.pluginName.Hooks.WorldGuard.WorldGuardAPI;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Objects.Components.Permissions.ProtectionPermission;
import company.pluginName.Modules.ProtectionsPckg.Utils.FlagsUtilities;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionUtilities;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
import company.pluginName.Modules.SQLPckg.SQLService;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaPlugin.PandaPluginClass;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Objects.PandaCachedPlayer;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaBooleanField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaDoubleField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaLongField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Utilities.Java.AsyncQueue.FailableRunnable;
import darkpanda73.PandaUtils.Utilities.Java.AsyncQueue.FailableSupplier;
import lombok.Getter;
import royale.RoyaleProtectionBlocks.Plugin.API.Events.Protection.ProtectionCreationAttemptEvent;
import royale.RoyaleProtectionBlocks.Plugin.API.Events.Protection.ProtectionCreationEvent;
import royale.RoyaleProtectionBlocks.Plugin.API.Events.Protection.ProtectionRemovalAttemptEvent;
import royale.RoyaleProtectionBlocks.Plugin.API.Events.Protection.ProtectionRemovalEvent;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.ProtectionBlocks.IProtectionBlock;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.SimpleLocation;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.SimpleLocation.SimpleLocationArea;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.ProtectionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.CachedQuery;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionCreationData;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionInvitation;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionRemovalData;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionTransferData;

@PandaService
public class ProtectionsServiceImpl implements ProtectionsService<Protection> {

	@RegisteredPandaField("config")
	public static final PandaBooleanField SETTINGS_PROTECTION_ALLOWMULTITHREADSEARCHING = new PandaBooleanField(
			"Settings.Protection.Allow-multi-thread-searching", false);

	@RegisteredPandaField("config")
	public static final PandaDoubleField SETTINGS_PROTECTION_RADIUSFORSEARCHCACHE = new PandaDoubleField(
			"Settings.Protection.Radius-for-search-cache", 50D);

	@RegisteredPandaField("config")
	public static final PandaLongField SETTINGS_PROTECTION_SEARCHCACHETTLINMILLIS = new PandaLongField(
			"Settings.Protection.Search-cache-ttl-in-millis", 30000L);

	@PandaInject
	private MainPluginClass plugin;

	@PandaInject
	private SQLService sqlService;

	@PandaInject
	private ProtectionBlocksService protectionBlocksService;

	@PandaInject
	private WorldGuardAPI worldGuardApi;

	@PandaInject
	private CoreProtectAPI coreProtectApi;

	private @Getter HashMap<UUID, List<Protection>> protectionsByOwner = new LinkedHashMap<>();
	private @Getter HashMap<String, List<Protection>> protectionsByWorld = new LinkedHashMap<>();
	private @Getter HashMap<String, Protection> protectionByRegion = new LinkedHashMap<>();
	private List<CachedQueryImpl> cachedQueries = new ArrayList<>();

	private BukkitTask cachedQueriesTask;

	@LoadMethod
	private void load() {
		AtomicInteger regeneratedRegions = new AtomicInteger();
		boolean viewOnly = Settings.SETTINGS_NETWORK_VIEWONLY.isTrue();

		sqlService.getProtections().stream().filter(protection -> {
			if (viewOnly) {
				loadViewOnly(protection);
				return false;
			}
			if (Bukkit.getWorld(protection.getWorldName()) != null) {
				if (protection.getProtectedRegion() == null) {
					try {
						protection.regenerateProtectedRegion();
					} catch (RoyaleProtectionBlocksExceptionImpl e) {
						MessageTemplate
								.inst(PandaPrefixedStringField
										.applyPrefix("&7Removing protection '" + protection.getProtectionId()
												+ "' as it couldn't be found on '" + protection.getWorldName()
												+ "' and were problems trying to regenerate the region."))
								.process().sendMessage(Bukkit.getConsoleSender());

						if (protection.getUtils().isProtectionBlockShown()) {
							protection.getUtils().hideProtectionBlock();
						}

						TasksUtils.executeOnAsync(() -> {
							try {
								sqlService.deleteProtection(protection);
							} catch (RoyaleProtectionBlocksExceptionImpl e1) {
								e1.sendError(Bukkit.getConsoleSender());
							}
						});

						return false;
					}
				}

				if (protection.getProtectedRegion().getMaximumPoint()
						.getX() != protection.getUtils().getProtectionArea().getMaxLocation().getX() - 1
						|| protection.getProtectedRegion().getMaximumPoint()
								.getY() != protection.getUtils().getProtectionArea().getMaxLocation().getY() - 1
						|| protection.getProtectedRegion().getMaximumPoint()
								.getZ() != protection.getUtils().getProtectionArea().getMaxLocation().getZ() - 1
						|| protection.getProtectedRegion().getMinimumPoint().getX() != protection.getUtils()
								.getProtectionArea().getMinLocation().getX()
						|| protection.getProtectedRegion().getMinimumPoint().getY() != protection.getUtils()
								.getProtectionArea().getMinLocation().getY()
						|| protection.getProtectedRegion().getMinimumPoint().getZ() != protection.getUtils()
								.getProtectionArea().getMinLocation().getZ()) {
					try {
						protection.regenerateProtectedRegion();
						regeneratedRegions.incrementAndGet();
					} catch (RoyaleProtectionBlocksExceptionImpl e) {
						e.sendError(Bukkit.getConsoleSender());
					}
				}

				return true;
			}

			return false;
		}).forEach(t -> {
			try {
				load(t);
			} catch (RoyaleProtectionBlocksException e) {
				e.sendError(Bukkit.getConsoleSender());
			}
		});

		sqlService.getProtectionMembers().forEach((id, members) -> {
			Protection protection = findProtectionById(id);
			if (protection != null) {
				protection.getPlayers().setMembers(members);
			}
		});

		if (!viewOnly) {
			PandaPluginClass.getSimpleLogger()
					.sendInfo("Checking for missing members on the database (This process may take a while).");

			this.protectionByRegion.forEach((key, protection) -> {
				protection.getPlayers().getWorldGuardMembers().list().stream()
						.filter(member -> !protection.isMainOwner(member) && !protection.isMember(member))
						.forEach(missingMember -> {
							try {
								protection.addMemberAndSave(missingMember, false);
							} catch (RoyaleProtectionBlocksException e) {
								e.sendError(Bukkit.getConsoleSender());
							}
						});
			});
		}

		sqlService.getProtectionOwners().forEach((id, owners) -> {
			Protection protection = findProtectionById(id);
			if (protection != null) {
				protection.getPlayers().setOwners(owners);
			}
		});

		PandaPluginClass.getSimpleLogger().sendInfo("Checking completed!");

		if (!viewOnly) {
			PandaPluginClass.getSimpleLogger()
					.sendInfo("Checking for missing owners on the database (This process may take a while).");

			this.protectionByRegion.forEach((key, protection) -> {
				protection.getPlayers().getWorldGuardOwners().list().stream()
						.filter(owner -> !protection.isMainOwner(owner) && !protection.isOwner(owner))
						.forEach(missingOwner -> {
							try {
								protection.addOwnerAndSave(missingOwner, false);
							} catch (RoyaleProtectionBlocksException e) {
								e.sendError(Bukkit.getConsoleSender());
							}
						});
			});
			this.protectionByRegion.forEach((key, protection) -> {
				protection.getPlayers().getWorldGuardOwners().list().stream()
						.filter(owner -> !protection.isMainOwner(owner) && !protection.isMember(owner))
						.forEach(missingMember -> {
							try {
								protection.addMemberAndSave(missingMember, false);
							} catch (RoyaleProtectionBlocksException e) {
								e.sendError(Bukkit.getConsoleSender());
							}
						});
			});
		}

		PandaPluginClass.getSimpleLogger().sendInfo("Checking completed!");

		sqlService.getProtectionBanneds().forEach((id, banneds) -> {
			Protection protection = findProtectionById(id);
			if (protection != null) {
				protection.getPlayers().setBanneds(banneds);
			}
		});

		sqlService.getProtectionSettings().forEach((id, settings) -> {
			Protection protection = findProtectionById(id);
			if (protection != null) {
				protection.getSettings().setSettings(settings.getSettings());
			}
		});

		sqlService.getProtectionPermissions().forEach((id, permissions) -> {
			Protection protection = findProtectionById(id);
			if (protection != null) {
				Map<String, ProtectionPermission> map = new HashMap<>();
				permissions.forEach(permission -> map.put(permission.getId(), permission));
				protection.getPermissions().setPermissions(map);
			}
		});

		sqlService.getProtectionInvitations().forEach((id, invitations) -> {
			Protection protection = findProtectionById(id);
			if (protection != null) {
				Map<UUID, ProtectionInvitation> invitationsMap = new HashMap<>();
				invitations.forEach(invitation -> invitationsMap.put(invitation.getPlayerUuid(),
						new ProtectionInvitation(protection, invitation.getPlayerUuid(), invitation.isAddAsOwner(),
								invitation.getCreatedDate())));
				protection.setInvitedPlayers(invitationsMap);
			}
		});

		if (!viewOnly) {
			this.protectionByRegion.values().forEach(prot -> {
				try {
					prot.synchronizeWorldGuardRegion();
				} catch (RoyaleProtectionBlocksExceptionImpl e) {
					if (e.getType() != Exceptions.Protections.REGENONLYONPARENT.getExceptionType()) {
						e.sendError(Bukkit.getConsoleSender());
					}
				}
			});
		}

		plugin.sendInfo(getClass(),
				String.format("Loaded a total amount of '%d' protection(s)", this.protectionByRegion.size()));
		if (regeneratedRegions.get() > 0) {
			plugin.sendWarning(getClass(), String.format(
					"A total of %d protections didn't have a proper WorldGuard region. This regions have been regenerated.",
					regeneratedRegions.get()));
		}

		this.protectionByRegion.values().forEach(Protection::clearCachedProtectedRegion);

		cachedQueriesTask = TasksUtils.executeOnAsyncWithTimer(() -> {
			try {
				executeSynchronized(() -> cachedQueries.removeIf(cachedQuery -> {
					if (System.currentTimeMillis()
							- cachedQuery.getLastRequest() > SETTINGS_PROTECTION_SEARCHCACHETTLINMILLIS.getContent()) {
						cachedQuery.setExpired(true);
						return true;
					}
					return false;
				}));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}, 20L, 20L);
	}

	@UnloadMethod
	private void unload() {
		this.protectionsByOwner.clear();
		this.protectionsByWorld.clear();
		this.protectionByRegion.clear();
		this.cachedQueries.clear();

		cachedQueriesTask.cancel();
	}

	/*
	 * Transfer methods
	 */

	public void transfer(ProtectionTransferData protectionTransferData) throws RoyaleProtectionBlocksException {
		ensureWritable();
		try {
			executeSynchronized(() -> {
				Protection existentProtection = this.protectionByRegion.get(protectionTransferData.getProtectionId());

				if (existentProtection == null) {
					throw Exceptions.Protections.NOTFOUND.generateException();
				}

				List<IProtection> protectionsToTransfer = existentProtection.getChildProtectionsRecursively();
				protectionsToTransfer.add(existentProtection);

				protectionsToTransfer.stream().map(prot -> (Protection) prot).forEach(prot -> {
					prot.clearPlayersAndSave();

					if (!prot.getOwnerUuid().equals(protectionTransferData.getNewOwner())) {
						UUID oldOwner = prot.getOwnerUuid();

						try {
							prot.setOwnerUuidAndSave(protectionTransferData.getNewOwner());
							protectionsByOwner.computeIfAbsent(oldOwner, (uuid) -> new ArrayList<>()).remove(prot);
							protectionsByOwner
									.computeIfAbsent(protectionTransferData.getNewOwner(), (uuid) -> new ArrayList<>())
									.add(prot);

							FlagsUtilities.resetFlags(existentProtection, prot.getOwnerUuid());
							prot.getPermissions();
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
					}
				});
			});
		} catch (RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RoyaleProtectionBlocksException) {
				throw (RoyaleProtectionBlocksException) e.getCause();
			} else {
				throw e;
			}
		} catch (RoyaleProtectionBlocksException e) {
			throw e;
		} catch (Throwable e) {
			throw Exceptions.Protections.UNKNOWN.generateException(e);
		}
	}

	/*
	 * Create/Delete methods
	 */

	@Override
	public Protection create(ProtectionCreationData protectionCreationData) throws RoyaleProtectionBlocksException {
		ensureWritable();
		try {
			Protection protection = new Protection(protectionCreationData.getOwnerUuid(),
					protectionCreationData.getLocation(), protectionCreationData.getProtectionBlock());

			ProtectionCreationAttemptEvent attemptEvent = new ProtectionCreationAttemptEvent(
					protectionCreationData.getExecutor(), protection, protectionCreationData.getCreationCause());
			Bukkit.getPluginManager().callEvent(attemptEvent);

			if (attemptEvent.isCancelled()) {
				throw Exceptions.Protections.Save.CANCELLED.generateException();
			}

			ProtectionUtilities.checkCreationConditions(protection, protectionCreationData);

			try {
				executeSynchronized(() -> {
					load(protection);
				});

				protection.setCreationInProgress(true);

				protection.init();
				if (protection.getOwnerName() != null) {
					protection.setDisplayName(ProtectionDisplayNames.automaticName(protection.getOwnerName(),
							ProtectionDisplayNames.nextAutomaticNumber(findProtectionsByOwner(protection.getOwnerUuid()))),
							false);
				}

				protection.setCreatedDate(System.currentTimeMillis());

				TasksUtils.execute(() -> Bukkit.getPluginManager().callEvent(new ProtectionCreationEvent(
						protectionCreationData.getExecutor(), protection, protectionCreationData.getCreationCause())));

				TasksUtils.executeOnAsync(() -> {
					try {
						sqlService.saveProtection(protection);
					} catch (RoyaleProtectionBlocksException e) {
						e.sendError(Bukkit.getConsoleSender());
					}
				});
			} catch (RoyaleProtectionBlocksException e) {
				unload(protection);

				throw e;
			}

			return protection;
		} catch (RoyaleProtectionBlocksException e) {
			throw e;
		} catch (Throwable e) {
			throw Exceptions.Protections.UNKNOWN.generateException(e);
		}
	}

	@Override
	public void delete(ProtectionRemovalData protectionRemovalData) throws RoyaleProtectionBlocksException {
		ensureWritable();
		try {
			Protection protection = findProtectionById(protectionRemovalData.getProtectionId());

			if (protection == null) {
				throw Exceptions.Protections.Delete.NOTFOUND.generateException();
			}

			if (protection.isCreationInProgress()) {
				throw Exceptions.Protections.Delete.CREATIONINPROGRESS.generateException();
			}

			ProtectionRemovalAttemptEvent attemptEvent = new ProtectionRemovalAttemptEvent(
					protectionRemovalData.getExecutor(), protection, protectionRemovalData.getRemovalCause());
			Bukkit.getPluginManager().callEvent(attemptEvent);

			if (attemptEvent.isCancelled()) {
				throw Exceptions.Protections.Delete.CANCELLED.generateException();
			}

			executeSynchronized(() -> {
				ProtectionUtilities.checkRemovalConditions(protection, protectionRemovalData);

				protection.remove(protectionRemovalData.isRemoveProtectedRegion());

				unload(protection);
			});

			FutureTask<Void> task = new FutureTask<Void>(() -> {
				if (protectionRemovalData.isHideBlock()) {
					if (coreProtectApi.isHooked()) {
						if (!protection.getUtils().isProtectionBlockShown()) {
							protection.getUtils().showProtectionBlock();
						}

						if (protectionRemovalData.getExecutor() instanceof Player) {

							coreProtectApi.getHook().registerProtectionRemoval(
									(Player) protectionRemovalData.getExecutor(),
									protection.getBukkitLocation().getBlock());
						} else {
							Player owner = Bukkit.getPlayer(protection.getOwnerUuid());
							if (owner != null) {
								coreProtectApi.getHook().registerProtectionRemoval(owner,
										protection.getBukkitLocation().getBlock());
							}
						}
					}

					if (protection.getUtils().isProtectionBlockShown()) {
						protection.getUtils().hideProtectionBlock();
					}
				}

				return null;
			});

			if (Bukkit.isPrimaryThread()) {
				task.run();
			} else {
				TasksUtils.execute(task::run);
			}

			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			TasksUtils.execute(() -> {
				Bukkit.getPluginManager().callEvent(new ProtectionRemovalEvent(protectionRemovalData.getExecutor(),
						protection, protectionRemovalData.getRemovalCause()));
			});

			TasksUtils.executeOnAsync(() -> {
				try {
					sqlService.deleteProtection(protection);
				} catch (RoyaleProtectionBlocksException e) {
					e.sendError(Bukkit.getConsoleSender());
				}
			});
		} catch (RoyaleProtectionBlocksException e) {
			throw e;
		} catch (Throwable e) {
			throw Exceptions.Protections.UNKNOWN.generateException(e);
		}
	}

	/*
	 * Load methods
	 */

	private void load(Protection protection) throws RoyaleProtectionBlocksException {
		try {
			executeSynchronized(() -> {
				protectionByRegion.put(protection.getProtectionId().toLowerCase(), protection);
				protectionsByOwner.computeIfAbsent(protection.getOwnerUuid(), (ownerUuid) -> new ArrayList<>())
						.add(protection);
				protectionsByWorld.computeIfAbsent(protection.getWorldName(), (worldName) -> new ArrayList<>())
						.add(protection);

				this.cachedQueries.forEach(cachedQuery -> {
					if (cachedQuery.getSimpleLocationArea().isInside(protection.getProtectionArea())) {
						cachedQuery.getProtections().add(protection);
					}
				});
			});
		} catch (RoyaleProtectionBlocksException e) {
			unload(protection);
			throw e;
		} catch (Throwable e) {
			unload(protection);
			throw Exceptions.Protections.UNKNOWN.generateException(e);
		}
	}

	private void loadViewOnly(Protection protection) {
		protectionByRegion.put(protection.getProtectionId().toLowerCase(), protection);
		protectionsByOwner.computeIfAbsent(protection.getOwnerUuid(), ownerUuid -> new ArrayList<>()).add(protection);
	}

	private void ensureWritable() throws RoyaleProtectionBlocksException {
		if (Settings.SETTINGS_NETWORK_VIEWONLY.isTrue()) {
			throw Exceptions.Protections.PERMISSIONDENIED.generateException();
		}
	}

	/*
	 * Unload methods
	 */

	private void unload(Protection protection) throws RoyaleProtectionBlocksException {
		try {
			executeSynchronized(() -> {
				protectionByRegion.remove(protection.getProtectionId().toLowerCase());
				protectionsByOwner.computeIfAbsent(protection.getOwnerUuid(), (ownerUuid) -> new ArrayList<>())
						.remove(protection);
				protectionsByWorld.computeIfAbsent(protection.getWorldName(), (worldName) -> new ArrayList<>())
						.remove(protection);

				this.cachedQueries.forEach(cachedQuery -> {
					if (cachedQuery.getSimpleLocationArea().isInside(protection.getProtectionArea())) {
						cachedQuery.getProtections().remove(protection);
					}
				});
			});
		} catch (RoyaleProtectionBlocksException e) {
			throw e;
		} catch (Throwable e) {
			throw Exceptions.Protections.UNKNOWN.generateException(e);
		}
	}

	/*
	 * Search methods
	 */

	public Protection findProtectionById(String id) {
		return this.protectionByRegion.get(id.toLowerCase());
	}

	public Protection findProtectionByLocation(Location location) {
		return findProtectionsByLocation(location).findFirst().orElse(null);
	}

	public Stream<Protection> findProtectionsByLocation(Location location) {
		return findProtectionsByLocation(location, true);
	}

	public Stream<Protection> findProtectionsByLocation(Location location, boolean includeBorder) {
		return findProtectionsByArea(location, location, includeBorder);
	}

	public Stream<Protection> findProtectionsByArea(Location location1, Location location2) {
		return findProtectionsByArea(location1, location2, true);
	}

	public Stream<Protection> findProtectionsByArea(Location location1, Location location2, boolean includeBorder) {
		try {
			return executeSynchronizedWithReturn(
					() -> queryAndFilter(SimpleLocationArea.of(location1, location2), includeBorder)
							.filter((prot) -> !prot.isDeleted())
							.sorted((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority())));
		} catch (Throwable e) {
			return Collections.<Protection>emptyList().stream();
		}
	}

	public Protection findProtectionBySourceBlock(Block sourceBlock) {
		SimpleLocation loc = SimpleLocation.of(sourceBlock.getLocation());
		try {
			return executeSynchronizedWithReturn(
					() -> queryAndFilter(loc, true).map(prot -> (Protection) prot.getParentProtection()).distinct()
							.filter(prot -> !prot.isDeleted() && ((Protection) prot).isProtectionBlock(sourceBlock))
							.findFirst().orElse(null));
		} catch (Throwable e) {
			return null;
		}
	}

	public List<Protection> findProtectionsByOwner(UUID owner) {
		return this.protectionsByOwner.getOrDefault(owner, Collections.emptyList());
	}

	public Protection findProtectionBySourceLocation(Location sourceLocation) {
		SimpleLocation loc = SimpleLocation.of(sourceLocation.getBlock().getLocation());
		try {
			return executeSynchronizedWithReturn(() -> queryAndFilter(loc, true)
					.filter(prot -> !prot.isDeleted() && loc.equals(((Protection) prot).getLocation())).findFirst()
					.orElse(null));
		} catch (Throwable e) {
			return null;
		}
	}

	public Protection findProtectionParentByLocation(Location location) {
		return findProtectionParentsByLocation(location).findFirst().orElse(null);
	}

	public Stream<Protection> findProtectionParentsByLocation(Location location) {
		return findProtectionParentsByLocation(location, true);
	}

	public Stream<Protection> findProtectionParentsByLocation(Location location, boolean includeBorder) {
		return findProtectionParentsByArea(location, location, includeBorder);
	}

	public Stream<Protection> findProtectionParentsByArea(Location location1, Location location2) {
		return findProtectionParentsByArea(location1, location2, true);
	}

	public Stream<Protection> findProtectionParentsByArea(Location location1, Location location2,
			boolean includeBorder) {
		return findProtectionParentsByArea(SimpleLocationArea.of(location1, location2), true);
	}

	public Stream<Protection> findProtectionParentsByArea(SimpleLocationArea locationArea) {
		return findProtectionParentsByArea(locationArea, true);
	}

	public Stream<Protection> findProtectionParentsByArea(SimpleLocationArea locationArea, boolean includeBorder) {
		try {
			return executeSynchronizedWithReturn(() -> query(locationArea, includeBorder).getProtections().stream()
					.map(prot -> (Protection) prot.getParentProtection()).distinct()
					.filter((prot) -> !prot.isDeleted() && prot.isInsideAny(locationArea, includeBorder))
					.sorted((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority())));
		} catch (Throwable e) {
			return Collections.<Protection>emptyList().stream();
		}
	}

	public Stream<Protection> findAllowedParentProtectionsByPlayer(OfflinePlayer pl) {
		return findAllowedParentProtectionsByPlayerUuid(pl.getUniqueId());
	}

	public Stream<Protection> findAllowedParentProtectionsByCachedPlayer(PandaCachedPlayer pl) {
		return findAllowedParentProtectionsByPlayerUuid(pl.getUuid());
	}

	@Override
	public Stream<Protection> findAllowedParentProtectionsByPlayerUuid(UUID uuid) {
		try {
			return executeSynchronizedWithReturn(() -> StreamSupport
					.stream(protectionByRegion.values().spliterator(),
							SETTINGS_PROTECTION_ALLOWMULTITHREADSEARCHING.isTrue())
					.map(prot -> (Protection) prot.getParentProtection()).distinct()
					.filter(prot -> !prot.isDeleted() && prot.getParentProtection() == prot
							&& (prot.isMainOwner(uuid) || prot.isOwner(uuid) || prot.isMember(uuid))));
		} catch (Throwable e) {
			return Collections.<Protection>emptyList().stream();
		}
	}

	public Stream<Protection> findProtectionsByProtectionBlock(IProtectionBlock protectionBlock) {
		return findProtectionsByProtectionBlockId(protectionBlock.getId());
	}

	public Stream<Protection> findProtectionsByProtectionBlockId(String protectionBlockId) {
		try {
			return executeSynchronizedWithReturn(() -> StreamSupport
					.stream(protectionByRegion.values().spliterator(),
							SETTINGS_PROTECTION_ALLOWMULTITHREADSEARCHING.isTrue())
					.filter(prot -> prot.getProtectionBlockId().equals(protectionBlockId)));
		} catch (Throwable e) {
			return Collections.<Protection>emptyList().stream();
		}
	}

	public Stream<Protection> findInvitedProtectionsByPlayerUuid(UUID uuid) {
		return this.protectionByRegion.values().stream().filter(Protection::isForSale);
	}

	public Stream<Protection> findProtectionsOnSale() {
		return this.protectionByRegion.values().stream().filter(Protection::isForSale);
	}

	public Stream<Protection> findPublicProtections() {
		return this.protectionByRegion.values().stream().filter(Protection::isPublicAccess);
	}

	public Stream<Protection> findAllProtections() {
		return this.protectionByRegion.values().stream();
	}

	public Stream<ProtectionInvitation> findAllProtectionInvitationsByPlayerUuid(UUID uuid) {
		return this.protectionByRegion.values().stream().filter(prot -> prot.isInvitedPlayer(uuid))
				.map(invitation -> invitation.getInvitedPlayer(uuid));
	}

	public CachedQueryImpl query(SimpleLocation location, boolean includeBorder) {
		return query(SimpleLocationArea.of(location, location), includeBorder);
	}

	public CachedQueryImpl query(SimpleLocationArea locationArea, boolean includeBorder) {
		try {
			return executeSynchronizedWithReturn(() -> {
				Optional<CachedQueryImpl> foundCachedQuery = this.cachedQueries.stream()
						.filter(cachedQuery -> cachedQuery.getSimpleLocationArea().isAllInside(locationArea))
						.findFirst();

				if (foundCachedQuery.isPresent()) {
					foundCachedQuery.get().setLastRequest(System.currentTimeMillis());
				} else {
					double offset = SETTINGS_PROTECTION_RADIUSFORSEARCHCACHE.getContent();
					SimpleLocationArea cacheLocationArea = locationArea.clone();
					cacheLocationArea.getMinLocation().substract(offset, 0, offset).setY(Double.NEGATIVE_INFINITY);
					cacheLocationArea.getMaxLocation().add(offset, 0, offset).setY(Double.POSITIVE_INFINITY);

					CachedQueryImpl cachedQuery = new CachedQueryImpl(cacheLocationArea, StreamSupport
							.stream(protectionsByWorld
									.getOrDefault(locationArea.getWorldName(), Collections.emptyList()).spliterator(),
									SETTINGS_PROTECTION_ALLOWMULTITHREADSEARCHING.isTrue())
							.filter(prot -> prot.getUtils().isInside(cacheLocationArea, true))
							.collect(Collectors.toList()), System.currentTimeMillis());
					foundCachedQuery = Optional.of(cachedQuery);

					this.cachedQueries.removeIf(existentCachedQuery -> cachedQuery.getSimpleLocationArea()
							.isAllInside(existentCachedQuery.getSimpleLocationArea()));
					this.cachedQueries.add(0, cachedQuery);
				}

				return foundCachedQuery.get();
			});
		} catch (Throwable e) {
			return null;
		}
	}

	/*
	 * Private methods
	 */

	private Stream<Protection> queryAndFilter(SimpleLocation location, boolean includeBorder) {
		return queryAndFilter(SimpleLocationArea.of(location, location), includeBorder);
	}

	private Stream<Protection> queryAndFilter(SimpleLocationArea locationArea, boolean includeBorder) {
		return query(locationArea, includeBorder).getProtections().stream()
				.filter(prot -> ((Protection) prot).getUtils().isInside(locationArea, includeBorder));
	}

	public <T> T executeSynchronizedWithReturn(FailableSupplier<T> func) throws Throwable {
		synchronized (this) {
			return func.get();
		}
	}

	public void executeSynchronized(FailableRunnable func) throws Throwable {
		executeSynchronizedWithReturn(() -> {
			func.run();
			return null;
		});
	}

	public static class CachedQueryImpl extends CachedQuery<Protection> {

		public CachedQueryImpl(SimpleLocationArea simpleLocationArea, List<Protection> protections, long lastRequest) {
			super(simpleLocationArea, protections, lastRequest);
		}

	}

}
