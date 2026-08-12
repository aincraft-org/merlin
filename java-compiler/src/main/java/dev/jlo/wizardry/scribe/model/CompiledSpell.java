package dev.jlo.wizardry.scribe.model;

import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public record CompiledSpell(
        String compilerVersion,
        String name,
        String identitySha256,
        byte[] canonical,
        List<Operation> operations
) {
    public CompiledSpell {
        compilerVersion = Objects.requireNonNull(compilerVersion, "compilerVersion");
        name = Objects.requireNonNull(name, "name");
        identitySha256 = Objects.requireNonNull(identitySha256, "identitySha256");
        canonical = Objects.requireNonNull(canonical, "canonical").clone();
        operations = List.copyOf(operations);
    }

    @Override
    public byte[] canonical() {
        return canonical.clone();
    }

    public String canonicalHex() {
        return HexFormat.of().formatHex(canonical);
    }
}
