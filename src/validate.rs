use crate::{
    ast::*,
    diagnostic::{sort, Diagnostic},
};

pub fn validate(program: &SpellProgram, source: &str) -> Result<(), Vec<Diagnostic>> {
    let mut ds = Vec::new();
    if program.name.is_empty() {
        ds.push(Diagnostic::error(
            "E1000",
            "spell name cannot be empty",
            program.span,
        ));
    }
    if program.statements.len() > 16 {
        ds.push(Diagnostic::error(
            "E1003",
            "program exceeds 16 statements",
            program.span,
        ));
    }
    let effects = program.statements.iter().filter(|s| s.is_effect()).count();
    if effects == 0 {
        ds.push(Diagnostic::error(
            "E1011",
            "program requires at least one effect",
            program.span,
        ));
    } else if effects > 4 {
        ds.push(Diagnostic::error(
            "E1004",
            "program exceeds 4 effects",
            program.span,
        ));
    }
    if program
        .statements
        .iter()
        .filter(|statement| matches!(statement, Statement::TargetRay { .. }))
        .count()
        > 1
    {
        ds.push(Diagnostic::error(
            "E1008",
            "program may declare only one target",
            program.span,
        ));
    }
    if program
        .statements
        .iter()
        .filter(|statement| matches!(statement, Statement::Cooldown { .. }))
        .count()
        > 1
    {
        ds.push(Diagnostic::error(
            "E1010",
            "program may declare only one cooldown",
            program.span,
        ));
    }
    for s in &program.statements {
        match s {
            Statement::TargetRay { range, span } if !finite_between(*range, 1.0, 32.0) => {
                ds.push(Diagnostic::error("E1005", "ray range must be 1..32", *span))
            }
            Statement::Damage {
                target,
                amount,
                span,
                ..
            } => {
                if *target != Target::Target {
                    ds.push(Diagnostic::error("E1006", "damage requires target", *span));
                }
                if !finite_between(*amount, 0.5, 20.0) {
                    ds.push(Diagnostic::error("E1001", "damage must be 0.5..20", *span));
                }
            }
            Statement::Heal {
                target,
                amount,
                span,
            } => {
                if *target != Target::SelfTarget {
                    ds.push(Diagnostic::error("E1007", "heal requires self", *span));
                }
                if !finite_between(*amount, 0.5, 20.0) {
                    ds.push(Diagnostic::error("E1001", "healing must be 0.5..20", *span));
                }
            }
            Statement::Push {
                target,
                strength,
                span,
            } => {
                if *target != Target::Target {
                    ds.push(Diagnostic::error("E1006", "push requires target", *span));
                }
                if !finite_between(*strength, 0.1, 3.0) {
                    ds.push(Diagnostic::error("E1002", "push must be 0.1..3", *span));
                }
            }
            Statement::Cooldown { seconds, span } if !finite_between(*seconds, 0.0, 60.0) => ds
                .push(Diagnostic::error(
                    "E1009",
                    "cooldown must be 0..60 seconds",
                    *span,
                )),
            _ => {}
        }
    }
    if ds.is_empty() {
        Ok(())
    } else {
        Err(sort(ds, source))
    }
}
fn finite_between(v: f64, min: f64, max: f64) -> bool {
    v.is_finite() && v >= min && v <= max
}
