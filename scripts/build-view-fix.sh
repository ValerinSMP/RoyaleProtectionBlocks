#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cache=/root/development/projects/survival-custom/vHiddenChests/.gradle-user-home/caches/modules-2/files-2.1
build_dir=build/view-fix
mkdir -p "$build_dir/src" "$build_dir/tests-src" "$build_dir/artifacts"
source=src/main/java/company/pluginName/Modules/ProtectionParticlesPckg/ProtectionParticlesService.java
test_source=src/test/java/company/pluginName/Modules/ProtectionParticlesPckg/ProtectionViewLifecycleTest.java
sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "$source" > "$build_dir/src/ProtectionParticlesService.java"
sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "$test_source" > "$build_dir/tests-src/ProtectionViewLifecycleTest.java"
for node in Custom Sync; do
 base="build/view-bases/$node.jar"
 if [[ $node == Custom ]]; then expected=daeca5c2998bb1f60a08eea67e168ade705589ff9be5d0c26b631b190820b51d; else expected=fe0ae2bdd94c3ec945cbde2a55ca59972c3cacc5323d7f90b5d28d56aa8e235c; fi
 [[ $(sha256sum "$base" | cut -d' ' -f1) == "$expected" ]]
 cp_path="$base"
 while IFS= read -r dependency; do cp_path="$cp_path:$dependency"; done < <(find "$cache" lib -name '*.jar' -type f)
 mkdir -p "$build_dir/$node/classes" "$build_dir/$node/tests"
 timeout 120s javac -J-Xmx512m --release 21 -proc:none -cp "$cp_path" -d "$build_dir/$node/classes" "$build_dir/src/ProtectionParticlesService.java"
 timeout 120s javac -J-Xmx384m --release 21 -proc:none -cp "$build_dir/$node/classes:$cp_path" -d "$build_dir/$node/tests" "$build_dir/tests-src/ProtectionViewLifecycleTest.java"
 timeout 45s java -Xmx256m -cp "$build_dir/$node/tests:$build_dir/$node/classes:$cp_path" royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionParticlesPckg.ProtectionViewLifecycleTest
 output="$build_dir/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-view-fix-$node.jar"
 cp "$base" "$output"
 jar --update --file "$output" -C "$build_dir/$node/classes" .
 python3 scripts/verify-view-artifact.py "$base" "$output"
 sha256sum "$output"
done
