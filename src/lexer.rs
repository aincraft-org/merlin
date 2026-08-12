use crate::{
    diagnostic::{sort, Diagnostic},
    span::Span,
};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum TokenKind {
    Word(String),
    Number(String),
    Symbol(char),
    Eof,
}
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Token {
    pub kind: TokenKind,
    pub span: Span,
}

pub struct Lexer<'a> {
    source: &'a str,
    pos: usize,
    diagnostics: Vec<Diagnostic>,
}
impl<'a> Lexer<'a> {
    pub fn new(source: &'a str) -> Self {
        Self {
            source,
            pos: 0,
            diagnostics: Vec::new(),
        }
    }
    pub fn lex(mut self) -> Result<Vec<Token>, Vec<Diagnostic>> {
        let bytes = self.source.as_bytes();
        let mut out = Vec::new();
        while self.pos < bytes.len() {
            let c = self.source[self.pos..]
                .chars()
                .next()
                .expect("position is before source end");
            if c.is_ascii_whitespace() {
                self.pos += 1;
                continue;
            }
            if "{};".contains(c) {
                let start = self.pos;
                self.pos += 1;
                out.push(Token {
                    kind: TokenKind::Symbol(c),
                    span: Span::new(start, self.pos),
                });
                continue;
            }
            if c.is_ascii_alphabetic() || c == '_' {
                let start = self.pos;
                self.pos += 1;
                while self.pos < bytes.len()
                    && ((bytes[self.pos] as char).is_ascii_alphanumeric()
                        || bytes[self.pos] as char == '_')
                {
                    self.pos += 1;
                }
                out.push(Token {
                    kind: TokenKind::Word(self.source[start..self.pos].to_string()),
                    span: Span::new(start, self.pos),
                });
                continue;
            }
            if c.is_ascii_digit() || c == '.' {
                let start = self.pos;
                let mut dots = 0;
                while self.pos < bytes.len()
                    && ((bytes[self.pos] as char).is_ascii_digit()
                        || bytes[self.pos] as char == '.')
                {
                    if bytes[self.pos] as char == '.' {
                        dots += 1;
                    }
                    self.pos += 1;
                }
                let text = &self.source[start..self.pos];
                if dots > 1 || text == "." {
                    self.diagnostics.push(Diagnostic::error(
                        "E0003",
                        "malformed number",
                        Span::new(start, self.pos),
                    ));
                } else {
                    out.push(Token {
                        kind: TokenKind::Number(text.to_string()),
                        span: Span::new(start, self.pos),
                    });
                }
                continue;
            }
            let start = self.pos;
            self.pos += c.len_utf8();
            self.diagnostics.push(Diagnostic::error(
                "E0002",
                format!("unexpected character `{c}`"),
                Span::new(start, self.pos),
            ));
        }
        if !self.diagnostics.is_empty() {
            return Err(sort(self.diagnostics, self.source));
        }
        out.push(Token {
            kind: TokenKind::Eof,
            span: Span::new(self.pos, self.pos),
        });
        Ok(out)
    }
}
