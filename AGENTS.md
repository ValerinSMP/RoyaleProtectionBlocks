# RoyaleProtectionBlocks: reglas locales

Alcance: únicamente este proyecto de Survival Custom/Sync.

## Fuentes y compatibilidad

- Build actual: `pom.xml`; toolchain declarada: pendiente; release detectado: pendiente.
- Estos valores son un inventario, no autorización para cambiar la compatibilidad.
- Falta README.md: documentar contratos desde la fuente, sin inventar mecánicas.
- Leer también el AGENTS.md y estándar visual de la modalidad, y ../../../DEVELOPMENT.md.
- JDK de desarrollo objetivo: 25 en personal; conservar release existente hasta
  comprobar APIs y tests. No cambiar el runtime de producción.

## Integraciones declaradas

- `main: ${project.package}.${project.groupId}.Plugin.MainPluginClass`
- `name: ${project.name}`
- `api-version: 1.13`
- `depend: [ WorldGuard ]`
- `softdepend: [ PlaceholderAPI, ItemsAdder, ProtectionStones, Oraxen, Nexo, DeluxeCombat, LuckPerms, CombatLogX, PvPManager, ProtocolLib, Vault, TokenManager, EssentialsX, CoinsEngine, PlayerPoints, CoreProtect, squaremap ]`

## Trabajo y presentación

- No trasladar responsabilidades, colores ni menús desde otras modalidades a RoyaleProtectionBlocks.
- Textos nuevos configurables; Adventure/MiniMessage y paleta semántica del estándar.
- Reutilizar navegación local y revalidar permisos/requisitos antes de mutar estado.
- No copiar IDs, comandos ni características de otro plugin por compartir nombre.
- Preservar cambios Git locales, personalizaciones y datos existentes.

## Build y operación

- Compilar en personal mediante /root/development/tools/run-personal-build.sh.
- No ejecutar servidores, migraciones SQL o integraciones productivas durante un build.
- En HomeSV, no iniciar builds sin autorización excepcional y controles locales.
- No ejecutar scripts de despliegue de HomeSV desde la copia personal.
- Build, test, instalado, activo y gameplay verificado son estados distintos.
- No reiniciar, recargar ni instalar en producción por inferencia.
- Nunca imprimir ni trasladar credenciales productivas.

