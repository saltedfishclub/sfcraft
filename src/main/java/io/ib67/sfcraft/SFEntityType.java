package io.ib67.sfcraft;

import io.ib67.sfcraft.entity.SFGuiderEntity;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class SFEntityType {
    public static final EntityType<SFGuiderEntity> GUIDER = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("sfcraft", "guider"),
            EntityType.Builder.<SFGuiderEntity>create(SFGuiderEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.35F, 0.6F)
                    .eyeHeight(0.36F)
                    .vehicleAttachment(0.04F)
                    .maxTrackingRange(8)
                    .trackingTickInterval(2)
                    .disableSaving()
                    .build()
    );
    private static SFEntityType registry;
    private final Map<EntityType<?>, EntityType<?>> mappedEntityTypes = new ConcurrentHashMap<>();

    private void init() {
        register(GUIDER, EntityType.ALLAY, SFGuiderEntity.createAllayAttributes().build());
    }

    private <A extends LivingEntity, B extends LivingEntity> void register(EntityType<B> custom, EntityType<A> origin, DefaultAttributeContainer attr) {
        FabricDefaultAttributeRegistry.register(custom, attr);
        mappedEntityTypes.put(custom, origin);
    }

    static void registerEntities() {
        registry = new SFEntityType();
        registry.init();
    }

    public static <T extends LivingEntity> EntityType<T> mapToVanilla(EntityType<?> type) {
        return (EntityType<T>) registry.mappedEntityTypes.getOrDefault(type, type);
    }
}
