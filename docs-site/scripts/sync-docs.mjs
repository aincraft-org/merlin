import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const defaultRootDirectory = path.resolve(scriptDirectory, '../..');
const defaultRepositoryUrl = 'https://github.com/aincraft-org/merlin';
const defaultBranch = 'master';

const pageSpecs = [
  {
    file: 'index.mdx',
    title: 'Merlin Paper Documentation',
    description: 'Current player-facing behavior and systems implemented by the Merlin Paper plugin.',
  },
  {
    file: 'enchantments.mdx',
    title: 'Enchantments',
    description: 'The complete registered Merlin enchantment catalog, targets, ranks, effects, and storage.',
    sections: ['Enchantment catalog'],
  },
  {
    file: 'altar.mdx',
    title: 'Eterna and Quanta Altar',
    description: 'The current custom enchanting-table flow, altar structure, offers, and transaction rules.',
    sections: ['Eterna and Quanta altar'],
  },
  {
    file: 'scribe.mdx',
    title: 'Scribe',
    description: 'Marked Scribe books, the closed spell language, compiler limits, and Paper authoring flow.',
    sections: ['Scribe books and spell DSL'],
    guide: 'scribeText',
    guideHeading: 'Full Scribe phrasebook',
  },
  {
    file: 'glyphcraft.mdx',
    title: 'Glyphcraft',
    description: 'Glyph maps, elemental ink, tomes, classification, binding, and spell casting.',
    sections: ['Glyphcraft maps and tomes'],
    guide: 'glyphcraftText',
    guideHeading: 'Full Glyphcraft tome language guide',
  },
  {
    file: 'ink.mdx',
    title: 'Magical Ink',
    description: 'Elemental ink bottles, durability fill, flowers, and Mortar & Pestle grinding.',
    sections: ['Magical ink and grinding'],
  },
  {
    file: 'ritual-crafting.mdx',
    title: 'Ritual Crafting',
    description: 'Ritual Anchors, Pedestals, glyph recipes, catalysts, and marked intermediate outputs.',
    sections: ['Ritual Crafting'],
  },
  {
    file: 'runtime.mdx',
    title: 'Runtime Enchantment Features',
    description: 'Event dispatch, passive equipment effects, and active custom-enchantment behavior.',
    sections: ['Custom-enchantment runtime features'],
  },
  {
    file: 'items-and-persistence.mdx',
    title: 'Marked Items and Persistence',
    description: 'Plugin-owned item markers, persistent data keys, and item lifecycle rules.',
    sections: ['Marked items and persistence'],
  },
  {
    file: 'configuration.mdx',
    title: 'Configuration and Permissions',
    description: 'Commands, permissions, configuration keys, and runtime safety limits.',
    sections: ['Permissions, configuration, and limits'],
  },
  {
    file: 'status.mdx',
    title: 'Status and Boundaries',
    description: 'What is current, next, future, or proposed, with links to the source specifications.',
    sections: ['Current boundaries and non-current proposals', 'Further reading'],
  },
];

