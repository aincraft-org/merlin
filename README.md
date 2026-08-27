# Merlin

[![CI](https://img.shields.io/github/actions/workflow/status/aincraft-org/merlin/ci.yml)](https://github.com/aincraft-org/merlin/actions/workflows/ci.yml)
[![Last commit](https://img.shields.io/github/last-commit/aincraft-org/merlin)](https://github.com/aincraft-org/merlin/commits/master)

Merlin is a Paper plugin with a platform-agnostic spell compiler and glyph recognition support. Player-facing language guides live in [docs/glyphcraft-language.md](docs/glyphcraft-language.md) and [docs/scribe-language.md](docs/scribe-language.md).

## Modules

- `merlin-api` — public, platform-agnostic compiler, glyph, and ML contracts.
- `merlin-common` — platform-agnostic compiler and glyph/ML implementations.
- `merlin-paper` — the unified Paper and MapGUI plugin.
- `merlin-test` — runnable test plugin (Merlin) for the development server.
- `training` — Python tooling and reviewed fixtures for glyph model training.

Merlin compiles against MapGUI 2.0.0 from Maven Central. `./gradlew :merlin-test:runServer` fetches that same plugin release into the development server.

## Build

```bash
./gradlew clean test
./gradlew :merlin-paper:jar
```

The Paper jar is written to `merlin-paper/build/libs/` and loads ONNX Runtime through the Paper plugin loader rather than embedding native runtime files. Start a development server with `./gradlew :merlin-test:runServer`.
