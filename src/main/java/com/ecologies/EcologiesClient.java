package com.ecologies;

import com.ecologies.block.SkinnyLogBlock;
import com.ecologies.client.Flurries;
import com.ecologies.client.FlurryParticle;
import com.ecologies.client.MushroomFarmRenderer;
import com.ecologies.client.MuskoxModel;
import com.ecologies.client.MuskoxRenderer;
import com.ecologies.registry.EcoBlockEntities;
import com.ecologies.registry.EcoEntities;
import com.ecologies.registry.EcoParticles;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
//? if >=26.1 {
import net.minecraft.client.color.block.BlockTintSources;
//?}

@Mod(value = Ecologies.MOD_ID, dist = Dist.CLIENT)
public final class EcologiesClient {
    public EcologiesClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(EcologiesClient::registerRenderers);
        modBus.addListener(EcologiesClient::registerLayers);
        modBus.addListener(EcologiesClient::registerParticles);
        modBus.addListener(EcologiesClient::registerColors);
        NeoForge.EVENT_BUS.addListener(
                (ClientTickEvent.Post event) -> Flurries.tick());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EcoBlockEntities.MUSHROOM_FARM.get(), MushroomFarmRenderer::new);
        event.registerEntityRenderer(EcoEntities.MUSKOX.get(), MuskoxRenderer::new);
        event.registerEntityRenderer(EcoEntities.COLONY_BAT.get(), BatRenderer::new);
    }

    //? if >=26.1 {
    private static void registerColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(BlockTintSources.constant(
                FoliageColor.FOLIAGE_EVERGREEN)), skinnyLogs());
    }
    //?} else {
    /*private static void registerColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? FoliageColor.getEvergreenColor() : -1, skinnyLogs());
    }
    *///?}

    private static Block[] skinnyLogs() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof SkinnyLogBlock).toArray(Block[]::new);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(EcoParticles.FLURRY.get(), FlurryParticle.Provider::new);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MuskoxModel.ADULT, MuskoxModel::createAdultLayer);
        event.registerLayerDefinition(MuskoxModel.BABY, MuskoxModel::createBabyLayer);
    }
}
