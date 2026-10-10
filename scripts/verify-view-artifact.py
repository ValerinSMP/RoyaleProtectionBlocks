import sys,zipfile,hashlib
base,candidate=sys.argv[1:]
prefix='royale/RoyaleProtectionBlocks/Plugin/Modules/ProtectionParticlesPckg/ProtectionParticlesService'
allowed={prefix+'.class',prefix+'$ProtectionParticlePlayerContainer.class'}
with zipfile.ZipFile(base) as b,zipfile.ZipFile(candidate) as c:
 assert len(c.namelist())==len(set(c.namelist())), 'duplicate archive entries'
 assert set(b.namelist())==set(c.namelist()), 'archive structure changed'
 changed={n for n in b.namelist() if b.read(n)!=c.read(n)}
 assert changed==allowed,changed
 for n in allowed:
  assert int.from_bytes(c.read(n)[6:8],'big')==65, 'release 21'
 print('Artifact preservation: only 2 view classes changed;',len(b.namelist())-2,'entries identical; Java release 21')
