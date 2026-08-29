# Merlin Paper documentation

This is the Fumadocs site for the current Merlin Paper feature reference. It is intentionally isolated from the Gradle plugin modules.

## Run locally

```bash
bun install
bun run dev
```

Open <http://localhost:3000> for the landing page or <http://localhost:3000/docs> for the documentation.

## Content workflow

The generated MDX pages under `content/docs/` are derived from:

- `../docs/merlin-feature-catalog.md`
- `../docs/glyphcraft-language.md`
- `../docs/scribe-language.md`

Regenerate them after source-document changes:

```bash
bun run sync
```

The sync script preserves the exact registry keys and rewrites repository source links to GitHub. It fails when a required source file or catalog section is missing.

## Checks

```bash
bun run test:sync
bun run lint
bun run types:check
bun run build
```

The site uses Fumadocs MDX, Fumadocs UI, and the generated Orama search endpoint.
