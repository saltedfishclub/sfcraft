package sfcraft;

import io.ib67.sfcraft.SFCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * 斩首附魔为数据驱动内容(data/sfcraft/enchantment/beheading.json),无需代码注册。
 * 本类只持有 ResourceKey,并提供运行时按 key 取 Holder 的辅助方法。
 */
public class SFEnchantments {
    public static final ResourceKey<Enchantment> BEHEADING = ResourceKey.create(
            Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "beheading"));

    private SFEnchantments() {
    }

    public static Optional<Holder.Reference<Enchantment>> getBeheading(RegistryAccess registryAccess) {
        return registryAccess.lookup(Registries.ENCHANTMENT).flatMap(registry -> registry.get(BEHEADING));
    }
}
