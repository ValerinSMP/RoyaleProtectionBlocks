#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node=${1:-custom}
case "$node" in
 custom) expected=5d457f35dfbbfd74418e836d4d2fd0a6f3cf650844437406aa7341607ac60653 ;;
 sync) expected=e0b50a1e6ff1d0e081ca70d642c2204e49dbf62bedf83915c8a22b7392c9d89d ;;
 *) exit 2 ;;
esac
base="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-block-destruction-$node.jar"
[[ $(sha256sum "$base" | cut -d' ' -f1) == "$expected" ]]
build_dir=$(mktemp -d build/pvp-toggle.XXXXXX)
output="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-pvp-toggle-$node.jar"
java_bin=/usr/lib/jvm/java-21-openjdk-amd64/bin
paper=/home/mtyn/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT/20961a0e95ea65d0af88dffc9b1a90421b4dfeca/paper-api-1.21.11-R0.1-SNAPSHOT.jar
lombok=build/m2/org/projectlombok/lombok/1.18.34/lombok-1.18.34.jar
wg=/var/lib/pterodactyl/volumes/9292cb5d-ec3b-4833-a0c3-52aff6c35045/plugins/worldguard-bukkit-7.0.17.jar
we=/var/lib/pterodactyl/volumes/9292cb5d-ec3b-4833-a0c3-52aff6c35045/plugins/FastAsyncWorldEdit-Bukkit-2.15.4.jar
classpath="$base:$paper:$lombok:$wg:$we"
while IFS= read -r dependency; do classpath="$classpath:$dependency"; done < <(
 find /home/mtyn/.gradle/caches/modules-2/files-2.1/net.kyori \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.guava/guava \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/net.md-5/bungeecord-chat -name '*.jar' -type f
)
mkdir -p "$build_dir/classes" "$build_dir/tests" "$build_dir/src"
sources=()
for name in Objects/Components/Permissions/ProtectionPermissionManager Utils/ProtectionPvpBridge Utils/InteractionPermissionRoute Listeners/ProtectionPermissionListener; do
 source="src/main/java/company/pluginName/Modules/ProtectionsPckg/$name.java"
 target="$build_dir/src/$(basename "$source")"
 sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' \
 -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "$source" > "$target"
 sources+=("$target")
done
timeout --signal=TERM --kill-after=5s 120s "$java_bin/javac" -J-Xmx384m --release 21 \
 -cp "$classpath" -processorpath "$lombok" -d "$build_dir/classes" "${sources[@]}"
timeout --signal=TERM --kill-after=5s 60s "$java_bin/javac" -J-Xmx192m --release 21 -proc:none \
 -cp "$classpath" -d "$build_dir/tests" \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/ProtectionPvpBridge.java \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/InteractionPermissionRoute.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/InteractionPermissionRouteTest.java \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/CrystalPvpGuard.java \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/BlockDestructionGuard.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/ProtectionPvpBridgeTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/CrystalPvpGuardTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/BlockDestructionGuardTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/OwnerRoleTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/VerifyPvpToggleArtifact.java
for test in InteractionPermissionRouteTest ProtectionPvpBridgeTest CrystalPvpGuardTest BlockDestructionGuardTest OwnerRoleTest; do
 timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx128m -cp "$build_dir/tests:$classpath" \
 "company.pluginName.Modules.ProtectionsPckg.Utils.$test"
done
cp "$base" "$output"
"$java_bin/jar" --update --file "$output" -C "$build_dir/classes" .
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$build_dir/tests" \
 company.pluginName.Modules.ProtectionsPckg.Utils.VerifyPvpToggleArtifact "$base" "$output"
sha256sum "$output"
