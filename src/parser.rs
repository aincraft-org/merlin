use crate::{
    ast::*,
    diagnostic::{sort, Diagnostic},
    lexer::{Token, TokenKind},
    span::Span,
};

pub struct Parser<'a> {
    tokens: Vec<Token>,
    pos: usize,
    diagnostics: Vec<Diagnostic>,
    source: &'a str,
}
impl<'a> Parser<'a> {
    pub fn new(mut tokens: Vec<Token>, source: &'a str) -> Self {
        if tokens.is_empty() {
            tokens.push(Token {
                kind: TokenKind::Eof,
                span: Span::new(0, 0),
            });
        }
        Self {
            tokens,
            pos: 0,
            diagnostics: Vec::new(),
            source,
        }
    }

    fn cur(&self) -> &Token {
        self.tokens
            .get(self.pos)
            .unwrap_or_else(|| self.tokens.last().expect("parser always has EOF token"))
    }
    fn take(&mut self) -> Token {
        let t = self.cur().clone();
        if self.pos + 1 < self.tokens.len() {
            self.pos += 1;
        }
        t
    }
    fn word(&mut self, expected: &str) -> Option<Token> {
        match &self.cur().kind {
            TokenKind::Word(w) if w == expected => Some(self.take()),
            _ => {
                let t = self.cur().clone();
                self.diagnostics.push(Diagnostic::error(
                    "E0100",
                    format!("expected `{expected}`"),
                    t.span,
                ));
                None
            }
        }
    }

    fn number(&mut self) -> Option<(f64, Span)> {
        let t = self.take();
        if let TokenKind::Number(s) = t.kind {
            match s.parse::<f64>() {
                Ok(value) if value.is_finite() => Some((value, t.span)),
                _ => {
                    self.diagnostics
                        .push(Diagnostic::error("E0101", "invalid number", t.span));
                    None
                }
            }
        } else {
            self.diagnostics
                .push(Diagnostic::error("E0101", "expected number", t.span));
            None
        }
    }
    fn symbol(&mut self, c: char) {
        if !matches!(self.cur().kind, TokenKind::Symbol(x) if x == c) {
            let t = self.cur().clone();
            self.diagnostics.push(Diagnostic::error(
                "E0102",
                format!("expected `{c}`"),
                t.span,
            ));
        } else {
            self.take();
        }
    }
    pub fn parse(mut self) -> Result<SpellProgram, Vec<Diagnostic>> {
        let start = self.cur().span.start;
        self.word("spell");
        let name_token = self.take();
        let name = match name_token.kind {
            TokenKind::Word(w) => w,
            _ => {
                self.diagnostics.push(Diagnostic::error(
                    "E0103",
                    "expected spell name",
                    name_token.span,
                ));
                String::new()
            }
        };
        self.symbol('{');
        let mut statements = Vec::new();
        while !matches!(self.cur().kind, TokenKind::Symbol('}') | TokenKind::Eof) {
            let st_start = self.cur().span.start;
            let kind_token = self.take();
            let kind = match kind_token.kind {
                TokenKind::Word(w) => w,
                _ => {
                    self.diagnostics.push(Diagnostic::error(
                        "E0104",
                        "expected statement",
                        kind_token.span,
                    ));
                    self.recover_statement();
                    continue;
                }
            };
            let statement = match kind.as_str() {
                "target" => {
                    self.word("ray");
                    self.number().map(|(v, s)| {
                        self.symbol(';');
                        Statement::TargetRay {
                            range: v,
                            span: Span::new(st_start, s.end),
                        }
                    })
                }
                "damage" => {
                    let target = self.parse_target();
                    let dtype = self.parse_damage_type();
                    self.number().map(|(v, s)| {
                        self.symbol(';');
                        Statement::Damage {
                            target,
                            kind: dtype,
                            amount: v,
                            span: Span::new(st_start, s.end),
                        }
                    })
                }
                "heal" => {
                    let target = self.parse_target();
                    self.number().map(|(v, s)| {
                        self.symbol(';');
                        Statement::Heal {
                            target,
                            amount: v,
                            span: Span::new(st_start, s.end),
                        }
                    })
                }
                "push" => {
                    let target = self.parse_target();
                    self.number().map(|(v, s)| {
                        self.symbol(';');
                        Statement::Push {
                            target,
                            strength: v,
                            span: Span::new(st_start, s.end),
                        }
                    })
                }
                "cooldown" => {
                    let (v, s) = self.number().unwrap_or((0.0, self.cur().span));
                    self.word("s");
                    self.symbol(';');
                    Some(Statement::Cooldown {
                        seconds: v,
                        span: Span::new(st_start, s.end),
                    })
                }
                _ => {
                    self.diagnostics.push(Diagnostic::error(
                        "E0105",
                        format!("unsupported statement `{kind}`"),
                        Span::new(st_start, kind_token.span.end),
                    ));
                    self.recover_statement();
                    None
                }
            };
            if let Some(s) = statement {
                statements.push(s);
            }
        }
        self.symbol('}');
        if !matches!(self.cur().kind, TokenKind::Eof) {
            self.diagnostics.push(Diagnostic::error(
                "E0106",
                "unexpected trailing input",
                self.cur().span,
            ));
        }
        if self.diagnostics.is_empty() {
            Ok(SpellProgram {
                name,
                statements,
                span: Span::new(start, self.cur().span.end),
            })
        } else {
            Err(sort(self.diagnostics, self.source))
        }
    }
    fn recover_statement(&mut self) {
        while !matches!(
            self.cur().kind,
            TokenKind::Symbol(';') | TokenKind::Symbol('}') | TokenKind::Eof
        ) {
            self.take();
        }
        if matches!(self.cur().kind, TokenKind::Symbol(';')) {
            self.take();
        }
    }
    fn parse_target(&mut self) -> Target {
        match &self.cur().kind {
            TokenKind::Word(w) if w == "target" => {
                self.take();
                Target::Target
            }
            TokenKind::Word(w) if w == "self" => {
                self.take();
                Target::SelfTarget
            }
            _ => {
                self.diagnostics.push(Diagnostic::error(
                    "E0107",
                    "expected target selector",
                    self.cur().span,
                ));
                Target::Target
            }
        }
    }
    fn parse_damage_type(&mut self) -> DamageType {
        let t = self.take();
        match t.kind {
            TokenKind::Word(w) => match w.as_str() {
                "physical" => DamageType::Physical,
                "fire" => DamageType::Fire,
                "frost" => DamageType::Frost,
                "arcane" => DamageType::Arcane,
                _ => {
                    self.diagnostics.push(Diagnostic::error(
                        "E0108",
                        "unsupported damage type",
                        t.span,
                    ));
                    DamageType::Physical
                }
            },
            _ => {
                self.diagnostics
                    .push(Diagnostic::error("E0108", "expected damage type", t.span));
                DamageType::Physical
            }
        }
    }
}
