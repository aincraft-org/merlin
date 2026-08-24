package dev.mintychochip.merlin.api.dsl;

public sealed interface Phrase permits Phrase.LookAhead, Phrase.Summon, Phrase.Burn, Phrase.Mend,
        Phrase.Shove, Phrase.Strike, Phrase.SendSkyward, Phrase.Vanish, Phrase.Rest {
    Span span();

    record LookAhead(double range, Span span) implements Phrase {}
    record Summon(Action.Noun noun, Action.Place place, Action.Noun riding, Span span) implements Phrase {}
    record Burn(Action.Patient patient, double amount, Span span) implements Phrase {}
    record Mend(Action.Patient patient, double amount, Span span) implements Phrase {}
    record Shove(Action.Patient patient, double amount, Span span) implements Phrase {}
    record Strike(Action.Place place, Span span) implements Phrase {}
    record SendSkyward(Span span) implements Phrase {}
    record Vanish(Action.Patient patient, double seconds, Span span) implements Phrase {}
    record Rest(double seconds, Span span) implements Phrase {}
}
