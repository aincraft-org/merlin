package dev.mintychochip.merlin.common.dsl.lexer;

import dev.mintychochip.merlin.api.dsl.Diagnostic;
import dev.mintychochip.merlin.api.dsl.Span;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Lexer {
    private Lexer() {}

    public record Token(Kind kind, String text, Span span) {
        public enum Kind { WORD, NUMBER }
    }

    public record Line(int indentSpaces, List<Token> tokens, Span span) {
        public Line {
            tokens = List.copyOf(tokens);
        }
    }

    public record Result(List<Line> lines, List<Diagnostic> diagnostics) {
        public Result {
            lines = List.copyOf(lines);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public static Result lex(String source) {
        var lines = new ArrayList<Line>();
        var diagnostics = new ArrayList<Diagnostic>();
        int pos = 0;
        int line = 1;
        while (pos < source.length()) {
            int lineStartByte = utf8Length(source, 0, pos);
            int indentSpaces = 0;
            boolean tabIndent = false;
            int tabStart = pos;
            while (pos < source.length()) {
                int cp = source.codePointAt(pos);
                if (cp == ' ') {
                    indentSpaces++;
                    pos++;
                    continue;
                }
                if (cp == '\t') {
                    if (!tabIndent) {
                        tabIndent = true;
                        tabStart = pos;
                    }
                    pos++;
                    continue;
                }
                break;
            }
            if (tabIndent) {
                int tabByte = utf8Length(source, 0, tabStart);
                diagnostics.add(new Diagnostic("E0200", "tab in indent",
                        new Span(tabByte, tabByte + 1, line, indentSpaces + 1)));
                pos = skipToNewline(source, pos);
                if (pos < source.length() && source.charAt(pos) == '\n') {
                    pos++;
                    line++;
                }
                continue;
            }
            if (pos >= source.length() || source.charAt(pos) == '\n') {
                if (pos < source.length()) {
                    pos++;
                    line++;
                }
                continue;
            }
            if (indentSpaces % 4 != 0) {
                int indentEndByte = utf8Length(source, 0, pos);
                diagnostics.add(new Diagnostic("E0201", "indent is not a multiple of 4 spaces",
                        new Span(lineStartByte, indentEndByte, line, 1)));
                pos = skipToNewline(source, pos);
                if (pos < source.length() && source.charAt(pos) == '\n') {
                    pos++;
                    line++;
                }
                continue;
            }
            var tokens = new ArrayList<Token>();
            int column = indentSpaces + 1;
            while (pos < source.length()) {
                int cp = source.codePointAt(pos);
                if (cp == '\n') {
                    break;
                }
                if (cp == '\r' || cp == ' ' || cp == '\t') {
                    pos++;
                    column++;
                    continue;
                }
                int byteStart = utf8Length(source, 0, pos);
                int width = Character.charCount(cp);
                if (isAsciiLetter(cp)) {
                    int start = pos;
                    int startColumn = column;
                    boolean uppercase = isAsciiUpper(cp);
                    pos += width;
                    column++;
                    while (pos < source.length()) {
                        int next = source.codePointAt(pos);
                        if (!isAsciiLetter(next)) {
                            break;
                        }
                        if (isAsciiUpper(next)) {
                            uppercase = true;
                        }
                        pos += Character.charCount(next);
                        column++;
                    }
                    String text = source.substring(start, pos);
                    Span span = new Span(byteStart, utf8Length(source, 0, pos), line, startColumn);
                    tokens.add(new Token(Token.Kind.WORD, text, span));
                    if (uppercase) {
                        diagnostics.add(new Diagnostic("E0202", "uppercase letter", span));
                    }
                    continue;
                }
                if (isAsciiDigit(cp) || cp == '.') {
                    int start = pos;
                    int startColumn = column;
                    int dots = 0;
                    while (pos < source.length()) {
                        int next = source.codePointAt(pos);
                        if (!isAsciiDigit(next) && next != '.') {
                            break;
                        }
                        if (next == '.') {
                            dots++;
                        }
                        pos += Character.charCount(next);
                        column++;
                    }
                    String text = source.substring(start, pos);
                    Span span = new Span(byteStart, utf8Length(source, 0, pos), line, startColumn);
                    if (dots > 1 || text.equals(".")) {
                        diagnostics.add(new Diagnostic("E0003", "malformed number", span));
                    } else {
                        tokens.add(new Token(Token.Kind.NUMBER, text, span));
                    }
                    continue;
                }
                Span span = new Span(byteStart, byteStart + widthBytes(cp), line, column);
                diagnostics.add(new Diagnostic(
                        "E0002",
                        "unexpected character `" + new String(Character.toChars(cp)) + "`",
                        span));
                pos += width;
                column++;
            }
            if (!tokens.isEmpty()) {
                int endByte = tokens.getLast().span().endByte();
                lines.add(new Line(indentSpaces, tokens, new Span(lineStartByte, endByte, line, 1)));
            }
            if (pos < source.length() && source.charAt(pos) == '\n') {
                pos++;
                line++;
            }
        }
        diagnostics.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte())
                .thenComparing(Diagnostic::code)
                .thenComparing(Diagnostic::message));
        return new Result(lines, diagnostics);
    }

    private static int skipToNewline(String source, int pos) {
        while (pos < source.length() && source.charAt(pos) != '\n') {
            pos++;
        }
        return pos;
    }

    private static boolean isAsciiLetter(int cp) {
        return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z');
    }

    private static boolean isAsciiUpper(int cp) {
        return cp >= 'A' && cp <= 'Z';
    }

    private static boolean isAsciiDigit(int cp) {
        return cp >= '0' && cp <= '9';
    }

    private static int widthBytes(int cp) {
        return new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
    }

    private static int utf8Length(String source, int from, int to) {
        return source.substring(from, to).getBytes(StandardCharsets.UTF_8).length;
    }
}
