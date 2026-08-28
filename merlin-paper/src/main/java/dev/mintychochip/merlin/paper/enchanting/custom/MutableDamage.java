package dev.mintychochip.merlin.paper.enchanting.custom;

public final class MutableDamage {
    private final double initialDamage;
    private double bonusDamage = 0.0;
    private double multiplier = 1.0;
    private boolean cancelled = false;

    public MutableDamage(double initialDamage) {
        this.initialDamage = initialDamage;
    }

    public double getInitialDamage() {
        return initialDamage;
    }

    public double getBonusDamage() {
        return bonusDamage;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void addBonus(double bonus) {
        this.bonusDamage += bonus;
    }

    public void multiply(double factor) {
        this.multiplier *= factor;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public double getFinalDamage() {
        if (cancelled) return 0.0;
        return Math.max(0.0, (initialDamage + bonusDamage) * multiplier);
    }
}
