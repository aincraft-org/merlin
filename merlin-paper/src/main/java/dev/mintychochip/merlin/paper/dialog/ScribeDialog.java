package dev.mintychochip.merlin.paper.dialog;

import dev.mintychochip.merlin.paper.book.ScribeBookStore;
import dev.mintychochip.merlin.common.dsl.ScribeCompiler;
import dev.mintychochip.merlin.api.dsl.CompileResult;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ScribeDialog {
    public static final String SOURCE_KEY = "source";
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

    public static String readSource(DialogResponseView view) {
        String text = view.getText(SOURCE_KEY);
        return text == null ? "" : text;
    }

    public void show(Player player, ItemStack book, long nowMillis) {
        show(player, book, nowMillis, null);
    }

    public void show(Player player, ItemStack book, long nowMillis, CompileResult lastCompile) {
        var session = open(player.getUniqueId(), book, nowMillis);
        if (session == null) return;
        var options = ClickCallback.Options.builder().uses(1).lifetime(MAX_CALLBACK_LIFETIME).build();
        var dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Scribe"))
                        .canCloseWithEscape(true)
                        .body(body(lastCompile))
                        .inputs(List.of(DialogInput.text(SOURCE_KEY, Component.text("page"))
                                .width(WIDTH_PIXELS)
                                .maxLength(MAX_SCALARS)
                                .initial(session.pendingSource() == null ? "" : session.pendingSource())
                                .multiline(TextDialogInput.MultilineOptions.create(MAX_LINES, HEIGHT_PIXELS))
                                .build()))
                        .build())
                .type(DialogType.multiAction(List.of(
                        button("Save", Action.SAVE, options),
                        button("Save & Cast", Action.SAVE_AND_CAST, options),
                        button("Cancel", Action.CANCEL, options)
                )).exitAction(button("Cancel", Action.ESCAPE, options)).build()));
        player.showDialog(dialog);
    }

    private static List<DialogBody> body(CompileResult lastCompile) {
        if (lastCompile instanceof CompileResult.Error error) {
            return error.diagnostics().stream()
                    .map(diagnostic -> (DialogBody) DialogBody.plainMessage(
                            Component.text(diagnostic.code() + ": " + diagnostic.message())))
                    .toList();
        }
        return List.of(DialogBody.plainMessage(Component.text("verb first, indent riding")));
    }

    private ActionButton button(String label, Action action, ClickCallback.Options options) {
        return ActionButton.builder(Component.text(label))
                .action(DialogAction.customClick((view, audience) -> handleClick(audience, view, action), options))
                .build();
    }

    private void handleClick(Audience audience, DialogResponseView view, Action action) {
        if (!(audience instanceof Player player)) return;
        var book = player.getInventory().getItemInMainHand();
        var source = readSource(view);
        var now = System.currentTimeMillis();
        draft(player.getUniqueId(), source);
        var outcome = submit(player.getUniqueId(), book, source, action, now);
        if (action == Action.SAVE_AND_CAST && outcome.compilation() instanceof CompileResult.Error) {
            show(player, book, now, outcome.compilation());
        }
    }

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
        if (session == null) return new Outcome(false, false, false, source, null);
        if (action == Action.CANCEL || action == Action.ESCAPE) {
            sessions.remove(playerId);
            return new Outcome(false, false, false, session.initialSource(), null);
        }
        if (session.expiresAtMillis() <= nowMillis || !session.bookId().equals(books.bookId(exactBook))) return new Outcome(false, false, false, source, null);
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
