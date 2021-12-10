package net.momostudios.coldsweat.client.gui.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.momostudios.coldsweat.config.ClientSettingsConfig;
import net.momostudios.coldsweat.config.ColdSweatConfig;
import net.momostudios.coldsweat.config.ConfigCache;
import net.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import net.momostudios.coldsweat.core.network.message.ClientConfigSendMessage;
import net.momostudios.coldsweat.core.util.MathHelperCS;
import net.momostudios.coldsweat.core.util.Units;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Objects;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ConfigScreen
{
    public static final int TITLE_HEIGHT = 16;
    public static final int BOTTOM_BUTTON_HEIGHT_OFFSET = 26;
    public static final int OPTION_SIZE = 25;
    public static final int BOTTOM_BUTTON_WIDTH = 150;
    private static final String ON = new TranslationTextComponent("options.on").getString();
    private static final String OFF = new TranslationTextComponent("options.off").getString();

    private static final ClientSettingsConfig CLIENT_CONFIG = ClientSettingsConfig.getInstance();
    public static Minecraft mc = Minecraft.getInstance();

    static DecimalFormat twoPlaces = new DecimalFormat("#.##");

    public static boolean isMouseDown = false;
    public static int mouseX = 0;
    public static int mouseY = 0;

    public static int FIRST_PAGE = 0;
    public static int LAST_PAGE = 1;

    public static Screen getPage(int index, Screen parentScreen, ConfigCache configCache)
    {
        index = Math.max(FIRST_PAGE, Math.min(LAST_PAGE, index));
        switch (index)
        {
            case 0:  return new PageOne(parentScreen, configCache);
            case 1:  return new PageTwo(parentScreen, configCache);
            default: return null;
        }
    }

    public static void saveConfig(ConfigCache configCache)
    {
        if (!mc.isSingleplayer() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissionLevel(2))
        {
            ColdSweatPacketHandler.INSTANCE.sendToServer(new ClientConfigSendMessage(configCache));
        }
        else
        {
            ColdSweatConfig.getInstance().writeValues(configCache);
        }
        ConfigCache.setInstance(configCache);
    }

    @SubscribeEvent
    public static void onClicked(GuiScreenEvent.MouseClickedEvent event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().currentScreen instanceof DifficultyPage)
            isMouseDown = true;
    }

    @SubscribeEvent
    public static void onReleased(GuiScreenEvent.MouseReleasedEvent event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().currentScreen instanceof DifficultyPage)
            isMouseDown = false;
    }

    public static String difficultyName(int difficulty)
    {
        return  difficulty == 0 ? new TranslationTextComponent("cold_sweat.config.difficulty.super_easy.name").getString() :
                difficulty == 1 ? new TranslationTextComponent("cold_sweat.config.difficulty.easy.name").getString() :
                difficulty == 2 ? new TranslationTextComponent("cold_sweat.config.difficulty.normal.name").getString() :
                difficulty == 3 ? new TranslationTextComponent("cold_sweat.config.difficulty.hard.name").getString() :
                difficulty == 4 ? new TranslationTextComponent("cold_sweat.config.difficulty.custom.name").getString() : "";
    }

    public static int difficultyColor(int difficulty)
    {
        return  difficulty == 0 ? 16777215 :
                difficulty == 1 ? 16768882 :
                difficulty == 2 ? 16755024 :
                difficulty == 3 ? 16731202 :
                difficulty == 4 ? 10631158 : 16777215;
    }

    public static class DifficultyPage extends Screen
    {
        private final Screen parentScreen;
        private final ConfigCache configCache;

        private static final int TITLE_HEIGHT = ConfigScreen.TITLE_HEIGHT;
        private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = ConfigScreen.BOTTOM_BUTTON_HEIGHT_OFFSET;
        private static final int BOTTOM_BUTTON_WIDTH = ConfigScreen.BOTTOM_BUTTON_WIDTH;

        ResourceLocation configButtons = new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png");
        ResourceLocation diffTextBox = new ResourceLocation("cold_sweat:textures/gui/screen/configs/difficulty_description.png");

        protected DifficultyPage(Screen parentScreen, ConfigCache configCache)
        {
            super(new TranslationTextComponent("cold_sweat.config.section.difficulty.name"));
            this.parentScreen = parentScreen;
            this.configCache = configCache;
        }

        public int index()
        {
            return -1;
        }


        @Override
        protected void init()
        {
            this.addButton(new Button(
                    this.width / 2 - BOTTOM_BUTTON_WIDTH / 2,
                    this.height - BOTTOM_BUTTON_HEIGHT_OFFSET,
                    BOTTOM_BUTTON_WIDTH, 20,
                    new TranslationTextComponent("gui.done"),
                    button -> this.close())
            );
        }

        @Override
        public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            ConfigScreen.mouseX = mouseX;
            ConfigScreen.mouseY = mouseY;
            this.renderBackground(matrixStack);

            drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);

            mc.getTextureManager().bindTexture(configButtons);
            // Slider
            this.blit(matrixStack, this.width / 2 - 76, this.height / 2 - 53, 12,
                    isMouseOverSlider(mouseX, mouseY) ? 174 : 168, 152, 6);
            // Head
            this.blit(matrixStack, this.width / 2 - 78 + (configCache.difficulty * 37), this.height / 2 - 58,
                    isMouseOverSlider(mouseX, mouseY) ? 0 : 6, 168, 6, 16);
            // Difficulty Text
            String difficultyName = ConfigScreen.difficultyName(configCache.difficulty);
            this.font.drawStringWithShadow(matrixStack, difficultyName, this.width / 2.0f - (font.getStringWidth(difficultyName) / 2f),
                    this.height / 2.0f - 84, ConfigScreen.difficultyColor(configCache.difficulty));

            mc.getTextureManager().bindTexture(diffTextBox);
            this.blit(matrixStack, this.width / 2 - 160, this.height / 2 - 30, 0, 0, 320, 128, 320, 128);

            int line = 0;
            for (ITextComponent text : DifficultyDescriptions.getListFor(configCache.difficulty))
            {
                this.font.drawString(matrixStack, text.getString(), this.width / 2f - 152, this.height / 2f - 22 + (line * 20f), 15393256);
                line++;
            }

            super.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        private void close()
        {
            configCache.setDifficulty(configCache.difficulty);

            // Super Easy
            if (configCache.difficulty == 0)
            {
                configCache.setMinHabitable(MathHelperCS.convertUnits(40, Units.F, Units.MC, true));
                configCache.setMaxHabitable(MathHelperCS.convertUnits(120, Units.F, Units.MC, true));
                configCache.setRateMultiplier(0.5);
                configCache.setShowAmbient(true);
                configCache.setDamageScaling(false);
                configCache.setFireResistanceEffect(true);
                configCache.setIceResistanceEffect(true);
                configCache.setDifficulty(0);
            }
            // Easy
            else if (configCache.difficulty == 1)
            {
                configCache.setMinHabitable(MathHelperCS.convertUnits(45, Units.F, Units.MC, true));
                configCache.setMaxHabitable(MathHelperCS.convertUnits(110, Units.F, Units.MC, true));
                configCache.setRateMultiplier(0.75);
                configCache.setShowAmbient(true);
                configCache.setDamageScaling(false);
                configCache.setFireResistanceEffect(true);
                configCache.setIceResistanceEffect(true);
                configCache.setDifficulty(1);
            }
            // Normal
            else if (configCache.difficulty == 2)
            {
                configCache.setMinHabitable(MathHelperCS.convertUnits(50, Units.F, Units.MC, true));
                configCache.setMaxHabitable(MathHelperCS.convertUnits(100, Units.F, Units.MC, true));
                configCache.setRateMultiplier(1.0);
                configCache.setShowAmbient(false);
                configCache.setDamageScaling(true);
                configCache.setFireResistanceEffect(false);
                configCache.setIceResistanceEffect(false);
                configCache.setDifficulty(2);
            }
            // Hard
            else if (configCache.difficulty == 3)
            {
                configCache.setMinHabitable(MathHelperCS.convertUnits(60, Units.F, Units.MC, true));
                configCache.setMaxHabitable(MathHelperCS.convertUnits(90, Units.F, Units.MC, true));
                configCache.setRateMultiplier(1.5);
                configCache.setShowAmbient(false);
                configCache.setDamageScaling(true);
                configCache.setFireResistanceEffect(false);
                configCache.setIceResistanceEffect(false);
                configCache.setDifficulty(3);
            }
            mc.displayGuiScreen(parentScreen);
            saveConfig(configCache);
        }

        boolean isMouseOverSlider(double mouseX, double mouseY)
        {
            return (mouseX >= this.width / 2.0 - 80 && mouseX <= this.width / 2.0 + 80 &&
                    mouseY >= this.height / 2.0 - 67 && mouseY <= this.height / 2.0 - 35);
        }

       @Override
       public void tick()
       {
           double x = mouseX;
           double y = mouseY;
           if (isMouseDown && isMouseOverSlider(x, y))
           {
               int newDifficulty = 0;
               if (x < this.width / 2.0 - 76 + (19)) {
                   newDifficulty = 0;
               }
               else if (x < this.width / 2.0 - 76 + (19 * 3)) {
                   newDifficulty = 1;
               }
               else if (x < this.width / 2.0 - 76 + (19 * 5)) {
                   newDifficulty = 2;
               }
               else if (x < this.width / 2.0 - 76 + (19 * 7)) {
                   newDifficulty = 3;
               }
               else if (x < this.width / 2.0 - 76 + (19 * 9)) {
                   newDifficulty = 4;
               }

               if (newDifficulty != configCache.difficulty) {
                   mc.getSoundHandler().play(SimpleSound.master(new SoundEvent(new ResourceLocation("minecraft:block.note_block.hat")), 2f, 1f));
               }
               configCache.difficulty = newDifficulty;
           }
       }
    }

    public static class PageOne extends ConfigPageBase
    {
        Screen parentScreen;
        ConfigCache configCache;

        boolean celsius = CLIENT_CONFIG.celsius();

        TextFieldWidget tempOffsetInput;
        TextFieldWidget maxTempInput;
        TextFieldWidget minTempInput;
        TextFieldWidget rateMultInput;
        Button difficultyButton;
        Button celsiusButton;
        Button iceResButton;
        Button fireResButton;
        Button damageScalingButton;
        Button showAmbientButton;

        public PageOne(Screen parentScreen, ConfigCache configCache)
        {
            super(parentScreen, configCache);
            this.parentScreen = parentScreen;
            this.configCache = configCache;
        }

        @Override
        public int index()
        {
            return 0;
        }

        @Override
        protected void init()
        {
            super.init();

            // The options

            // Celsius
            celsiusButton = new ConfigButton(this.width / 2 - 185, this.height / 4 - 8, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.units.name").getString() + ": " +
                        (this.celsius ? new TranslationTextComponent("cold_sweat.config.celsius.name").getString() :
                                        new TranslationTextComponent("cold_sweat.config.fahrenheit.name").getString())), button -> this.toggleCelsius(), configCache)
            {
                @Override
                public boolean setsCustomDifficulty() { return false; }
            };

            // Temp Offset
            this.tempOffsetInput = new TextFieldWidget(font, this.width / 2 - 86, this.height / 4 + 20, 51, 22, new StringTextComponent(""));
            this.tempOffsetInput.setText(String.valueOf(CLIENT_CONFIG.tempOffset()));

            // Max Temperature
            this.maxTempInput = new TextFieldWidget(font, this.width / 2 - 86, this.height / 4 + 52, 51, 22, new StringTextComponent(""));
            this.maxTempInput.setText(String.valueOf(twoPlaces.format(
                    MathHelperCS.convertUnits(configCache.maxTemp, Units.MC, celsius ? Units.C : Units.F, true))));

            // Min Temperature
            this.minTempInput = new TextFieldWidget(font, this.width / 2 - 86, this.height / 4 + 84, 51, 22, new StringTextComponent(""));
            this.minTempInput.setText(String.valueOf(twoPlaces.format(
                    MathHelperCS.convertUnits(configCache.minTemp, Units.MC, celsius ? Units.C : Units.F, true))));

            // Rate Multiplier
            this.rateMultInput = new TextFieldWidget(font, this.width / 2 - 86, this.height / 4 + 116, 51, 22, new StringTextComponent(""));
            this.rateMultInput.setText(String.valueOf(configCache.rate));

            // Difficulty button
            difficultyButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8, 152, 20,
                    new StringTextComponent(new TranslationTextComponent("cold_sweat.config.difficulty.name").getString() +
                    " (" + difficultyName(configCache.difficulty) + ")..."),
                    button -> mc.displayGuiScreen(new DifficultyPage(this, configCache)), configCache)
            {
                @Override
                public boolean setsCustomDifficulty() { return false; }
            };


            // Misc. Temp Effects
            iceResButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8 + OPTION_SIZE * 2, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.ice_resistance.name").getString() + ": " + (configCache.iceRes ? ON : OFF)),
                button -> this.toggleIceRes(), configCache);

            fireResButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8 + OPTION_SIZE * 3, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.fire_resistance.name").getString() + ": " + (configCache.fireRes ? ON : OFF)),
                button -> this.toggleFireRes(), configCache);

            showAmbientButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8 + OPTION_SIZE * 4, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.require_thermometer.name").getString() + ": " + (configCache.showAmbient ? ON : OFF)),
                button -> this.toggleShowAmbient(), configCache);

            damageScalingButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8 + OPTION_SIZE * 5, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.damage_scaling.name").getString() + ": " + (configCache.damageScaling ? ON : OFF)),
                button -> this.toggleDamageScaling(), configCache);

            if (mc.player == null || mc.player.hasPermissionLevel(3))
            {
                this.addButton(difficultyButton);

                this.addButton(iceResButton);
                this.addButton(fireResButton);
                this.addButton(showAmbientButton);
                this.addButton(damageScalingButton);

                this.children.add(this.maxTempInput);
                this.children.add(this.minTempInput);
                this.children.add(this.rateMultInput);
            }

            this.addButton(celsiusButton);
            this.children.add(this.tempOffsetInput);
        }

        @Override
        public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            super.render(matrixStack, mouseX, mouseY, partialTicks);

            /*
             * Render config options
             */

            // Temp Offset
            this.tempOffsetInput.render(matrixStack, mouseX, mouseY, partialTicks);
            drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.temp_offset.name"), this.width / 2 - 185, tempOffsetInput.y + 6, 16777215);

            if (mc.player == null || mc.player.hasPermissionLevel(3))
            {
                // Max Temp
                this.maxTempInput.render(matrixStack, mouseX, mouseY, partialTicks);
                drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.max_temperature.name"), this.width / 2 - 185, maxTempInput.y + 6, 16777215);

                // Min Temp
                this.minTempInput.render(matrixStack, mouseX, mouseY, partialTicks);
                drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.min_temperature.name"), this.width / 2 - 185, minTempInput.y + 6, 16777215);

                // Rate Multiplier
                this.rateMultInput.render(matrixStack, mouseX, mouseY, partialTicks);
                drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.rate_multiplier.name"), this.width / 2 - 185, rateMultInput.y + 6, 16777215);
            }
        }

        @Override
        public void tick()
        {
            super.tick();
            tempOffsetInput.tick();
            maxTempInput.tick();
            minTempInput.tick();
            rateMultInput.tick();
        }

        private void save()
        {
            CLIENT_CONFIG.setCelsius(this.celsius);
            configCache.setIceResistanceEffect(configCache.iceRes);
            configCache.setFireResistanceEffect(configCache.fireRes);
            configCache.setDamageScaling(configCache.damageScaling);
            configCache.setShowAmbient(configCache.showAmbient);

            try
            {
                CLIENT_CONFIG.setTempOffset(Integer.parseInt(tempOffsetInput.getText()));
            } catch (Exception e) {}

            try
            {
                configCache.setMaxHabitable(MathHelperCS.convertUnits(Double.parseDouble(maxTempInput.getText()), celsius ? Units.C : Units.F, Units.MC, true));
            } catch (Exception e) {}

            try
            {
                configCache.setMinHabitable(MathHelperCS.convertUnits(Double.parseDouble(minTempInput.getText()), celsius ? Units.C : Units.F, Units.MC, true));
            } catch (Exception e) {}

            try
            {
                double rateModifier = Double.parseDouble(rateMultInput.getText());
                configCache.setRateMultiplier(rateModifier);
            } catch (Exception e) {}

            saveConfig(configCache);

        }

        @Override
        public void onClose()
        {
            save();
            super.onClose();
        }

        public void toggleCelsius()
        {
            this.celsius = !this.celsius;
            celsiusButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.units.name").getString() + ": " +
                (this.celsius ? new TranslationTextComponent("cold_sweat.config.celsius.name").getString() :
                                new TranslationTextComponent("cold_sweat.config.fahrenheit.name").getString())));

            minTempInput.setText(String.valueOf(twoPlaces.format(MathHelperCS.convertUnits(configCache.minTemp, Units.MC, celsius ? Units.C : Units.F, true))));
            maxTempInput.setText(String.valueOf(twoPlaces.format(MathHelperCS.convertUnits(configCache.maxTemp, Units.MC, celsius ? Units.C : Units.F, true))));
        }

        public void toggleIceRes()
        {
            configCache.iceRes = !configCache.iceRes;
            iceResButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.ice_resistance.name").getString() + ": " +
                (configCache.iceRes ? ON : OFF)));
        }

        public void toggleFireRes()
        {
            configCache.fireRes = !configCache.fireRes;
            fireResButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.fire_resistance.name").getString() + ": " +
                (configCache.fireRes ? ON : OFF)));
        }
        public void toggleDamageScaling()
        {
            configCache.damageScaling = !configCache.damageScaling;
            damageScalingButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.damage_scaling.name").getString() + ": " +
                (configCache.damageScaling ? ON : OFF)));
        }

        public void toggleShowAmbient()
        {
            configCache.showAmbient = !configCache.showAmbient;
            showAmbientButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.require_thermometer.name").getString() + ": " +
                (configCache.showAmbient ? ON : OFF)));
        }
    }

    public static class PageTwo extends ConfigPageBase
    {
        private final Screen parentScreen;
        private final ConfigCache configCache;

        boolean customHotbar = CLIENT_CONFIG.customHotbar();
        boolean iconBobbing = CLIENT_CONFIG.iconBobbing();

        ImageButton upSteveButton;
        ImageButton downSteveButton;
        ImageButton rightSteveButton;
        ImageButton leftSteveButton;
        ImageButton resetSteveButton;

        ImageButton upTempReadoutButton;
        ImageButton downTempReadoutButton;
        ImageButton rightTempReadoutButton;
        ImageButton leftTempReadoutButton;
        ImageButton resetTempReadoutButton;

        Button customHotbarButton;
        Button iconBobbingButton;


        public PageTwo(Screen parentScreen, ConfigCache configCache)
        {
            super(parentScreen, configCache);
            this.parentScreen = parentScreen;
            this.configCache = configCache;
        }

        @Override
        public int index()
        {
            return 1;
        }

        @Override
        public ITextComponent sectionOneTitle() {
            return new TranslationTextComponent("cold_sweat.config.section.other");
        }

        @Nullable
        @Override
        public ITextComponent sectionTwoTitle() {
            return new TranslationTextComponent("cold_sweat.config.section.hud_settings");
        }

        @Override
        protected void init()
        {
            super.init();

            // The options

            // Direction Buttons: Steve Head
            leftSteveButton = new ImageButton(this.width / 2 + 140, this.height / 4 - 8, 14, 20, 0, 0, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeSelfIndicatorPos(0, -1));
            upSteveButton = new ImageButton(this.width / 2 + 154, this.height / 4 - 8, 20, 10, 14, 0, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeSelfIndicatorPos(1, -1));
            downSteveButton = new ImageButton(this.width / 2 + 154, this.height / 4 + 2, 20, 10, 14, 10, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeSelfIndicatorPos(1, 1));
            rightSteveButton = new ImageButton(this.width / 2 + 174, this.height / 4 - 8, 14, 20, 34, 0, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeSelfIndicatorPos(0, 1));
            resetSteveButton = new ImageButton(this.width / 2 + 192, this.height / 4 - 8, 20, 20, 0, 128, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> resetSelfIndicatorPos());

            // Direction Buttons: Temp Readout
            leftTempReadoutButton = new ImageButton(this.width / 2 + 140, this.height / 4 - 8 + (int) (OPTION_SIZE * 1.5), 14, 20, 0, 0, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeTempReadoutPos(0, -1));
            upTempReadoutButton = new ImageButton(this.width / 2 + 154, this.height / 4 - 8 + (int) (OPTION_SIZE * 1.5), 20, 10, 14, 0, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeTempReadoutPos(1, -1));
            downTempReadoutButton = new ImageButton(this.width / 2 + 154, this.height / 4 + 2 + (int) (OPTION_SIZE * 1.5), 20, 10, 14, 10, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeTempReadoutPos(1, 1));
            rightTempReadoutButton = new ImageButton(this.width / 2 + 174, this.height / 4 - 8 + (int) (OPTION_SIZE * 1.5), 14, 20, 34, 0, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> changeTempReadoutPos(0, 1));
            resetTempReadoutButton = new ImageButton(this.width / 2 + 192, this.height / 4 - 8 + (int) (OPTION_SIZE * 1.5), 20, 20, 0, 128, 20,
                new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button -> resetTempReadoutPos());

            // Custom Hotbar
            customHotbarButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8 + OPTION_SIZE * 3, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.custom_hotbar.name").getString() + ": " + (this.customHotbar ? ON : OFF)),
                button -> this.toggleCustomHotbar(), configCache);
            this.addButton(customHotbarButton);

            // Icon Bobbing
            iconBobbingButton = new ConfigButton(this.width / 2 + 51, this.height / 4 - 8 + OPTION_SIZE * 4, 152, 20,
                new StringTextComponent(new TranslationTextComponent("cold_sweat.config.icon_bobbing.name").getString() + ": " + (this.iconBobbing ? ON : OFF)),
                button -> this.toggleIconBobbing(), configCache);

            this.addButton(upSteveButton);
            this.addButton(downSteveButton);
            this.addButton(leftSteveButton);
            this.addButton(rightSteveButton);
            this.addButton(resetSteveButton);

            this.addButton(upTempReadoutButton);
            this.addButton(downTempReadoutButton);
            this.addButton(leftTempReadoutButton);
            this.addButton(rightTempReadoutButton);
            this.addButton(resetTempReadoutButton);
            this.addButton(iconBobbingButton);
        }

        @Override
        public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            super.render(matrixStack, mouseX, mouseY, partialTicks);

            drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.temperature_icon.name"), this.width / 2 + 51, this.height / 4 - 2, 16777215);
            drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.temperature_readout.name"), this.width / 2 + 51, this.height / 4 - 2 + (int) (OPTION_SIZE * 1.5), 16777215);
            drawString(matrixStack, this.font, new TranslationTextComponent("cold_sweat.config.offset_shift.name"), this.width / 2 + 51, this.height / 4 + 128, 16777215);
        }

        public boolean isShiftPressed()
        {
            return InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), 340);
        }

        @Override
        public void onClose()
        {
            Minecraft.getInstance().displayGuiScreen(parentScreen);
            saveConfig(configCache);
        }

        private void changeSelfIndicatorPos(int axis, int amount)
        {
            if (isShiftPressed()) amount *= 10;
            if (axis == 0)
            {
                CLIENT_CONFIG.setSteveHeadX(CLIENT_CONFIG.steveHeadX() + amount);
            }
            else if (axis == 1)
            {
                CLIENT_CONFIG.setSteveHeadY(CLIENT_CONFIG.steveHeadY() + amount);
            }
        }

        private void resetSelfIndicatorPos()
        {
            CLIENT_CONFIG.setSteveHeadX(0);
            CLIENT_CONFIG.setSteveHeadY(0);
        }

        private void changeTempReadoutPos(int axis, int amount)
        {
            if (isShiftPressed()) amount *= 10;
            if (axis == 0)
            {
                CLIENT_CONFIG.setTempGaugeX(CLIENT_CONFIG.tempGaugeX() + amount);
            }
            else if (axis == 1)
            {
                CLIENT_CONFIG.setTempGaugeY(CLIENT_CONFIG.tempGaugeY() + amount);
            }
        }

        private void resetTempReadoutPos()
        {
            CLIENT_CONFIG.setTempGaugeX(0);
            CLIENT_CONFIG.setTempGaugeY(0);
        }

        private void toggleCustomHotbar()
        {
            this.customHotbar = !this.customHotbar;
            customHotbarButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.custom_hotbar.name").getString() + ": " +
                (this.customHotbar ? ON : OFF)));
            CLIENT_CONFIG.setCustomHotbar(this.customHotbar);
        }

        private void toggleIconBobbing()
        {
            this.iconBobbing = !this.iconBobbing;
            iconBobbingButton.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.icon_bobbing.name").getString() + ": " +
                (this.iconBobbing ? ON : OFF)));
            CLIENT_CONFIG.setIconBobbing(this.iconBobbing);
        }
    }
}
