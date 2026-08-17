package dev.mintychochip.wizardry.paper.dialog;

import dev.mintychochip.wizardry.paper.book.ScribeBookStore;
import dev.mintychochip.wizardry.common.dsl.ScribeCompiler;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import org.bukkit.inventory.ItemStack;

public final class ScribeDialog {
    public static final int WIDTH_PIXELS = 1024;
    public static final int HEIGHT_PIXELS = 512;
    public static final int MAX_LINES = 128;
    public static final int MAX_SCALARS = 4096;
    public static final Duration MAX_CALLBACK_LIFETIME = Duration.ofMinutes(15);
    public enum Action { SAVE, SAVE_AND_CAST, CANCEL, ESCAPE }
    public record Session(UUID playerId, UUID bookId, String initialSource, String pendingSource, long expiresAtMillis) {}
    public record Outcome(boolean persisted, boolean cast, boolean reopen, String source, CompileResult compilation) {}
    private final ScribeBookStore books;
    private final BiFunction<UUID, CompileResult, Boolean> caster;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public ScribeDialog(ScribeBookStore books) { this(books, (playerId, compilation) -> true); }
    public ScribeDialog(ScribeBookStore books, BiFunction<UUID, CompileResult, Boolean> caster) { this.books = books; this.caster = caster; }
    public Session open(UUID playerId, ItemStack book, long nowMillis) {
        var id = books.bookId(book);
        if (id == null) return null;
        var source = books.source(book);
        var session = new Session(playerId, id, source, source, nowMillis + MAX_CALLBACK_LIFETIME.toMillis());
        sessions.put(playerId, session);
        return session;
    }
    public Session session(UUID playerId) { return sessions.get(playerId); }
    public void draft(UUID playerId, String source) {
        sessions.computeIfPresent(playerId, (id, session) -> new Session(session.playerId(), session.bookId(), session.initialSource(), source, session.expiresAtMillis()));
    }
    public Outcome submit(UUID playerId, ItemStack exactBook, String source, Action action, long nowMillis) {
        var session = sessions.get(playerId);
        if (session == null || session.expiresAtMillis() <= nowMillis || !session.bookId().equals(books.bookId(exactBook))) return new Outcome(false, false, false, source, null);
        if (action == Action.CANCEL || action == Action.ESCAPE) { sessions.remove(playerId); return new Outcome(false, false, false, session.initialSource(), null); }
        if (!validInput(source)) return new Outcome(false, false, true, source, null);
        CompileResult compilation = action == Action.SAVE_AND_CAST ? ScribeCompiler.INSTANCE.compile(source) : null;
        if (!books.save(exactBook, session.bookId(), source)) return new Outcome(false, false, false, source, compilation);
        sessions.remove(playerId);
        if (action == Action.SAVE) return new Outcome(true, false, false, source, null);
        if (!(compilation instanceof CompileResult.Ok)) return new Outcome(true, false, true, source, compilation);
        return new Outcome(true, caster.apply(playerId, compilation), false, source, compilation);
    }
    public boolean validInput(String source) {
        return source != null && source.codePointCount(0, source.length()) <= MAX_SCALARS
                && source.getBytes(StandardCharsets.UTF_8).length <= 16384
                && physicalLineCount(source) <= MAX_LINES;
    }
    private static long physicalLineCount(String source) { return source.isEmpty() ? 1 : source.endsWith("\n") ? source.split("\n", -1).length - 1 : source.split("\n", -1).length; }
    public boolean hasSession(UUID playerId) { return sessions.containsKey(playerId); }
}
