package dev.mintychochip.wizardry.common.dsl.lexer;

import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Span;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Lexer {
    private Lexer() {}

    public record Token(Kind kind, String text, Span span) {
        public enum Kind { WORD, NUMBER, SYMBOL, EOF }
    }

    public record Result(List<Token> tokens, List<Diagnostic> diagnostics) {
        public Result {
            tokens = List.copyOf(tokens);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public static Result lex(String source) {
        var tokens = new ArrayList<Token>();
        var diagnostics = new ArrayList<Diagnostic>();
        int pos = 0;
        int line = 1;
        int column = 1;
        while (pos < source.length()) {
            int cp = source.codePointAt(pos);
            int width = Character.charCount(cp);
            int byteStart = utf8Length(source, 0, pos);
            if (Character.isWhitespace(cp)) {
                if (cp == '\n') { line++; column = 1; } else { column++; }
                pos += width;
                continue;
            }
            if (cp == '{' || cp == '}' || cp == ';') {
                tokens.add(new Token(Token.Kind.SYMBOL, new String(Character.toChars(cp)),
                        new Span(byteStart, byteStart + utf8Length(source, pos, pos + width), line, column)));
                pos += width; column++;
                continue;
            }
            if (isAsciiLetter(cp) || cp == '_') {
                int start = pos; int startByte = byteStart; int startColumn = column;
                pos += width; column++;
                while (pos < source.length()) {
                    int next = source.codePointAt(pos);
                    if (!isAsciiLetter(next) && !isAsciiDigit(next) && next != '_') break;
                    pos += Character.charCount(next); column++;
                }
                tokens.add(new Token(Token.Kind.WORD, source.substring(start, pos),
                        new Span(startByte, utf8Length(source, 0, pos), line, startColumn)));
                continue;
            }
            if (isAsciiDigit(cp) || cp == '.') {
                int start = pos; int startByte = byteStart; int startColumn = column; int dots = 0;
                while (pos < source.length()) {
                    int next = source.codePointAt(pos);
                    if (!isAsciiDigit(next) && next != '.') break;
                    if (next == '.') dots++;
                    pos += Character.charCount(next); column++;
                }
                String text = source.substring(start, pos);
                Span span = new Span(startByte, utf8Length(source, 0, pos), line, startColumn);
                if (dots > 1 || text.equals(".")) diagnostics.add(new Diagnostic("E0003", "malformed number", span));
                else tokens.add(new Token(Token.Kind.NUMBER, text, span));
                continue;
            }
            Span span = new Span(byteStart, byteStart + widthBytes(cp), line, column);
            diagnostics.add(new Diagnostic("E0002", "unexpected character `" + new String(Character.toChars(cp)) + "`", span));
            pos += width; column++;
        }
        int end = utf8Length(source, 0, source.length());
        tokens.add(new Token(Token.Kind.EOF, "", new Span(end, end, line, column)));
        diagnostics.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte())
                .thenComparing(Diagnostic::code).thenComparing(Diagnostic::message));
        return new Result(tokens, List.copyOf(diagnostics));
    }

    private static boolean isAsciiLetter(int cp) { return cp < 128 && Character.isAlphabetic(cp); }
    private static boolean isAsciiDigit(int cp) { return cp >= '0' && cp <= '9'; }
    private static int widthBytes(int cp) { return new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length; }
    private static int utf8Length(String source, int from, int to) {
        return source.substring(from, to).getBytes(StandardCharsets.UTF_8).length;
    }
}
