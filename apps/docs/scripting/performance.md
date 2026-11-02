# Performance

## Interpreter and compiler modes

GraalJS optimizes JavaScript only when the JVM provides the Graal compiler:

- **Standard JDK (Temurin and others):** scripts run in *interpreter mode*. Everything works, but compute-heavy code is much slower.
- **GraalVM:** scripts are JIT-compiled. Run the server on GraalVM CE 25 or newer, which is a drop-in JDK.

Spectra logs the active mode at startup:

```
[Spectra] Scripts run on GraalJS 25.4 (Interpreted) in interpreter mode. For compiled performance, run the server on GraalVM.
```

`/spectra info` shows the mode too.

## Guidelines

- **Keep hot handlers small.** Move-event and per-tick handlers run often.
- **Prefer Java APIs that work in bulk** (for example `ChunkData.setRegion`) over loops of single-block calls. Each call between JavaScript and Java has a cost.
- **World generation runs one call at a time per script.** See [World generation](/scripting/world-generation).
- **All scripts share one engine,** so parsed code is cached across scripts and reloads.

## JVM flags

Pass `--enable-native-access=ALL-UNNAMED` to silence the JDK's warnings about the Truffle runtime loading native code.
