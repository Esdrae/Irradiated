package net.momostudios.coldsweat.common.temperature.modifier.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.momostudios.coldsweat.common.block.IceboxBlock;
import net.momostudios.coldsweat.common.te.IceboxTileEntity;
import net.momostudios.coldsweat.core.util.MathHelperCS;
import net.momostudios.coldsweat.core.util.Units;

public class IceboxBlockEffect extends BlockEffect
{
    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        if (this.hasBlock(state) && ((IceboxTileEntity) player.world.getTileEntity(pos)).getFuel() > 0)
        {
            double temp = -0.04;
            return Math.min(0, temp * (9 - distance));
        }
        return 0;
    }

    @Override
    public boolean hasBlock(BlockState block)
    {
        return block.getBlock() instanceof IceboxBlock;
    }

    @Override
    public double minTemp() {
        return MathHelperCS.convertUnits(-40, Units.F, Units.MC, false);
    }
}
