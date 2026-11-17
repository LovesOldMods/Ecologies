package com.ecologies.worldgen;

import com.ecologies.Ecologies;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.Regions;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
//? if <26.1 {
/*import net.minecraft.world.level.levelgen.SurfaceRules;
import terrablender.api.SurfaceRuleManager;
*///?}
//? if >=26.1 {
import terrablender.api.MaterialRuleManager;
//?}

// 1.21.1 reads the surface rules from ecologies/surface_rules.json in the jar.
public final class EcoWorldgen {
    private EcoWorldgen() {}

    public static void register(IEventBus modBus) {
        EcoFeatures.register(modBus);
        modBus.addListener(EcoWorldgen::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new TundraRegion());
            //? if >=26.1 {
            MaterialRuleManager.addRules(MaterialRuleManager.RuleCategory.OVERWORLD, Ecologies.MOD_ID,
                    ResourceKey.create(Registries.MATERIAL_RULE, Ecologies.id("overworld")));
            //?} else {
            /*try (var in = EcoWorldgen.class.getResourceAsStream("/ecologies/surface_rules.json")) {
                var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                var rules = SurfaceRules.RuleSource.CODEC
                        .parse(JsonOps.INSTANCE, json).getOrThrow();
                SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Ecologies.MOD_ID, rules);
            } catch (Exception e) {
                Ecologies.LOGGER.error("Couldn't load the tundra surface rules", e);
            }
            *///?}
        });
    }
}
