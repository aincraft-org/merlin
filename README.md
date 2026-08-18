# Wizardry

Wizardry is a Paper plugin with a platform-agnostic spell compiler and glyph recognition support. Player-facing language guides live in [docs/glyphcraft-language.md](docs/glyphcraft-language.md) and [docs/scribe-language.md](docs/scribe-language.md).

## Modules

- `api` — public, platform-agnostic compiler, glyph, and ML contracts.
- `common` — platform-agnostic compiler and glyph/ML implementations.
- `paper` — the unified Paper and MapGUI plugin.
- `training` — Python tooling and reviewed fixtures for glyph model training.

MapGUI is included as a sibling Gradle build at `../MapGUI` for local development.

## Build

```bash
./gradlew clean test
./gradlew :paper:jar
```

The Paper jar is written to `paper/build/libs/` and loads ONNX Runtime through the Paper plugin loader rather than embedding native runtime files.
