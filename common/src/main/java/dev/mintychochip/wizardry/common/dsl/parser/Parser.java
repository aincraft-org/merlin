package dev.mintychochip.wizardry.common.dsl.parser;

import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Operation;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.api.dsl.Program;
import dev.mintychochip.wizardry.api.dsl.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Parser {
    public record Result(java.util.Optional<Program> program, List<Diagnostic> diagnostics) {
        public Result {
            program = java.util.Objects.requireNonNull(program, "program");
            diagnostics = List.copyOf(diagnostics);
        }
    }

    private final List<Lexer.Token> tokens;
    private final String source;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int pos;

    private Parser(List<Lexer.Token> tokens, String source) {
        this.tokens = tokens.isEmpty()
                ? List.of(new Lexer.Token(Lexer.Token.Kind.EOF, "", new Span(0, 0, 1, 1)))
                : tokens;
        this.source = source;
    }

    public static Result parse(List<Lexer.Token> tokens, String source) {
        return new Parser(tokens, source).parseProgram();
    }

    private Lexer.Token current() { return tokens.get(Math.min(pos, tokens.size() - 1)); }
    private Lexer.Token take() { var token = current(); if (pos < tokens.size() - 1) pos++; return token; }

    private boolean word(String expected) {
        if (current().kind() == Lexer.Token.Kind.WORD && current().text().equals(expected)) { take(); return true; }
        var t = current(); diagnostics.add(new Diagnostic("E0100", "expected `" + expected + "`", t.span())); return false;
    }

    private Double number() {
        var t = take();
        if (t.kind() != Lexer.Token.Kind.NUMBER) {
            diagnostics.add(new Diagnostic("E0101", "expected number", t.span())); return null;
        }
        try {
            double value = Double.parseDouble(t.text());
            if (!Double.isFinite(value)) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) {
            diagnostics.add(new Diagnostic("E0101", "invalid number", t.span())); return null;
        }
    }

    private Span numberSpan() { return tokens.get(Math.max(0, pos - 1)).span(); }

    private void symbol(char expected) {
        if (current().kind() == Lexer.Token.Kind.SYMBOL && current().text().charAt(0) == expected) take();
        else diagnostics.add(new Diagnostic("E0102", "expected `" + expected + "`", current().span()));
    }

    private Result parseProgram() {
        int start = current().span().startByte();
        word("spell");
        var nameToken = take();
        String name;
        if (nameToken.kind() == Lexer.Token.Kind.WORD) name = nameToken.text();
        else { diagnostics.add(new Diagnostic("E0103", "expected spell name", nameToken.span())); name = ""; }
        symbol('{');
        var statements = new ArrayList<Statement>();
        while (current().kind() != Lexer.Token.Kind.EOF
                && !(current().kind() == Lexer.Token.Kind.SYMBOL && current().text().equals("}"))) {
            int statementStart = current().span().startByte();
            var kindToken = take();
            if (kindToken.kind() != Lexer.Token.Kind.WORD) {
                diagnostics.add(new Diagnostic("E0104", "expected statement", kindToken.span())); recover(); continue;
            }
            Statement statement = switch (kindToken.text()) {
                case "target" -> parseTarget(statementStart);
                case "damage" -> parseDamage(statementStart);
                case "heal" -> parseHeal(statementStart);
                case "push" -> parsePush(statementStart);
                case "cooldown" -> parseCooldown(statementStart);
                default -> { diagnostics.add(new Diagnostic("E0105", "unsupported statement `" + kindToken.text() + "`",
                        new Span(statementStart, kindToken.span().endByte(), kindToken.span().line(), kindToken.span().column()))); recover(); yield null; }
            };
            if (statement != null) statements.add(statement);
        }
        symbol('}');
        if (current().kind() != Lexer.Token.Kind.EOF)
            diagnostics.add(new Diagnostic("E0106", "unexpected trailing input", current().span()));
        diagnostics.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte())
                .thenComparing(Diagnostic::code).thenComparing(Diagnostic::message));
        if (!diagnostics.isEmpty()) return new Result(java.util.Optional.empty(), diagnostics);
        return new Result(java.util.Optional.of(new Program(name, statements,
                new Span(start, current().span().endByte(), 1, 1))), List.of());
    }

    private Statement parseTarget(int start) {
        word("ray"); Double value = number(); if (value == null) { recover(); return null; }
        Span end = numberSpan(); symbol(';'); return new Statement.TargetRay(value, span(start, end));
    }
    private Statement parseDamage(int start) {
        Operation.Target target = parseTargetSelector(); Operation.DamageType type = parseDamageType();
        Double value = number(); if (value == null) { recover(); return null; }
        Span end = numberSpan(); symbol(';'); return new Statement.Damage(target, type, value, span(start, end));
    }
    private Statement parseHeal(int start) {
        Operation.Target target = parseTargetSelector(); Double value = number();
        if (value == null) { recover(); return null; } Span end = numberSpan(); symbol(';');
        return new Statement.Heal(target, value, span(start, end));
    }
    private Statement parsePush(int start) {
        Operation.Target target = parseTargetSelector(); Double value = number();
        if (value == null) { recover(); return null; } Span end = numberSpan(); symbol(';');
        return new Statement.Push(target, value, span(start, end));
    }
    private Statement parseCooldown(int start) {
        Double value = number(); Span end = value == null ? current().span() : numberSpan();
        word("s"); symbol(';'); return new Statement.Cooldown(value == null ? 0.0 : value, span(start, end));
    }
    private Operation.Target parseTargetSelector() {
        if (current().kind() == Lexer.Token.Kind.WORD && current().text().equals("target")) { take(); return Operation.Target.TARGET; }
        if (current().kind() == Lexer.Token.Kind.WORD && current().text().equals("self")) { take(); return Operation.Target.SELF; }
        diagnostics.add(new Diagnostic("E0107", "expected target selector", current().span())); return Operation.Target.TARGET;
    }
    private Operation.DamageType parseDamageType() {
        var t = take();
        if (t.kind() == Lexer.Token.Kind.WORD) {
            try { return Operation.DamageType.valueOf(t.text().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        diagnostics.add(new Diagnostic("E0108", t.kind() == Lexer.Token.Kind.WORD ? "unsupported damage type" : "expected damage type", t.span()));
        return Operation.DamageType.PHYSICAL;
    }
    private void recover() {
        while (current().kind() != Lexer.Token.Kind.EOF
                && !(current().kind() == Lexer.Token.Kind.SYMBOL && (current().text().equals(";") || current().text().equals("}")))) take();
        if (current().kind() == Lexer.Token.Kind.SYMBOL && current().text().equals(";")) take();
    }
    private Span span(int start, Span end) { return new Span(start, end.endByte(), end.line(), end.column()); }
}
