package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;

public class EnchantmentRequirement
{
    public final Enchantment enchantment;
    public final Optional<IntegerBounds> level;
    
    public EnchantmentRequirement(Enchantment enchantment, Optional<IntegerBounds> level)
    {
        this.enchantment = enchantment;
        this.level = level;
    }
    
    public static final Codec<EnchantmentRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registry.ENCHANTMENT.fieldOf("enchantment").forGetter(requirement -> requirement.enchantment),
            IntegerBounds.CODEC.optionalFieldOf("levels").forGetter(requirement -> requirement.level)
    ).apply(instance, EnchantmentRequirement::new));

    public boolean test(Enchantment enchantment, int level)
    {   return this.enchantment == enchantment && this.level.map(bounds -> bounds.test(level)).orElse(true);
    }

    public boolean test(Map<Enchantment, Integer> enchantments)
    {
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet())
        {
            if (test(entry.getKey(), entry.getValue()))
            {   return true;
            }
        }
        return false;
    }

    public CompoundNBT serialize()
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("enchantment", ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString());
        level.ifPresent(bounds -> tag.put("level", bounds.serialize()));
        return tag;
    }

    public static EnchantmentRequirement deserialize(CompoundNBT tag)
    {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(tag.getString("enchantment")));
        IntegerBounds level = tag.contains("level") ? IntegerBounds.deserialize(tag.getCompound("level")) : null;
        return new EnchantmentRequirement(enchantment, Optional.ofNullable(level));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        EnchantmentRequirement that = (EnchantmentRequirement) obj;

        if (!enchantment.equals(that.enchantment))
        {   return false;
        }
        return level.equals(that.level);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString());
        level.ifPresent(bounds -> builder.append(bounds.toString()));

        return builder.toString();
    }
}
