package dev.mintychochip.merlin.paper.command;

import dev.mintychochip.merlin.paper.MerlinPlugin;
import dev.mintychochip.merlin.paper.dialog.ScribeDialog;
import dev.mintychochip.merlin.common.dsl.ScribeCompiler;
import dev.mintychochip.merlin.api.dsl.Action;
import dev.mintychochip.merlin.api.dsl.CompileResult;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ScribeCommand implements BasicCommand {
    private final MerlinPlugin plugin;
    private final ScribeDialog dialog;

    public ScribeCommand(MerlinPlugin plugin, ScribeDialog dialog) {
        this.plugin = plugin;
        this.dialog = dialog;
    }

    @Override
    public String permission() {
        return "merlin.scribe.book";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission(permission())) {
            sender.sendMessage("You do not have permission to use Scribe.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Scribe requires a player.");
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("book")) {
            player.getInventory().addItem(plugin.books().createBook());
            player.sendMessage("Created a Scribe book.");
            return;
        }
        if (args[0].equalsIgnoreCase("begin")) {
            var held = player.getInventory().getItemInMainHand();
            if (!plugin.books().isScribeBook(held)) {
                player.sendMessage("You must hold a Scribe book.");
                return;
            }
            dialog.open(player.getUniqueId(), held, System.currentTimeMillis());
            player.sendMessage("Scribe editor started. Send source in chat, then use /scribe save, /scribe cast, or /scribe cancel.");
            return;
        }
        if (args[0].equalsIgnoreCase("save") || args[0].equalsIgnoreCase("cast") || args[0].equalsIgnoreCase("cancel")) {
            var session = dialog.session(player.getUniqueId());
            if (session != null) {
                var held = player.getInventory().getItemInMainHand();
                var action = args[0].equalsIgnoreCase("save") ? ScribeDialog.Action.SAVE
                        : args[0].equalsIgnoreCase("cast") ? ScribeDialog.Action.SAVE_AND_CAST : ScribeDialog.Action.CANCEL;
                var outcome = dialog.submit(player.getUniqueId(), held, session.pendingSource(), action, System.currentTimeMillis());
                if (outcome.compilation() instanceof CompileResult.Error error) {
                    error.diagnostics().forEach(d -> player.sendMessage(d.code() + ": " + d.message()));
                }
                player.sendMessage(outcome.cast() ? "Spell cast."
                        : outcome.persisted() ? "Scribe source saved." : "Scribe action cancelled or rejected.");
                return;
            }
            if (!args[0].equalsIgnoreCase("cast")) {
                player.sendMessage("No active Scribe editor.");
                return;
            }
        }
        if (!args[0].equalsIgnoreCase("cast")) {
            player.sendMessage("Usage: /scribe book|begin|save|cast|cancel");
            return;
        }
        var held = player.getInventory().getItemInMainHand();
        if (!plugin.books().isScribeBook(held)) {
            player.sendMessage("You must hold a Scribe book.");
            return;
        }
        var result = ScribeCompiler.INSTANCE.compile(plugin.books().source(held));
        if (!(result instanceof CompileResult.Ok ok)) {
            if (result instanceof CompileResult.Error error) {
                error.diagnostics().forEach(d -> player.sendMessage(d.code() + ": " + d.message()));
            }
            return;
        }
        var spell = ok.spell();
        var range = spell.actions().stream()
                .filter(action -> action instanceof Action.LookAhead)
                .mapToDouble(action -> ((Action.LookAhead) action).range())
                .findFirst()
                .orElse(32);
        var target = player.getTargetEntity((int) range);
        var living = target instanceof org.bukkit.entity.LivingEntity entity ? entity : null;
        if (!plugin.runtime().cast(player, living, spell, System.currentTimeMillis(), range)) {
            player.sendMessage("Spell is unavailable: target or cooldown check failed.");
            return;
        }
        player.sendMessage("Spell cast.");
    }
}
