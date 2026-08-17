package dev.mintychochip.wizardry.api.dsl;

public sealed interface Action permits Action.LookAhead, Action.Summon, Action.Burn, Action.Mend,
        Action.Shove, Action.Strike, Action.SendSkyward, Action.Vanish, Action.Rest {
    enum Noun { SHEEP, ROCKET, FANGS }
    enum Patient { SELF, TARGET }

    sealed interface Place permits Place.Caster, Place.Self, Place.Target, Place.Ahead {
        record Caster() implements Place {}
        record Self() implements Place {}
        record Target() implements Place {}
        record Ahead(double range) implements Place {}
    }

    record LookAhead(double range) implements Action {}
    record Summon(Noun noun, Place place, Noun riding) implements Action {}
    record Burn(Patient patient, double amount) implements Action {}
    record Mend(Patient patient, double amount) implements Action {}
    record Shove(Patient patient, double amount) implements Action {}
    record Strike(Place place) implements Action {}
    record SendSkyward() implements Action {}
    record Vanish(Patient patient, double seconds) implements Action {}
    record Rest(double seconds) implements Action {}
}
