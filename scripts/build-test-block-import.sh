#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
base=../../staging/end-protection-20261006/royale-test-circle.jar
[[ $(sha256sum "$base" | cut -d' ' -f1) == 459880fb078942cbb03d9ec46296c055446e987ff786c61bcd39a17e6f98730e ]]
build_dir=$(mktemp -d build/test-block-import.XXXXXX)
mkdir -p "$build_dir/classes" "$build_dir/src"
paper=/home/mtyn/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT/20961a0e95ea65d0af88dffc9b1a90421b4dfeca/paper-api-1.21.11-R0.1-SNAPSHOT.jar
classpath="$base:$paper"
while IFS= read -r dependency; do classpath="$classpath:$dependency"; done < <(find /home/mtyn/.gradle/caches/modules-2/files-2.1/net.kyori /home/mtyn/.gradle/caches/modules-2/files-2.1/org.yaml /home/mtyn/.gradle/caches/modules-2/files-2.1/com.google.guava/guava /home/mtyn/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations -name '*.jar' -type f)
sed -e 's/company\.pluginName/royale.RoyaleProtectionBlocks.Plugin/g' -e 's/darkpanda73\.PandaUtils/royale.RoyaleProtectionBlocks.PandaUtils/g' src/main/java/company/pluginName/Modules/ProtectionBlocksPckg/TestBlockImportService.java > "$build_dir/src/TestBlockImportService.java"
timeout --signal=TERM --kill-after=5s 120s /usr/lib/jvm/java-21-openjdk-amd64/bin/javac -J-Xmx384m --release 21 -proc:none -cp "$classpath" -d "$build_dir/classes" "$build_dir/src/TestBlockImportService.java"
output=build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-test-block-import.jar
cp "$base" "$output"
/usr/lib/jvm/java-21-openjdk-amd64/bin/jar --update --file "$output" -C "$build_dir/classes" .
python3 - "$base" "$output" <<'PY'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1]) as old,zipfile.ZipFile(sys.argv[2]) as new:
    assert set(old.namelist()) <= set(new.namelist())
    importer='royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionBlocksPckg/TestBlockImportService.class'
    assert all(old.read(p)==new.read(p) for p in old.namelist() if p!=importer)
    added={p for p in set(new.namelist())-set(old.namelist()) if not p.endswith('/')}
    assert not added,added
    print('PASS: all TEST JAR entries preserved; test-only fixture importer added')
PY
sha256sum "$output"
