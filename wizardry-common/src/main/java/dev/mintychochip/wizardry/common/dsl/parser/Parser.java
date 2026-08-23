package dev.mintychochip.wizardry.common.dsl.parser;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Page;
import dev.mintychochip.wizardry.api.dsl.Phrase;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Parser {
    private static final Set<String> RESERVED = Set.of(
            "when", "if", "spell", "damage", "heal", "push", "cooldown", "upon", "the", "a", "call", "forth");

    public record Result(Optional<Page> page, List<Diagnostic> diagnostics) {
        public Result {
            page = Objects.requireNonNull(page, "page");
            diagnostics = List.copyOf(diagnostics);
        }
    }

    private final List<Lexer.Line> lines;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final List<Phrase> phrases = new ArrayList<>();

    private Lexer.Line line;
    private int index;

    private Parser(Lexer.Result lexed) {
        this.lines = lexed.lines();
    }

    public static Result parse(Lexer.Result lexed, String source) {
        Objects.requireNonNull(source, "source");
        if (!lexed.diagnostics().isEmpty()) {
            return new Result(Optional.empty(), lexed.diagnostics());
        }
        return new Parser(lexed).parsePage();
    }

    private Result parsePage() {
        for (var next : lines) {
            line = next;
            index = 0;
            parseLine();
        }
        diagnostics.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte())
                .thenComparing(Diagnostic::code)
                .thenComparing(Diagnostic::message));
        if (!diagnostics.isEmpty()) {
            return new Result(Optional.empty(), diagnostics);
        }
        Span span = phrases.isEmpty()
                ? new Span(0, 0, 1, 1)
                : new Span(
                        phrases.getFirst().span().startByte(),
                        phrases.getLast().span().endByte(),
                        phrases.getFirst().span().line(),
                        phrases.getFirst().span().column());
        return new Result(Optional.of(new Page(phrases, span)), List.of());
    }

    private void parseLine() {
        int depth = line.indentSpaces() / 4;
        if (line.indentSpaces() >= 8 || depth > 1) {
            diagnostics.add(error("E0216", "indent depth greater than 1", firstSpan()));
            return;
        }
        Lexer.Token first = peek();
        if (first == null || first.kind() != Lexer.Token.Kind.WORD) {
            diagnostics.add(error("E0210", "expected verb", first == null ? line.span() : first.span()));
            return;
        }
        if (depth == 1) {
            parseIndented(first);
            return;
        }
        parseTopLevel(first);
    }

    private void parseIndented(Lexer.Token first) {
        String word = first.text();
        if (word.equals("look") || word.equals("rest")) {
            take();
            diagnostics.add(error("E0219", "look or rest cannot be indented", first.span()));
            return;
        }
        if (word.equals("send")) {
            take();
            diagnostics.add(error("E0220", "send cannot be indented", first.span()));
            return;
        }
        if (RESERVED.contains(word)) {
            take();
            diagnostics.add(error("E0222", "reserved word `" + word + "`", first.span()));
            return;
        }
        if (!word.equals("riding")) {
            take();
            diagnostics.add(error("E0218", "indented line is not riding", first.span()));
            return;
        }
        parseRiding();
    }

    private void parseRiding() {
        Lexer.Token ridingToken = take();
        if (phrases.isEmpty() || !(phrases.getLast() instanceof Phrase.Summon summon) || summon.riding() != null) {
            diagnostics.add(error("E0217", "riding is not indented under a summon", ridingToken.span()));
            return;
        }
        Action.Noun noun = takeNoun();
        if (noun == null) {
            return;
        }
        rejectExtra();
        Span span = new Span(
                summon.span().startByte(),
                previous().span().endByte(),
                summon.span().line(),
                summon.span().column());
        phrases.set(phrases.size() - 1, new Phrase.Summon(summon.noun(), summon.place(), noun, span));
    }

    private void parseTopLevel(Lexer.Token first) {
        String word = first.text();
        if (word.equals("riding")) {
            take();
            diagnostics.add(error("E0217", "riding is not indented under a summon", first.span()));
            return;
        }
        if (RESERVED.contains(word)) {
            take();
            diagnostics.add(error("E0222", "reserved word `" + word + "`", first.span()));
            return;
        }
        int start = first.span().startByte();
        Phrase phrase = switch (word) {
            case "look" -> parseLook(start);
            case "summon" -> parseSummon(start);
            case "burn" -> parseBurn(start);
            case "mend" -> parseMend(start);
            case "shove" -> parseShove(start);
            case "strike" -> parseStrike(start);
            case "send" -> parseSend(start);
            case "vanish" -> parseVanish(start);
            case "rest" -> parseRest(start);
            default -> {
                take();
                diagnostics.add(error("E0105", "unknown word `" + word + "`", first.span()));
                yield null;
            }
        };
        if (phrase != null) {
            rejectExtra();
            phrases.add(phrase);
        }
    }

    private Phrase parseLook(int start) {
        take();
        if (!takeWord("ahead")) {
            diagnostics.add(error("E0214", "expected `ahead`", currentSpan()));
            return null;
        }
        Double range = takeNumber();
        if (range == null) {
            return null;
        }
        return new Phrase.LookAhead(range, span(start));
    }

    private Phrase parseSummon(int start) {
        take();
        Action.Noun noun = takeNoun();
        if (noun == null) {
            return null;
        }
        Action.Place place = new Action.Place.Caster();
        if (peekWord("at")) {
            take();
            place = takePlace();
            if (place == null) {
                return null;
            }
        }
        return new Phrase.Summon(noun, place, null, span(start));
    }

    private Phrase parseBurn(int start) {
        take();
        Action.Patient patient = takePatient();
        if (patient == null) {
            return null;
        }
        double amount = optionalNumber(1.0);
        return new Phrase.Burn(patient, amount, span(start));
    }

    private Phrase parseMend(int start) {
        take();
        Action.Patient patient = takePatient();
        if (patient == null) {
            return null;
        }
        double amount = optionalNumber(1.0);
        return new Phrase.Mend(patient, amount, span(start));
    }

    private Phrase parseShove(int start) {
        take();
        Action.Patient patient = takePatient();
        if (patient == null) {
            return null;
        }
        double amount = optionalNumber(1.0);
        return new Phrase.Shove(patient, amount, span(start));
    }

    private Phrase parseStrike(int start) {
        take();
        Lexer.Token next = peek();
        if (next == null) {
            diagnostics.add(error("E0221", "strike requires a patient or at place", previous().span()));
            return null;
        }
        if (next.kind() == Lexer.Token.Kind.WORD && next.text().equals("at")) {
            take();
            Action.Place place = takePlace();
            if (place == null) {
                return null;
            }
            return new Phrase.Strike(place, span(start));
        }
        if (next.kind() == Lexer.Token.Kind.WORD && (next.text().equals("self") || next.text().equals("target"))) {
            Action.Patient patient = takePatient();
            Action.Place place = patient == Action.Patient.SELF
                    ? new Action.Place.Self()
                    : new Action.Place.Target();
            return new Phrase.Strike(place, span(start));
        }
        diagnostics.add(error("E0221", "strike requires a patient or at place", next.span()));
        return null;
    }

    private Phrase parseSend(int start) {
        take();
        if (!takeWord("skyward")) {
            diagnostics.add(error("E0214", "expected `skyward`", currentSpan()));
            return null;
        }
        return new Phrase.SendSkyward(span(start));
    }

    private Phrase parseVanish(int start) {
        take();
        Action.Patient patient = takePatient();
        if (patient == null) {
            return null;
        }
        if (!takeWord("for")) {
            diagnostics.add(error("E0214", "expected `for`", currentSpan()));
            return null;
        }
        Double seconds = takeNumber();
        if (seconds == null) {
            return null;
        }
        if (!takeWord("seconds")) {
            diagnostics.add(error("E0214", "expected `seconds`", currentSpan()));
            return null;
        }
        return new Phrase.Vanish(patient, seconds, span(start));
    }

    private Phrase parseRest(int start) {
        take();
        Double seconds = takeNumber();
        if (seconds == null) {
            return null;
        }
        if (!takeWord("seconds")) {
            diagnostics.add(error("E0214", "expected `seconds`", currentSpan()));
            return null;
        }
        return new Phrase.Rest(seconds, span(start));
    }

    private Action.Noun takeNoun() {
        Lexer.Token token = peek();
        if (token == null || token.kind() != Lexer.Token.Kind.WORD) {
            diagnostics.add(error("E0211", "expected summon noun", currentSpan()));
            return null;
        }
        Action.Noun noun = switch (token.text()) {
            case "sheep" -> Action.Noun.SHEEP;
            case "rocket" -> Action.Noun.ROCKET;
            case "fangs" -> Action.Noun.FANGS;
            default -> null;
        };
        if (noun == null) {
            diagnostics.add(error("E0211", "expected summon noun", token.span()));
            return null;
        }
        take();
        return noun;
    }

    private Action.Patient takePatient() {
        Lexer.Token token = peek();
        if (token == null || token.kind() != Lexer.Token.Kind.WORD) {
            diagnostics.add(error("E0212", "expected patient", currentSpan()));
            return null;
        }
        Action.Patient patient = switch (token.text()) {
            case "self" -> Action.Patient.SELF;
            case "target" -> Action.Patient.TARGET;
            default -> null;
        };
        if (patient == null) {
            diagnostics.add(error("E0212", "expected patient", token.span()));
            return null;
        }
        take();
        return patient;
    }

    private Action.Place takePlace() {
        Lexer.Token token = peek();
        if (token == null || token.kind() != Lexer.Token.Kind.WORD) {
            diagnostics.add(error("E0213", "expected place", currentSpan()));
            return null;
        }
        return switch (token.text()) {
            case "caster" -> {
                take();
                yield new Action.Place.Caster();
            }
            case "self" -> {
                take();
                yield new Action.Place.Self();
            }
            case "target" -> {
                take();
                yield new Action.Place.Target();
            }
            case "ahead" -> {
                take();
                Double range = takeNumber();
                if (range == null) {
                    yield null;
                }
                yield new Action.Place.Ahead(range);
            }
            default -> {
                diagnostics.add(error("E0213", "expected place", token.span()));
                yield null;
            }
        };
    }

    private Double takeNumber() {
        Lexer.Token token = peek();
        if (token == null || token.kind() != Lexer.Token.Kind.NUMBER) {
            diagnostics.add(error("E0101", "expected number", currentSpan()));
            return null;
        }
        take();
        try {
            double value = Double.parseDouble(token.text());
            if (!Double.isFinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            diagnostics.add(error("E0101", "invalid number", token.span()));
            return null;
        }
    }

    private double optionalNumber(double fallback) {
        Lexer.Token token = peek();
        if (token != null && token.kind() == Lexer.Token.Kind.NUMBER) {
            Double value = takeNumber();
            return value == null ? fallback : value;
        }
        return fallback;
    }

    private void rejectExtra() {
        Lexer.Token extra = peek();
        if (extra != null) {
            diagnostics.add(error("E0215", "extra tokens", extra.span()));
        }
    }

    private boolean peekWord(String expected) {
        Lexer.Token token = peek();
        return token != null && token.kind() == Lexer.Token.Kind.WORD && token.text().equals(expected);
    }

    private boolean takeWord(String expected) {
        if (peekWord(expected)) {
            take();
            return true;
        }
        return false;
    }

    private Lexer.Token peek() {
        if (index >= line.tokens().size()) {
            return null;
        }
        return line.tokens().get(index);
    }

    private Lexer.Token take() {
        var token = peek();
        if (token != null) {
            index++;
        }
        return token;
    }

    private Lexer.Token previous() {
        return line.tokens().get(index - 1);
    }

    private Span firstSpan() {
        return line.tokens().isEmpty() ? line.span() : line.tokens().getFirst().span();
    }

    private Span currentSpan() {
        Lexer.Token token = peek();
        if (token != null) {
            return token.span();
        }
        if (index > 0) {
            var last = previous().span();
            return new Span(last.endByte(), last.endByte(), last.line(), last.column());
        }
        return line.span();
    }

    private Span span(int start) {
        var end = previous().span();
        return new Span(start, end.endByte(), end.line(), end.column());
    }

    private static Diagnostic error(String code, String message, Span span) {
        return new Diagnostic(code, message, span);
    }
}
