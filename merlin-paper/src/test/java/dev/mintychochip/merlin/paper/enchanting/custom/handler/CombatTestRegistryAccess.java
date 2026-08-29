package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffectType;
import org.mockito.Mockito;

/** Supplies the API-only test runtime with the registry values needed by Bukkit value classes. */
public final class CombatTestRegistryAccess implements RegistryAccess {
    private final Map<String, Registry<Keyed>> registries = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        return (Registry<T>) registries.computeIfAbsent(String.valueOf(key), this::registryProxy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return (Registry<T>) registries.computeIfAbsent(type.getName(), this::registryProxy);
    }

    @SuppressWarnings("unchecked")
    private Registry<Keyed> registryProxy(String kind) {
        Map<String, Object> values = new HashMap<>();
        return (Registry<Keyed>) Proxy.newProxyInstance(
                Registry.class.getClassLoader(),
                new Class<?>[]{Registry.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getOrThrow") && args != null && args.length == 1) {
                        String key = String.valueOf(args[0]);
                        return values.computeIfAbsent(key, value -> valueFor(kind, value));
                    }
                    if (method.getName().equals("get") && args != null && args.length == 1) {
                        return values.get(String.valueOf(args[0]));
                    }
                    if (method.getName().equals("toString")) return "combat-test-registry:" + kind;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == double.class) return 0.0;
                    return null;
                });
    }

    private Object valueFor(String kind, String key) {
        String lowerKind = kind.toLowerCase();
        if (lowerKind.contains("attribute")) {
            return Mockito.mock(Attribute.class);
        }
        if (lowerKind.contains("mob_effect") || lowerKind.endsWith("potion.potioneffecttype")) {
            return Mockito.mock(PotionEffectType.class);
        }
        if (lowerKind.contains("item") || lowerKind.endsWith("inventory.itemtype")) {
            return Mockito.mock(ItemType.class);
        }
        return Mockito.mock(Keyed.class);
    }
}
