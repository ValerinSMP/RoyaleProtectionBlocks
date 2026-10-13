# Presentación de RoyaleProtectionBlocks — 2026-10-13

Cambios de textos instalados en archivos de Custom, Sync y Test. Código adicional
instalado/activado únicamente en Test. Producción no fue recargada ni reiniciada
y conserva sus JAR. Aspecto y mensajes con clientes aún no verificados.

## Qué se cambió

- Se completó la traducción de Lang.yml, incluidas las claves Error.Exception
  utilizadas por las operaciones, textos de información, valores y resultados.
- Aviso único de límites, espera de teletransporte, límites, fusión, permisos,
  combate y fallos tienen textos más breves o instrucciones concretas.
- La ayuda de Sync queda traducida. Se tradujeron etiquetas de argumentos y se
  corrigió la ayuda que sugería `confirmar`: el parser exige `confirm`.
- La ayuda de tienda deja de anunciar selectores antiguos que ese comando no usa.
- Se cambió «Click para» por «Haz clic para» y se quitaron los colores de los
  títulos de confirmación/advertencia. Distribución, slots, materiales y acciones
  de los menús actuales se conservan. No hubo recoloreado general.
- Se conserva la intersección necesaria para merge; únicamente su error la
  explica mejor. No se cambió el parser ni la funcionalidad de merge.

La primera fase modificó 22 archivos y 798 valores; el ajuste final de ayuda
modificó 54 etiquetas adicionales. Hay 24 archivos de presentación únicos entre
los tres nodos. Claves, tipos YAML, callbacks, permisos, aliases, cooldowns,
placeholders y reglas se compararon con los originales. Las etiquetas de ayuda
conservan flags y confirmaciones literales. No se cambió Config.yml.

## Código y fuentes

Fuente de desarrollo: personal, fork ValerinSMP/RoyaleProtectionBlocks, rama
valerin; PandaUtils es su submódulo. Las fuentes originales HomeSV no se editaron.

- MessageTemplate reconoce la numeración 26.x. ModernMessageBuilder mantiene
  componentes/acciones del chat y usa Adventure para action bars. Consola conserva
  su respuesta textual. No se interpreta el texto del jugador como MiniMessage.
- PagedChestInventoryObject elimina solo el contador configurado de página única,
  antes de sustituir los nombres; títulos con varias páginas permanecen iguales.
- ReadOnlyFeedback registra un mensaje independiente de solo lectura. Cambia
  únicamente la referencia de rechazo en checkIfWritable, manteniendo el tipo
  público PROTECTIONS_PERMISSIONDENIED y la misma condición de prohibición.

La compilación inicial del servicio completo encontró divergencias previas entre
fuente y API/JAR, incluidos cambios de teletransporte no instalados. No se
forzaron dependencias ni se introdujeron esos cambios. El adaptador focal modifica
una sola referencia de campo en el método confirmado del binario instalado.
Verificación: los otros 54 métodos, todos los campos y los atributos de clase
permanecen idénticos; el método conserva instrucciones y metadatos salvo esa
referencia. ASM se usa solo en build; no se añade al runtime del plugin.

Se actualizaron los defaults de presentación en fuente y se añadió README. El
candidato binario conserva recursos de cada base; los YAML instalados se entregan
por separado. No se anuncia una build Maven completa.

## Build y pruebas

JDK 25/release 21 en personal, mediante el ejecutor limitado de 2 CPU/4 GiB/sin
swap/una build. 124 comprobaciones pasadas sobre los JAR finales:

- 15 de presentación para cada numeración simulada 1.21.11, 26.2 y 26.3, sobre
  las dos bases: 90 comprobaciones. Incluyen canales, chat multilínea, texto literal,
  consola, contador según páginas y rechazo de solo lectura frente a permisos.
- 17 de ciclo de view sobre cada base: 34 comprobaciones. Incluyen 200 ticks sin
  aviso repetido, 50 comandos, desconexión, reconexión, cambio de mundo, expiración,
  reload y ausencia de tareas residuales.

