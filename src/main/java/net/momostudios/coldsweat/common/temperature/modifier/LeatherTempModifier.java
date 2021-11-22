package net.momostudios.coldsweat.common.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.NumberNBT;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.momostudios.coldsweat.common.temperature.Temperature;
import net.momostudios.coldsweat.core.util.PlayerTemp;

import java.util.List;

public class LeatherTempModifier extends TempModifier
{
    public float amount = 0;
    public LeatherTempModifier() {}

    public LeatherTempModifier(List<INBT> args)
    {
        this.amount = ((IntNBT) args.get(0)).getInt();
    }


    public LeatherTempModifier with(List<INBT> args)
    {
        return new LeatherTempModifier(args);
    }

    @Override
    public float calculate(Temperature temp, PlayerEntity player)
    {
        PlayerTemp.removeModifier(player, LeatherTempModifier.class, PlayerTemp.Types.RATE, 1);

        return temp.get() / Math.max(1, amount / 15);
    }

    public String getID()
    {
        return "cold_sweat:insulated_armor";
    }
}