package com.momosoftworks.coldsweat.config.spec;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ClientSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue celsius;
    private static final ForgeConfigSpec.IntValue tempOffset;
    private static final ForgeConfigSpec.DoubleValue tempSmoothing;

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> bodyIconPos;
    private static final ForgeConfigSpec.BooleanValue bodyIconEnabled;

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> bodyReadoutPos;
    private static final ForgeConfigSpec.BooleanValue bodyReadoutEnabled;

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> worldGaugePos;
    private static final ForgeConfigSpec.BooleanValue worldGaugeEnabled;

    private static final ForgeConfigSpec.BooleanValue customHotbarLayout;
    private static final ForgeConfigSpec.BooleanValue iconBobbing;

    private static final ForgeConfigSpec.BooleanValue hearthDebug;

    private static final ForgeConfigSpec.BooleanValue showConfigButton;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> configButtonPos;
    private static final ForgeConfigSpec.BooleanValue distortionEffects;

    private static final ForgeConfigSpec.BooleanValue highContrast;

    private static final ForgeConfigSpec.BooleanValue enableCreativeWarning;


    static 
    {
        /*
         Temperature Display Preferences
         */
        BUILDER.push("Visual Preferences");
            celsius = BUILDER
                    .comment("Sets all temperatures to be displayed in Celsius")
                    .define("Celsius", false);
            tempOffset = BUILDER
                    .comment("Visually offsets the world temperature to better match the user's definition of \"hot\" and \"cold\"")
                    .defineInRange("Temperature Offset", 0, 0, Integer.MAX_VALUE);
            tempSmoothing = BUILDER
                    .comment("The amount of smoothing applied to gauges in the UI",
                             "A value of 1 has no smoothing")
                    .defineInRange("Temperature Smoothing", 10, 1.0, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("UI Options");
            customHotbarLayout = BUILDER
                    .define("Custom hotbar layout", true);
            iconBobbing = BUILDER
                    .comment("Controls whether UI elements will shake when in critical conditions")
                    .define("Icon Bobbing", true);

            bodyIconPos = BUILDER
                    .comment("The position of the body temperature icon relative to default")
                    .defineList("Body Temperature Icon Offset", Arrays.asList(0, 0), it -> it instanceof Integer);
            bodyIconEnabled = BUILDER
                    .comment("Enables the body temperature icon")
                    .define("Body Temperature Icon Enabled", true);

            bodyReadoutPos = BUILDER
                    .comment("The position of the body temperature readout relative to default")
                    .defineList("Body Temperature Readout Offset", Arrays.asList(0, 0), it -> it instanceof Integer);
            bodyReadoutEnabled = BUILDER
                    .comment("Enables the body temperature readout")
                    .define("Body Temperature Readout Enabled", true);

            worldGaugePos = BUILDER
                    .comment("The position of the world temperature gauge relative to default")
                    .defineList("World Temperature UI Offset", Arrays.asList(0, 0), it -> it instanceof Integer);
            worldGaugeEnabled = BUILDER
                    .comment("Enables the world temperature gauge")
                    .define("World Temperature UI Enabled", true);
        BUILDER.pop();

        BUILDER.push("Accessibility");
            distortionEffects = BUILDER
                    .comment("Enables visual distortion effects when the player is too hot or cold")
                    .define("Distortion Effects", true);
            highContrast = BUILDER
                    .comment("Enables high contrast mode for UI elements")
                    .define("High Contrast", false);
        BUILDER.pop();

        BUILDER.push("Misc");
            showConfigButton = BUILDER
                    .comment("Show the config menu button in the Options menu")
                    .define("Enable In-Game Config", true);
            configButtonPos = BUILDER
                    .comment("The position (offset) of the config button on the screen")
                    .defineList("Config Button Position", Arrays.asList(0, 0),
                                it -> it instanceof Integer);
            enableCreativeWarning = BUILDER
                    .comment("Warns the player about a bug that clears armor insulation when in creative mode")
                    .define("Enable Creative Mode Warning", true);
            hearthDebug = BUILDER
                    .comment("Displays areas that the Hearth is affecting when the F3 debug menu is open")
                    .define("Hearth Debug", true);
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "coldsweat/client.toml");
    }

    public static ClientSettingsConfig getInstance()
    {   return new ClientSettingsConfig();
    }

    /*
     * Non-private values for use elsewhere
     */

    public boolean isHighContrast()
    {   return highContrast.get();
    }
    public void setHighContrast(boolean enabled)
    {   highContrast.set(enabled);
    }

    public boolean isCelsius()
    {   return celsius.get();
    }

    public int getTempOffset()
    {   return tempOffset.get();
    }

    public int getBodyIconX()
    {   return bodyIconPos.get().get(0);
    }
    public int getBodyIconY()
    {   return bodyIconPos.get().get(1);
    }

    public int getBodyReadoutX()
    {   return bodyReadoutPos.get().get(0);
    }
    public int getBodyReadoutY()
    {   return bodyReadoutPos.get().get(1);
    }

    public int getWorldGaugeX()
    {   return worldGaugePos.get().get(0);
    }
    public int getWorldGaugeY()
    {   return worldGaugePos.get().get(1);
    }

    public double getTempSmoothing()
    {   return tempSmoothing.get();
    }

    public boolean customHotbarEnabled()
    {   return customHotbarLayout.get();
    }

    public boolean isIconBobbingEnabled()
    {   return iconBobbing.get();
    }

    public boolean isHearthDebugEnabled()
    {   return hearthDebug.get();
    }

    public boolean isCreativeWarningEnabled()
    {   return enableCreativeWarning.get();
    }

    public boolean isBodyIconEnabled()
    {   return bodyIconEnabled.get();
    }
    public boolean isBodyReadoutEnabled()
    {   return bodyReadoutEnabled.get();
    }
    public boolean isWorldGaugeEnabled()
    {   return worldGaugeEnabled.get();
    }

    /*
     * Safe set methods for config values
     */

    public void setCelsius(boolean enabled)
    {   celsius.set(enabled);
    }

    public void setTempOffset(int offset)
    {   tempOffset.set(offset);
    }

    public void setBodyIconX(int pos)
    {   bodyIconPos.set(Arrays.asList(pos, getBodyIconY()));
    }
    public void setBodyIconY(int pos)
    {   bodyIconPos.set(Arrays.asList(getBodyIconX(), pos));
    }

    public void setBodyReadoutX(int pos)
    {   bodyReadoutPos.set(Arrays.asList(pos, getBodyReadoutY()));
    }
    public void setBodyReadoutY(int pos)
    {   bodyReadoutPos.set(Arrays.asList(getBodyReadoutX(), pos));
    }

    public void setWorldGaugeX(int pos)
    {   worldGaugePos.set(Arrays.asList(pos, getWorldGaugeY()));
    }
    public void setWorldGaugeY(int pos)
    {   worldGaugePos.set(Arrays.asList(getWorldGaugeX(), pos));
    }

    public void setCustomHotbar(boolean enabled)
    {   customHotbarLayout.set(enabled);
    }

    public void setIconBobbing(boolean enabled)
    {   iconBobbing.set(enabled);
    }

    public void setHearthDebug(boolean enabled)
    {   hearthDebug.set(enabled);
    }

    public boolean isConfigButtonEnabled()
    {   return showConfigButton.get();
    }
    public List<? extends Integer> getConfigButtonPos()
    {   return configButtonPos.get();
    }
    public void setConfigButtonPos(List<Integer> pos)
    {   configButtonPos.set(pos);
    }

    public boolean areDistortionsEnabled()
    {   return distortionEffects.get();
    }
    public void setDistortionsEnabled(boolean sway)
    {   distortionEffects.set(sway);
    }

    public void setCreativeWarningEnabled(boolean enabled)
    {   enableCreativeWarning.set(enabled);
    }

    public void setBodyIconEnabled(boolean enabled)
    {   bodyIconEnabled.set(enabled);
    }
    public void setBodyReadoutEnabled(boolean enabled)
    {   bodyReadoutEnabled.set(enabled);
    }
    public void setWorldGaugeEnabled(boolean enabled)
    {   worldGaugeEnabled.set(enabled);
    }

    public void setTempSmoothing(double smoothing)
    {   tempSmoothing.set(smoothing);
    }


    public void save()
    {   SPEC.save();
    }
}