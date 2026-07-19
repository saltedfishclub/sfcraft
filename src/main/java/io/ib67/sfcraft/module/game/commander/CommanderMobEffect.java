package io.ib67.sfcraft.module.game.commander;

import eu.pb4.polymer.core.api.other.PolymerMobEffect;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;

/**
 * 「统帅」药水效果。行为(紫光、纽带、为周围怪物供 buff、死亡即失效)由
 * {@link CommanderModule} 驱动;本类只承载注册与 Polymer 客户端替身。
 *
 * <p>MobEffect 是静态非同步注册表,自定义效果必须实现 {@link PolymerMobEffect}
 * 并在 {@link #getPolymerReplacement} 返回原版效果,否则原版客户端收到未知 id 会错乱。
 */
public class CommanderMobEffect extends MobEffect implements PolymerMobEffect {
    public CommanderMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0xAA00FF); // 紫色
    }

    @Override
    public MobEffect getPolymerReplacement(MobEffect effect, PacketContext context) {
        // 原版客户端见到的替身:发光(无害,且与我们用 GLOWING+队伍颜色实现的紫光一致)
        return MobEffects.GLOWING.value();
    }
}
