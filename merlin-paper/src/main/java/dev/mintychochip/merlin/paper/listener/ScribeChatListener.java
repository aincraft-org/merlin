package dev.mintychochip.merlin.paper.listener;

import dev.mintychochip.merlin.paper.dialog.ScribeDialog;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ScribeChatListener implements Listener {
    private final Plugin plugin;
    private final ScribeDialog dialog;
    public ScribeChatListener(Plugin plugin, ScribeDialog dialog) { this.plugin = plugin; this.dialog = dialog; }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var id = player.getUniqueId();
        if (!dialog.hasSession(id)) return;
        event.setCancelled(true);
        var message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!dialog.hasSession(id)) return;
            var current = dialog.session(id).pendingSource();
            var draft = current.isEmpty() ? message : current + "\n" + message;
            if (!dialog.validInput(draft)) { player.sendMessage("Draft exceeds Scribe input limits."); return; }
            dialog.draft(id, draft);
            player.sendMessage("Draft updated. Use /scribe save, /scribe cast, or /scribe cancel.");
        });
    }
}
