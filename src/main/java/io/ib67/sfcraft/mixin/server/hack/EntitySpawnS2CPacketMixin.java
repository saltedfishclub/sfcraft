package io.ib67.sfcraft.mixin.server.hack;

import io.ib67.sfcraft.SFCraftInitializer;
import io.ib67.sfcraft.SFEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntitySpawnS2CPacket.class)
public class EntitySpawnS2CPacketMixin {
    @ModifyArg(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/PacketCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V"), index = 1)
    public Object mapModType(Object type) {
        return SFEntityType.mapToVanilla((EntityType<?>) type);
    }
}
