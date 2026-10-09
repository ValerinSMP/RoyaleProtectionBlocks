#!/usr/bin/env python3
"""Narrow additive build. Never edits production or compiles unrelated dirty sources."""
import hashlib
import pathlib
import subprocess
import tempfile
import zipfile
import sys

root = pathlib.Path(__file__).resolve().parent.parent
node = sys.argv[1] if len(sys.argv) > 1 else 'custom'
expected = {'custom': '1add33e48abe1846f60ba52877a2bf4c1bd659dece71ffcdc864b4bf9be857ee',
            'sync': '86d29445e977f8763d5c90263132768e52f85eec809862fd462c231769fe984e'}
base = root / ('build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-pvp-toggle-' + node + '.jar')
assert hashlib.sha256(base.read_bytes()).hexdigest() == expected[node]
java = '/usr/lib/jvm/java-21-openjdk-amd64/bin/'
cache = pathlib.Path('/home/mtyn/.gradle/caches/modules-2/files-2.1')
paper = next((cache / 'io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT').rglob('*.jar'))
square = pathlib.Path('/var/lib/pterodactyl/volumes/9292cb5d-ec3b-4833-a0c3-52aff6c35045/plugins/squaremap-paper-mc1.21.11-1.3.12.jar')
dependencies = [base, paper]
for group in ['net.kyori', 'org.jetbrains/annotations', 'com.google.guava/guava', 'com.google.code.gson/gson', 'net.md-5/bungeecord-chat']:
    dependencies.extend((cache / group).rglob('*.jar'))
cp = ':'.join(map(str, dependencies))
def run(*args):
    result = subprocess.run(['timeout', '--signal=TERM', '--kill-after=5s', '60s', *map(str,args)])
    if result.returncode: raise SystemExit('Build/test failed: ' + str(result.returncode))
with tempfile.TemporaryDirectory(prefix='squaremap-', dir=root / 'build') as temp:
    work = pathlib.Path(temp)
    classes = work / 'classes'; classes.mkdir()
    tests = work / 'tests'; tests.mkdir()
    sources = []
    for source in (root / 'src/main/java/company/pluginName/Hooks/Squaremap').glob('*.java'):
        target = work / source.name
        target.write_text(source.read_text().replace('company.pluginName', 'royale.RoyaleProtectionBlocks.Plugin')
                          .replace('darkpanda73.PandaUtils','royale.RoyaleProtectionBlocks.PandaUtils'))
        sources.append(target)
    run(java+'javac', '-J-Xmx256m', '--release','21','-proc:none','-cp',cp+':'+str(square),'-d',classes,*sources)
    run(java+'javac','-J-Xmx128m','--release','21','-proc:none','-d',tests,
        root/'src/main/java/company/pluginName/Hooks/Squaremap/ClaimOutline.java',
        root/'src/test/java/company/pluginName/Hooks/Squaremap/ClaimOutlineTest.java')
    run(java+'java','-Xmx96m','-cp',tests,'company.pluginName.Hooks.Squaremap.ClaimOutlineTest')
    test_sources = root/'src/main/java/company/pluginName/Hooks/Squaremap'
    run(java+'javac','-J-Xmx128m','--release','21','-proc:none','-cp',str(tests)+':'+cp+':'+str(square),'-d',tests,
        test_sources/'OutlineRenderer.java',test_sources/'SquaremapRenderer.java',
        root/'src/test/java/company/pluginName/Hooks/Squaremap/SquaremapRendererTest.java')
    run(java+'java','-Xmx96m','-cp',str(tests)+':'+cp+':'+str(square),
        'company.pluginName.Hooks.Squaremap.SquaremapRendererTest')
    # Class discovery/injection metadata must be safe on Sync, where squaremap is absent.
    probe = work/'AbsentApiProbe.java'
    probe.write_text('public class AbsentApiProbe { public static void main(String[] a) throws Exception {'
                     'try { Class.forName("xyz.jpenilla.squaremap.api.Squaremap"); throw new AssertionError(); }'
                     'catch (ClassNotFoundException expected) {}'
                     'for (String n:a) { Class<?> c=Class.forName(n); c.getDeclaredFields(); c.getDeclaredMethods();'
                     'c.getDeclaredConstructors(); c.getAnnotations(); } System.out.println("Optional API metadata: OK"); }}')
    run(java+'javac','-J-Xmx96m','--release','21','-d',tests,probe)
    names = ['.'.join(p.relative_to(classes).with_suffix('').parts) for p in classes.rglob('*.class')
             if p.name != 'SquaremapRenderer.class']
    run(java+'java','-Xmx128m','-cp',str(tests)+':'+str(classes)+':'+cp,'AbsentApiProbe',*names)
    output = root/('build/artifacts/RoyaleProtectionBlocks-1.18.0-Valerin-squaremap-' + node + '.jar')
    with zipfile.ZipFile(base) as old, zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as new:
        for entry in old.infolist():
            data = old.read(entry.filename)
            if entry.filename == 'plugin.yml':
                text = data.decode()
                lines = text.splitlines(keepends=True)
                found = False
                for i,line in enumerate(lines):
                    if line.startswith('softdepend:'):
                        assert '[' in line and ']' in line
                        lines[i] = line.replace(']', ', squaremap ]'); found = True
                assert found
                data = ''.join(lines).encode()
            new.writestr(entry,data)
        for item in classes.rglob('*.class'):
            new.write(item,item.relative_to(classes).as_posix())
        new.write(root/'src/main/resources/squaremap.yml','squaremap.yml')
    with zipfile.ZipFile(base) as old, zipfile.ZipFile(output) as new:
        changed = [n for n in old.namelist() if old.read(n) != new.read(n)]
        assert changed == ['plugin.yml'], changed
        added = set(new.namelist())-set(old.namelist())
        assert all(n=='squaremap.yml' or n.startswith('royale/RoyaleProtectionBlocks/Plugin/Hooks/Squaremap/') for n in added)
        assert len(new.namelist()) == len(set(new.namelist()))
        print('Artifact: existing bytecode unchanged; additions:', len(added))
    print(hashlib.sha256(output.read_bytes()).hexdigest(),output)
