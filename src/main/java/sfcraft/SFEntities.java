package sfcraft;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.ib67.sfcraft.SFCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import sfcraft.entity.BombEntity;

public class SFEntities {
    public static final EntityType<BombEntity> BOMB = register(
            "bomb",
            EntityType.Builder.<BombEntity>of(BombEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
    );

    public static void initialize() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name));
        var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
}
