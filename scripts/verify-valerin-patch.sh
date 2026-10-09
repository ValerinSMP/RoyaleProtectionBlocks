#!/usr/bin/env bash
set -euo pipefail

jar_path="${1:-build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-dev.jar}"

javap -classpath "$jar_path" -p -c \
  royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.Listeners.ProtectionsListener \
  royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.Listeners.ProtectionPermissionListener \
  royale.RoyaleProtectionBlocks.PandaUtils.Services.PandaCachedPlayersModule.PandaCachedPlayersService \
  royale.RoyaleProtectionBlocks.PandaUtils.Services.PandaMessageListenerModule.Listeners.PandaMessageListener \
  | rg -q 'isProtectedFromExplosions'

for class_name in \
  royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.ProtectionsServiceImpl \
  royale.RoyaleProtectionBlocks.Plugin.Modules.PlayerInteractionsPckg.PlayerInteractionsServiceImpl \
  royale.RoyaleProtectionBlocks.Plugin.Utils.EventsUtils \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.ProtectionsListInventory; do
  javap -classpath "$jar_path" -p -c "$class_name" | rg -q 'SETTINGS_NETWORK_VIEWONLY'
done

unzip -p "$jar_path" royale/RoyaleProtectionBlocks/Plugin/Resources/Config.yml \
  | rg -q 'View-only: false'
unzip -p "$jar_path" royale/RoyaleProtectionBlocks/Plugin/Resources/Config.yml \
  | rg -q 'Home-server: "survival"'

javap -classpath "$jar_path" -p -c \
  royale.RoyaleProtectionBlocks.Plugin.Modules.PlayerInteractionsPckg.PlayerInteractionsServiceImpl \
  | rg -q 'savePendingProtectionTeleport'
javap -classpath "$jar_path" -p -c \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Events.Listeners.BukkitListener \
  | rg -q 'getPendingProtectionTeleport'

javap -classpath "$jar_path" -p \
  royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.Listeners.ProtectionPermissionListener \
  | rg -q 'onArmorStandManipulate'

javap -classpath "$jar_path" -p \
  royale.RoyaleProtectionBlocks.PandaUtils.Services.PandaCachedPlayersModule.PandaCachedPlayersService \
  | rg -q 'java.util.concurrent.ConcurrentHashMap'

if jar tf "$jar_path" | rg -q \
  'Plugin/(Modules/CommandsPckg/Commands/ProtectionBlocks/(Files/|Admin/(LastLoginSubCommand|VersionCommand)|PrioritySubCommand|PublicSubCommand|PrivateSubCommand|PublicListSubCommand|SellSubCommand|BuySubCommand)|Bukkit/Inventories/(Protections/ProtectionsPublicListInventory|Store/(ProtectionsStoreInventory|StoreInventory)))'; then
  echo "RoyaleProtectionBlocks Valerin patch: legacy protection marketplace still present" >&2
  exit 1
fi

javap -classpath "$jar_path" -p -c \
	royale.RoyaleProtectionBlocks.Plugin.Modules.CommandsPckg.Commands.ProtectionBlocks.StoreSubCommand \
	| rg -q 'ProtectionBlocksStoreInventory'

javap -classpath "$jar_path" -p \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.v2.Protections.ProtectionsManageInventory \
  | rg -q 'executeRulesButton'

javap -classpath "$jar_path" -p -c \
  royale.RoyaleProtectionBlocks.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject \
  | rg -q 'setTitleReplacements'

for class_name in \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.v2.Protections.ProtectionsManageInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Members.ProtectionMembersInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Banneds.ProtectionBannedsInventory; do
  javap -classpath "$jar_path" -p -c "$class_name" | rg -q 'org/bukkit/entity/Player.closeInventory'
done

javap -classpath "$jar_path" -p \
  royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames \
  | rg -q 'nextAutomaticNumber'

javap -classpath "$jar_path" -p -c \
  royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.ProtectionsServiceImpl \
  | rg -q 'nextAutomaticNumber'

javap -classpath "$jar_path" -p \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Events.Listeners.BukkitListener \
  | rg -q 'migrateLegacyProtectionNames'

if jar tf "$jar_path" | rg -q 'ProtectionDisplayNamesTest.class'; then
  echo "RoyaleProtectionBlocks Valerin patch: test class bundled" >&2
  exit 1
fi

unzip -p "$jar_path" \
  royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_protections_permissions_list.yml \
  | rg -q 'Title: "Ajustes de la protección"'

inventory_dump="$(mktemp)"
trap 'rm -f "$inventory_dump"' EXIT
for inventory in $(jar tf "$jar_path" | rg 'Plugin/Resources/Inventories/inventory_.*\.yml$'); do
  unzip -p "$jar_path" "$inventory" >> "$inventory_dump"
done

for inventory in $(jar tf "$jar_path" | rg 'Plugin/Resources/Inventories/inventory_.*\.yml$' | rg -v 'inventory_(confirmation|warning)\.yml$'); do
  layout="$(unzip -p "$jar_path" "$inventory")"
  rg -q 'Size: 54' <<<"$layout"
  rg -q 'Type: "BLACK_STAINED_GLASS_PANE"' <<<"$layout"
  rg -q 'Slots: "4"' <<<"$layout"
  rg -q 'Slots: "49"' <<<"$layout"
done

