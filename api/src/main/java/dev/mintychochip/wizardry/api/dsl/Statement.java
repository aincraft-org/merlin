package dev.mintychochip.wizardry.api.dsl;

import dev.mintychochip.wizardry.api.dsl.Operation;
import dev.mintychochip.wizardry.api.dsl.Span;

public sealed interface Statement permits Statement.TargetRay, Statement.Damage, Statement.Heal,
        Statement.Push, Statement.Cooldown {
    Span span();

    record TargetRay(double range, Span span) implements Statement {}
    record Damage(Operation.Target target, Operation.DamageType damageType, double amount, Span span)
            implements Statement {}
    record Heal(Operation.Target target, double amount, Span span) implements Statement {}
    record Push(Operation.Target target, double strength, Span span) implements Statement {}
    record Cooldown(double seconds, Span span) implements Statement {}
}
