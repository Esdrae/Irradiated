package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.util.compat.CompatManager;
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
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> boilerItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> iceboxItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> hearthItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedPotions;
    private static final ForgeConfigSpec.BooleanValue allowPotionsInHearth;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> soulLampItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> soulLampDimensions;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> temperatureFoods;

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatingItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> insulationBlacklist;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatingArmor;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Number>> insulationSlots;

    private static final ForgeConfigSpec.IntValue waterskinStrength;

    private static ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatingCurios;

    static final ItemSettingsConfig INSTANCE = new ItemSettingsConfig();

    static
    {
        /*
          Fuel Items
         */
        BUILDER.push("Fuel Items")
                .comment("Defines items that can be used as fuel",
                         "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]");
        boilerItems = BUILDER
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

        iceboxItems = BUILDER
                .defineListAllowEmpty(Arrays.asList("Icebox"), () -> ListBuilder.begin(
                                Arrays.asList("minecraft:snowball",           37),
                                Arrays.asList("minecraft:clay_ball",          37),
                                Arrays.asList("minecraft:snow_block",         333),
                                Arrays.asList("minecraft:ice",                333),
                                Arrays.asList("minecraft:clay",               333),
                                Arrays.asList("minecraft:powder_snow_bucket", 333),
                                Arrays.asList("minecraft:water_bucket",       1000),
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

        hearthItems = BUILDER
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
        blacklistedPotions = BUILDER
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
        allowPotionsInHearth = BUILDER
                .comment("If true, potions can be used as fuel in the hearth",
                         "This gives all players in range the potion effect")
                .define("Allow Potions in Hearth", true);
        BUILDER.pop();

        /*
          Soulspring Lamp Items
         */
        BUILDER.push("Soulspring Lamp");
        soulLampItems = BUILDER
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

        soulLampDimensions = BUILDER
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
        insulatingItems = BUILDER
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
                                Arrays.asList("cold_sweat:chameleon_molt",    2, 0.0085, "adaptive"),
                                Arrays.asList("cold_sweat:hoglin_hide",       0,  2),
                                Arrays.asList("cold_sweat:fur",               2,  0),
                                Arrays.asList("#minecraft:wool",              1.5, 0),
                                Arrays.asList("minecraft:rabbit_hide",        0,  1.5),
                                Arrays.asList("cold_sweat:hoglin_headpiece",  0,  8),
                                Arrays.asList("cold_sweat:hoglin_tunic",      0,  12),
                                Arrays.asList("cold_sweat:hoglin_trousers",   0,  10),
                                Arrays.asList("cold_sweat:hoglin_hooves",     0,  8),
                                Arrays.asList("cold_sweat:fur_cap",           8,  0),
                                Arrays.asList("cold_sweat:fur_parka",         12, 0),
                                Arrays.asList("cold_sweat:fur_pants",         10, 0),
                                Arrays.asList("cold_sweat:fur_boots",         8,  0))
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

        insulatingArmor = BUILDER
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
                                Arrays.asList("cold_sweat:fur_cap",       8,  0),
                                Arrays.asList("cold_sweat:fur_parka",     12, 0),
                                Arrays.asList("cold_sweat:fur_pants",     10, 0),
                                Arrays.asList("cold_sweat:fur_boots",     8,  0))
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
            insulatingCurios = BUILDER
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

        insulationSlots = BUILDER
                .comment("Defines how many insulation slots armor pieces have",
                         "Format: [head, chest, legs, feet]")
                .defineList("Insulation Slots", Arrays.asList(4, 6, 5, 4),
                        it -> it instanceof Number);

        insulationBlacklist = BUILDER
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
        temperatureFoods = BUILDER
                .comment("Defines items that affect the player's temperature when consumed",
                        "Format: [[\"item_id\", amount], [\"item_id\", amount], ...etc]",
                        "Negative values are cold foods, positive values are hot foods")
                .defineListAllowEmpty(Arrays.asList("Temperature-Affecting Foods"), () -> Arrays.asList(
                        // Nothing defined
                ),
                it ->
                {
                    if (it instanceof List<?>)
                    {
                        List<?> list = ((List<?>) it);
                        return list.size() >= 2
                            && list.get(0) instanceof String
                            && list.get(1) instanceof Number
                            && (list.size() < 3 || list.get(2) instanceof String);
                    }
                    return false;
                });
        waterskinStrength = BUILDER
                .comment("Defines how much a waterskin will change the player's body temperature by when used")
                .defineInRange("Waterskin Strength", 50, 0, Integer.MAX_VALUE);
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

    public static ItemSettingsConfig getInstance()
    {   return INSTANCE;
    }

    public List<? extends List<?>> getBoilerFuelItems()
    {   return boilerItems.get();
    }

    public List<? extends List<?>> getIceboxFuelItems()
    {   return iceboxItems.get();
    }

    public List<? extends List<?>> getHearthFuelItems()
    {   return hearthItems.get();
    }

    public List<? extends List<?>> getInsulationItems()
    {   return insulatingItems.get();
    }

    public List<? extends List<?>> getInsulatingArmorItems()
    {   return insulatingArmor.get();
    }

    public List<? extends Number> getArmorInsulationSlots()
    {   return insulationSlots.get();
    }

    public List<? extends String> getInsulationBlacklist()
    {   return insulationBlacklist.get();
    }

    public List<? extends List<?>> getSoulLampFuelItems()
    {   return soulLampItems.get();
    }

    public List<? extends List<?>> getFoodTemperatures()
    {   return temperatureFoods.get();
    }

    public List<? extends String> getValidSoulLampDimensions()
    {   return soulLampDimensions.get();
    }

    public int getWaterskinStrength()
    {   return waterskinStrength.get();
    }

    public boolean arePotionsEnabled()
    {   return allowPotionsInHearth.get();
    }

    public List<String> getPotionBlacklist()
    {   return (List<String>) blacklistedPotions.get();
    }

    public List<? extends List<?>> getInsulatingCurios()
    {   return CompatManager.isCuriosLoaded() ? insulatingCurios.get() : Arrays.asList();
    }

    public synchronized void setBoilerFuelItems(List<? extends List<?>> itemMap)
    {   boilerItems.set(itemMap);
    }

    public synchronized void setIceboxFuelItems(List<? extends List<?>> itemMap)
    {   iceboxItems.set(itemMap);
    }

    public synchronized void setHearthFuelItems(List<? extends List<?>> itemMap)
    {   hearthItems.set(itemMap);
    }

    public synchronized void setInsulationItems(List<? extends List<?>> items)
    {   insulatingItems.set(items);
    }

    public synchronized void setInsulatingArmorItems(List<? extends List<?>> itemMap)
    {   insulatingArmor.set(itemMap);
    }

    public synchronized void setArmorInsulationSlots(List<? extends Number> slots)
    {   insulationSlots.set(slots);
    }

    public synchronized void setSoulLampFuelItems(List<? extends List<?>> items)
    {   soulLampItems.set(items);
    }

    public synchronized void setFoodTemperatures(List<? extends List<?>> itemMap)
    {   temperatureFoods.set(itemMap);
    }

    public synchronized void setValidSoulLampDimensions(List<? extends String> items)
    {   soulLampDimensions.set(items);
    }

    public synchronized void setWaterskinStrength(int strength)
    {   waterskinStrength.set(strength);
    }

    public synchronized void setPotionsEnabled(Boolean saver)
    {   allowPotionsInHearth.set(saver);
    }

    public synchronized void setPotionBlacklist(List<String> saver)
    {   blacklistedPotions.set(saver);
    }

    public synchronized void setInsulationBlacklist(List<String> blacklist)
    {   insulationBlacklist.set(blacklist);
    }

    public synchronized void setInsulatingCurios(List<? extends List<?>> items)
    {
        if (CompatManager.isCuriosLoaded())
        {   insulatingCurios.set(items);
        }
    }
}