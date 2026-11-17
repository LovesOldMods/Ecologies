package com.ecologies;

import com.ecologies.data.EcoDataTypes;
import com.ecologies.ecology.BatEvents;
import com.ecologies.ecology.Crops;
import com.ecologies.ecology.Herds;
import com.ecologies.ecology.LichenEvents;
import com.ecologies.ecology.Predation;
import com.ecologies.registry.EcoBlockEntities;
import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoEntities;
import com.ecologies.registry.EcoItems;
import com.ecologies.registry.EcoParticles;
import com.ecologies.worldgen.EcoWorldgen;
import com.ecologies.worldgen.TundraEvents;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Ecologies.MOD_ID)
public final class Ecologies {
    public static final String MOD_ID = "ecologies";
    public static final Logger LOGGER = LoggerFactory.getLogger("Ecologies");
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";

    public Ecologies(IEventBus modBus, ModContainer container) {
        LOGGER.info("Ecologies {} for Minecraft {}", VERSION, MINECRAFT);
        //? if >=26.1 {
        container.registerConfig(ModConfig.Type.LOCAL, EcoConfig.SPEC, "ecologies.toml");
        //?} else
        /*container.registerConfig(ModConfig.Type.COMMON, EcoConfig.SPEC, "ecologies.toml");*/
        EcoBlocks.register(modBus);
        EcoItems.register(modBus);
        EcoBlockEntities.register(modBus);
        EcoEntities.register(modBus);
        EcoParticles.register(modBus);
        EcoDataTypes.register();
        Predation.register();
        LichenEvents.register();
        Herds.register();
        BatEvents.register();
        Crops.register(modBus);
        EcoCommands.register();
        EcoWorldgen.register(modBus);
        TundraEvents.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
