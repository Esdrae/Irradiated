package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FluidRequirement
{
    public final Optional<List<Fluid>> fluids;
    public final Optional<ITag<Fluid>> tag;
    public final Optional<BlockRequirement.StateRequirement> state;
    public final Optional<NbtRequirement> nbt;

    public FluidRequirement(Optional<List<Fluid>> fluids, Optional<ITag<Fluid>> tag, Optional<BlockRequirement.StateRequirement> state, Optional<NbtRequirement> nbt)
    {
        this.fluids = fluids;
        this.tag = tag;
        this.state = state;
        this.nbt = nbt;
    }

    public static final Codec<FluidRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registry.FLUID.listOf().optionalFieldOf("fluids").forGetter(predicate -> predicate.fluids),
            ITag.codec(FluidTags::getAllTags).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            BlockRequirement.StateRequirement.CODEC.optionalFieldOf("state").forGetter(predicate -> predicate.state),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(predicate -> predicate.nbt)
    ).apply(instance, FluidRequirement::new));

    public boolean test(World pLevel, BlockPos pPos)
    {
        if (!pLevel.isLoaded(pPos))
        {   return false;
        }
        else
        {   FluidState lState = pLevel.getFluidState(pPos);
            return this.test(lState);
        }
    }

    public boolean test(FluidState pState)
    {
        if (this.tag.isPresent() && !pState.is(this.tag.get()))
        {   return false;
        }
        else if (this.fluids.isPresent() && !fluids.get().contains(pState.getType()))
        {   return false;
        }
        else
        {   return !this.state.isPresent() || state.get().matches(pState);
        }
    }

    public CompoundNBT serialize()
    {
        CompoundNBT lTag = new CompoundNBT();
        this.fluids.ifPresent(fluids -> lTag.put("fluids", NBTHelper.listTagOf(fluids.stream().map(ForgeRegistries.FLUIDS::getKey).map(Object::toString).collect(Collectors.toList()))));
        this.tag.ifPresent(tag -> lTag.putString("tag", FluidTags.getAllTags().getId(tag).toString()));
        this.state.ifPresent(state -> lTag.put("state", state.serialize()));
        this.nbt.ifPresent(nbt -> lTag.put("nbt", nbt.serialize()));
        return lTag;
    }

    public static FluidRequirement deserialize(CompoundNBT pTag)
    {
        Optional<List<Fluid>> lFluids = pTag.contains("fluids") ? Optional.of(pTag.getList("fluids", 8).stream().map(tag -> ForgeRegistries.FLUIDS.getValue(new ResourceLocation(tag.getAsString()))).collect(Collectors.toList())) : Optional.empty();
        Optional<ITag<Fluid>> lTag = pTag.contains("tag") ? Optional.of(FluidTags.getAllTags().getTag(new ResourceLocation(pTag.getString("tag")))) : Optional.empty();
        Optional<BlockRequirement.StateRequirement> lState = pTag.contains("state") ? Optional.of(BlockRequirement.StateRequirement.deserialize(pTag.getCompound("state"))) : Optional.empty();
        Optional<NbtRequirement> lNbt = pTag.contains("nbt") ? Optional.of(NbtRequirement.deserialize(pTag.getCompound("nbt"))) : Optional.empty();
        return new FluidRequirement(lFluids, lTag, lState, lNbt);
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

        FluidRequirement that = (FluidRequirement) obj;

        if (!fluids.equals(that.fluids))
        {   return false;
        }
        if (!tag.equals(that.tag))
        {   return false;
        }
        if (!state.equals(that.state))
        {   return false;
        }
        return nbt.equals(that.nbt);
    }

    @Override
    public String toString()
    {
        StringBuilder lBuilder = new StringBuilder();
        this.fluids.ifPresent(fluids -> lBuilder.append("Fluids: ").append(fluids.toString()));
        this.tag.ifPresent(tag -> lBuilder.append("Tag: ").append(tag.toString()));
        this.state.ifPresent(state -> lBuilder.append("State: ").append(state.toString()));
        this.nbt.ifPresent(nbt -> lBuilder.append("NBT: ").append(nbt.toString()));

        return lBuilder.toString();
    }
}
