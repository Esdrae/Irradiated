package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.entity.LivingEntity;

import java.util.function.Function;

public class WaterskinTempModifier extends TempModifier
{
    public WaterskinTempModifier()
    {   this(0.0);
    }

    public WaterskinTempModifier(double temp)
    {   this.getNBT().putDouble("Temperature", temp);
    }

    @Override
    public Function<Double, Double>  calculate(LivingEntity entity, Temperature.Trait trait)
    {   return temp -> temp + this.getNBT().getDouble("Temperature");
    }
}