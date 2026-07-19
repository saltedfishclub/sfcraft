package sfcraft;

import io.ib67.sfcraft.SFCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import sfcraft.effect.CommanderMobEffect;

public class SFMobEffects {
    // 注册须发生在 ModInit(GameExtensionModule.onInitialize),此时静态注册表尚未冻结
    public static final Holder<MobEffect> COMMANDER = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "commander"),
            new CommanderMobEffect());

    public static void initialize() {
    }
}
