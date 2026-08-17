#!/usr/bin/env python3
"""One-time migration: split java-compiler into api and common modules.

This script is intentionally mechanical and must be audited after it runs.
It does not touch paper, mapgui, or glyph-training.
"""

import os, re, shutil, sys

REPO = "/home/jlo/dev/wizardry"
OLD_MAIN = os.path.join(REPO, "java-compiler/src/main/java/dev/mintychochip/wizardry")
OLD_TEST = os.path.join(REPO, "java-compiler/src/test/java/dev/mintychochip/wizardry")
API_MAIN = os.path.join(REPO, "api/src/main/java/dev/mintychochip/wizardry/api")
COMMON_MAIN = os.path.join(REPO, "common/src/main/java/dev/mintychochip/wizardry/common")
COMMON_TEST = os.path.join(REPO, "common/src/test/java/dev/mintychochip/wizardry/common")

# (old_path_relative_to OLD_MAIN) -> (module, new_package_suffix)
MAIN_MOVES = {
    # glyph public API
    "glyph/GlyphBitmap.java": ("api", "glyph"),
    "glyph/GlyphCaptureSession.java": ("api", "glyph"),
    "glyph/GlyphDraft.java": ("api", "glyph"),
    "glyph/GlyphLimits.java": ("api", "glyph"),
    "glyph/GlyphPoint.java": ("api", "glyph"),
    "glyph/GlyphStroke.java": ("api", "glyph"),
    # glyph implementation
    "glyph/GlyphRasterizer.java": ("common", "glyph"),

    # ml public API
    "ml/Classification.java": ("api", "ml"),
    "ml/ClassificationCandidate.java": ("api", "ml"),
    "ml/GlyphClassifier.java": ("api", "ml"),
    "ml/Label.java": ("api", "ml"),
    "ml/ModelBundle.java": ("api", "ml"),
    # ml implementation
    "ml/GlyphPreprocessor.java": ("common", "ml"),
    "ml/OnnxGlyphClassifier.java": ("common", "ml"),
    "ml/PreprocessedGlyph.java": ("common", "ml"),

    # dsl public API (was scribe)
    "scribe/CompilerConstants.java": ("api", "dsl"),
    "scribe/syntax/Program.java": ("api", "dsl"),
    "scribe/syntax/Statement.java": ("api", "dsl"),
    "scribe/model/CompileResult.java": ("api", "dsl"),
    "scribe/model/CompiledSpell.java": ("api", "dsl"),
    "scribe/model/Diagnostic.java": ("api", "dsl"),
    "scribe/model/Operation.java": ("api", "dsl"),
    "scribe/model/Span.java": ("api", "dsl"),
    # dsl implementation
    "scribe/ScribeCompiler.java": ("common", "dsl"),
    "scribe/lexer/Lexer.java": ("common", "dsl.lexer"),
    "scribe/parser/Parser.java": ("common", "dsl.parser"),
}

# Build a simple-class-name -> new FQN package mapping.
# Nested types (e.g. Operation.TargetRay) are reached through their top-level class,
# so we only need to rewrite top-level class imports.
CLASS_TO_PKG = {}
for old_rel, (mod, pkg) in MAIN_MOVES.items():
    cls = os.path.basename(old_rel).replace(".java", "")
    CLASS_TO_PKG[cls] = f"dev.mintychochip.wizardry.{mod}.{pkg}"


def package_from_path(path, root, module):
    """Compute package declaration from a file's new location."""
    rel = os.path.relpath(os.path.dirname(path), root)
    parts = rel.split(os.sep)
    # rel is like 'dsl' or 'dsl/lexer'; root already ends at the module root
    return f"dev.mintychochip.wizardry.{module}." + ".".join(parts)


def move_main_sources():
    if not os.path.isdir(OLD_MAIN):
        print(f"No {OLD_MAIN}; nothing to do.")
        return
    for old_rel, (mod, pkg) in MAIN_MOVES.items():
        old = os.path.join(OLD_MAIN, old_rel)
        if not os.path.exists(old):
            print(f"  skip missing {old}")
            continue
        new_dir = os.path.join(
            REPO, mod, "src/main/java/dev/mintychochip/wizardry", mod, pkg.replace(".", "/")
        )
        os.makedirs(new_dir, exist_ok=True)
        new = os.path.join(new_dir, os.path.basename(old))
        shutil.move(old, new)


