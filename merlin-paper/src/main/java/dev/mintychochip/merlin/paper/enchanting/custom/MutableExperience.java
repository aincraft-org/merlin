package dev.mintychochip.merlin.paper.enchanting.custom;

public final class MutableExperience {
    private int amount;

    public MutableExperience(int amount) {
        setAmount(amount);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    public void add(int amount) {
        long updated = (long) this.amount + amount;
        this.amount = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, updated));
    }
}
