#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node=${1:-custom}
case "$node" in
 custom) base=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-crystal-pvp-dev.jar
         expected=03e936c801bd38bc4c5e97c7f6f76e61834882340ab809a3ef7a750491d8f2e6 ;;
 sync) base=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-crystal-pvp-sync.jar
       expected=5a0c50f7c3fcf908177be53346d41adf564a6608ad89f0933a65f235224a13a0 ;;
 *) exit 2 ;;
esac
[[ $(sha256sum "$base" | cut -d' ' -f1) == "$expected" ]]
build_dir=$(mktemp -d build/block-destruction.XXXXXX)
output="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-block-destruction-$node.jar"
java_bin=/usr/lib/jvm/java-21-openjdk-amd64/bin
paper=/home/mtyn/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT/20961a0e95ea65d0af88dffc9b1a90421b4dfeca/paper-api-1.21.11-R0.1-SNAPSHOT.jar
classpath="$base:$paper"
while IFS= read -r dependency; do classpath="$classpath:$dependency"; done < <(
 find /home/mtyn/.gradle/caches/modules-2/files-2.1/net.kyori \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.guava/guava \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/net.md-5/bungeecord-chat \
 /home/mtyn/.gradle/caches/modules-2/files-2.1/org.joml/joml -name '*.jar' -type f
)
mkdir -p "$build_dir/classes" "$build_dir/tests" "$build_dir/src"
sources=()
for name in Listeners/ProtectionsListener Utils/BlockDestructionGuard; do
 source="src/main/java/company/pluginName/Modules/ProtectionsPckg/$name.java"
 target="$build_dir/src/$(basename "$source")"
 sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' \
 -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "$source" > "$target"
 sources+=("$target")
done
timeout --signal=TERM --kill-after=5s 120s "$java_bin/javac" -J-Xmx384m --release 21 -proc:none \
 -cp "$classpath" -d "$build_dir/classes" "${sources[@]}"
timeout --signal=TERM --kill-after=5s 60s "$java_bin/javac" -J-Xmx192m --release 21 -proc:none \
 -cp "$build_dir/classes:$classpath" -d "$build_dir/tests" \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/BlockDestructionGuard.java \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/CrystalPvpGuard.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/BlockDestructionGuardTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/BlockDestructionListenerTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/CrystalPvpGuardTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/OwnerRoleTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/VerifyBlockDestructionArtifact.java
for test in BlockDestructionGuardTest BlockDestructionListenerTest CrystalPvpGuardTest OwnerRoleTest; do
 timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx128m -cp "$build_dir/tests:$build_dir/classes:$classpath" \
 "company.pluginName.Modules.ProtectionsPckg.Utils.$test"
done
cp "$base" "$output"
"$java_bin/jar" --update --file "$output" -C "$build_dir/classes" .
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$build_dir/tests" \
 company.pluginName.Modules.ProtectionsPckg.Utils.VerifyBlockDestructionArtifact "$base" "$output"
sha256sum "$output"
