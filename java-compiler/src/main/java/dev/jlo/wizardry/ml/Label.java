package dev.jlo.wizardry.ml;

public enum Label {
    TARGET_RAY("target-ray"), DAMAGE("damage"), HEAL("heal"), PUSH("push"), COOLDOWN("cooldown"),
    SELF("self"), TARGET("target"), PHYSICAL("physical"), FIRE("fire"), FROST("frost"), ARCANE("arcane"), REJECT("reject");
    private final String id;
    Label(String id) { this.id = id; }
    public String id() { return id; }
    public static Label fromId(String id) { for (var v : values()) if (v.id.equals(id)) return v; throw new IllegalArgumentException("unknown label: " + id); }
}
