package dev.mintychochip.merlin.api.dsl;

import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public record CompiledSpell(
        String compilerVersion,
        String identitySha256,
        byte[] canonical,
        List<Action> actions
) {
    public CompiledSpell {
        compilerVersion = Objects.requireNonNull(compilerVersion, "compilerVersion");
        identitySha256 = Objects.requireNonNull(identitySha256, "identitySha256");
        canonical = Objects.requireNonNull(canonical, "canonical").clone();
        actions = List.copyOf(actions);
    }

    @Override
    public byte[] canonical() {
        return canonical.clone();
    }

    public String canonicalHex() {
        return HexFormat.of().formatHex(canonical);
    }
}
