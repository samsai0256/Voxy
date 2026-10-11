# Voxy 0.2.4-alpha for Minecraft 1.21.1 / NeoForge

Unofficial source port of the supplied Fabric release. Source provenance is in
`PORT_SOURCE.json`. The Minecraft APIs, rendering integration, mixins, loader
entrypoint, config discovery and native-library packaging have been ported;
this is not a metadata-only conversion.

## Install

1. Use Minecraft **1.21.1**, Java **21**, and NeoForge **21.1.209**.
2. Install **Sodium 0.6.13 for Minecraft 1.21.1, NeoForge edition**.
3. Put `voxy-1.21.1-0.2.4-alpha-neoforge.1.jar` in the client instance's `mods`
   folder. Remove the original Fabric Voxy JAR from that instance.
4. Launch and open a world. Voxy records visited chunks for distant rendering.
   Its settings are available through NeoForge's Mods menu.

Native libraries are bundled for **Windows x64 and Linux x64**. Rendering
requires **OpenGL 4.6** plus compute shaders and indirect draw parameters.
Unsupported graphics disable Voxy's LOD renderer. This is a client mod.

Iris **1.8.12 for Minecraft 1.21.1, NeoForge edition** is optional. Shaderpack
compatibility is experimental and has not been validated in this port.
Fabric API, Fabric Loader and Mod Menu are not required.

## Build and development

With a Java 21 JDK, run:

```sh
./gradlew test build
```

The output is in `build/libs/`. Dependencies are pinned in `build.gradle`.
`./gradlew runClient` launches development classes. `./gradlew runPackagedClient`
loads the actual built JAR in `run-packaged/mods`, exercising nested dependency
and native resource loading. `-PwithIris` adds the optional Iris dependency.
A display and supported graphics driver are needed for client runs.

## Validation and limitations

- Seven automated tests pass: mixin targets/callback signatures against the
  pinned Minecraft, Sodium and Iris classes; bundled shader import resolution
  and missing-resource errors; biome NBT serialization; LMDB persistence;
  RocksDB persistence; and Zstd compression round trips.
- The packaged JAR reached the title screen, loaded a singleplayer world,
  initialized the normal Voxy rendering pipeline, populated its on-disk world
  cache, successfully ran `/voxy reload`, and saved/disconnected with Voxy
  renderer and instance shutdown completing.
- These graphics checks used Linux Mesa llvmpipe under Xvfb with
  `MESA_GL_VERSION_OVERRIDE=4.6` and `MESA_GLSL_VERSION_OVERRIDE=460`. They are
  software smoke tests, not evidence of performance or physical GPU support.
  A software-driver memory-release warning occurred during renderer reload.
- Iris 1.8.12 was also installed and reached an existing world with shaders
  disabled; Iris created its overworld pipeline and Voxy created its normal
  render pipeline. Actual shaderpacks remain untested.
- Windows, real GPUs, multiplayer, dimension changes, large modpacks,
  shaderpacks, and Vivecraft have not been validated.
- Fabric-specific Nvidium and Chunky integration hooks were removed. The
  optional Distant Horizons database importer remains unavailable without its
  SQLite JDBC dependency; normal Voxy world storage does not require SQLite.

Treat this as a test build until it has been checked on your graphics hardware.
The original author's attribution and All-Rights-Reserved metadata are retained.
