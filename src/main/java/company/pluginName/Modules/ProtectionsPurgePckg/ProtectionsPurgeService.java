package company.pluginName.Modules.ProtectionsPurgePckg;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import company.pluginName.MainPluginClass;
import company.pluginName.Modules.DebugPckg.DebugService;
import company.pluginName.Modules.DebugPckg.Objects.Protection.ProtectionPurgeSummaryDebugMessage;
import company.pluginName.Modules.FilePckg.FilesService;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPurgePckg.Objects.PurgeConfiguration;
import company.pluginName.Modules.SQLPckg.SQLService;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaJSON.Objects.PandaJSONArray;
import darkpanda73.PandaUtils.PandaJSON.Objects.PandaJSONObject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.ReloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Exceptions.ReportResult;
import darkpanda73.PandaUtils.PandaPlugin.Exceptions.ReportResult.ReportError;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import darkpanda73.PandaUtils.PandaUtilities.OfflinePlayerUtilities;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import darkpanda73.PandaUtils.Utilities.Java.SleepingLoop;
import darkpanda73.PandaUtils.Utilities.Java.Objects.Pair;
import darkpanda73.PandaUtils.Utilities.Java.Time.TimeUtilities;
import lombok.Getter;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.RemovalCause;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.PlayerInteractionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionRemovalData;

@PandaService(priority = 1000)
public class ProtectionsPurgeService {

	@RegisteredPandaField("config")
	public static final PandaStringField SETTINGS_PROTECTION_AUTOPURGE_PURGEOLDERTHAN = new PandaStringField(
			"Settings.Protection.Auto-purge.Purge-older-than", "");

	@PandaInject
	private MainPluginClass plugin;

	@PandaInject
	private PlayerInteractionsService playerInteractionsService;

	@PandaInject
	private ProtectionsServiceImpl protectionsService;

	@PandaInject
	private FilesService filesService;

	@PandaInject
	private SQLService sqlService;

	@PandaInject
	private DebugService debugService;

	private static final DateFormat FILE_DATE_FORMAT = new SimpleDateFormat("'/purge.'yyyyMMddHHmmss'.json'");
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private @Getter PurgeConfiguration configuredPurgeConfiguration;
	private SleepingLoop sleepingLoop;

	@LoadMethod
	public void load() throws ReportError {
		try {
			if (!SETTINGS_PROTECTION_AUTOPURGE_PURGEOLDERTHAN.getContent().isEmpty()) {
				long olderThan = TimeUtilities
						.stringToSeconds(SETTINGS_PROTECTION_AUTOPURGE_PURGEOLDERTHAN.getContent());

				if (olderThan > 0) {
					plugin.sendInfo(getClass(),
							String.format("Set auto purge to remove regions older than %d seconds", olderThan));
					configuredPurgeConfiguration = new PurgeConfiguration().setMillis(olderThan * 1000);

					enableAutoPurge();
				}
			}
		} catch (NumberFormatException e) {
			throw new ReportResult.ReportError(PandaPrefixedStringField
					.applyPrefix(String.format("Unable to create purge timer: %s", e.getMessage())), false, e);
		}
	}

	@UnloadMethod
	public void unload() {
		disableAutoPurge();
	}

	@ReloadMethod
	private void reload() throws ReportError {
		unload();
		load();
	}

	public void enableAutoPurge() {
		if (this.configuredPurgeConfiguration != null) {

			sleepingLoop = new SleepingLoop(() -> {
				List<Protection> protectionsToPurge = retrieveProtectionsToPurge(configuredPurgeConfiguration);

				if (protectionsToPurge.size() > 0) {
					List<Protection> purgedProtections = purgeProtections(protectionsToPurge, RemovalCause.AUTO_PURGE);

					debugService.sendDebugMessage(
							debugService.getDebugMessage(ProtectionPurgeSummaryDebugMessage.class),
							ProtectionPurgeSummaryDebugMessage.Data.inst(null, this.configuredPurgeConfiguration,
									purgedProtections));
				}
			}, () -> {
				Pair<Protection, Long> protectionFound = this.protectionsService
						.executeSynchronizedWithReturn(() -> this.protectionsService.getProtectionByRegion().values()
								.stream().filter(prot -> !prot.isDeleted())
								.map(prot -> Pair.of(prot, configuredPurgeConfiguration.getRemainingTime(prot)))
								.sorted((prot1, prot2) -> Long.compare(prot1.getSecond(), prot2.getSecond()))
								.findFirst().orElse(null));

				if (protectionFound != null) {
					return protectionFound.getSecond();
				}
				return Long.MAX_VALUE;
			});

			this.sleepingLoop.start();
		}
	}

	public void disableAutoPurge() {
		if (sleepingLoop != null) {
			this.sleepingLoop.interrupt();
			this.sleepingLoop = null;
		}
	}

	public void awakePurgeTask() {
		if (this.sleepingLoop != null) {
			this.sleepingLoop.awake();
		}
	}

	public boolean isRunning() {
		return this.sleepingLoop != null;
	}

	public long calculateExpiresIn(IProtection protection) {
		if (this.configuredPurgeConfiguration != null) {
			return this.configuredPurgeConfiguration.getRemainingTime(protection);
		}
		return Long.MAX_VALUE;
	}

