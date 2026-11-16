package com.b6core.registry;

import com.b6core.B6Mod;
import com.b6core.entity.B6Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, B6Mod.MOD_ID);

    public static final RegistryObject<EntityType<B6Entity>> B6 = ENTITIES.register("b6",
            () -> EntityType.Builder.of(B6Entity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(B6Mod.MOD_ID, "b6").toString()));

    private ModEntities() {}
}
