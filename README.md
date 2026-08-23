# Wizardry

[![CI](https://img.shields.io/github/actions/workflow/status/aincraft-org/wizardry/ci.yml)](https://github.com/aincraft-org/wizardry/actions/workflows/ci.yml)
[![Last commit](https://img.shields.io/github/last-commit/aincraft-org/wizardry)](https://github.com/aincraft-org/wizardry/commits/master)

Wizardry is a Paper plugin with a platform-agnostic spell compiler and glyph recognition support. Player-facing language guides live in [docs/glyphcraft-language.md](docs/glyphcraft-language.md) and [docs/scribe-language.md](docs/scribe-language.md).

## Modules

- `wizardry-api` — public, platform-agnostic compiler, glyph, and ML contracts.
- `wizardry-common` — platform-agnostic compiler and glyph/ML implementations.
- `wizardry-paper` — the unified Paper and MapGUI plugin.
- `training` — Python tooling and reviewed fixtures for glyph model training.

MapGUI is included as a sibling Gradle build at `../MapGUI` for local development.

## Build

```bash
./gradlew clean test
./gradlew :wizardry-paper:jar
```

The Paper jar is written to `wizardry-paper/build/libs/` and loads ONNX Runtime through the Paper plugin loader rather than embedding native runtime files.
