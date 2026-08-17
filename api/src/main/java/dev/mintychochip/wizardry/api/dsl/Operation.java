package dev.mintychochip.wizardry.api.dsl;

import java.util.Objects;

public sealed interface Operation permits Operation.TargetRay, Operation.Damage, Operation.Heal, Operation.Push, Operation.Cooldown {
    enum Target { SELF, TARGET }
    enum DamageType { PHYSICAL, FIRE, FROST, ARCANE }

    record TargetRay(double range) implements Operation {}

    record Damage(Target target, DamageType damageType, double amount) implements Operation {
        public Damage {
            target = Objects.requireNonNull(target, "target");
            damageType = Objects.requireNonNull(damageType, "damageType");
        }
    }

    record Heal(Target target, double amount) implements Operation {
        public Heal {
            target = Objects.requireNonNull(target, "target");
        }
    }

    record Push(Target target, double strength) implements Operation {
        public Push {
            target = Objects.requireNonNull(target, "target");
        }
    }

    record Cooldown(double seconds) implements Operation {}
}
