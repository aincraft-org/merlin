use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct Span {
    pub start: usize,
    pub end: usize,
}
impl Span {
    pub const fn new(start: usize, end: usize) -> Self {
        Self { start, end }
    }
    pub fn line_col(self, source: &str) -> (usize, usize) {
        let start = self.start.min(source.len());
        let bytes = source.as_bytes();
        let line_start = bytes[..start]
            .iter()
            .rposition(|b| *b == b'\n')
            .map_or(0, |i| i + 1);
        let line = bytes[..start].iter().filter(|b| **b == b'\n').count() + 1;
        let column = String::from_utf8_lossy(&bytes[line_start..start])
            .chars()
            .count()
            + 1;
        (line, column)
    }
}
