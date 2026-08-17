package dev.mintychochip.wizardry.common.dsl;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.Compiler;
import dev.mintychochip.wizardry.api.dsl.CompilerConstants;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Page;
import dev.mintychochip.wizardry.api.dsl.Phrase;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.common.dsl.parser.Parser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class ScribeCompiler implements Compiler {
    public static final ScribeCompiler INSTANCE = new ScribeCompiler();

    private ScribeCompiler() {}

    @Override
    public CompileResult compile(String source) {
        var limitDiagnostics = sourceLimitDiagnostics(source);
        if (!limitDiagnostics.isEmpty()) {
            return CompileResult.error(limitDiagnostics);
        }
        var lexed = Lexer.lex(source);
        if (!lexed.diagnostics().isEmpty()) {
            return CompileResult.error(cap(lexed.diagnostics()));
        }
        var parsed = Parser.parse(lexed, source);
        if (parsed.page().isEmpty()) {
            var diagnostics = parsed.diagnostics();
            if (diagnostics.isEmpty()) {
                diagnostics = List.of(error("E1011", "page requires at least one effect", new Span(0, 0, 1, 1)));
            }
            return CompileResult.error(cap(diagnostics));
        }
        var page = parsed.page().orElseThrow();
        var validation = validate(page, lexed.lines().size());
        if (!validation.isEmpty()) {
            return CompileResult.error(cap(validation));
        }
        var actions = actions(page);
        byte[] canonical = canonicalize(actions);
        return CompileResult.ok(new CompiledSpell(
                CompilerConstants.COMPILER_VERSION, sha256(canonical), canonical, actions));
    }

    private static List<Diagnostic> sourceLimitDiagnostics(String source) {
        var out = new ArrayList<Diagnostic>();
        int bytes = source.getBytes(StandardCharsets.UTF_8).length;
        if (source.codePointCount(0, source.length()) > CompilerConstants.MAX_SOURCE_SCALARS) {
            int offset = source.offsetByCodePoints(0, CompilerConstants.MAX_SOURCE_SCALARS);
            out.add(new Diagnostic("E0004", "source exceeds 4096 Unicode scalars", span(source, offset, source.length())));
        }
        if (bytes > CompilerConstants.MAX_SOURCE_UTF8_BYTES) {
            int offset = utf16IndexAtOrAfterUtf8Bytes(source, CompilerConstants.MAX_SOURCE_UTF8_BYTES);
            out.add(new Diagnostic("E0005", "source exceeds 16384 UTF-8 bytes", span(source, offset, source.length())));
        }
        return sort(out);
    }

    private static List<Diagnostic> validate(Page page, int lexedLines) {
        var ds = new ArrayList<Diagnostic>();
        if (page.phrases().size() > CompilerConstants.MAX_STATEMENTS || lexedLines > CompilerConstants.MAX_STATEMENTS) {
            ds.add(error("E1003", "page exceeds 16 non-blank lines", page.span()));
        }
        long effects = page.phrases().stream().filter(ScribeCompiler::isEffect).count();
        if (effects == 0) {
            ds.add(error("E1011", "page requires at least one effect", page.span()));
        } else if (effects > CompilerConstants.MAX_EFFECTS) {
            ds.add(error("E1004", "page exceeds 4 effects", page.span()));
        }
        if (page.phrases().stream().filter(p -> p instanceof Phrase.LookAhead).count() > 1) {
            ds.add(error("E1012", "page may declare only one look", page.span()));
        }
        if (page.phrases().stream().filter(p -> p instanceof Phrase.Rest).count() > 1) {
            ds.add(error("E1013", "page may declare only one rest", page.span()));
        }
        boolean seenSummon = false;
        for (var phrase : page.phrases()) {
            if (phrase instanceof Phrase.Summon) {
                seenSummon = true;
            }
            if (phrase instanceof Phrase.SendSkyward && !seenSummon) {
                ds.add(error("E1014", "send requires a prior summon", phrase.span()));
            }
            switch (phrase) {
                case Phrase.LookAhead x -> {
                    if (!between(x.range(), 1, 32)) {
                        ds.add(error("E1015", "look range must be 1..32", x.span()));
                    }
                }
                case Phrase.Burn x -> {
                    if (!between(x.amount(), 0.5, 20)) {
                        ds.add(error("E1001", "burn must be 0.5..20", x.span()));
                    }
                }
                case Phrase.Mend x -> {
                    if (!between(x.amount(), 0.5, 20)) {
                        ds.add(error("E1001", "mend must be 0.5..20", x.span()));
                    }
                }
                case Phrase.Shove x -> {
                    if (!between(x.amount(), 0.1, 3)) {
                        ds.add(error("E1002", "shove must be 0.1..3", x.span()));
                    }
                }
                case Phrase.Rest x -> {
                    if (!between(x.seconds(), 0, 60)) {
                        ds.add(error("E1009", "rest must be 0..60 seconds", x.span()));
                    }
                }
                case Phrase.Vanish x -> {
                    if (!between(x.seconds(), 0.5, 20)) {
                        ds.add(error("E1016", "vanish duration must be 0.5..20", x.span()));
                    }
                }
                default -> { }
            }
        }
        return sort(ds);
    }

    private static boolean isEffect(Phrase phrase) {
        return phrase instanceof Phrase.Summon
                || phrase instanceof Phrase.Burn
                || phrase instanceof Phrase.Mend
                || phrase instanceof Phrase.Shove
                || phrase instanceof Phrase.Strike
                || phrase instanceof Phrase.SendSkyward
                || phrase instanceof Phrase.Vanish;
    }

    private static boolean between(double v, double min, double max) {
        return Double.isFinite(v) && v >= min && v <= max;
    }

    private static Diagnostic error(String code, String message, Span span) {
        return new Diagnostic(code, message, span);
    }

    private static List<Action> actions(Page page) {
        var out = new ArrayList<Action>();
        for (var phrase : page.phrases()) {
            out.add(switch (phrase) {
                case Phrase.LookAhead x -> new Action.LookAhead(x.range());
                case Phrase.Summon x -> new Action.Summon(x.noun(), x.place(), x.riding());
                case Phrase.Burn x -> new Action.Burn(x.patient(), x.amount());
                case Phrase.Mend x -> new Action.Mend(x.patient(), x.amount());
                case Phrase.Shove x -> new Action.Shove(x.patient(), x.amount());
                case Phrase.Strike x -> new Action.Strike(x.place());
                case Phrase.SendSkyward ignored -> new Action.SendSkyward();
                case Phrase.Vanish x -> new Action.Vanish(x.patient(), x.seconds());
                case Phrase.Rest x -> new Action.Rest(x.seconds());
            });
        }
        return out;
    }

    private static byte[] canonicalize(List<Action> actions) {
        var lines = new ArrayList<String>();
        lines.add(CompilerConstants.COMPILER_VERSION);
        for (var action : actions) {
            lines.add(canonicalLine(action));
        }
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    private static String canonicalLine(Action action) {
        return switch (action) {
            case Action.LookAhead x -> "look_ahead|" + bits(x.range());
            case Action.Summon x -> canonicalSummon(x);
            case Action.Burn x -> "burn|" + patient(x.patient()) + "|" + bits(x.amount());
            case Action.Mend x -> "mend|" + patient(x.patient()) + "|" + bits(x.amount());
            case Action.Shove x -> "shove|" + patient(x.patient()) + "|" + bits(x.amount());
            case Action.Strike x -> canonicalStrike(x);
            case Action.SendSkyward ignored -> "send_skyward";
            case Action.Vanish x -> "vanish|" + patient(x.patient()) + "|" + bits(x.seconds());
            case Action.Rest x -> "rest|" + bits(x.seconds());
        };
    }

    private static String canonicalSummon(Action.Summon summon) {
        String noun = summon.noun().name().toLowerCase(Locale.ROOT);
        String riding = summon.riding() == null ? "-" : summon.riding().name().toLowerCase(Locale.ROOT);
        if (summon.place() instanceof Action.Place.Ahead ahead) {
            return "summon|" + noun + "|ahead|" + bits(ahead.range()) + "|" + riding;
        }
        return "summon|" + noun + "|" + place(summon.place()) + "|" + riding;
    }

    private static String canonicalStrike(Action.Strike strike) {
        if (strike.place() instanceof Action.Place.Ahead ahead) {
            return "strike|ahead|" + bits(ahead.range());
        }
        return "strike|" + place(strike.place());
    }

    private static String bits(double v) {
        return String.format(Locale.ROOT, "%016x", Double.doubleToRawLongBits(v));
    }

    private static String patient(Action.Patient patient) {
        return patient.name().toLowerCase(Locale.ROOT);
    }

    private static String place(Action.Place p) {
        return switch (p) {
            case Action.Place.Caster ignored -> "caster";
            case Action.Place.Self ignored -> "self";
            case Action.Place.Target ignored -> "target";
            case Action.Place.Ahead ignored -> "ahead";
        };
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static List<Diagnostic> cap(List<Diagnostic> ds) {
        return sort(ds).subList(0, Math.min(ds.size(), CompilerConstants.MAX_DIAGNOSTICS));
    }

    private static List<Diagnostic> sort(List<Diagnostic> ds) {
        var out = new ArrayList<>(ds);
        out.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte())
                .thenComparing(Diagnostic::code)
                .thenComparing(Diagnostic::message));
        return out.size() > CompilerConstants.MAX_DIAGNOSTICS
                ? new ArrayList<>(out.subList(0, CompilerConstants.MAX_DIAGNOSTICS))
                : out;
    }

    private static Span span(String source, int startChar, int endChar) {
        int start = utf8Offset(source, startChar);
        int end = utf8Offset(source, endChar);
        int line = 1;
        int col = 1;
        for (int i = 0; i < startChar; ) {
            int cp = source.codePointAt(i);
            if (cp == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
            i += Character.charCount(cp);
        }
        return new Span(start, end, line, col);
    }

    private static int utf8Offset(String source, int charIndex) {
        return source.substring(0, charIndex).getBytes(StandardCharsets.UTF_8).length;
    }

    private static int utf16IndexAtOrAfterUtf8Bytes(String source, int byteLimit) {
        int bytes = 0;
        for (int index = 0; index < source.length(); ) {
            if (bytes >= byteLimit) {
                return index;
            }
            int cp = source.codePointAt(index);
            bytes += new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            index += Character.charCount(cp);
            if (bytes >= byteLimit) {
                return index;
            }
        }
        return source.length();
    }
}