Todos los demás recursos/clases del JAR se conservan. Custom mantiene 2.035
entradas idénticas; Sync 2.032. Tres clases reemplazadas y dos añadidas. El runtime
verifica clases con -Xverify:all en las pruebas. La metadata Code del método
adaptado se comparó por atributos, tolerando únicamente el orden de serialización.
No quedan procesos propios de build, pruebas ni consola adjunta.

## Test y producción

Test: f2ea450e-02a0-416c-9838-e73458d37b1c. Sus instrucciones se revisaron; el
README de preparación es histórico y los logs actuales confirman Leaf 26.2.
Royale usa SQLite local allí. Se confirmó cero jugadores mediante `list`, se
apagó con `stop`, se respaldó/verificó el JAR y se sustituyó de forma atómica
preservando propietario y permisos. Se arrancó únicamente Test.

La nueva instancia llegó a Done y cargó ProtectionParticlesService y
PlayerInteractionsServiceImpl. El nuevo mensaje ReadOnly apareció en Lang.yml,
confirmando registro. `/pb admin reload` completó sus servicios y mostró la
respuesta en español; `/pb help` mostró descripciones traducidas. Los avisos
sobre integraciones opcionales ausentes se conservan; no son pruebas fallidas
de esta entrega. Tras la recarga las traducciones esperadas permanecieron.
La ayuda final de argumentos se recargó y comprobó aparte.

No se ejecutaron fusiones, compras, creación/eliminación de regiones ni SQL de
prueba. La validación con clientes de títulos, action bars y controles permanece
pendiente. No equivale a aceptación de gameplay ni prueba general de otros plugins.

Custom: 9292cb5d-ec3b-4833-a0c3-52aff6c35045 y Sync:
93749c9d-401f-4f07-9872-8a2cb6c587da mantienen sus JAR previos:
8620d74631bd081c65785fbab50d748cca9c72963b93b4503d1c3ef7f22b1313 y
e780716bf18ad20d6a4bf9cbf34becc411ffc6d349198d16d5c11581deaed47e,
respectivamente. Los YAML están escritos; falta autorización de recarga para
activarlos. Los cambios de código continúan solo en Test y como candidatos.

## Entrega y reversión

- Candidato Custom/Test SHA-256:
  c0338a2c765a09210db66c0633c1a62ba22307dbc1180ffed8604bbaabb7c6f3.
- Candidato Sync SHA-256:
  27007bb5a0dbc75b4585c2277f20f3a640a3d59a7aeb068e3e0ee319c55fd9c8.
- Respaldos originales: /mnt/storage/backups/ai/survival-custom/royale-texts-20261013/.
  El subdirectorio before-help-usage conserva el estado intermedio de Commands
  en Sync; los originales generales permiten volver al estado anterior a la tarea.
- Restaurar solo los archivos del nodo afectado desde su respaldo, verificar su
  hash y activar mediante la recarga/lifecycle autorizado. El JAR anterior de
  Test está en Test/RoyaleProtectionBlocks-1.18.0-Valerin-player-search-dev.jar.
- Manifiestos, diffs, templates y candidatos:
  /home/mtyn/staging/survival-custom/royale-texts-20261013/.

API contrastada con Context7 y documentación oficial de Paper 26.2:
https://jd.papermc.io/paper/26.2/org/bukkit/entity/Player.html.

## Activación productiva autorizada

2026-10-13: Custom recargado a las 12:44:28 y Sync a las 12:44:43 mediante
`pb admin reload`, tras aprobación explícita. Ambos confirmaron recarga y
respuesta en español, sin errores críticos. Traducciones y controles conservados
tras la recarga. JAR productivos sin cambios; código adicional sigue solo en Test.
Gameplay con clientes pendiente. Evidencia HomeSV: production-reload.json.
