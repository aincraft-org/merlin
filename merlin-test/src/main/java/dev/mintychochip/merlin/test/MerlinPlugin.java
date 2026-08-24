package dev.mintychochip.merlin.test;

import org.bukkit.plugin.java.JavaPlugin;

/** Minimal test harness plugin for the development server. */
public final class MerlinPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    getLogger().info("Merlin test harness enabled.");
  }

  @Override
  public void onDisable() {
    getLogger().info("Merlin test harness disabled.");
  }
}
