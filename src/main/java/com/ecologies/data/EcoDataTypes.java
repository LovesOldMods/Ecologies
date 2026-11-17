package com.ecologies.data;

import com.ecologies.Ecologies;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
//? if <26.1 {
/*import net.neoforged.neoforge.event.AddReloadListenerEvent;
*///?}
//? if >=26.1 {
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
//?}

public final class EcoDataTypes {
    public static final EcoData<MushroomSpecies> MUSHROOM_FARM = new EcoData<>("mushroom_farm", MushroomSpecies.CODEC);

    public static final EcoData<PredatorRules> PREDATION = new EcoData<>("predation", PredatorRules.CODEC);

    public static final EcoData<SubstrateRules> SUBSTRATE = new EcoData<>("substrate", SubstrateRules.CODEC);

    public static final EcoData<AnimalRules.Grazing> GRAZING = new EcoData<>("grazing", AnimalRules.Grazing.CODEC);
    public static final EcoData<AnimalRules.Herding> HERDING = new EcoData<>("herding", AnimalRules.Herding.CODEC);
    public static final EcoData<AnimalRules.NaturalBreeding> NATURAL_BREEDING = new EcoData<>("natural_breeding", AnimalRules.NaturalBreeding.CODEC);

    public static final EcoData<RoostRules> ROOST = new EcoData<>("roost", RoostRules.CODEC);

    public static final EcoData<DefenseRules> DEFENSE = new EcoData<>("defense", DefenseRules.CODEC);

    public static final List<EcoData<?>> ALL = List.of(MUSHROOM_FARM, PREDATION, SUBSTRATE, GRAZING, HERDING, NATURAL_BREEDING, ROOST, DEFENSE);

    // Fall back to the built-in bat rules.
    public static RoostRules roostRules(EntityType<?> type) {
        for (RoostRules rules : ROOST.values()) {
            if (rules.mob().test(type)) return rules;
        }
        return RoostRules.DEFAULT;
    }

    public static <T> T forAnimal(EcoData<T> data, EntityType<?> type,
                                  Function<T, Matcher> animal) {
        for (T rules : data.values()) {
            if (animal.apply(rules).test(type)) return rules;
        }
        return null;
    }

    private EcoDataTypes() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EcoDataTypes::onAddReloadListeners);
    }

    //? if >=26.1 {
    private static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        for (EcoData<?> data : ALL) {
            event.addListener(Ecologies.id("ecologies_" + data.folder()), data);
        }
    }
    //?} else {
    /*private static void onAddReloadListeners(AddReloadListenerEvent event) {
        for (EcoData<?> data : ALL) {
            event.addListener(data);
        }
    }
    *///?}

    public static PredatorRules predatorRules(EntityType<?> type) {
        for (PredatorRules rules : PREDATION.values()) {
            if (rules.predator().test(type)) return rules;
        }
        return null;
    }

    public static SubstrateRules substrateRules(BlockState state) {
        for (SubstrateRules rules : SUBSTRATE.values()) {
            if (rules.actor().test(state)) return rules;
        }
        return null;
    }

    public static Map.Entry<Identifier, MushroomSpecies> speciesFor(ItemStack stack) {
        for (Map.Entry<Identifier, MushroomSpecies> entry : MUSHROOM_FARM.entries().entrySet()) {
            if (entry.getValue().input().test(stack)) return entry;
        }
        return null;
    }
}
