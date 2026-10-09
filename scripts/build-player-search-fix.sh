#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build_dir=build/player-search-fix
base_jar=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-network-dev.jar
output_jar=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-player-search-dev.jar
test_dir="$build_dir/tests"
mkdir -p "$build_dir/src" "$build_dir/classes" "$test_dir"
java_bin=/usr/lib/jvm/java-21-openjdk-amd64/bin
paper_jar=/home/mtyn/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT/20961a0e95ea65d0af88dffc9b1a90421b4dfeca/paper-api-1.21.11-R0.1-SNAPSHOT.jar
lombok_jar=build/m2/org/projectlombok/lombok/1.18.34/lombok-1.18.34.jar
signgui_jar=build/m2/de/rapha149/signgui/signgui/2.5.4/signgui-2.5.4.jar
classpath="$base_jar:$paper_jar:$signgui_jar:$lombok_jar"
while IFS= read -r dependency; do classpath="$classpath:$dependency"; done < <(
    find /home/mtyn/.gradle/caches/modules-2/files-2.1/net.kyori \
         /home/mtyn/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations \
         /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.guava/guava \
         -name '*.jar' -type f
)
sources=(
    src/main/java/company/pluginName/Modules/PlayersDataPckg/PlayerNameLookupService.java
    utils/PandaCachedPlayersModule/src/main/java/darkpanda73/PandaUtils/Services/PandaCachedPlayersModule/Utils/AsyncNameLookup.java
    utils/PandaCachedPlayersModule/src/main/java/darkpanda73/PandaUtils/Services/PandaCachedPlayersModule/Utils/OfflinePlayerUtilities.java
    utils/PandaUtilities/src/main/java/darkpanda73/PandaUtils/PandaUtilities/OfflinePlayerUtilities.java
    src/main/java/company/pluginName/Bukkit/Inventories/Shared/SearchResultGuard.java
    src/main/java/company/pluginName/Bukkit/Inventories/Protections/Members/ProtectionMembersInventory.java
    src/main/java/company/pluginName/Bukkit/Inventories/Protections/Banneds/ProtectionBannedsInventory.java
)
generated=()
for source in "${sources[@]}"; do
    target="$build_dir/src/$source"
    mkdir -p "$(dirname "$target")"
    sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' \
        -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' \
        "$source" > "$target"
    generated+=("$target")
done
timeout --signal=TERM --kill-after=5s 120s "$java_bin/javac" -J-Xmx384m --release 21 \
    -cp "$classpath" -processorpath "$lombok_jar" -d "$build_dir/classes" "${generated[@]}"
timeout --signal=TERM --kill-after=5s 45s "$java_bin/javac" -J-Xmx128m --release 21 -d "$test_dir" \
    utils/PandaCachedPlayersModule/src/main/java/darkpanda73/PandaUtils/Services/PandaCachedPlayersModule/Utils/AsyncNameLookup.java \
    src/main/java/company/pluginName/Bukkit/Inventories/Shared/SearchResultGuard.java \
    src/test/java/company/pluginName/Bukkit/Inventories/Shared/PlayerSearchRegressionTest.java \
    src/test/java/company/pluginName/Bukkit/Inventories/Shared/VerifyPlayerSearchArtifact.java
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$test_dir" \
    company.pluginName.Bukkit.Inventories.Shared.PlayerSearchRegressionTest
cp "$base_jar" "$output_jar"
"$java_bin/jar" --update --file "$output_jar" -C "$build_dir/classes" .
# The old whole-plugin validator targets a newer, uninstalled resource schema;
# validate this narrow patch against the exact installed network artifact instead.
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$test_dir" \
    company.pluginName.Bukkit.Inventories.Shared.VerifyPlayerSearchArtifact "$base_jar" "$output_jar"
for utility in \
    royale.RoyaleProtectionBlocks.PandaUtils.Services.PandaCachedPlayersModule.Utils.OfflinePlayerUtilities \
    royale.RoyaleProtectionBlocks.PandaUtils.PandaUtilities.OfflinePlayerUtilities; do
    bytecode=$("$java_bin/javap" -cp "$output_jar" -p -c "$utility")
    if [[ "$bytecode" == *getOfflinePlayers* ]]; then echo 'Forbidden playerdata scan' >&2; exit 1; fi
    [[ "$bytecode" == *getOfflinePlayerIfCached* ]]
done
sha256sum "$output_jar"
