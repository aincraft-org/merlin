use crate::{ast::*, canonical::identity, COMPILER_VERSION};
use serde::{Deserialize, Serialize};
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct CompiledSpell {
    compiler_version: String,
    name: String,
    identity: String,
    canonical: Vec<u8>,
    operations: Vec<Operation>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Operation {
    opcode: String,
    target: Option<Target>,
    value: Option<f64>,
    damage_type: Option<DamageType>,
}

impl CompiledSpell {
    pub(crate) fn from_program(p: SpellProgram, canonical: Vec<u8>) -> Self {
        let operations = p
            .statements
            .iter()
            .map(|s| match s {
                Statement::TargetRay { range, .. } => Operation {
                    opcode: "target_ray".into(),
                    target: None,
                    value: Some(*range),
                    damage_type: None,
                },
                Statement::Damage {
                    target,
                    kind,
                    amount,
                    ..
                } => Operation {
                    opcode: "damage".into(),
                    target: Some(*target),
                    value: Some(*amount),
                    damage_type: Some(*kind),
                },
                Statement::Heal { target, amount, .. } => Operation {
                    opcode: "heal".into(),
                    target: Some(*target),
                    value: Some(*amount),
                    damage_type: None,
                },
                Statement::Push {
                    target, strength, ..
                } => Operation {
                    opcode: "push".into(),
                    target: Some(*target),
                    value: Some(*strength),
                    damage_type: None,
                },
                Statement::Cooldown { seconds, .. } => Operation {
                    opcode: "cooldown".into(),
                    target: None,
                    value: Some(*seconds),
                    damage_type: None,
                },
            })
            .collect();
        Self {
            compiler_version: COMPILER_VERSION.into(),
            name: p.name,
            identity: identity(&canonical),
            canonical,
            operations,
        }
    }

    pub fn compiler_version(&self) -> &str {
        &self.compiler_version
    }

    pub fn name(&self) -> &str {
        &self.name
    }

    pub fn identity(&self) -> &str {
        &self.identity
    }

    pub fn canonical(&self) -> &[u8] {
        &self.canonical
    }

    pub fn operations(&self) -> &[Operation] {
        &self.operations
    }
}

impl Operation {
    pub fn opcode(&self) -> &str {
        &self.opcode
    }

    pub fn target(&self) -> Option<Target> {
        self.target
    }

    pub fn value(&self) -> Option<f64> {
        self.value
    }

    pub fn damage_type(&self) -> Option<DamageType> {
        self.damage_type
    }
}
