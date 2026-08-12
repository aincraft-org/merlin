pub mod ast;
pub mod canonical;
pub mod diagnostic;
pub mod lexer;
pub mod parser;
pub mod span;
pub mod validate;
pub mod wire;

use canonical::canonicalize;
use diagnostic::Diagnostic;
use lexer::Lexer;
use parser::Parser;
use validate::validate;
use wire::CompiledSpell;

pub const COMPILER_VERSION: &str = "scribe-compiler/0.1";
pub const MAX_SOURCE_CHARS: usize = 4096;
pub const MAX_SOURCE_BYTES: usize = 16 * 1024;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CompileFailure {
    pub diagnostics: Vec<Diagnostic>,
}

impl CompileFailure {
    pub fn diagnostics(&self) -> &[Diagnostic] {
        &self.diagnostics
    }
}

pub fn compile(source: &str) -> Result<CompiledSpell, CompileFailure> {
    let first_excess_scalar = source
        .char_indices()
        .nth(MAX_SOURCE_CHARS)
        .map(|(index, _)| index);
    if let Some(start) = first_excess_scalar {
        return Err(CompileFailure {
            diagnostics: vec![Diagnostic::error(
                "E0001",
                "source exceeds 4096 characters",
                span::Span::new(start, source.len()),
            )
            .with_source(source)],
        });
    }
    if source.len() > MAX_SOURCE_BYTES {
        return Err(CompileFailure {
            diagnostics: vec![Diagnostic::error(
                "E0001",
                "source exceeds 16384 UTF-8 bytes",
                span::Span::new(source.len(), source.len()),
            )
            .with_source(source)],
        });
    }
    let tokens = match Lexer::new(source).lex() {
        Ok(tokens) => tokens,
        Err(diagnostics) => return Err(CompileFailure { diagnostics }),
    };
    let program = match Parser::new(tokens, source).parse() {
        Ok(program) => program,
        Err(diagnostics) => return Err(CompileFailure { diagnostics }),
    };
    if let Err(diagnostics) = validate(&program, source) {
        return Err(CompileFailure { diagnostics });
    }
    let canonical = canonicalize(&program);
    Ok(CompiledSpell::from_program(program, canonical))
}

pub fn compile_json(source: &str) -> Result<String, String> {
    compile(source)
        .map(|spell| serde_json::to_string(&spell).expect("compiled spell is serializable"))
        .map_err(|failure| {
            serde_json::to_string(&failure.diagnostics).expect("diagnostics are serializable")
        })
}

#[cfg(test)]
mod tests {
    use super::*;
    use proptest::prelude::*;

    #[test]
    fn compiles_example_and_hashes_canonically() {
        let source = "spell ember_bolt { target ray 16; damage target fire 4; push target 0.6; cooldown 3s; }";
        let spell = compile(source).unwrap();
        assert_eq!(spell.name(), "ember_bolt");
        assert_eq!(spell.identity().len(), 64);
        assert_eq!(spell.operations().len(), 4);
    }

    #[test]
    fn equivalent_formatting_has_same_identity() {
        let a = compile("spell x {target ray 1;damage target fire 0.5;}").unwrap();
        let b = compile("spell x { target ray 1.0; damage target fire 0.50; }").unwrap();
        assert_eq!(a.identity(), b.identity());
    }
    #[test]
    fn failures_are_atomic_and_ordered() {
        let err = compile("spell bad { damage target fire 99; push target 9; }").unwrap_err();
        assert_eq!(err.diagnostics[0].code, "E1001");
        assert_eq!(err.diagnostics[1].code, "E1002");
    }

    #[test]
    fn diagnostics_use_one_based_source_locations() {
        let err = compile("spell x {\n damage target fire 99;\n}").unwrap_err();
        assert_eq!((err.diagnostics[0].line, err.diagnostics[0].column), (2, 2));
    }
    #[test]
    fn lexer_consumes_a_multibyte_scalar_as_one_character() {
        let diagnostics = Lexer::new("é").lex().unwrap_err();
        assert_eq!(diagnostics.len(), 1);
        assert_eq!(diagnostics[0].message, "unexpected character `é`");
        assert_eq!(diagnostics[0].span, span::Span::new(0, 2));
        assert_eq!((diagnostics[0].line, diagnostics[0].column), (1, 1));
    }

    #[test]
    fn source_limit_counts_unicode_scalars_not_utf8_bytes() {
        let source = "é".repeat(4096);
        let failure = compile(&source).unwrap_err();
        assert!(failure.diagnostics.iter().all(|d| d.code != "E0001"));
    }

    #[test]
    fn diagnostics_are_deterministically_capped_at_32() {
        let source = "@".repeat(33);
        let failure = compile(&source).unwrap_err();
        assert_eq!(failure.diagnostics.len(), 32);
        assert!(failure
            .diagnostics
            .windows(2)
            .all(|pair| pair[0].span.start <= pair[1].span.start));
    }

