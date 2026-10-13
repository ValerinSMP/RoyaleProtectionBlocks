#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work=build/presentation-fix
mkdir -p "$work/src" "$work/tests-src" "$work/artifacts"
sources=(
 src/main/java/company/pluginName/Modules/PlayerInteractionsPckg/ReadOnlyFeedback.java
 utils/PandaColors/src/main/java/darkpanda73/PandaUtils/PandaColors/Messages/Objects/MessageTemplate.java
 utils/PandaColors/src/main/java/darkpanda73/PandaUtils/PandaColors/Messages/NMS/Modern/ModernMessageBuilder.java
 utils/PandaInventories/src/main/java/darkpanda73/PandaUtils/Services/PandaInventoriesModule/v1/Objects/ChestInventory/Paged/PagedChestInventoryObject.java
)
compiled_sources=()
for source in "${sources[@]}"; do
 sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "$source" > "$work/src/$(basename "$source")"
 compiled_sources+=("$work/src/$(basename "$source")")
done
sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' src/test/java/company/pluginName/Tests/PresentationRegressionTest.java > "$work/tests-src/PresentationRegressionTest.java"
sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' src/test/java/company/pluginName/Modules/ProtectionParticlesPckg/ProtectionViewLifecycleTest.java > "$work/tests-src/ProtectionViewLifecycleTest.java"
lombok=/root/.gradle/caches/modules-2/files-2.1/org.projectlombok/lombok/1.18.48/6858f13541bab505384f07053c5a7b539bbfd3e3/lombok-1.18.48.jar
asm=$(find /root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.8 -name '*.jar' -type f | head -1)
mkdir -p "$work/tools"
timeout 60s javac -J-Xmx192m --release 21 -proc:none -cp "$asm" -d "$work/tools" scripts/ReadOnlyFieldPatcher.java
for node in Custom Sync; do
 base="build/text-bases/$node.jar"
 if [[ $node == Custom ]]; then expected=8620d74631bd081c65785fbab50d748cca9c72963b93b4503d1c3ef7f22b1313; else expected=e780716bf18ad20d6a4bf9cbf34becc411ffc6d349198d16d5c11581deaed47e; fi
 [[ $(sha256sum "$base" | cut -d' ' -f1) == "$expected" ]]
 cp_path="$base"
 while IFS= read -r dependency; do cp_path="$cp_path:$dependency"; done < <(find /root/development/projects/survival-custom/vHiddenChests/.gradle-user-home/caches/modules-2/files-2.1 lib -name '*.jar' -type f)
 mkdir -p "$work/$node/classes" "$work/$node/tests"
 timeout 150s javac -J-Xmx768m --release 21 -cp "$cp_path:$lombok" -processorpath "$lombok" -processor lombok.launch.AnnotationProcessorHider\$AnnotationProcessor -d "$work/$node/classes" "${compiled_sources[@]}"
 timeout 30s java -Xmx128m -cp "$work/tools:$asm" ReadOnlyFieldPatcher "$base" "$work/$node/classes"
 output="$work/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-presentation-$node.jar"
 cp "$base" "$output"
 class_paths=(
 royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/ReadOnlyFeedback.class
 royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/PlayerInteractionsServiceImpl.class
 royale/RoyaleProtectionBlocks/PandaUtils/PandaColors/Messages/Objects/MessageTemplate.class
 royale/RoyaleProtectionBlocks/PandaUtils/PandaColors/Messages/NMS/Modern/ModernMessageBuilder.class
 royale/RoyaleProtectionBlocks/PandaUtils/Services/PandaInventoriesModule/v1/Objects/ChestInventory/Paged/PagedChestInventoryObject.class
 )
 for class_path in "${class_paths[@]}"; do jar --update --file "$output" -C "$work/$node/classes" "$class_path"; done
 python3 scripts/verify-presentation-artifact.py "$base" "$output"
 python3 scripts/verify_readonly_preservation.py "$base" "$output"
 timeout 90s javac -J-Xmx384m --release 21 -proc:none -cp "$output:$cp_path" -d "$work/$node/tests" "$work/tests-src/PresentationRegressionTest.java" "$work/tests-src/ProtectionViewLifecycleTest.java"
 for version in 1.21.11 26.2 26.3; do
  timeout 35s java -Xverify:all -Xmx256m -cp "$work/$node/tests:$output:$cp_path" royale.RoyaleProtectionBlocks.Plugin.Tests.PresentationRegressionTest "$version"
 done
 timeout 35s java -Xverify:all -Xmx256m -cp "$work/$node/tests:$output:$cp_path" royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionParticlesPckg.ProtectionViewLifecycleTest
 sha256sum "$output"
done