	public List<Protection> retrieveProtectionsToPurge(PurgeConfiguration configuration) {
		return configuration.getRemainingTime(protectionsService.getProtectionByRegion().values())
				.filter(pair -> pair.getSecond() <= 0 && (configuration.getWorldsNames().isEmpty()
						|| configuration.getWorldsNames().contains(pair.getFirst().getWorldName())))
				.map(Pair::getFirst).collect(Collectors.toList());
	}

	public List<Protection> purgeProtections(List<Protection> protectionsToPurge, RemovalCause removalCause) {
		MessageTemplate
				.inst(PandaPrefixedStringField.applyPrefix(String.format(
						"&7[System] Starting purge process with &e%d &7protection(s)...", protectionsToPurge.size())))
				.process().sendMessage(Bukkit.getConsoleSender());

		if (protectionsToPurge.size() != 0) {
			List<Protection> protectionsToPurgeOnTask = new ArrayList<>();
			List<Protection> purgedProtections = new ArrayList<>();

			for (; !protectionsToPurge.isEmpty();) {
				protectionsToPurgeOnTask.add(protectionsToPurge.get(0));
				protectionsToPurge.remove(0);

				if (protectionsToPurgeOnTask.size() >= 10 || protectionsToPurge.isEmpty()) {
					FutureTask<Void> task = new FutureTask<Void>(() -> {
						protectionsToPurgeOnTask.forEach(prot -> {
							try {
								protectionsService
										.delete(ProtectionRemovalData.inst(null, prot.getProtectionId(), removalCause));
								purgedProtections.add(prot);
							} catch (RoyaleProtectionBlocksException e) {
								e.sendError(Bukkit.getConsoleSender());
							}
						});
						protectionsToPurgeOnTask.clear();
						return null;
					});

					TasksUtils.execute(task::run);

					try {
						task.get();

						Thread.sleep(10);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			MessageTemplate.inst(Arrays.asList(MainPluginClass.PREFIX.getContent(),
					PandaPrefixedStringField
							.applyPrefix(String.format("&a[System] Purged protections: %d", purgedProtections.size())),
					PandaPrefixedStringField
							.applyPrefix(String.format("&c[System] Failed protections: %d", protectionsToPurge.size())),
					MainPluginClass.PREFIX.getContent())).process().sendMessage(Bukkit.getConsoleSender());

			return purgedProtections;
		} else {
			MessageTemplate.inst(PandaPrefixedStringField.applyPrefix(String.format(
					"&c[System] Cancelled purge process as no protections were found", protectionsToPurge.size())))
					.process().sendMessage(Bukkit.getConsoleSender());

			return Collections.emptyList();
		}
	}

	public File exportProtections(PurgeConfiguration configuration, List<Protection> protectionsToPurge)
			throws Exception {
		File file = new File(filesService.getPluginFolder().getFile().getPath() + FILE_DATE_FORMAT.format(new Date()));

		MessageTemplate
				.inst(PandaPrefixedStringField.applyPrefix(String
						.format("&7[System] Exporting JSON with &e%d &7protection(s)...", protectionsToPurge.size())))
				.process().sendMessage(Bukkit.getConsoleSender());

		PandaJSONObject json = new PandaJSONObject();

		long currentTime = System.currentTimeMillis();
		long olderThan = currentTime - (configuration.getDays() * PurgeConfiguration.DAYS_IN_MILLIS)
				- (configuration.getHours() * PurgeConfiguration.HOURS_IN_MILLIS)
				- (configuration.getMinutes() * PurgeConfiguration.MINUTES_IN_MILLIS) - configuration.getMillis();
		json.put("beforeDate", DATE_FORMAT.format(new Date(olderThan)));

		json.put("foundProtections", protectionsToPurge.size());
		json.put("totalProtections", protectionsService.getProtectionsByOwner().values().stream()
				.map(list -> list.size()).collect(Collectors.reducing(0, (a, b) -> a + b)));

		PandaJSONObject protectionsObject = new PandaJSONObject();

		HashMap<UUID, List<Protection>> protectionsPerOwner = new HashMap<>();
		protectionsToPurge.forEach(protection -> protectionsPerOwner
				.computeIfAbsent(protection.getOwnerUuid(), (uuid) -> new ArrayList<>()).add(protection));

		protectionsService.getProtectionsByOwner().keySet().forEach(owner -> {
			OfflinePlayer pl = OfflinePlayerUtilities.getOfflinePlayer(owner);
			if (pl != null) {
				List<Protection> protections = protectionsPerOwner.getOrDefault(owner, Collections.emptyList());
				if (!protections.isEmpty() || configuration.isShowIgnoredPlayers()) {
					PandaJSONObject data = new PandaJSONObject();
					data.put("ownerName", pl.getName());
					data.put("ownerLastTime",
							pl.isOnline() ? "Online" : DATE_FORMAT.format(new Date(pl.getLastPlayed())));

					PandaJSONArray protectionsFromOwner = new PandaJSONArray();

					protections.forEach(protection -> protectionsFromOwner.add(protection.getProtectionId()));

					data.put("protectionsTopurge", protectionsFromOwner);

					protectionsObject.put(owner.toString(), data);
				}
			}
		});

		json.put("protections", protectionsObject);

		json.save(file);

		return file;
	}

}
