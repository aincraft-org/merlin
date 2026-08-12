use crate::span::Span;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Diagnostic {
    pub code: String,
    pub message: String,
    pub span: Span,
    pub line: usize,
    pub column: usize,
}
impl Diagnostic {
    pub fn error(code: &str, message: impl Into<String>, span: Span) -> Self {
        Self {
            code: code.into(),
            message: message.into(),
            span,
            line: 0,
            column: 0,
        }
    }
    pub fn with_source(mut self, source: &str) -> Self {
        (self.line, self.column) = self.span.line_col(source);
        self
    }
}
pub fn sort(mut diagnostics: Vec<Diagnostic>, source: &str) -> Vec<Diagnostic> {
    for diagnostic in &mut diagnostics {
        (diagnostic.line, diagnostic.column) = diagnostic.span.line_col(source);
    }
    diagnostics.sort_by(|a, b| {
        (a.span.start, &a.code, &a.message).cmp(&(b.span.start, &b.code, &b.message))
    });
    diagnostics.truncate(32);
    diagnostics
}
