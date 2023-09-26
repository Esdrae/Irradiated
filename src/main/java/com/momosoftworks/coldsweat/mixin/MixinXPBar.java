package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.config.ClientSettingsConfig;
import net.minecraft.client.gui.IngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IngameGui.class)
public class MixinXPBar
{
    @Inject(method = "renderExperienceBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At
            (
                value = "INVOKE",
                target = "Lnet/minecraft/profiler/IProfiler;push(Ljava/lang/String;)V",
                shift = At.Shift.AFTER
            ),
            slice = @Slice
            (
                from = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;pop()V", ordinal = 0),
                to   = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;width(Ljava/lang/String;)I")
            ),
            remap = ColdSweat.REMAP_MIXINS)
    public void moveXPNumberDown(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        // Render XP bar
        if (ClientSettingsConfig.getInstance().customHotbarEnabled())
        {   poseStack.translate(0.0D, 4.0D, 0.0D);
        }
    }

    @Inject(method = "renderExperienceBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;pop()V", ordinal = 1),
            remap = ColdSweat.REMAP_MIXINS)
    public void renderXPNumberPost(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        // Render XP bar
        if (ClientSettingsConfig.getInstance().customHotbarEnabled())
        {   poseStack.translate(0.0D, -4.0D, 0.0D);
        }
    }

    @Mixin(IngameGui.class)
    public static class MixinItemLabel
    {
        @Inject(method = "renderSelectedItemName(Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
                at = @At(value = "HEAD"), remap = ColdSweat.REMAP_MIXINS)
        public void moveItemNameUp(MatrixStack matrixStack, CallbackInfo ci)
        {
            if (ClientSettingsConfig.getInstance().customHotbarEnabled())
            {   matrixStack.translate(0, -4, 0);
            }
        }

        @Inject(method = "renderSelectedItemName(Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
                at = @At(value = "TAIL"), remap = ColdSweat.REMAP_MIXINS)
        public void renderItemNamePost(MatrixStack matrixStack, CallbackInfo ci)
        {
            if (ClientSettingsConfig.getInstance().customHotbarEnabled())
            {   matrixStack.translate(0, 4, 0);
            }
        }
    }
}