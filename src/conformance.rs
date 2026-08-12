use crate::{ast::*, compile};
use serde_json::{json, Value};

pub const SCHEMA_VERSION: u64 = 1;

pub fn fixture(id: &str, source: &str) -> Value {
    match compile(source) {
        Ok(spell) => {
            let operations = spell
                .operations()
                .iter()
                .map(|operation| {
                    json!({
                        "damageType": operation.damage_type().map(damage_type_name),
                        "opcode": operation.opcode(),
                        "target": operation.target().map(target_name),
                        "valueBits": operation.value().map(|value| format!("{:016x}", value.to_bits())),
                    })
                })
                .collect::<Vec<_>>();
            json!({
                "id": id,
                "result": {
                    "canonicalHex": hex::encode(spell.canonical()),
                    "compilerVersion": spell.compiler_version(),
                    "identitySha256": spell.identity(),
                    "name": spell.name(),
                    "operations": operations,
                    "status": "accepted",
                },
                "schemaVersion": SCHEMA_VERSION,
                "source": source,
            })
        }
        Err(failure) => {
            let diagnostics = failure
                .diagnostics()
                .iter()
                .map(|diagnostic| {
                    json!({
                        "code": diagnostic.code,
                        "column": diagnostic.column,
                        "endByte": diagnostic.span.end,
                        "line": diagnostic.line,
                        "message": diagnostic.message,
                        "startByte": diagnostic.span.start,
                    })
                })
                .collect::<Vec<_>>();
            json!({
                "id": id,
                "result": {
                    "diagnostics": diagnostics,
                    "status": "rejected",
                },
                "schemaVersion": SCHEMA_VERSION,
                "source": source,
            })
        }
    }
}

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