    #[test]
    fn parser_reports_the_offending_spell_name_token() {
        let source = "spell {}";
        let tokens = Lexer::new(source).lex().unwrap();
        let failure = Parser::new(tokens, source).parse().unwrap_err();
        let diagnostic = failure.iter().find(|d| d.code == "E0103").unwrap();
        assert_eq!(diagnostic.span, span::Span::new(6, 7));
    }

    #[test]
    fn parser_empty_token_input_returns_diagnostics_without_panicking() {
        let failure = Parser::new(Vec::new(), "").parse().unwrap_err();
        assert!(!failure.is_empty());
        assert!(failure.iter().all(|d| d.line > 0 && d.column > 0));
    }

    #[test]
    fn statement_and_effect_limits_are_inclusive() {
        let sixteen = format!(
            "spell x {{ {} }}",
            std::iter::repeat_n("target ray 1;", 16)
                .collect::<Vec<_>>()
                .join(" ")
        );
        let sixteen_failure = compile(&sixteen).unwrap_err();
        assert!(sixteen_failure
            .diagnostics
            .iter()
            .all(|d| d.code != "E1003"));

        let seventeen = format!(
            "spell x {{ {} }}",
            std::iter::repeat_n("target ray 1;", 17)
                .collect::<Vec<_>>()
                .join(" ")
        );
        let seventeen_failure = compile(&seventeen).unwrap_err();
        assert!(seventeen_failure
            .diagnostics
            .iter()
            .any(|d| d.code == "E1003"));

        let four = compile(
            "spell x { damage target fire 1; heal self 1; push target 1; damage target frost 1; }",
        );
        assert!(four.is_ok());
        let five = compile(
            "spell x { damage target fire 1; heal self 1; push target 1; damage target frost 1; heal self 1; }",
        )
        .unwrap_err();
        assert!(five.diagnostics.iter().any(|d| d.code == "E1004"));
    }

    #[test]
    fn canonical_bytes_use_frozen_names_bits_and_newlines() {
        let compiled =
            compile("spell x { target ray 1; damage target fire 0.5; cooldown 3s; }").unwrap();
        assert_eq!(
            std::str::from_utf8(compiled.canonical()).unwrap(),
            concat!(
                "scribe-compiler/0.1|spell|x|\n",
                "target_ray|3ff0000000000000\n",
                "damage|target|fire|3fe0000000000000\n",
                "cooldown|4008000000000000"
            )
        );
    }

    #[test]
    fn numeric_bounds_are_inclusive_and_outside_values_reject() {
        for source in [
            "spell x { target ray 1; damage target fire 0.5; cooldown 0s; }",
            "spell x { target ray 32; damage target fire 20; cooldown 60s; }",
            "spell x { heal self 0.5; push target 0.1; }",
            "spell x { heal self 20; push target 3; }",
        ] {
            assert!(compile(source).is_ok(), "{source}");
        }
        for (source, code) in [
            ("spell x { target ray 0; heal self 1; }", "E1005"),
            ("spell x { target ray 33; heal self 1; }", "E1005"),
            ("spell x { damage target fire 0.49; }", "E1001"),
            ("spell x { heal self 20.01; }", "E1001"),
            ("spell x { push target 0.09; }", "E1002"),
            ("spell x { push target 3.01; }", "E1002"),
            ("spell x { heal self 1; cooldown 60.01s; }", "E1009"),
        ] {
            let failure = compile(source).unwrap_err();
            assert!(
                failure.diagnostics.iter().any(|d| d.code == code),
                "{source}"
            );
        }
    }

    #[test]
    fn malformed_numbers_reject_atomically() {
        for source in [
            "spell x { heal self .; }",
            "spell x { heal self 1..2; }",
            "spell x { heal self 1.2.3; }",
        ] {
            let failure = compile(source).unwrap_err();
            assert!(failure.diagnostics.iter().any(|d| d.code == "E0003"));
        }
    }

    #[test]
    fn unicode_diagnostic_spans_use_utf8_bytes_and_scalar_columns() {
        let diagnostics = Lexer::new("é中😀@").lex().unwrap_err();
        assert_eq!(diagnostics.len(), 4);
        assert_eq!(diagnostics[0].span, span::Span::new(0, 2));
        assert_eq!(diagnostics[1].span, span::Span::new(2, 5));
        assert_eq!(diagnostics[2].span, span::Span::new(5, 9));
        assert_eq!(diagnostics[3].span, span::Span::new(9, 10));
        assert_eq!(
            diagnostics
                .iter()
                .map(|d| (d.line, d.column))
                .collect::<Vec<_>>(),
            vec![(1, 1), (1, 2), (1, 3), (1, 4)]
        );
    }

    proptest! {
        #[test]
        fn arbitrary_utf8_never_panics(s in any::<String>()) {
            let _ = compile(&s);
        }
    }
}
