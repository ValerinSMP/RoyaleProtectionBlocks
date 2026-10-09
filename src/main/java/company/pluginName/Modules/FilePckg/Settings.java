package company.pluginName.Modules.FilePckg;

import java.util.Arrays;

import darkpanda73.PandaUtils.Services.PandaEconomiesModule.Enums.EconomyService;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaFieldContainer;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaBooleanField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaDoubleField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaIntegerField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaLongField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringListField;

@RegisteredPandaFieldContainer("config")
public class Settings {

	public static final PandaBooleanField SETTINGS_NETWORK_VIEWONLY = new PandaBooleanField(
			"Settings.Network.View-only", false);
	public static final PandaStringField SETTINGS_NETWORK_HOMESERVER = new PandaStringField(
			"Settings.Network.Home-server", "survival");
	public static final PandaLongField SETTINGS_NETWORK_PENDINGTELEPORTEXPIRATIONSECONDS = new PandaLongField(
			"Settings.Network.Pending-teleport-expiration-seconds", 120L);

	public static final PandaStringField SETTINGS_GENERAL_DATEFORMAT = new PandaStringField(
			"Settings.General.Date-format", "dd/MM/yyyy HH:mm aa");

	public static final PandaStringListField SETTINGS_BANNEDWORLDS = new PandaStringListField(
			"Settings.Protection.Banned-worlds", Arrays.asList("World1", "World2", "World3"));
	public static final PandaStringListField SETTINGS_COMMANDSONCREATION = new PandaStringListField(
			"Settings.Protection.Commands-on-creation", Arrays.asList());
	public static final PandaStringListField SETTINGS_COMMANDSONREMOVAL = new PandaStringListField(
			"Settings.Protection.Commands-on-removal", Arrays.asList());

	public static final PandaStringField SETTINGS_PROTECTION_STARTERBLOCK = new PandaStringField(
			"Settings.Protection.Starter-block", "Settings.Protection.Stater-block", "");
	public static final PandaIntegerField SETTINGS_PROTECTION_BOUNDARIESVIEWDURATIONINSECONDS = new PandaIntegerField(
			"Settings.Protection.Boundaries-view-duration-in-seconds", 30);
	public static final PandaStringField SETTINGS_PROTECTION_TELEPORTECONOMY = new PandaStringField(
			"Settings.Protection.Teleport-economy", EconomyService.VAULT.name());
	public static final PandaDoubleField SETTINGS_PROTECTION_TELEPORTCOST = new PandaDoubleField(
			"Settings.Protection.Teleport-cost", 0D);
	public static final PandaLongField SETTINGS_PROTECTION_TELEPORTCOOLDOWN = new PandaLongField(
			"Settings.Protection.Teleport-cooldown", 30L);
	public static final PandaBooleanField SETTINGS_PROTECTION_ALLOWMEMBERPROMOTIONS = new PandaBooleanField(
			"Settings.Protection.Allow-member-promotions", true);

	public static final PandaBooleanField SETTINGS_PROTECTION_SETPLAYERPOSITIONASHOMEONCREATION = new PandaBooleanField(
			"Settings.Protection.Set-player-position-as-home-on-creation", true);
	public static final PandaBooleanField SETTINGS_PROTECTION_OPENINVENTORYONINTERACT = new PandaBooleanField(
			"Settings.Protection.Open-inventory-on-interact", true);
	public static final PandaBooleanField SETTINGS_PROTECTION_ALLOWREGIONSINSIDEANOTHERFROMSAMEOWNER = new PandaBooleanField(
			"Settings.Protection.Allow-regions-inside-another-from-same-owner", true);
	public static final PandaBooleanField SETTINGS_PROTECTION_DROPITEMONFULLINVENTORY = new PandaBooleanField(
			"Settings.Protection.Drop-item-on-full-inventory", false);

	public static final PandaBooleanField SETTINGS_PROTECTION_PUBLICLIST_IGNORETELEPORTCOST = new PandaBooleanField(
			"Settings.Protection.Public-list.Ignore-teleport-cost", true);

	public static final PandaBooleanField SETTINGS_STORE_REQUESTCONFIRMATIONONPURCHASETHROUGHGUI = new PandaBooleanField(
			"Settings.Store.Request-confirmation-on-purchase-through-gui",
			"Settings.Protection-block.Request-confirmation-on-purchase-through-gui", false);
	public static final PandaBooleanField SETTINGS_STORE_ALLOWTELEPORT = new PandaBooleanField(
			"Settings.Store.Allow-teleport", true);
	public static final PandaBooleanField SETTINGS_STORE_IGNORETELEPORTCOST = new PandaBooleanField(
			"Settings.Store.Ignore-teleport-cost", true);
	public static final PandaStringField SETTINGS_STORE_PROTECTIONECONOMYSERVICE = new PandaStringField(
			"Settings.Store.Protection-economy-service", "");
	public static final PandaStringField SETTINGS_STORE_PROTECTIONBLOCKECONOMYSERVICE = new PandaStringField(
			"Settings.Store.Protection-block-economy-service", EconomyService.VAULT.name());

	public static final PandaBooleanField SETTINGS_PROTECTION_MERGE_ENABLED = new PandaBooleanField(
			"Settings.Protection.Merge.Enabled", true);

}