function splitTopLevelSections(markdown) {
  const headings = [...markdown.matchAll(/^# (.+)$/gm)];
  const sections = new Map();

  for (let index = 0; index < headings.length; index += 1) {
    const heading = headings[index];
    const start = heading.index;
    const end = headings[index + 1]?.index ?? markdown.length;
    sections.set(heading[1].trim(), markdown.slice(start, end).trim());
  }

  return sections;
}

function sectionBody(sections, name) {
  const section = sections.get(name);
  if (!section) {
    throw new Error(`Missing required catalog section: ${name}`);
  }

  return section.replace(/^# .*(?:\n|$)/, '').trim();
}

function demoteTopLevelHeadings(markdown) {
  return markdown.replace(/^# /gm, '## ');
}

function rewriteLinks(markdown, { repositoryUrl, branch }) {
  const localRoutes = new Map([
    ['glyphcraft-language.md', '/docs/glyphcraft'],
    ['scribe-language.md', '/docs/scribe'],
  ]);

  return markdown.replace(/\]\(([^)]+)\)/g, (match, target) => {
    const repositoryPath = target.startsWith('../merlin-paper/')
      ? target.slice('../'.length)
      : target.startsWith('superpowers/') || target.startsWith('living-specs/')
        ? `docs/${target}`
        : null;

    if (repositoryPath) {
      return `](${repositoryUrl}/blob/${branch}/${repositoryPath})`;
    }

    const route = localRoutes.get(target);
    return route ? `](${route})` : match;
  });
}

function frontmatter(title, description) {
  return `---\ntitle: ${JSON.stringify(title)}\ndescription: ${JSON.stringify(description)}\n---`;
}

function makePage({ title, description, body }) {
  return `${frontmatter(title, description)}\n\n${body.trim()}\n`;
}

function makeIndexPage() {
  return makePage({
    title: 'Merlin Paper Documentation',
    description: 'Current player-facing behavior and systems implemented by the Merlin Paper plugin.',
    body: `# Merlin Paper\n\nThe current reference for Merlin Paper: enchantments, magical authoring systems, ritual crafting, runtime behavior, and configuration.\n\n## What is implemented\n\n- **81 registered altar definitions:** 6 vanilla or over-cap definitions and 75 custom definitions.\n- A custom **Eterna and Quanta Enchanter\'s Altar Matrix** opened from an enchanting table.\n- Marked Scribe books with a bounded natural-language spell DSL.\n- Glyph maps, elemental ink, glyph tomes, classification, and casting.\n- Ritual Anchors and Pedestals that produce marked crafting intermediates.\n- A modular custom-enchantment dispatcher covering combat, tools, armor, movement, fishing, buckets, projectiles, and passive effects.\n\n## Browse the reference\n\n- [Enchantments](/docs/enchantments)\n- [Eterna and Quanta Altar](/docs/altar)\n- [Scribe](/docs/scribe)\n- [Glyphcraft](/docs/glyphcraft)\n- [Magical Ink](/docs/ink)\n- [Ritual Crafting](/docs/ritual-crafting)\n- [Runtime Enchantment Features](/docs/runtime)\n- [Marked Items and Persistence](/docs/items-and-persistence)\n- [Configuration and Permissions](/docs/configuration)\n- [Status and Boundaries](/docs/status)`,
  });
}

export function buildPages({ catalogText, glyphcraftText, scribeText, repositoryUrl, branch }) {
  const sections = splitTopLevelSections(catalogText);
  const normalizedRepositoryUrl = repositoryUrl.replace(/\/$/, '');
  const linkOptions = { repositoryUrl: normalizedRepositoryUrl, branch };
  const pages = new Map();

  for (const spec of pageSpecs) {
    if (spec.file === 'index.mdx') {
      pages.set(spec.file, makeIndexPage());
      continue;
    }

    let body = spec.sections.map((name) => sectionBody(sections, name)).join('\n\n');
    body = demoteTopLevelHeadings(body);

    if (spec.guide) {
      const guideText = spec.guide === 'glyphcraftText' ? glyphcraftText : scribeText;
      body += `\n\n## ${spec.guideHeading}\n\n${demoteTopLevelHeadings(guideText.trim()).replace(/^## /, '')}`;
    }

    pages.set(spec.file, makePage({
      title: spec.title,
      description: spec.description,
      body: rewriteLinks(body, linkOptions),
    }));
  }

  return pages;
}

export async function syncDocs({ rootDirectory = defaultRootDirectory } = {}) {
  const sourceDirectory = path.join(rootDirectory, 'docs');
  const outputDirectory = path.join(rootDirectory, 'docs-site', 'content', 'docs');
  const pages = buildPages({
    catalogText: await readFile(path.join(sourceDirectory, 'merlin-feature-catalog.md'), 'utf8'),
    glyphcraftText: await readFile(path.join(sourceDirectory, 'glyphcraft-language.md'), 'utf8'),
    scribeText: await readFile(path.join(sourceDirectory, 'scribe-language.md'), 'utf8'),
    repositoryUrl: process.env.MERLIN_REPOSITORY_URL ?? defaultRepositoryUrl,
    branch: process.env.MERLIN_DOCS_BRANCH ?? defaultBranch,
  });

  await mkdir(outputDirectory, { recursive: true });
  for (const [file, content] of pages) {
    await writeFile(path.join(outputDirectory, file), content, 'utf8');
  }
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  await syncDocs();
}
