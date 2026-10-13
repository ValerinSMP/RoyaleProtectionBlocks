import zipfile,sys
base,candidate=sys.argv[1:]
allowed={
 'royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/PlayerInteractionsServiceImpl.class',
 'royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/ReadOnlyFeedback.class',
 'royale/RoyaleProtectionBlocks/PandaUtils/PandaColors/Messages/Objects/MessageTemplate.class',
 'royale/RoyaleProtectionBlocks/PandaUtils/Services/PandaInventoriesModule/v1/Objects/ChestInventory/Paged/PagedChestInventoryObject.class',
 'royale/RoyaleProtectionBlocks/PandaUtils/PandaColors/Messages/NMS/Modern/ModernMessageBuilder.class',
}
with zipfile.ZipFile(base) as b,zipfile.ZipFile(candidate) as c:
 before=set(b.namelist());after=set(c.namelist())
 assert before-after==set()
 assert len(c.namelist())==len(after)
 changed={p for p in before&after if b.read(p)!=c.read(p)}|(after-before)
 assert changed==allowed,changed
 assert after-before=={p for p in allowed if p.endswith(('ModernMessageBuilder.class','ReadOnlyFeedback.class'))}
 for path in allowed:assert int.from_bytes(c.read(path)[6:8],'big')==65
 print('Artifact verified: 3 classes replaced + 2 added;',len(before)-3,'entries byte-identical; no resources or data changed.')