def move_tests():
    if not os.path.isdir(OLD_TEST):
        return
    for dirpath, _, filenames in os.walk(OLD_TEST):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            old = os.path.join(dirpath, fn)
            rel = os.path.relpath(old, OLD_TEST)
            # All tests go under common.<domain>; infer domain from the top dir
            top = rel.split(os.sep)[0]
            if top == "scribe":
                domain = "dsl"
            else:
                domain = top  # glyph, ml
            sub = os.path.dirname(rel)
            if sub:
                # e.g. scribe/lexer -> common.dsl.lexer
                sub_path = sub.replace(os.sep, ".").replace("scribe", "dsl")
                pkg = f"common.{sub_path}"
            else:
                pkg = f"common.{domain}"
            new_dir = os.path.join(
                REPO, "common", "src/test/java/dev/mintychochip/wizardry", pkg.replace(".", "/")
            )
            os.makedirs(new_dir, exist_ok=True)
            shutil.move(old, os.path.join(new_dir, fn))


def delete_empty_dirs(path):
    for dirpath, dirnames, _ in os.walk(path, topdown=False):
        for d in dirnames:
            full = os.path.join(dirpath, d)
            if os.path.isdir(full) and not os.listdir(full):
                os.rmdir(full)


def fix_package_and_imports_in_tree(root, module):
    """Rewrite package declarations and imports for api/common source trees."""
    for dirpath, _, filenames in os.walk(root):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            fpath = os.path.join(dirpath, fn)
            with open(fpath, "r", encoding="utf-8") as f:
                text = f.read()

            # Set package from path
            pkg = package_from_path(fpath, root, module)
            text = re.sub(
                r"^package\s+[\w.]+;",
                f"package {pkg};",
                text,
                count=1,
                flags=re.MULTILINE,
            )

            # Rewrite specific and wildcard imports from old package roots.
            # We only have three old roots: glyph, ml, scribe (with subpackages).

            # 1. Old scribe.model, scribe.syntax -> api.dsl
            text = re.sub(
                r"import\s+dev\.mintychochip\.wizardry\.scribe\.(model|syntax)\.([*\w]+);",
                r"import dev.mintychochip.wizardry.api.dsl.\2;",
                text,
            )

            # 2. Old scribe.lexer, scribe.parser -> common.dsl.lexer/parser
            text = re.sub(
                r"import\s+dev\.mintychochip\.wizardry\.scribe\.lexer\.([*\w]+);",
                r"import dev.mintychochip.wizardry.common.dsl.lexer.\1;",
                text,
            )
            text = re.sub(
                r"import\s+dev\.mintychochip\.wizardry\.scribe\.parser\.([*\w]+);",
                r"import dev.mintychochip.wizardry.common.dsl.parser.\1;",
                text,
            )

            # 3. Old top-level scribe.* -> both api.dsl.* and common.dsl.*
            # This may produce two imports; that is safe because the two packages
            # have no overlapping class names by design.
            text = re.sub(
                r"import\s+dev\.mintychochip\.wizardry\.scribe\.\*;",
                "import dev.mintychochip.wizardry.api.dsl.*;\nimport dev.mintychochip.wizardry.common.dsl.*;",
                text,
            )

            # 4. Old top-level scribe.<Class>; -> look up class
            text = re.sub(
                r"import\s+dev\.mintychochip\.wizardry\.scribe\.([A-Z]\w+);",
                lambda m: f"import {CLASS_TO_PKG.get(m.group(1), 'dev.mintychochip.wizardry.common.dsl')}.{m.group(1)};",
                text,
            )

            # 5. Old glyph.* and ml.* -> both api and common wildcards
            for domain in ("glyph", "ml"):
                text = re.sub(
                    rf"import\s+dev\.mintychochip\.wizardry\.{domain}\.\*;",
                    f"import dev.mintychochip.wizardry.api.{domain}.*;\nimport dev.mintychochip.wizardry.common.{domain}.*;",
                    text,
                )

            # 6. Old glyph.<Class> and ml.<Class> -> look up class
            for domain in ("glyph", "ml"):
                text = re.sub(
                    rf"import\s+dev\.mintychochip\.wizardry\.{domain}\.([A-Z]\w+);",
                    lambda m, d=domain: f"import {CLASS_TO_PKG.get(m.group(1), f'dev.mintychochip.wizardry.common.{d}')}.{m.group(1)};",
                    text,
                )

            with open(fpath, "w", encoding="utf-8", newline="") as f:
                f.write(text)


def main():
    print("Moving main source files...")
    move_main_sources()
    print("Moving test files...")
    move_tests()

    print("Fixing packages and imports in api/...")
    fix_package_and_imports_in_tree(API_MAIN, "api")
    print("Fixing packages and imports in common/...")
    fix_package_and_imports_in_tree(COMMON_MAIN, "common")
    print("Fixing packages and imports in common tests/...")
    fix_package_and_imports_in_tree(COMMON_TEST, "common")

    # Clean up empty java-compiler source directories, but do not delete the
    # module directory itself (the build.gradle will be removed by the plan).
    for d in (OLD_MAIN, OLD_TEST):
        if os.path.isdir(d):
            delete_empty_dirs(d)

    print("Done. Run ./gradlew :api:compileJava :common:compileJava and fix any remaining issues.")


if __name__ == "__main__":
    main()
