#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
base=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-player-search-dev.jar
[[ $(sha256sum "$base" | cut -d' ' -f1) == 293dd678fb27be218ff72b83cf395f9dfa0b61fb88f3a65eff429c4fe233748e ]]
build_dir=$(mktemp -d build/crystal-pvp.XXXXXX)
output=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-crystal-pvp-dev.jar
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
for name in Listeners/ProtectionPermissionListener Utils/CrystalPvpGuard; do
 source="src/main/java/company/pluginName/Modules/ProtectionsPckg/$name.java"
 target="$build_dir/src/$(basename "$source")"
 sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' \
 -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "$source" > "$target"
 sources+=("$target")
done
timeout --signal=TERM --kill-after=5s 120s "$java_bin/javac" -J-Xmx384m --release 21 -proc:none \
 -cp "$classpath" -d "$build_dir/classes" "${sources[@]}"
timeout --signal=TERM --kill-after=5s 60s "$java_bin/javac" -J-Xmx192m --release 21 -proc:none \
 -cp "$classpath" -d "$build_dir/tests" \
 src/main/java/company/pluginName/Modules/ProtectionsPckg/Utils/CrystalPvpGuard.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/CrystalPvpGuardTest.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/VerifyCrystalArtifact.java \
 src/test/java/company/pluginName/Modules/ProtectionsPckg/Utils/OwnerRoleTest.java
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx128m -cp "$build_dir/tests:$classpath" \
 company.pluginName.Modules.ProtectionsPckg.Utils.CrystalPvpGuardTest
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx128m -cp "$build_dir/tests:$classpath" \
 company.pluginName.Modules.ProtectionsPckg.Utils.OwnerRoleTest
cp "$base" "$output"
"$java_bin/jar" --update --file "$output" -C "$build_dir/classes" .
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$build_dir/tests" \
 company.pluginName.Modules.ProtectionsPckg.Utils.VerifyCrystalArtifact "$base" "$output"
sha256sum "$output"