rg -q 'Type: "BLACK_STAINED_GLASS_PANE"' "$inventory_dump"
rg -q 'Name: "&#FFD166Miembros"' "$inventory_dump"
rg -q 'Title: "Ajustes de la protección"' "$inventory_dump"
rg -q 'Size: 54' "$inventory_dump"
rg -q 'Slots: "9-44"' "$inventory_dump"
rg -q 'Name: "&#FF3300✘ Cerrar"' "$inventory_dump"
rg -q 'Slots: "0-3;5-8;45-48;50-51"' "$inventory_dump"

for inventory in protections_manage_v2 protections_members protections_permissions_list protections_flags protections_settings_list; do
  navigation="$(unzip -p "$jar_path" "royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_${inventory}.yml")"
  rg -q 'Back-button:' <<<"$navigation"
  rg -q 'Type: "SPECTRAL_ARROW"' <<<"$navigation"
  rg -q 'Slots: "45"' <<<"$navigation"
done

for inventory in protections_banneds protections_merge protections_permissions_manage protections_settings_greeting_farewell_messages protections_split protections_invitations protectionblocks_allowedworlds protectionblocks_store admin_protections_list_filters admin_protections_list_filters_select_sort searchplayer searchworld searchprotection searchprotectionblock; do
  navigation="$(unzip -p "$jar_path" "royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_${inventory}.yml")"
  rg -q 'Back-button:' <<<"$navigation"
  rg -q 'Type: "SPECTRAL_ARROW"' <<<"$navigation"
  rg -q 'Slots: "45"' <<<"$navigation"
done

for inventory in admin_protections_list protectionblocks_allowedworlds protectionblocks_list protectionblocks_store protections_banneds protections_invitations protections_list protections_members protections_merge protections_permissions_list protections_settings_list protections_split searchplayer searchprotection searchprotectionblock searchworld protections_flags; do
  navigation="$(unzip -p "$jar_path" "royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_${inventory}.yml")"
  rg -q 'Slots: "9-44"' <<<"$navigation"
done

for class_name in \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.v2.Protections.ProtectionsManageInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Members.ProtectionMembersInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Permissions.ProtectionsPermissionsListInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Flags.ProtectionFlagsInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Settings.ProtectionsSettingsListInventory; do
  javap -classpath "$jar_path" -p "$class_name" | rg -q 'executeBackButton'
done

for class_name in \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Admin.AdminProtectionsListFiltersInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Admin.AdminProtectionsListFiltersSelectSortInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.ProtectionBlocks.BannedWorlds.ProtectionBlockAllowedWorldsInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Banneds.ProtectionBannedsInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Invitations.ProtectionInvitationsInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Merge.ProtectionMergeInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Permissions.ProtectionsPermissionsManageInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Settings.ProtectionSettingsGreetingFarewellMessagesInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Protections.Split.ProtectionSplitInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Shared.SearchPlayerInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Shared.SearchProtectionBlockInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Shared.SearchProtectionInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Shared.SearchWorldsInventory \
  royale.RoyaleProtectionBlocks.Plugin.Bukkit.Inventories.Store.ProtectionBlocksStoreInventory; do
  javap -classpath "$jar_path" -p "$class_name" | rg -q 'generateBackButton'
done

for inventory in protections_flags protections_settings_list; do
  navigation="$(unzip -p "$jar_path" "royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_${inventory}.yml")"
  rg -q 'Slots: "51"' <<<"$navigation"
  rg -q 'Slots: "53"' <<<"$navigation"
done

central_settings="$(unzip -p "$jar_path" royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_protections_permissions_list.yml)"
rg -q 'Slots: "48"' <<<"$central_settings"
rg -q 'Slots: "50"' <<<"$central_settings"
rg -q 'Slots: "53"' <<<"$central_settings"
rg -q 'Mensajes de entrada y salida' <<<"$central_settings"
if rg -q '(Permissions|Environment|Settings)-tab:' <<<"$central_settings"; then
  echo "RoyaleProtectionBlocks Valerin patch: settings categories are still split into tabs" >&2
  exit 1
fi

for inventory in searchplayer searchworld; do
  navigation="$(unzip -p "$jar_path" "royale/RoyaleProtectionBlocks/Plugin/Resources/Inventories/inventory_${inventory}.yml")"
  rg -q 'Type: "SPECTRAL_ARROW"' <<<"$navigation"
  rg -q 'Slots: "45"' <<<"$navigation"
  rg -q 'Slots: "48"' <<<"$navigation"
done

if rg -q 'Name: ".*\b(Previous|Next|Close|Back|Current|Search|Protection|Player|World|Member|Owner|Allowed|Recipe)\b|Title: ".*\b(Protection|Player|Select|Settings|Filters|Recipe)\b|§' "$inventory_dump"; then
  echo "RoyaleProtectionBlocks Valerin patch: old inventory theme still present" >&2
  exit 1
fi

lang_dump="$(unzip -p "$jar_path" royale/RoyaleProtectionBlocks/Plugin/Resources/Lang.yml)"
rg -q '^Prefix: "&8\[&#FF3300ᴘʀᴏᴛᴇᴄᴄɪᴏɴᴇꜱ&8\] &7"' <<<"$lang_dump"

echo "RoyaleProtectionBlocks Valerin patch: OK"
