use crate::span::Span;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct SpellProgram {
    pub name: String,
    pub statements: Vec<Statement>,
    pub span: Span,
}
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum Statement {
    TargetRay {
        range: f64,
        span: Span,
    },
    Damage {
        target: Target,
        kind: DamageType,
        amount: f64,
        span: Span,
    },
    Heal {
        target: Target,
        amount: f64,
        span: Span,
    },
    Push {
        target: Target,
        strength: f64,
        span: Span,
    },
    Cooldown {
        seconds: f64,
        span: Span,
    },
}
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Target {
    SelfTarget,
    Target,
}
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum DamageType {
    Physical,
    Fire,
    Frost,
    Arcane,
}
impl Statement {
    pub fn span(&self) -> Span {
        match self {
            Self::TargetRay { span, .. }
            | Self::Damage { span, .. }
            | Self::Heal { span, .. }
            | Self::Push { span, .. }
            | Self::Cooldown { span, .. } => *span,
        }
    }
    pub fn is_effect(&self) -> bool {
        matches!(
            self,
            Self::Damage { .. } | Self::Heal { .. } | Self::Push { .. }
        )
    }
}
