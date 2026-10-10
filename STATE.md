# Estado RoyaleProtectionBlocks

- Fuente de esta entrega: personal `/root/development/projects/survival-custom/RoyaleProtectionBlocks`, rama `valerin`, base `836ad59`; copia HomeSV preservada sin editar.
- Tarea: corregir spam de `/pb view` en Leaf 26.2 y auditar `/pb merge` sin modificarlo.
- Modificado: `ProtectionParticlesService.java`; añadido test del servicio y scripts `build-view-fix.sh`/`verify-view-artifact.py`. Informe técnico en `docs/VIEW-FIX-20261010.md`.
- Preservar: `DEVELOPMENT_HANDOFF.md` preexistente y todas las modificaciones históricas del fork; no incluir datos ni bases JAR en Git.
- Compilado: parche focal de dos clases sobre las bases exactas de Custom/Sync, JDK 25/release 21; no build completo Maven.
- Pruebas: 17 comprobaciones por base pasadas; archivo ZIP conserva 2.036/2.033 entradas ajenas idénticas. Primeros dos intentos del simulador fallaron y fueron corregidos. Una ejecución fue rechazada por bloqueo de otra build; no se evadió.
- Entrega: Custom SHA-256 `8620d74631bd081c65785fbab50d748cca9c72963b93b4503d1c3ef7f22b1313`; Sync `e780716bf18ad20d6a4bf9cbf34becc411ffc6d349198d16d5c11581deaed47e`.
- Instalado: no. Activo: no. Gameplay: no verificado. No se usó Test ni se modificaron configuraciones/datos productivos.
- Siguiente paso: validación acotada en Test de partículas reales, registro de listeners y coste con vistas concurrentes. Producción no autorizada en esta tarea.
