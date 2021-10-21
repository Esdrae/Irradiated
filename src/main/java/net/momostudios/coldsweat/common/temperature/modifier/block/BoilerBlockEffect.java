package net.momostudios.coldsweat.common.temperature.modifier.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.momostudios.coldsweat.common.block.BoilerBlock;
import net.momostudios.coldsweat.core.util.MathHelperCS;

public class BoilerBlockEffect extends BlockEffect
{
    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        if (this.hasBlock(state) && state.get(BoilerBlock.LIT))
        {
            double temp = 0.04;
            return Math.max(0, temp * (9 - distance));
        }
        return 0;
    }

    @Override
    public boolean hasBlock(BlockState block)
    {
        return block.getBlock() instanceof BoilerBlock;
    }

    @Override
    public double maxTemp() {
        return MathHelperCS.convertFromF(212);
    }
}