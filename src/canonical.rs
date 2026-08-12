use crate::{ast::*, COMPILER_VERSION};
use sha2::{Digest, Sha256};

fn target_name(target: Target) -> &'static str {
    match target {
        Target::SelfTarget => "self",
        Target::Target => "target",
    }
}

fn damage_type_name(kind: DamageType) -> &'static str {
    match kind {
        DamageType::Physical => "physical",
        DamageType::Fire => "fire",
        DamageType::Frost => "frost",
        DamageType::Arcane => "arcane",
    }
}

fn number_bits(value: f64) -> String {
    format!("{:016x}", value.to_bits())
}

pub fn canonicalize(p: &SpellProgram) -> Vec<u8> {
    let mut lines = vec![format!("{}|spell|{}|", COMPILER_VERSION, p.name)];
    for st in &p.statements {
        let line = match st {
            Statement::TargetRay { range, .. } => {
                format!("target_ray|{}", number_bits(*range))
            }
            Statement::Damage {
                target,
                kind,
                amount,
                ..
            } => format!(
                "damage|{}|{}|{}",
                target_name(*target),
                damage_type_name(*kind),
                number_bits(*amount)
            ),
            Statement::Heal { target, amount, .. } => {
                format!("heal|{}|{}", target_name(*target), number_bits(*amount))
            }
            Statement::Push {
                target, strength, ..
            } => format!("push|{}|{}", target_name(*target), number_bits(*strength)),
            Statement::Cooldown { seconds, .. } => {
                format!("cooldown|{}", number_bits(*seconds))
            }
        };
        lines.push(line);
    }
    lines.join("\n").into_bytes()
}

pub fn identity(canonical: &[u8]) -> String {
    let mut h = Sha256::new();
    h.update(canonical);
    hex::encode(h.finalize())
}
