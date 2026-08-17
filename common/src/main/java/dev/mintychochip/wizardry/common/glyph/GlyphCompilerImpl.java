package dev.mintychochip.wizardry.common.glyph;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.api.dsl.CompilerConstants;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.api.glyph.CharmBind;
import dev.mintychochip.wizardry.api.glyph.GlyphCompiler;
import dev.mintychochip.wizardry.api.glyph.GlyphRoles;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.ml.Label;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class GlyphCompilerImpl implements GlyphCompiler {
    public static final GlyphCompilerImpl INSTANCE = new GlyphCompilerImpl();

    static final String COMPILER_VERSION = "glyph-compiler/0.1";

    private static final int MAX_PAGES = 3;
    private static final double LOOK_AHEAD = 32.0;
    private static final double SHOVE_CAP = 3.0;

    private GlyphCompilerImpl() {}

    @Override
    public CompileResult compile(List<GlyphToken> pages) {
        pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
        var diagnostics = new ArrayList<Diagnostic>();
        if (pages.isEmpty()) {
            diagnostics.add(error("G0100", "empty page list", new Span(0, 0, 1, 1)));
            return CompileResult.error(cap(diagnostics));
        }
        if (pages.size() > MAX_PAGES) {
            diagnostics.add(error("G0103", "more than 3 pages", span(MAX_PAGES)));
        }

        var effects = new ArrayList<PageToken>();
        var schools = new ArrayList<PageToken>();
        var patients = new ArrayList<PageToken>();
        for (int i = 0; i < pages.size(); i++) {
            var token = pages.get(i);
            var label = token.label();
            if (label == Label.REJECT) {
                diagnostics.add(error("G0101", "reject is not a word", span(i)));
                continue;
            }
            if (GlyphRoles.reserved(label) || label == Label.ATTACKER) {
                diagnostics.add(error("G0102", "reserved label", span(i)));
                continue;
            }
            if (label == Label.SHARPNESS) {
                diagnostics.add(error("G0108", "charm is not a combat compile", span(i)));
                continue;
            }
            var page = new PageToken(i, token);
            switch (token.role()) {
                case EFFECT -> effects.add(page);
                case SCHOOL -> schools.add(page);
                case PATIENT -> patients.add(page);
                default -> { }
            }
        }

        if (effects.size() > 1) {
            diagnostics.add(error("G0104", "more than one effect", span(effects.get(1).index)));
        }
        if (schools.size() > 1) {
            diagnostics.add(error("G0105", "more than one school", span(schools.get(1).index)));
        }
        if (patients.size() > 1) {
            diagnostics.add(error("G0106", "more than one patient", span(patients.get(1).index)));
        }
        if (effects.isEmpty() && (!schools.isEmpty() || !patients.isEmpty())) {
            int unfinished = !schools.isEmpty() ? schools.getFirst().index : patients.getFirst().index;
            diagnostics.add(error("G0107", "school or patient with no effect", span(unfinished)));
        }
        for (var effect : effects) {
            if (effect.token.label() == Label.SHIELD) {
                diagnostics.add(error("G0111", "shield has no tape lowering", span(effect.index)));
            }
        }
        if (!diagnostics.isEmpty()) {
            return CompileResult.error(cap(diagnostics));
        }

        var effect = effects.getFirst().token;
        var patient = Action.Patient.TARGET;
        if (!patients.isEmpty() && patients.getFirst().token.label() == Label.SELF) {
            patient = Action.Patient.SELF;
        }
        var actions = new ArrayList<Action>();
        if (patient == Action.Patient.TARGET) {
            actions.add(new Action.LookAhead(LOOK_AHEAD));
        }
        double pips = effect.pips();
        actions.add(switch (effect.label()) {
            case DAMAGE -> new Action.Burn(patient, pips);
            case HEAL -> new Action.Mend(patient, pips);
            case PUSH -> new Action.Shove(patient, Math.min(SHOVE_CAP, pips));
            default -> throw new AssertionError(effect.label());
        });
        byte[] canonical = canonicalize(actions);
        return CompileResult.ok(new CompiledSpell(COMPILER_VERSION, sha256(canonical), canonical, actions));
    }

    @Override
    public Optional<CharmBind> charm(GlyphToken token) {
        Objects.requireNonNull(token, "token");
        if (token.label() != Label.SHARPNESS) {
            return Optional.empty();
        }
        return Optional.of(new CharmBind(token.label(), token.pips()));
    }

    private static byte[] canonicalize(List<Action> actions) {
        var lines = new ArrayList<String>();
        lines.add(COMPILER_VERSION);
        for (var action : actions) {
            lines.add(canonicalLine(action));
        }
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    private static String canonicalLine(Action action) {
        return switch (action) {
            case Action.LookAhead x -> "look_ahead|" + bits(x.range());
            case Action.Burn x -> "burn|" + patient(x.patient()) + "|" + bits(x.amount());
            case Action.Mend x -> "mend|" + patient(x.patient()) + "|" + bits(x.amount());
            case Action.Shove x -> "shove|" + patient(x.patient()) + "|" + bits(x.amount());
            default -> throw new IllegalStateException("unsupported action: " + action);
        };
    }

    private static String bits(double v) {
        return String.format(Locale.ROOT, "%016x", Double.doubleToRawLongBits(v));
    }

    private static String patient(Action.Patient patient) {
        return patient.name().toLowerCase(Locale.ROOT);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static Diagnostic error(String code, String message, Span span) {
        return new Diagnostic(code, message, span);
    }

    private static Span span(int pageIndex) {
        return new Span(0, 0, pageIndex + 1, 1);
    }

    private static List<Diagnostic> cap(List<Diagnostic> ds) {
        var out = new ArrayList<>(ds);
        out.sort(Comparator.comparingInt((Diagnostic d) -> d.span().startByte())
                .thenComparing(Diagnostic::code)
                .thenComparing(Diagnostic::message));
        return out.size() > CompilerConstants.MAX_DIAGNOSTICS
                ? new ArrayList<>(out.subList(0, CompilerConstants.MAX_DIAGNOSTICS))
                : out;
    }

    private record PageToken(int index, GlyphToken token) {}
}
