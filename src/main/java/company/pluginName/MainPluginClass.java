package company.pluginName;

import org.bukkit.Bukkit;

import company.pluginName.Hooks.ProtocolLib.ProtocolLibAPI;
import company.pluginName.Modules.BStatsPckg.Objects.PremiumVersionCustomChart;
import company.pluginName.Modules.BStatsPckg.Objects.ProvidedProtectionsCustomChart;
import darkpanda73.PandaUtils.PandaBStats.Annotations.EnableBStats;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaPlugin.PandaPluginClass;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaListeners;
import darkpanda73.PandaUtils.PandaUpdateChecker.Annotations.EnableUpdateChecker;
import darkpanda73.PandaUtils.Services.PandaBossBarModule.Annotations.EnableBossBarService;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Annotations.EnableCachedPlayersService;
import darkpanda73.PandaUtils.Services.PandaCommandsModule.Annotations.EnableCommandsService;
import darkpanda73.PandaUtils.Services.PandaDiscordWebhook.Annotations.EnableDiscordWebhookService;
import darkpanda73.PandaUtils.Services.PandaEconomiesModule.Annotations.EnableEconomiesService;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.EnableInventoriesService;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.EnablePlayerHeadCacheService;
import darkpanda73.PandaUtils.Services.PandaLoggingModule.Annotations.EnableLoggingService;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Annotations.EnableMessageListenerService;
import darkpanda73.PandaUtils.Services.PandaPlayerDataModule.Listeners.PandaPlayerDataEvents;

@PandaListeners({ PandaPlayerDataEvents.class })
@EnableCommandsService
@EnableMessageListenerService
@EnableInventoriesService
@EnablePlayerHeadCacheService
@EnableUpdateChecker("109118")
@EnableBStats(value = 21346, customCharts = { ProvidedProtectionsCustomChart.class, PremiumVersionCustomChart.class })
@EnableDiscordWebhookService
@EnableLoggingService
@EnableEconomiesService
@EnableCachedPlayersService
@EnableBossBarService
public class MainPluginClass extends PandaPluginClass {

	@RegisteredPandaField("lang")
	public static final PandaStringField PREFIX = new PandaStringField("Prefix",
			"&d&lRoyale&fProtectionBlocks >>&r&7 ");

	@PandaInject
	private static ProtocolLibAPI protocolLibApi;

	@Override
	public String getPrefix() {
		return PREFIX.toString();
	}

	@Override
	public void onAfterEnablePlugin() {
		super.onAfterEnablePlugin();
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		if (!protocolLibApi.isHooked()) {
			MessageTemplate.inst(PandaPrefixedStringField.applyPrefix(
					"&cProtocolLib could not be found. This will affect some features, like the '/pb view' command, which won't show the glowing blocks to indicate where protections are located."))
					.process().sendMessage(Bukkit.getConsoleSender());
		}
	}

	@Override
	public void onBeforeDisablePlugin() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
		super.onBeforeDisablePlugin();
	}

}
