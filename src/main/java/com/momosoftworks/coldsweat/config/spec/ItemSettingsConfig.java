package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.util.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> BOILER_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> ICEBOX_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> HEARTH_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEARTH_POTION_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue ALLOW_POTIONS_IN_HEARTH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> SOULSPRING_LAMP_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOULSPRING_LAMP_DIMENSIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> FOOD_TEMPERATURES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> CARRIED_ITEM_TEMPERATURE;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATION_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INSULATION_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATING_ARMOR;
    public static final ForgeConfigSpec.ConfigValue<List<?>> INSULATION_SLOTS;
    public static final ForgeConfigSpec.DoubleValue INSULATION_STRENGTH;

    public static final ForgeConfigSpec.IntValue WATERSKIN_STRENGTH;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATING_CURIOS;

    private static final ItemSettingsConfig INSTANCE = new ItemSettingsConfig();

    static
    {
        /*
          Fuel Items
         */
        BUILDER.push("Fuel Items")
                .comment("Defines items that can be used as fuel",
                         "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]");
        BOILER_FUELS = BUILDER
                .defineListAllowEmpty(Arrays.asList("Boiler"), () -> ListBuilder.begin(
                                Arrays.asList("#minecraft:planks",         10),
                                Arrays.asList("minecraft:coal",            37),
                                Arrays.asList("minecraft:charcoal",        37),
                                Arrays.asList("#minecraft:logs_that_burn", 37),
                                Arrays.asList("minecraft:coal_block",      333),
                                Arrays.asList("minecraft:magma_block",     333),
                                Arrays.asList("minecraft:lava_bucket",     1000)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        ICEBOX_FUELS = BUILDER
                .defineListAllowEmpty(Arrays.asList("Icebox"), () -> ListBuilder.begin(
                                Arrays.asList("minecraft:snowball",           10),
                                Arrays.asList("minecraft:clay_ball",          37),
                                Arrays.asList("minecraft:snow_block",         100),
                                Arrays.asList("minecraft:ice",                250),
                                Arrays.asList("minecraft:clay",               333),
                                Arrays.asList("minecraft:powder_snow_bucket", 100),
                                //List.of("minecraft:water_bucket",       1000),
                                Arrays.asList("minecraft:packed_ice",         1000)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        HEARTH_FUELS = BUILDER
                .comment("Negative values indicate cold fuel")
                .defineListAllowEmpty(Collections.singletonList("Hearth"), () -> ListBuilder.begin(
                                // Hot
                                Arrays.asList("#minecraft:planks",         10),
                                Arrays.asList("minecraft:coal",            37),
                                Arrays.asList("minecraft:charcoal",        37),
                                Arrays.asList("#minecraft:logs_that_burn", 37),
                                Arrays.asList("minecraft:coal_block",      333),
                                Arrays.asList("minecraft:magma_block",     333),
                                Arrays.asList("minecraft:lava_bucket",     1000),
                                // Cold
                                Arrays.asList("minecraft:snowball",           -37),
                                Arrays.asList("minecraft:clay_ball",          -37),
                                Arrays.asList("minecraft:snow_block",         -333),
                                Arrays.asList("minecraft:ice",                -333),
                                Arrays.asList("minecraft:clay",               -333),
                                Arrays.asList("minecraft:powder_snow_bucket", -333),
                                Arrays.asList("minecraft:water_bucket",       -1000),
                                Arrays.asList("minecraft:packed_ice",         -1000)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });
        HEARTH_POTION_BLACKLIST = BUILDER
                .comment("Potions containing any of these effects will not be allowed in the hearth",
                         "Format: [\"effect_id\", \"effect_id\", ...etc]")
                .defineListAllowEmpty(Arrays.asList("Blacklisted Hearth Potions"), () -> ListBuilder.begin(
                                "minecraft:instant_damage",
                                "minecraft:poison",
                                "minecraft:wither",
                                "minecraft:weakness",
                                "minecraft:mining_fatigue",
                                "minecraft:slowness"
                        ).build(),
                        it -> it instanceof String);
        ALLOW_POTIONS_IN_HEARTH = BUILDER
                .comment("If true, potions can be used as fuel in the hearth",
                         "This gives all players in range the potion effect")
                .define("Allow Potions in Hearth", true);
        BUILDER.pop();

        /*
          Soulspring Lamp Items
         */
        BUILDER.push("Soulspring Lamp");
        SOULSPRING_LAMP_FUELS = BUILDER
                .comment("Defines items that the Soulspring Lamp can use as fuel",
                        "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]")
                .defineListAllowEmpty(Arrays.asList("Fuel Items"), () -> ListBuilder.<List<?>>begin(
                                    Arrays.asList("cold_sweat:soul_sprout", 4)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        SOULSPRING_LAMP_DIMENSIONS = BUILDER
                .comment("Defines the dimensions that the Soulspring Lamp can be used in",
                        "Format: [\"dimension-id-1\", \"dimension-id-2\", ...etc]")
                .defineListAllowEmpty(Collections.singletonList("Valid Dimensions"), () -> ListBuilder.begin(
                                "minecraft:the_nether"
                        ).build(),
                        it -> it instanceof String);
        BUILDER.pop();

        /*
         Insulation
         */
        BUILDER.push("Insulation");
        INSULATION_ITEMS = BUILDER
                .comment("Defines the items that can be used for insulating armor in the Sewing Table",
                         "Format: [[\"item_id\", cold, hot, \"static\", *nbt], [\"item_id\", amount, adapt-speed, \"adaptive\", *nbt], ...etc]",
                         "\"item_id\": The item's ID (i.e. \"minecraft:iron_ingot\"). Accepts tags with \"#\" (i.e. \"#minecraft:wool\").",
                         "",
                         "Adaptive Insulation: ",
                         "\"amount\": The amount of insulation the item provides.",
                         "\"adapt-speed\": The speed at which the insulation adapts to the environment.",
                         "*\"type\": Optional. Either \"static\" or \"adaptive\". Defines the insulation type. Defaults to static.",
                         "*\"nbt\": Optional. If set, the item will only provide insulation if it has the specified NBT tag.",
                         "",
                         "Static Insulation: ",
                         "\"cold\": The amount of cold insulation the item provides.",
                         "\"hot\": The amount of heat insulation the item provides.",
                         "*\"type\": Optional. Either \"static\" or \"adaptive\". Defines the insulation type. Defaults to static.",
                         "*\"nbt\": Optional. If set, the item will only provide insulation if it has the specified NBT tag."
                )
                .defineListAllowEmpty(Arrays.asList("Insulation Ingredients"), () -> ListBuilder.begin(
                                Arrays.asList("minecraft:leather_helmet",     4,  4),
                                Arrays.asList("minecraft:leather_chestplate", 6,  6),
                                Arrays.asList("minecraft:leather_leggings",   5,  5),
                                Arrays.asList("minecraft:leather_boots",      4,  4),
                                Arrays.asList("minecraft:leather",            1,  1),
                                Arrays.asList("cold_sweat:chameleon_molt",    2,  0.0085, "adaptive"),
                                Arrays.asList("cold_sweat:hoglin_hide",       0,  2),
                                Arrays.asList("cold_sweat:goat_fur",          2,  0),
                                Arrays.asList("#minecraft:wool",              1.5, 0),
                                Arrays.asList("minecraft:rabbit_hide",        0,  1.5),
                                Arrays.asList("cold_sweat:hoglin_headpiece",  0,  8),
                                Arrays.asList("cold_sweat:hoglin_tunic",      0,  12),
                                Arrays.asList("cold_sweat:hoglin_trousers",   0,  10),
                                Arrays.asList("cold_sweat:hoglin_hooves",     0,  8),
                                Arrays.asList("cold_sweat:goat_fur_cap",      8,  0),
                                Arrays.asList("cold_sweat:goat_fur_parka",    12, 0),
                                Arrays.asList("cold_sweat:goat_fur_pants",    10, 0),
                                Arrays.asList("cold_sweat:goat_fur_boots",    8,  0))
                            .addIf(CompatManager.isEnvironmentalLoaded(),
                                () -> Arrays.asList("environmental:yak_hair", 1.5, -1)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {
                                List<?> list = ((List<?>) it);
                                return list.size() >= 3
                                    && list.get(0) instanceof String
                                    && list.get(1) instanceof Number
                                    && list.get(2) instanceof Number
                                    && (list.size() < 4 || list.get(3) instanceof String)
                                    && (list.size() < 5 || list.get(4) instanceof String);
                            }
                            return false;
                        });

        INSULATING_ARMOR = BUILDER
                .comment("Defines the items that provide insulation when worn",
                        "See Insulation Ingredients for formatting")
                .defineListAllowEmpty(Arrays.asList("Insulating Armor"), () -> ListBuilder.begin(
                                Arrays.asList("minecraft:leather_helmet",      4,  4),
                                Arrays.asList("minecraft:leather_chestplate",  6,  6),
                                Arrays.asList("minecraft:leather_leggings",    5,  5),
                                Arrays.asList("minecraft:leather_boots",       4,  4),
                                Arrays.asList("cold_sweat:hoglin_headpiece",   0,  8),
                                Arrays.asList("cold_sweat:hoglin_tunic",       0,  12),
                                Arrays.asList("cold_sweat:hoglin_trousers",    0,  10),
                                Arrays.asList("cold_sweat:hoglin_hooves",      0,  8),
                                Arrays.asList("cold_sweat:goat_fur_cap",       8,  0),
                                Arrays.asList("cold_sweat:goat_fur_parka",     12, 0),
                                Arrays.asList("cold_sweat:goat_fur_pants",     10, 0),
                                Arrays.asList("cold_sweat:goat_fur_boots",     8,  0))
                            .addIf(CompatManager.isEnvironmentalLoaded(),
                                () -> Arrays.asList("environmental:yak_pants", 7.5, -5)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {
                                List<?> list = ((List<?>) it);
                                return list.size() >= 3
                                        && list.get(0) instanceof String
                                        && list.get(1) instanceof Number
                                        && list.get(2) instanceof Number
                                        && (list.size() < 4 || list.get(3) instanceof String)
                                        && (list.size() < 5 || list.get(4) instanceof String);
                            }
                            return false;
                        });

        if (CompatManager.isCuriosLoaded())
        {
            INSULATING_CURIOS = BUILDER
                    .comment("Defines the items that provide insulation when worn in a curio slot",
                             "See Insulation Ingredients for formatting")
                    .defineListAllowEmpty(Arrays.asList("Insulating Curios"), () -> Arrays.asList(
                            // Nothing defined
                        ),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {
                                List<?> list = ((List<?>) it);
                                return list.size() >= 3
                                        && list.get(0) instanceof String
                                        && list.get(1) instanceof Number
                                        && list.get(2) instanceof Number
                                        && (list.size() < 4 || list.get(3) instanceof String)
                                        && (list.size() < 5 || list.get(4) instanceof String);
                            }
                            return false;
                        });
        }
        else INSULATING_CURIOS = null;

        INSULATION_SLOTS = BUILDER
                .comment("Defines how many insulation slots armor pieces have",
                         "There are 4 modes for this setting:",
                         "Static: Each armor slot (head, body, legs, feet) has a fixed number of insulation slots",
                         "- Format: [head, body, legs, feet] (a list of integers; insulation slot count for each armor slot)",
                         " ",
                         "Linear: Number of slots increases steadily with protection",
                         "Exponential: Number of slots increases rapidly with protection",
                         "Logarithmic: Number of slots increases with protection, with diminishing returns",
                         "- Format: [number, max-slots] (a positive integer or decimal; the rate of increase)")
                .defineList("Insulation Slots", Arrays.asList("static", 4, 6, 5, 4),
                        it -> it instanceof Number || it instanceof String);

        INSULATION_STRENGTH = BUILDER
                .comment("Defines the effectiveness of insulating items in protecting against temperature")
                .defineInRange("Insulation Strength", 1.0, 0, Double.MAX_VALUE);

        INSULATION_BLACKLIST = BUILDER
                .comment("Defines wearable items that cannot be insulated",
                        "Format: [\"item_id\", \"item_id\", ...etc]")
                .defineListAllowEmpty(Collections.singletonList("Insulation Blacklist"), () -> Arrays.asList(
                ),
                it -> it instanceof String);

        BUILDER.pop();

        /*
         Consumables
         */
        BUILDER.push("Consumables");
        FOOD_TEMPERATURES = BUILDER
                .comment("Defines items that affect the player's temperature when consumed",
                        "Format: [[\"item_id\", amount, *nbt, *duration], [\"item_id\", amount, *nbt, *duration], ...etc]",
                        "Negative values are cold foods, positive values are hot foods",
                        "nbt: Optional. If set, the item will only affect the player's temperature if it has the specified NBT tag.",
                        "duration: Optional. If set, the player's temperature will remain increased/decreased for this amount of time.")
                .defineListAllowEmpty(Arrays.asList("Temperature-Affecting Foods"), () -> Arrays.asList(
                        Arrays.asList("cold_sweat:soul_sprout", -20, "{}", 1200)
                ),
                it ->
                {
                    if (it instanceof List<?>)
                    {
                        List<?> list = ((List<?>) it);
                        return list.size() >= 2
                            && list.get(0) instanceof String
                            && list.get(1) instanceof Number
                            && (list.size() < 3 || list.get(2) instanceof String)
                            && (list.size() < 4 || list.get(3) instanceof Number);
                    }
                    return false;
                });
        WATERSKIN_STRENGTH = BUILDER
                .comment("Defines how much a waterskin will change the player's body temperature by when used")
                .defineInRange("Waterskin Strength", 50, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        /*
         Misc
         */
        BUILDER.push("Misc");

        CARRIED_ITEM_TEMPERATURE = BUILDER
                .comment("Defines items that affect the player's temperature when in the inventory",
                         "Format: [[\"item_id\", temperature, strict_type, trait, *nbt, *max_effect], [\"item_id\", temperature, strict_type, trait, *nbt, *max_effect], ...etc]",
                         "temperature: The temperature change the item will apply to the entity. For core temperature, this is applied every tick",
                         "strict_type: Either \"inventory\", \"hotbar\", or \"hand\". Defines what slots the item must be in to apply to the entity",
                         "trait: The temperature trait to apply the effect to. Typical values are \"core\" for body temperature or \"world\" for ambient temperature. More on the mod documentation page.",
                         "nbt: Optional. The NBT data the item must have to apply to the entity.",
                         "max_effect: Optional. The maximum temperature effect the item can apply to the entity.")
                .defineListAllowEmpty(Arrays.asList("Carried Item Temperatures"), () -> Arrays.asList(
                ),
                it ->
                {

                    if (!(it instanceof List<?>)) return false;
                    List<?> list = ((List<?>) it);

                    return CSMath.betweenInclusive(list.size(), 4, 6)
                        && list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && list.get(2) instanceof String
                        && list.get(3) instanceof String
                        && (list.size() < 5 || list.get(4) instanceof String)
                        && (list.size() < 6 || list.get(5) instanceof Number);
                });

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "coldsweat/item_settings.toml");
    }

    public void save()
    {   SPEC.save();
    }

    public static ItemSettingsConfig getInstance()
    {   return INSTANCE;
    }

    /* Getters */

    public List<? extends List<?>> getBoilerFuelItems()
    {   return BOILER_FUELS.get();
    }

    public List<? extends List<?>> getIceboxFuelItems()
    {   return ICEBOX_FUELS.get();
    }

    public List<? extends List<?>> getHearthFuelItems()
    {   return HEARTH_FUELS.get();
    }

    public List<? extends List<?>> getInsulationItems()
    {   return INSULATION_ITEMS.get();
    }

    public List<? extends List<?>> getInsulatingArmorItems()
    {   return INSULATING_ARMOR.get();
    }

    public List<?> getArmorInsulationSlots()
    {   return INSULATION_SLOTS.get();
    }

    public double getInsulationStrength()
    {   return INSULATION_STRENGTH.get();
    }

    public List<? extends String> getInsulationBlacklist()
    {   return INSULATION_BLACKLIST.get();
    }

    public List<? extends List<?>> getSoulLampFuelItems()
    {   return SOULSPRING_LAMP_FUELS.get();
    }

    public List<? extends List<?>> getFoodTemperatures()
    {   return FOOD_TEMPERATURES.get();
    }

    public List<? extends String> getValidSoulLampDimensions()
    {   return SOULSPRING_LAMP_DIMENSIONS.get();
    }

    public int getWaterskinStrength()
    {   return WATERSKIN_STRENGTH.get();
    }

    public boolean arePotionsEnabled()
    {   return ALLOW_POTIONS_IN_HEARTH.get();
    }

    public List<String> getPotionBlacklist()
    {   return (List<String>) HEARTH_POTION_BLACKLIST.get();
    }

    public List<? extends List<?>> getInsulatingCurios()
    {   return CompatManager.isCuriosLoaded() ? INSULATING_CURIOS.get() : Arrays.asList();
    }

    public List<? extends List<?>> getCarriedTemps()
    {   return CARRIED_ITEM_TEMPERATURE.get();
    }

    /* Setters */

    public synchronized void setBoilerFuelItems(List<? extends List<?>> itemMap)
    {   synchronized (BOILER_FUELS)
        {   BOILER_FUELS.set(itemMap);
        }
    }

    public synchronized void setIceboxFuelItems(List<? extends List<?>> itemMap)
    {   synchronized (ICEBOX_FUELS)
        {   ICEBOX_FUELS.set(itemMap);
        }
    }

    public synchronized void setHearthFuelItems(List<? extends List<?>> itemMap)
    {   synchronized (HEARTH_FUELS)
        {   HEARTH_FUELS.set(itemMap);
        }
    }

    public synchronized void setInsulationItems(List<? extends List<?>> items)
    {   synchronized (INSULATION_ITEMS)
        {   INSULATION_ITEMS.set(items);
        }
    }

    public synchronized void setInsulatingArmorItems(List<? extends List<?>> itemMap)
    {   synchronized (INSULATING_ARMOR)
        {   INSULATING_ARMOR.set(itemMap);
        }
    }

    public synchronized void setArmorInsulationSlots(List<?> slots)
    {   synchronized (INSULATION_SLOTS)
        {   INSULATION_SLOTS.set(slots);
        }
    }

    public synchronized void setInsulationStrength(double effectiveness)
    {   synchronized (INSULATION_STRENGTH)
        {   INSULATION_STRENGTH.set(effectiveness);
        }
    }

    public synchronized void setSoulLampFuelItems(List<? extends List<?>> items)
    {   synchronized (SOULSPRING_LAMP_FUELS)
        {   SOULSPRING_LAMP_FUELS.set(items);
        }
    }

    public synchronized void setFoodTemperatures(List<? extends List<?>> itemMap)
    {   synchronized (FOOD_TEMPERATURES)
        {   FOOD_TEMPERATURES.set(itemMap);
        }
    }

    public synchronized void setValidSoulLampDimensions(List<? extends String> items)
    {   synchronized (SOULSPRING_LAMP_DIMENSIONS)
        {   SOULSPRING_LAMP_DIMENSIONS.set(items);
        }
    }

    public synchronized void setWaterskinStrength(int strength)
    {   synchronized (WATERSKIN_STRENGTH)
        {   WATERSKIN_STRENGTH.set(strength);
        }
    }

    public synchronized void setPotionsEnabled(Boolean saver)
    {   synchronized (ALLOW_POTIONS_IN_HEARTH)
        {   ALLOW_POTIONS_IN_HEARTH.set(saver);
        }
    }

    public synchronized void setPotionBlacklist(List<String> saver)
    {   synchronized (HEARTH_POTION_BLACKLIST)
        {   HEARTH_POTION_BLACKLIST.set(saver);
        }
    }

    public synchronized void setInsulationBlacklist(List<String> blacklist)
    {   synchronized (INSULATION_BLACKLIST)
        {   INSULATION_BLACKLIST.set(blacklist);
        }
    }

    public synchronized void setInsulatingCurios(List<? extends List<?>> items)
    {   if (CompatManager.isCuriosLoaded())
        {   synchronized (INSULATING_CURIOS)
            {   INSULATING_CURIOS.set(items);
            }
        }
    }

    public synchronized void setCarriedTemps(List<? extends List<?>> items)
    {   synchronized (CARRIED_ITEM_TEMPERATURE)
        {   CARRIED_ITEM_TEMPERATURE.set(items);
        }
    }
}
