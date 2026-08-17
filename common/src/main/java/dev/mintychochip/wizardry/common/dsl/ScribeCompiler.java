package dev.mintychochip.wizardry.common.dsl;

import dev.mintychochip.wizardry.api.dsl.Compiler;
import dev.mintychochip.wizardry.api.dsl.CompilerConstants;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Operation;
import dev.mintychochip.wizardry.api.dsl.Program;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.api.dsl.Statement;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.common.dsl.parser.Parser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class ScribeCompiler implements Compiler {
    public static final ScribeCompiler INSTANCE = new ScribeCompiler();

    private ScribeCompiler() {}

    @Override
    public CompileResult compile(String source) {
        var limitDiagnostics = sourceLimitDiagnostics(source);
        if (!limitDiagnostics.isEmpty()) return CompileResult.error(limitDiagnostics);
        var lexed = Lexer.lex(source);
        if (!lexed.diagnostics().isEmpty()) return CompileResult.error(cap(lexed.diagnostics()));
        var parsed = Parser.parse(lexed.tokens(), source);
        if (parsed.program().isEmpty()) return CompileResult.error(cap(parsed.diagnostics()));
        var validation = validate(parsed.program().orElseThrow(), source);
        if (!validation.isEmpty()) return CompileResult.error(cap(validation));
        var program = parsed.program().orElseThrow();
        byte[] canonical = canonicalize(program);
        return CompileResult.ok(new CompiledSpell(
                CompilerConstants.COMPILER_VERSION, program.name(), sha256(canonical), canonical, operations(program)));
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

    private static List<Diagnostic> validate(Program p, String source) {
        var ds = new ArrayList<Diagnostic>();
        if (p.name().isEmpty()) ds.add(error("E1000", "spell name cannot be empty", p.span()));
        if (p.statements().size() > 16) ds.add(error("E1003", "program exceeds 16 statements", p.span()));
        long effects = p.statements().stream().filter(ScribeCompiler::isEffect).count();
        if (effects == 0) ds.add(error("E1011", "program requires at least one effect", p.span()));
        else if (effects > 4) ds.add(error("E1004", "program exceeds 4 effects", p.span()));
        if (p.statements().stream().filter(s -> s instanceof Statement.TargetRay).count() > 1)
            ds.add(error("E1008", "program may declare only one target", p.span()));
        if (p.statements().stream().filter(s -> s instanceof Statement.Cooldown).count() > 1)
            ds.add(error("E1010", "program may declare only one cooldown", p.span()));
        for (var s : p.statements()) {
            switch (s) {
                case Statement.TargetRay x -> { if (!between(x.range(), 1, 32)) ds.add(error("E1005", "ray range must be 1..32", x.span())); }
                case Statement.Damage x -> {
                    if (x.target() != Operation.Target.TARGET) ds.add(error("E1006", "damage requires target", x.span()));
                    if (!between(x.amount(), .5, 20)) ds.add(error("E1001", "damage must be 0.5..20", x.span()));
                }
                case Statement.Heal x -> {
                    if (x.target() != Operation.Target.SELF) ds.add(error("E1007", "heal requires self", x.span()));
                    if (!between(x.amount(), .5, 20)) ds.add(error("E1001", "healing must be 0.5..20", x.span()));
                }
                case Statement.Push x -> {
                    if (x.target() != Operation.Target.TARGET) ds.add(error("E1006", "push requires target", x.span()));
                    if (!between(x.strength(), .1, 3)) ds.add(error("E1002", "push must be 0.1..3", x.span()));
                }
                case Statement.Cooldown x -> { if (!between(x.seconds(), 0, 60)) ds.add(error("E1009", "cooldown must be 0..60 seconds", x.span())); }
            }
        }
        return sort(ds);
    }

    private static boolean between(double v, double min, double max) { return Double.isFinite(v) && v >= min && v <= max; }
    private static boolean isEffect(Statement s) { return s instanceof Statement.Damage || s instanceof Statement.Heal || s instanceof Statement.Push; }
    private static Diagnostic error(String code, String message, Span span) { return new Diagnostic(code, message, span); }

    private static byte[] canonicalize(Program p) {
        var lines = new ArrayList<String>();
        lines.add(CompilerConstants.COMPILER_VERSION + "|spell|" + p.name() + "|");
        for (var s : p.statements()) {
            lines.add(switch (s) {
                case Statement.TargetRay x -> "target_ray|" + bits(x.range());
                case Statement.Damage x -> "damage|" + target(x.target()) + "|" + damageType(x.damageType()) + "|" + bits(x.amount());
                case Statement.Heal x -> "heal|" + target(x.target()) + "|" + bits(x.amount());
                case Statement.Push x -> "push|" + target(x.target()) + "|" + bits(x.strength());
                case Statement.Cooldown x -> "cooldown|" + bits(x.seconds());
            });
        }
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }
    private static String bits(double v) { return String.format(java.util.Locale.ROOT, "%016x", Double.doubleToRawLongBits(v)); }
    private static String target(Operation.Target t) { return t == Operation.Target.SELF ? "self" : "target"; }
    private static String damageType(Operation.DamageType t) { return t.name().toLowerCase(java.util.Locale.ROOT); }

    private static List<Operation> operations(Program p) {
        var out = new ArrayList<Operation>();
        for (var s : p.statements()) {
            switch (s) {
                case Statement.TargetRay x -> out.add(new Operation.TargetRay(x.range()));
                case Statement.Damage x -> out.add(new Operation.Damage(x.target(), x.damageType(), x.amount()));
                case Statement.Heal x -> out.add(new Operation.Heal(x.target(), x.amount()));
                case Statement.Push x -> out.add(new Operation.Push(x.target(), x.strength()));
                case Statement.Cooldown x -> out.add(new Operation.Cooldown(x.seconds()));
            }
        }
        return out;
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    private static List<Diagnostic> cap(List<Diagnostic> ds) { return sort(ds).subList(0, Math.min(ds.size(), CompilerConstants.MAX_DIAGNOSTICS)); }
    private static List<Diagnostic> sort(List<Diagnostic> ds) {
        var out = new ArrayList<>(ds);
        out.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte()).thenComparing(Diagnostic::code).thenComparing(Diagnostic::message));
        return out.size() > CompilerConstants.MAX_DIAGNOSTICS ? new ArrayList<>(out.subList(0, CompilerConstants.MAX_DIAGNOSTICS)) : out;
    }
    private static Span span(String source, int startChar, int endChar) {
        int start = utf8Offset(source, startChar), end = utf8Offset(source, endChar);
        int line = 1, col = 1;
        for (int i = 0; i < startChar; ) { int cp = source.codePointAt(i); if (cp == '\n') { line++; col = 1; } else col++; i += Character.charCount(cp); }
        return new Span(start, end, line, col);
    }
    private static int utf8Offset(String source, int charIndex) { return source.substring(0, charIndex).getBytes(StandardCharsets.UTF_8).length; }
    private static int utf16IndexAtOrAfterUtf8Bytes(String source, int byteLimit) {
        int bytes = 0;
        for (int index = 0; index < source.length(); ) {
            if (bytes >= byteLimit) return index;
            int cp = source.codePointAt(index);
            bytes += new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            index += Character.charCount(cp);
            if (bytes >= byteLimit) return index;
        }
        return source.length();
    }
}
