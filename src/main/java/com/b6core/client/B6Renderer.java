package com.b6core.client;

import com.b6core.B6Mod;
import com.b6core.entity.B6Entity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Draws B6 with the normal player model; the skin depends on his form. Always fully lit (glow). */
public class B6Renderer extends HumanoidMobRenderer<B6Entity, PlayerModel<B6Entity>> {
    private static final ResourceLocation TEX_MAIN = tex("b6_main");     // half shadow / half white
    private static final ResourceLocation TEX_GLITCH = tex("b6");        // black-and-white checker
    private static final ResourceLocation TEX_STONE = tex("b6_stone");   // dark stone, glowing cracks

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(B6Mod.MOD_ID, "textures/entity/" + name + ".png");
    }

    public B6Renderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(B6Entity entity) {
        return switch (entity.getForm()) {
            case B6Entity.FORM_GLITCH -> TEX_GLITCH;
            case B6Entity.FORM_STONE -> TEX_STONE;
            default -> TEX_MAIN;
        };
    }

    @Override
    protected int getBlockLightLevel(B6Entity entity, BlockPos pos) {
        return 15;
    }
}
