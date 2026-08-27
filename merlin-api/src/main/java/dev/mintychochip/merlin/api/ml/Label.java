package dev.mintychochip.merlin.api.ml;

public enum Label {
    TARGET_RAY("target-ray"), DAMAGE("damage"), HEAL("heal"), PUSH("push"), COOLDOWN("cooldown"),
    SELF("self"), TARGET("target"), PHYSICAL("physical"), FLAME("flame"), FROST("frost"), ARCANE("arcane"),
    ON_HIT("on-hit"), ON_HURT("on-hurt"), ON_USE("on-use"), PERIODIC("periodic"),
    IF_HEALTH("if-health"), IF_UNDEAD("if-undead"), IF_OUTDOORS("if-outdoors"),
    SHIELD("shield"), ATTACKER("attacker"), AREA("area"), REPEAT("repeat"), CHARGES("charges"),
    REJECT("reject"), SHARPNESS("sharpness");
    private final String id;
    Label(String id) { this.id = id; }
    public String id() { return id; }
    public static Label fromId(String id) {
        if ("fire".equals(id)) return FLAME;
        for (var v : values()) if (v.id.equals(id)) return v;
        throw new IllegalArgumentException("unknown label: " + id);
    }
}
