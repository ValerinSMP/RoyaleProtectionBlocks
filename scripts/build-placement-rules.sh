#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node=${1:-custom}
case "$node" in
 custom) expected=33b249a0aaf37dc00822a44f33ce6aa0b1a9fb7463b0765d5e21c8e79e6673c9 ;;
 sync) expected=18573bdaa46b8e7cb484df33758315e2d7d1375eb62e56e1ead130bf4ecf3b8c ;;
 test) expected=5d457f35dfbbfd74418e836d4d2fd0a6f3cf650844437406aa7341607ac60653 ;;
 *) exit 2 ;;
esac
base="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-configurable-price-lore-$node.jar"
if [[ "$node" == test ]]; then base="../../staging/end-protection-20261006/royale-test-baseline.jar"; fi
[[ $(sha256sum "$base" | cut -d' ' -f1) == "$expected" ]]
build_dir=$(mktemp -d build/placement-rules.XXXXXX)
output="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-placement-rules-$node.jar"
java_bin=/usr/lib/jvm/java-21-openjdk-amd64/bin
paper=/home/mtyn/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT/20961a0e95ea65d0af88dffc9b1a90421b4dfeca/paper-api-1.21.11-R0.1-SNAPSHOT.jar
classpath="$base:$paper:/home/mtyn/.gradle/caches/modules-2/files-2.1/net.md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/a87a9222a1dcfa429b4a06264899f65313a4ed5c/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"
while IFS= read -r dependency; do classpath="$classpath:$dependency"; done < <(
 find /home/mtyn/.gradle/caches/modules-2/files-2.1/net.kyori /home/mtyn/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.guava/guava /home/mtyn/.gradle/caches/modules-2/files-2.1/org.yaml -name '*.jar' -type f
)
mkdir -p "$build_dir/classes" "$build_dir/tests" "$build_dir/src"
for name in PlacementRulesService PlacementGeometry; do
 sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "src/main/java/company/pluginName/Modules/ProtectionsPckg/$name.java" > "$build_dir/src/$name.java"
done
timeout --signal=TERM --kill-after=5s 120s "$java_bin/javac" -J-Xmx384m --release 21 -proc:none -cp "$classpath" -d "$build_dir/classes" "$build_dir/src/PlacementRulesService.java" "$build_dir/src/PlacementGeometry.java"
timeout --signal=TERM --kill-after=5s 45s "$java_bin/javac" -J-Xmx128m --release 21 -d "$build_dir/tests" src/main/java/company/pluginName/Modules/ProtectionsPckg/PlacementGeometry.java src/test/java/company/pluginName/Modules/ProtectionsPckg/PlacementGeometryTest.java
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$build_dir/tests" company.pluginName.Modules.ProtectionsPckg.PlacementGeometryTest
cp "$base" "$output"
"$java_bin/jar" --update --file "$output" -C "$build_dir/classes" .
python3 - "$base" "$output" <<'PY'
import sys, zipfile
prefix='royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionsPckg/Placement'
with zipfile.ZipFile(sys.argv[1]) as old, zipfile.ZipFile(sys.argv[2]) as new:
    assert set(old.namelist()) <= set(new.namelist())
    assert all(old.read(p)==new.read(p) for p in old.namelist())
    added={p for p in set(new.namelist())-set(old.namelist()) if not p.endswith('/')}
    assert added and all(p.startswith(prefix) and p.endswith('.class') for p in added), added
    print('PASS: all existing JAR entries preserved; placement classes added only')
PY
sha256sum "$output"
