package dev.mintychochip.merlin.paper.enchanting.custom;

public final class CascadeGuard {
    public static final int MAX_CASCADE_DEPTH = 3;
    private static final ThreadLocal<Integer> CURRENT_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static int getDepth() {
        return CURRENT_DEPTH.get();
    }

    public static boolean canCascade() {
        return CURRENT_DEPTH.get() < MAX_CASCADE_DEPTH;
    }

    public static void runInScope(Runnable action) {
        int depth = CURRENT_DEPTH.get();
        if (depth >= MAX_CASCADE_DEPTH) return;
        CURRENT_DEPTH.set(depth + 1);
        try {
            action.run();
        } finally {
            CURRENT_DEPTH.set(depth);
        }
    }

    private CascadeGuard() {}
}
