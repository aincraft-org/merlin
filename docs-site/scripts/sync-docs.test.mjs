import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { buildPages } from './sync-docs.mjs';

const repositoryRoot = new URL('../../', import.meta.url);

async function readRepositoryFile(path) {
  return readFile(new URL(path, repositoryRoot), 'utf8');
}

test('buildPages emits the complete Merlin documentation page set', async () => {
  const pages = buildPages({
    catalogText: await readRepositoryFile('docs/merlin-feature-catalog.md'),
    glyphcraftText: await readRepositoryFile('docs/glyphcraft-language.md'),
    scribeText: await readRepositoryFile('docs/scribe-language.md'),
    repositoryUrl: 'https://github.com/aincraft-org/merlin',
    branch: 'master',
  });

  assert.deepEqual([...pages.keys()], [
    'index.mdx',
    'enchantments.mdx',
    'altar.mdx',
    'scribe.mdx',
    'glyphcraft.mdx',
    'ink.mdx',
    'ritual-crafting.mdx',
    'runtime.mdx',
    'items-and-persistence.mdx',
    'configuration.mdx',
    'status.mdx',
  ]);

  const enchantments = pages.get('enchantments.mdx');
  const enchantmentRows = enchantments.match(/^\| .* \(`(?:merlin|minecraft):[^`]+`\)/gm) ?? [];
  assert.equal(enchantmentRows.length, 81);
  assert.match(enchantments, /`merlin:overcap_enchantments`/);
  assert.match(enchantments, /CustomEnchantmentRegistryTest\.java/);
  assert.doesNotMatch(enchantments, /\.\.\/merlin-paper\//);

  assert.match(pages.get('scribe.mdx'), /summon sheep/);
  assert.match(pages.get('glyphcraft.mdx'), /\/glyph bind/);
  assert.match(pages.get('ink.mdx'), /Mortar & Pestle/);
  assert.match(pages.get('ritual-crafting.mdx'), /Ritual Anchor/);
  assert.doesNotMatch(pages.get('status.mdx'), /\]\((?!https?:\/\/|\/docs\/)[^)]+\)/);
  assert.match(pages.get('status.mdx'), /github\.com\/aincraft-org\/merlin\/blob\/master\/docs\/living-specs\/glyphcraft\.md/);
  assert.match(pages.get('status.mdx'), /github\.com\/aincraft-org\/merlin\/blob\/master\/docs\/living-specs\/ritual-crafting\.md/);
});

test('buildPages rejects an incomplete catalog', async () => {
  const catalog = await readRepositoryFile('docs/merlin-feature-catalog.md');
  const glyphcraftText = await readRepositoryFile('docs/glyphcraft-language.md');
  const scribeText = await readRepositoryFile('docs/scribe-language.md');

  assert.throws(
    () => buildPages({
      catalogText: catalog.replace('# Ritual Crafting', '# Removed Ritual Crafting'),
      glyphcraftText,
      scribeText,
      repositoryUrl: 'https://github.com/aincraft-org/merlin',
      branch: 'master',
    }),
    /Missing required catalog section: Ritual Crafting/,
  );
});
