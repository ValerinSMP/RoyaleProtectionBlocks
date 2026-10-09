#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node=${1:-custom}
case "$node" in
 custom) expected=de32bcea7ce0771f6042735cb2c39863f30ba3a49d6bb73bad4a3cd140ae0b7c ;;
 sync) expected=2c658d29b91f53066551f67982bb42f200b4f08c245dcd292832a0a137c8d536 ;;
 *) exit 2 ;;
esac
base="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-squaremap-$node.jar"
[[ $(sha256sum "$base" | cut -d' ' -f1) == "$expected" ]]
build_dir=$(mktemp -d build/configurable-price-lore.XXXXXX)
output="build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-configurable-price-lore-$node.jar"
java_bin=/usr/lib/jvm/java-21-openjdk-amd64/bin
paper=/home/mtyn/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT/20961a0e95ea65d0af88dffc9b1a90421b4dfeca/paper-api-1.21.11-R0.1-SNAPSHOT.jar
lombok=build/m2/org/projectlombok/lombok/1.18.34/lombok-1.18.34.jar
classpath="$base:$paper:$lombok"
while IFS= read -r dependency; do classpath="$classpath:$dependency"; done < <(
 find /home/mtyn/.gradle/caches/modules-2/files-2.1/net.kyori /home/mtyn/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.guava/guava -name '*.jar' -type f
)
mkdir -p "$build_dir/classes" "$build_dir/tests" "$build_dir/src"
for name in ProtectionBlocksService PriceLore; do
 sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' "src/main/java/company/pluginName/Modules/ProtectionBlocksPckg/$name.java" > "$build_dir/src/$name.java"
done
timeout --signal=TERM --kill-after=5s 120s "$java_bin/javac" -J-Xmx384m --release 21 -cp "$classpath" -processorpath "$lombok" -d "$build_dir/classes" "$build_dir/src/ProtectionBlocksService.java" "$build_dir/src/PriceLore.java"
timeout --signal=TERM --kill-after=5s 45s "$java_bin/javac" -J-Xmx128m --release 21 -d "$build_dir/tests" src/main/java/company/pluginName/Modules/ProtectionBlocksPckg/PriceLore.java src/test/java/company/pluginName/Modules/ProtectionBlocksPckg/PriceLoreTest.java src/test/java/company/pluginName/Modules/ProtectionBlocksPckg/PriceLoreWiringTest.java
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$build_dir/tests" company.pluginName.Modules.ProtectionBlocksPckg.PriceLoreTest
timeout --signal=TERM --kill-after=5s 30s "$java_bin/java" -Xmx96m -cp "$build_dir/tests:$build_dir/classes:$classpath" company.pluginName.Modules.ProtectionBlocksPckg.PriceLoreWiringTest
cp "$base" "$output"
"$java_bin/jar" --update --file "$output" -C "$build_dir/classes" .
python3 - "$base" "$output" <<'PY'
import sys,zipfile
prefix='royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionBlocksPckg/'
with zipfile.ZipFile(sys.argv[1]) as old,zipfile.ZipFile(sys.argv[2]) as new:
    assert set(old.namelist()) <= set(new.namelist())
    changed={p for p in old.namelist() if old.read(p)!=new.read(p)}
    added={p for p in set(new.namelist())-set(old.namelist()) if not p.endswith('/')}
    assert changed=={prefix+'ProtectionBlocksService.class'},changed
    assert added=={prefix+'PriceLore.class'},added
    assert prefix+'ApprovedPriceLore.class' not in new.namelist()
    print('PASS: all other JAR entries preserved')
PY
sha256sum "$output"
