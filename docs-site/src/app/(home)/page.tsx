import Link from 'next/link';

const sections = [
  ['Enchantments', 'The complete 81-entry registry, targets, ranks, effects, and storage.', '/docs/enchantments'],
  ['Eterna and Quanta Altar', 'Structure scanning, offer tiers, GUI slots, and transaction rules.', '/docs/altar'],
  ['Scribe', 'Marked books and the bounded natural-language spell DSL.', '/docs/scribe'],
  ['Glyphcraft', 'Maps, elemental ink, tomes, classification, and casting.', '/docs/glyphcraft'],
  ['Magical Ink', 'Ink bottles, flowers, and Mortar & Pestle grinding.', '/docs/ink'],
  ['Ritual Crafting', 'Ritual stations, glyph recipes, catalysts, and intermediates.', '/docs/ritual-crafting'],
  ['Runtime Features', 'Event dispatch and passive equipment effects.', '/docs/runtime'],
  ['Configuration', 'Permissions, configuration keys, limits, and persistence.', '/docs/configuration'],
];

export default function HomePage() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center px-6 py-20">
      <p className="mb-4 text-sm font-medium uppercase tracking-[0.25em] text-fd-muted-foreground">
        Merlin Paper
      </p>
      <h1 className="max-w-3xl text-4xl font-semibold tracking-tight md:text-6xl">
        A source-backed reference for every current feature.
      </h1>
      <p className="mt-6 max-w-2xl text-lg leading-8 text-fd-muted-foreground">
        Browse the enchantment registry, player authoring systems, ritual crafting, runtime behavior,
        and configuration without confusing implemented behavior with design proposals.
      </p>
      <div className="mt-10 flex flex-wrap gap-3">
        <Link
          href="/docs"
          className="rounded-full bg-fd-primary px-5 py-2.5 text-sm font-medium text-fd-primary-foreground transition-opacity hover:opacity-80"
        >
          Open documentation
        </Link>
        <Link
          href="/docs/status"
          className="rounded-full border border-fd-border px-5 py-2.5 text-sm font-medium transition-colors hover:bg-fd-accent"
        >
          View status boundaries
        </Link>
      </div>
      <div className="mt-16 grid gap-4 sm:grid-cols-2">
        {sections.map(([title, description, href]) => (
          <Link
            key={href}
            href={href}
            className="rounded-xl border border-fd-border p-5 transition-colors hover:bg-fd-accent"
          >
            <h2 className="font-medium">{title}</h2>
            <p className="mt-2 text-sm leading-6 text-fd-muted-foreground">{description}</p>
          </Link>
        ))}
      </div>
    </main>
  );
}
