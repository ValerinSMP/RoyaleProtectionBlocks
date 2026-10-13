# RoyaleProtectionBlocks — fork ValerinSMP

Protecciones de Survival Custom/Sync, con presentación en español. La fuente de
esta entrega está en personal bajo
`/root/development/projects/survival-custom/RoyaleProtectionBlocks`; HomeSV
conserva la copia histórica y opera los backends. Leer AGENTS.md y STATE.md.

## Presentación

- `Lang.yml`: mensajes de comandos, errores, resultados y valores visibles.
  Los errores de operaciones usan `Error.Exception.*`; traducir únicamente
  `Error.Protections.*` no cubre esos flujos. Conservar nombres de claves,
  placeholders `{...}` / `%...%`, listas y formato legacy.
- `Commands.yml`: nombres, permisos, aliases, descripciones y ayuda. Traducir
  descripciones y etiquetas de argumentos; mantener subcomandos, opciones y la
  palabra literal `confirm`. El comando de tienda abre bloques y no necesita
  los antiguos selectores `protections|blocks`.
- `Inventories/*.yml`: títulos, nombres, lore y mensajes de entrada. Slots,
  materiales, callbacks, comandos y condiciones pertenecen al comportamiento
  existente y no se cambian por una traducción. `Old-items` conserva información
  de compatibilidad y no debe confundirse con controles activos.
- `Flags.yml`, `Protection-Permissions.yml`, `Protection-Settings.yml`: contienen
  tanto presentación como reglas. Editar textos no autoriza cambiar permisos,
  costes, grupos, valores ni editabilidad.
- Los textos admiten colores legacy permanentes. En 26.x, las action bars usan
  Adventure; el chat conserva sus componentes y acciones. Los títulos paginados
  omiten solo ` ({current_page}/{max_page})` cuando existe una sola página,
  antes de sustituir nombres de jugadores/protecciones.

El estándar visual es `/home/mtyn/valerin-network/docs/ESTANDAR-VISUAL.md`.
No copiar Config.yml entre nodos: Sync conserva su modo de solo lectura.
La denegación por ese modo tiene su propio texto:
`Error.Exception.protection.ReadOnly`, conservando el tipo público de excepción
PROTECTIONS_PERMISSIONDENIED. Una falta de permisos usa su mensaje habitual.

## Configuración y activación

La recarga nativa verificada es `/pb admin reload`. Recarga servicios del plugin,
incluidas las vistas temporales; producción requiere autorización explícita.
Cambiar un JAR requiere el ciclo de activación correspondiente, no una recarga
de configuración. Test permite validación acotada según sus instrucciones.
Una carga o recarga sin errores no confirma aspecto ni mensajes con clientes.

## Build y conservación del fork

Maven es el build upstream. La fuente preservada contiene cambios de red/API que
no están en todos los JAR instalados, por lo que no se recompila ese servicio
completo para entregar una corrección de presentación.

El candidato focal se construye sobre cada base exacta mediante:

```bash
/root/development/tools/run-personal-build.sh \
 /root/development/projects/survival-custom/RoyaleProtectionBlocks \
 bash scripts/build-presentation-fix.sh
```

JDK 25, release 21, ejecutor de 2 CPU/4 GiB/sin swap/una build. El script compila
las clases de presentación y el diagnóstico configurable. En el servicio ya
instalado reemplaza una referencia de mensaje dentro de checkIfWritable;
verifica los demás métodos, campos y atributos. No se modifica el flujo de
teletransporte, dinero, miembros, propietarios, regiones ni fusión.

Las pruebas verifican canales de mensajes, listas, placeholders, títulos,
diagnóstico de solo lectura y ciclo de `/pb view`. Los números de versión usados
por el simulador no equivalen a servidores probados. El candidato conserva los
recursos del JAR base y se entrega junto a sus configuraciones traducidas;
los defaults de fuente quedan documentados/traducidos para una futura build
completa validada. Estado y alcance de entrega: STATE.md.
