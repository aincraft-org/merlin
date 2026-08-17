package dev.mintychochip.wizardry.api.dsl;

public final class CompilerConstants {
    public static final String COMPILER_VERSION = "scribe-compiler/0.2";
    public static final int MAX_SOURCE_SCALARS = 4_096;
    public static final int MAX_SOURCE_UTF8_BYTES = 16 * 1_024;
    public static final int MAX_STATEMENTS = 16;
    public static final int MAX_EFFECTS = 4;
    public static final int MAX_DIAGNOSTICS = 32;

    private CompilerConstants() {}
}
