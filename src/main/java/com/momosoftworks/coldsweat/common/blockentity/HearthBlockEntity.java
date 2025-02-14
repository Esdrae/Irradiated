package com.momosoftworks.coldsweat.common.blockentity;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.contraptions.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.contraptions.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.contraptions.fluids.pipes.GlassFluidPipeBlock;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.common.BlockStateChangedEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.BlockInsulationTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.block.HearthBottomBlock;
import com.momosoftworks.coldsweat.common.block.SmokestackBlock;
import com.momosoftworks.coldsweat.common.container.HearthContainer;
import com.momosoftworks.coldsweat.common.event.HearthSaveDataHandler;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.BlockEntityInit;
import com.momosoftworks.coldsweat.core.init.ParticleTypesInit;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.HearthResetMessage;
import com.momosoftworks.coldsweat.core.network.message.UpdateHearthSignalsMessage;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.util.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.world.SpreadPath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber
public class HearthBlockEntity extends LockableLootTileEntity implements ITickableTileEntity
{
    // List of SpreadPaths, which determine where the Hearth is affecting and how it spreads through/around blocks
    List<SpreadPath> paths = new ArrayList<>(this.getMaxPaths());
    // Used as a lookup table for detecting duplicate paths (faster than ArrayList#contains())
    Set<BlockPos> pathLookup = new HashSet<>(this.getMaxPaths());
    Map<Pair<Integer, Integer>, Pair<Integer, Boolean>> seeSkyMap = new FastMap<>(this.getMaxPaths());

    List<EffectInstance> effects = new ArrayList<>();

    FluidStack coldFuel = new FluidStack(Fluids.WATER, 0);
    FluidStack hotFuel = new FluidStack(Fluids.LAVA, 0);

    FluidHandler bottomFuelHandler = new BottomFluidHandler();
    final LazyOptional<IFluidHandler> bottomFuelHolder = LazyOptional.of(() -> {
        return this.bottomFuelHandler;
    });
    FluidHandler sidesFuelHandler = new SidesFluidHandler();
    final LazyOptional<IFluidHandler> sidesFuelHolder = LazyOptional.of(() -> {
        return this.sidesFuelHandler;
    });

    NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    Pair<BlockPos, ResourceLocation> levelPos = Pair.of(null, null);
    int x = 0;
    int y = 0;
    int z = 0;

    int lastHotFuel = 0;
    int lastColdFuel = 0;
    boolean isSidePowered = false;
    boolean isBackPowered = false;
    boolean shouldUseHotFuel = false;
    boolean shouldUseColdFuel = false;
    boolean hasHotFuel = false;
    boolean hasColdFuel = false;
    int insulationLevel = 0;

    boolean isPlayerNearby = false;
    List<PlayerEntity> players = new ArrayList<>();
    int rebuildCooldown = 0;
    boolean forceRebuild = false;
    boolean isRebuildQueued = false;
    public int ticksExisted = 0;

    boolean registeredLocation = false;

    boolean showParticles = true;
    int frozenPaths = 0;
    boolean spreading = true;

    boolean hasSmokestack = false;
    int smokestackHeight = 2;

    static final Direction[] DIRECTIONS = Direction.values();

    static Method TICK_DOWN_EFFECT;
    static
    {
        try
        {   TICK_DOWN_EFFECT = ObfuscationReflectionHelper.findMethod(EffectInstance.class, "func_76454_e");
            TICK_DOWN_EFFECT.setAccessible(true);
        }
        catch (Exception ignored) {}
    }

    public HearthBlockEntity()
    {   this(BlockEntityInit.HEARTH_BLOCK_ENTITY_TYPE.get());
    }

    public HearthBlockEntity(TileEntityType<? extends HearthBlockEntity> type)
    {
        super(type);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockUpdate(BlockStateChangedEvent event)
    {
        BlockPos pos = event.getPosition();
        World level = event.getWorld();
        if (level == this.level
        && CSMath.withinCubeDistance(pos, this.getBlockPos(), this.getMaxRange())
        && !event.getOldState().getCollisionShape(level, pos).equals(event.getNewState().getCollisionShape(level, pos)))
        {   this.sendBlockUpdate();
        }
    }

    /**
     * Range of the Hearth starting from an exit point
     */
    public int getSpreadRange()
    {   return 20;
    }

    /**
     * Range of the Hearth starting from the Hearth's position
     */
    public int getMaxRange()
    {   return 96;
    }

    public int getMaxPaths()
    {   return 9000;
    }

    public int getMaxFuel()
    {   return 1000;
    }

    public int getInsulationTime()
    {   return 1200;
    }

    public int getInsulationLevel()
    {   return insulationLevel;
    }

    public int getMaxInsulationLevel()
    {   return 10;
    }

    public boolean hasSmokeStack()
    {   return true;
    }

    @Override
    protected ITextComponent getDefaultName()
    {   return new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".hearth");
    }

    @Override
    public ITextComponent getDisplayName()
    {   return this.getCustomName() != null ? this.getCustomName() : this.getDefaultName();
    }

    @Override
    protected NonNullList<ItemStack> getItems()
    {   return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn)
    {   this.items = itemsIn;
    }

    @Override
    public void tick()
    {
        BlockPos pos = this.getBlockPos();
        // Register the hearth's position to the global map
        this.registerLocation();

        // Easy access to clientside testList::stream
        boolean isClient = level.isClientSide;

        this.ticksExisted++;

        if (rebuildCooldown > 0) rebuildCooldown--;

        // Locate nearby players
        if (this.level != null && this.ticksExisted % 20 == 0)
        {
            this.isPlayerNearby = false;
            players.clear();
            for (PlayerEntity player : this.level.players())
            {
                if (player.blockPosition().closerThan(pos, this.getMaxRange()))
                {   players.add(player);
                    this.isPlayerNearby = true;
                }
            }
        }

        // Tick down the time for each effect
        this.tickPotionEffects();
        // Check for redstone power to this block
        this.checkInputSignal();

        // Determine what types of fuel to use
        boolean wasUsingColdFuel = this.shouldUseColdFuel;
        boolean wasUsingHotFuel = this.shouldUseHotFuel;
        if (!ConfigSettings.SMART_HEARTH.get())
        {
            this.shouldUseColdFuel = this.isSidePowered && this.getColdFuel() > 0;
            this.shouldUseHotFuel = this.isBackPowered && this.getHotFuel() > 0;
        }
        if (!this.shouldUseColdFuel && !this.shouldUseHotFuel)
        {   this.resetPaths();
        }

        // Clear paths every 5 minutes to account for calculation errors
        if (this.ticksExisted % 6000 == 0)
        {   this.replacePaths(new ArrayList<>(Collections.singletonList(new SpreadPath(pos).setOrigin(pos))));
        }

        // Reset if a nearby block has been updated
        if (forceRebuild || (rebuildCooldown <= 0 && isRebuildQueued))
        {   this.resetPaths();
        }

        if (this.getColdFuel() > 0 || this.getHotFuel() > 0)
        {
            // Gradually increases insulation amount
            if (insulationLevel < this.getInsulationTime())
            {   insulationLevel++;
            }

            if (this.shouldUseColdFuel || this.shouldUseHotFuel || (ConfigSettings.SMART_HEARTH.get() && this.isPlayerNearby))
            {
                // Determine whether particles are enabled
                if (this.ticksExisted % 20 == 0)
                {
                    showParticles = isClient
                            && Minecraft.getInstance().options.particles == ParticleStatus.ALL
                            && !HearthSaveDataHandler.DISABLED_HEARTHS.contains(levelPos);
                }

                if (paths.isEmpty())
                {   this.addPath(new SpreadPath(pos.above(2)).setOrigin(pos.above(2)));
                    pathLookup.add(pos.above());
                }

                // Mark as not spreading if all paths are frozen
                this.spreading = this.frozenPaths < paths.size();

                /*
                 Partition the points into logical "sub-maps" to be iterated over separately each tick
                */
                int pathCount = paths.size();
                // Size of each partition (sub-list) of paths
                int partSize = spreading ? CSMath.clamp(pathCount / 3, 100, 4000)
                                         : CSMath.clamp(pathCount / 20, 10, 100);
                // Number of partitions
                int partCount = (int) Math.ceil(pathCount / (float) partSize);
                // Index of the last point being worked on this tick
                int lastIndex = partSize * ((this.ticksExisted % partCount) + 1);
                // Index of the first point being worked on this tick
                int firstIndex = Math.max(0, lastIndex - partSize);

                // Spread to new blocks
                this.tickPaths(pathCount, firstIndex, lastIndex);

                // Give insulation to players
                if (!isClient && this.ticksExisted % 20 == 0)
                {
                    // Reset the usage status for cold/hot fuel
                    if (ConfigSettings.SMART_HEARTH.get())
                    {   this.resetFuelStatus();
                    }
                    // Provide insulation to players & calculate fuel usage
                    for (int i = 0; i < players.size(); i++)
                    {
                        PlayerEntity player = players.get(i);
                        if (player != null && pathLookup.contains(player.blockPosition()))
                        {   this.insulatePlayer(player);
                        }
                    }
                    players.clear();
                }

                // Drain fuel
                this.tickDrainFuel();
            }
        }

        // Input fuel
        if (this.ticksExisted % 20 == 0)
        {   this.checkConsumeFuel();
        }

        // Update fuel
        if (!this.level.isClientSide && this.isFuelChanged()
        || (wasUsingColdFuel != this.shouldUseColdFuel || wasUsingHotFuel != this.shouldUseHotFuel))
        {   this.updateFuelState();
        }

        // Particles
        if (isClient)
        {   this.tickParticles();
        }
    }

    protected void tickPaths(int pathCount, int firstIndex, int lastIndex)
    {
        for (int i = firstIndex; i < Math.min(paths.size(), lastIndex); i++)
        {   // This operation is really fast because it's an ArrayList
            SpreadPath spreadPath = paths.get(i);
            BlockPos pathPos = spreadPath.pos;
            if (spreadPath.origin == null)
            {   spreadPath.setOrigin(this.getBlockPos());
            }

            int spX = spreadPath.x;
            int spY = spreadPath.y;
            int spZ = spreadPath.z;

            // Use try-finally because there's still stuff to do even if {continue} skips the rest of the loop
            try
            {   // Don't try to spread if the path is frozen
                if (spreadPath.frozen)
                {
                    // Remove a 3D-checkerboard of paths after the Hearth is finished spreading to reduce pointless iteration overhead
                    // The Hearth is "finished spreading" when all paths are frozen
                    if (!spreading && (Math.abs(spY % 2) == 0) == (Math.abs(spX % 2) == Math.abs(spZ % 2)))
                    {   paths.remove(i);
                        // Go back and reiterate over the new path at this index
                        i--;
                    }
                    // Don't do anything else with this path
                    continue;
                }

                /*
                 Try to spread to new blocks
                 */

                // The origin of the path is usually the hearth's position,
                // but if it's spreading through Create pipes then the origin is the end of the pipe
                if (pathCount < this.getMaxPaths() && spreadPath.withinDistance(spreadPath.origin, this.getSpreadRange())
                && CSMath.withinCubeDistance(spreadPath.origin, this.getBlockPos(), this.getMaxRange()))
                {
                    /*
                     Spreading algorithm
                     */
                    BlockState state = level.getBlockState(pathPos);

                    // Build a map of what positions can see the sky
                    Pair<Integer, Integer> flatPos = Pair.of(spX, spZ);
                    Pair<Integer, Boolean> seeSkyState = seeSkyMap.get(flatPos);
                    boolean canSeeSky;
                    if (seeSkyState == null || (seeSkyState.getFirst() < spY != seeSkyState.getSecond()))
                    {   seeSkyMap.put(flatPos, Pair.of(spY, canSeeSky = WorldHelper.canSeeSky(level, pathPos.above(), 64)));
                    }
                    else
                    {   canSeeSky = seeSkyState.getSecond();
                    }

                    if (!canSeeSky || isPipe(state))
                    {
                        // Try to spread in every direction from the current position
                        for (int d = 0; d < DIRECTIONS.length; d++)
                        {
                            Direction direction = DIRECTIONS[d];

                            // Don't try to spread backwards
                            Direction pathDir = spreadPath.direction;
                            if (direction.getAxis() == pathDir.getAxis() && direction != pathDir) continue;

                            BlockPos tryPos = pathPos.relative(direction);

                            // Avoid duplicate paths (ArrayList isn't duplicate-safe like Sets/Maps)
                            // .add() functions to both add the path and check if it's already in the list
                            if (pathLookup.add(tryPos))
                            {
                                SpreadPath newPath = new SpreadPath(tryPos, direction).setOrigin(spreadPath.origin);

                                // If the BlockState is a pipe, check if the new path is following the direction of the pipe
                                if (!WorldHelper.isSpreadBlocked(level, state, pathPos, direction, spreadPath.direction)
                                && this.isValidPipeAt(tryPos, state, newPath, direction))
                                {   // Add the new path to the list
                                    paths.add(newPath);
                                }
                            }
                        }
                    }
                    // Remove this path if it has skylight access
                    else
                    {   pathLookup.remove(pathPos);
                        paths.remove(i);
                        i--;
                        continue;
                    }
                }
                // Track frozen paths to know when the Hearth is done spreading
                spreadPath.frozen = true;
                this.frozenPaths++;
            }

            /*
             Give insulation & spawn particles
             */
            finally
            {
                // Air Particles
                if (this.getLevel().isClientSide && showParticles)
                {   Random rand = new Random();
                    if (!(Minecraft.getInstance().options.renderDebug && ConfigSettings.HEARTH_DEBUG.get()))
                    {   this.spawnAirParticle(spX, spY, spZ, rand);
                    }
                }
            }
        }
    }

    public void checkInputSignal()
    {
        boolean wasBackPowered = this.isBackPowered;
        boolean wasSidePowered = this.isSidePowered;
        // Get signals
        if (!this.level.isClientSide())
        {
            this.isBackPowered = this.hasSignalFromBack();
            this.isSidePowered = this.hasSignalFromSides();
            // Update block state (if this is a hearth)
            if (this.getBlockState().is(ModBlocks.HEARTH_BOTTOM))
            {
                if (wasBackPowered != this.isBackPowered)
                {   level.setBlock(this.getBlockPos(), this.getBlockState().setValue(HearthBottomBlock.BACK_POWERED, this.isBackPowered), 3);
                }
                if (wasSidePowered != this.isSidePowered)
                {   level.setBlock(this.getBlockPos(), this.getBlockState().setValue(HearthBottomBlock.SIDE_POWERED, this.isSidePowered), 3);
                }
            }
            // Update signals for client
            this.syncInputSignal(wasBackPowered, wasSidePowered);
        }
    }

    protected boolean hasSignalFromSides()
    {
        Direction facing = this.getBlockState().getValue(HearthBottomBlock.FACING);
        return this.level.hasSignal(this.getBlockPos().relative(facing.getClockWise()), facing.getClockWise())
            || this.level.hasSignal(this.getBlockPos().relative(facing.getCounterClockWise()), facing.getCounterClockWise());
    }

    protected boolean hasSignalFromBack()
    {
        Direction facing = this.getBlockState().getValue(HearthBottomBlock.FACING);
        return this.level.hasSignal(this.getBlockPos().relative(facing.getOpposite()), facing.getOpposite())
            || this.level.hasSignal(this.getBlockPos().relative(Direction.DOWN), Direction.DOWN);
    }

    protected void syncInputSignal(boolean wasBackPowered, boolean wasSidePowered)
    {
        // Update signals for client
        if (!this.level.isClientSide() && (wasBackPowered != this.isBackPowered || wasSidePowered != this.isSidePowered))
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> this.level.getChunkSource().getChunkNow(this.getBlockPos().getX() >>4, this.getBlockPos().getZ() >>4)),
                                                 new UpdateHearthSignalsMessage(isSidePowered, isBackPowered, this.getBlockPos()));
        }
    }

    public void checkConsumeFuel()
    {
        BlockPos pos = this.getBlockPos();
        ItemStack fuelStack = this.getItems().get(0);
        if (!fuelStack.isEmpty())
        {   // Potion items
            List<EffectInstance> itemEffects = PotionUtils.getMobEffects(fuelStack);
            if (ConfigSettings.HEARTH_POTIONS_ENABLED.get()
            && !itemEffects.isEmpty() && !itemEffects.equals(effects)
            && itemEffects.stream().noneMatch(eff -> ConfigSettings.HEARTH_POTION_BLACKLIST.get().contains(eff.getEffect())))
            {
                if (fuelStack.getItem() instanceof PotionItem)
                {   this.getItems().set(0, Items.GLASS_BOTTLE.getDefaultInstance());
                }
                else if (!fuelStack.hasContainerItem() || fuelStack.getCount() > 1)
                {   fuelStack.shrink(1);
                }
                else
                {   this.getItems().set(0, fuelStack.getContainerItem());
                }

                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BREWING_STAND_BREW, SoundCategory.BLOCKS, 1, 1);
                effects.clear();
                // Convert to NBT and back again to create new instances of the effects (otherwise we would be ticking down the global instances)
                effects.addAll(itemEffects.stream().map(eff -> eff.save(new CompoundNBT())).map(EffectInstance::load).collect(Collectors.toList()));
                WorldHelper.syncBlockEntityData(this);
            }
            else if (fuelStack.getItem() == Items.MILK_BUCKET && !effects.isEmpty())
            {
                this.getItems().set(0, fuelStack.getContainerItem());
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1, 1);
                effects.clear();
                WorldHelper.syncBlockEntityData(this);
            }
            // Normal fuel items
            else
            {
                int itemFuel = getItemFuel(fuelStack);
                if (itemFuel != 0)
                {
                    this.storeFuel(fuelStack, itemFuel);
                }
            }
        }
    }

    protected boolean isFuelChanged()
    {
        return Math.abs(this.getColdFuel() - lastColdFuel) >= this.getMaxFuel()/36 || Math.abs(this.getHotFuel() - lastHotFuel) >= this.getMaxFuel()/36;
    }

    protected void storeFuel(ItemStack stack, int amount)
    {
        int fuel = amount > 0 ? this.getHotFuel() : this.getColdFuel();
        if (fuel < this.getMaxFuel() - Math.abs(amount) * 0.75)
        {
            if (!stack.hasContainerItem() || stack.getCount() > 1)
            {   int consumeCount = Math.min((int) Math.floor((this.getMaxFuel() - fuel) / (double) Math.abs(amount)), stack.getCount());
                stack.shrink(consumeCount);
                addFuel(amount * consumeCount);
            }
            else
            {   this.setItem(0, stack.getContainerItem());
                addFuel(amount);
            }
        }
    }

    protected void drainFuel()
    {
        if (this.shouldUseColdFuel)
        {   this.setColdFuel(this.getColdFuel() - 1, true);
        }
        if (this.shouldUseHotFuel)
        {   this.setHotFuel(this.getHotFuel() - 1, true);
        }
    }

    protected void tickDrainFuel()
    {
        if (this.ticksExisted % 40 == 0)
        {   this.drainFuel();
        }
    }

    protected void resetFuelStatus()
    {
        if (this.level == null || !this.level.isClientSide)
        {
            this.shouldUseColdFuel = false;
            this.shouldUseHotFuel = false;
        }
    }

    void insulatePlayer(PlayerEntity player)
    {
        for (int i = 0; i < effects.size(); i++)
        {
            EffectInstance effect = effects.get(i);
            player.addEffect(new EffectInstance(effect.getEffect(),
                                                   effect.getEffect() == Effects.NIGHT_VISION
                                                       ? 399
                                                       : 119,
                                                   effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }

        if (!ConfigSettings.SMART_HEARTH.get() || this.shouldInsulatePlayer(player))
        {
            int maxEffect = this.getMaxInsulationLevel() - 1;
            int effectLevel = (int) Math.min(maxEffect, (insulationLevel / (double) this.getInsulationTime()) * maxEffect);
            if (shouldUseColdFuel)
            {   player.addEffect(new EffectInstance(ModEffects.CHILL, 120, effectLevel, false, false, true));
            }
            if (shouldUseHotFuel)
            {   player.addEffect(new EffectInstance(ModEffects.WARMTH, 120, effectLevel, false, false, true));
            }
        }
    }

    protected boolean shouldInsulatePlayer(PlayerEntity player)
    {
        AtomicBoolean shouldInsulate = new AtomicBoolean(false);
        if (!shouldUseColdFuel || !shouldUseHotFuel)
        EntityTempManager.getTemperatureCap(player).ifPresent(cap ->
        {
            double temp = CSMath.getIfNotNull(Temperature.getModifier(cap, Temperature.Trait.WORLD, BlockInsulationTempModifier.class).orElse(null),
                                              TempModifier::getLastInput,
                                              cap.getTrait(Temperature.Trait.WORLD));
            double min = cap.getTrait(Temperature.Trait.FREEZING_POINT);
            double max = cap.getTrait(Temperature.Trait.BURNING_POINT);

            // Tell the hearth to use hot fuel
            shouldUseHotFuel |= this.getHotFuel() > 0 && temp < min;
            // Tell the hearth to use cold fuel
            shouldUseColdFuel |= this.getColdFuel() > 0 && temp > max;
            shouldInsulate.set(!CSMath.betweenInclusive(temp, min, max));
        });
        return shouldInsulate.get();
    }

    protected boolean isValidPipeAt(BlockPos newPos, BlockState fromState, SpreadPath newPath, Direction direction)
    {
        if (!isPipe(fromState)) return true;
        if (CompatManager.isCreateLoaded())
        {
            Block block = fromState.getBlock();
            if (!(block instanceof FluidPipeBlock || block instanceof GlassFluidPipeBlock || block instanceof EncasedPipeBlock))
            {   return true;
            }
            if ((block instanceof FluidPipeBlock && fromState.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction)))
            || (block instanceof GlassFluidPipeBlock && fromState.getValue(RotatedPillarBlock.AXIS) == direction.getAxis())
            || (block instanceof EncasedPipeBlock && fromState.getValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(direction))))
            {   newPath.setOrigin(newPos);
                return true;
            }
            return false;
        }
        return true;
    }

    protected boolean isPipe(BlockState state)
    {   return CompatManager.isCreateLoaded() && (state.getBlock() instanceof FluidPipeBlock
                                               || state.getBlock() instanceof GlassFluidPipeBlock
                                               || state.getBlock() instanceof EncasedPipeBlock);
    }

    private void registerLocation()
    {
        if (!this.registeredLocation)
        {   levelPos = Pair.of(this.getBlockPos(), level.dimension().location());
            HearthSaveDataHandler.HEARTH_POSITIONS.add(levelPos);
            this.x = this.getBlockPos().getX();
            this.y = this.getBlockPos().getY();
            this.z = this.getBlockPos().getZ();
            this.registeredLocation = true;
        }
    }

    private void unregisterLocation()
    {
        if (this.registeredLocation)
        {   HearthSaveDataHandler.HEARTH_POSITIONS.remove(levelPos);
            this.registeredLocation = false;
        }
    }

    protected void tickPotionEffects()
    {
        if (!effects.isEmpty())
        {
            effects.removeIf(effect ->
            {
                try
                {   TICK_DOWN_EFFECT.invoke(effect);
                    if (effect.getDuration() <=0) return true;
                }
                catch (Exception ignored) {}
                return false;
            });
        }
    }

    void resetPaths()
    {   // Reset cooldown
        this.rebuildCooldown = 100;

        // Clear paths & lookup
        paths.clear();
        pathLookup.clear();
        seeSkyMap.clear();

        // Un-freeze paths so areas can be re-checked
        frozenPaths = 0;
        spreading = true;

        // Tell client to reset paths too
        this.sendResetPacket();

        forceRebuild = false;
        this.isRebuildQueued = false;
    }

    public List<EffectInstance> getEffects()
    {   return effects;
    }

    public int getItemFuel(ItemStack item)
    {   return CSMath.getIfNotNull(ConfigSettings.HEARTH_FUEL.get().get(item.getItem()),
                                   fuel -> fuel.test(item) ? fuel.value : 0,
                                   0).intValue();
    }

    public int getHotFuel()
    {   return this.hotFuel.getAmount();
    }

    public int getColdFuel()
    {   return this.coldFuel.getAmount();
    }

    public void setHotFuel(int amount, boolean update)
    {
        if (this.hotFuel.isEmpty())
        {   this.hotFuel = new FluidStack(Fluids.LAVA, amount);
        }
        else this.hotFuel.setAmount(amount);

        if (amount == 0)
        {
            if (this.hasHotFuel)
            {
                hasHotFuel = false;
                if (level != null)
                {   level.playSound(null, this.getBlockPos(), this.getFuelDepleteSound(), SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
                }
            }
        }
        else hasHotFuel = true;

        if (update) this.updateFuelState();
    }

    public void setHotFuelAndUpdate(int amount)
    {   this.setHotFuel(amount, true);
    }

    public void setColdFuel(int amount, boolean update)
    {
        if (this.coldFuel.isEmpty())
        {   this.coldFuel = new FluidStack(Fluids.WATER, amount);
        }
        else this.coldFuel.setAmount(amount);

        if (amount <= 0)
        {
            if (this.hasColdFuel)
            {
                hasColdFuel = false;
                if (level != null)
                {   level.playSound(null, this.getBlockPos(), this.getFuelDepleteSound(), SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
                }
            }
        }
        else
        {   hasColdFuel = true;
        }

        if (update) this.updateFuelState();
    }

    public void setColdFuelAndUpdate(int amount)
    {   this.setColdFuel(amount, true);
    }

    /**
     * Negative numbers are cold, positive numbers are hot
     */
    public void addFuel(int amount)
    {   if (amount > 0)
        {   this.setHotFuelAndUpdate(this.getHotFuel() + amount);
        }
        else if (amount < 0)
        {   this.setColdFuelAndUpdate(this.getColdFuel() + Math.abs(amount));
        }
    }

    public void updateFuelState()
    {
        if (level != null && !level.isClientSide)
        {   this.setChanged();
            WorldHelper.syncBlockEntityData(this);
            this.lastColdFuel = this.getColdFuel();
            this.lastHotFuel = this.getHotFuel();
        }
    }

    protected SoundEvent getFuelDepleteSound()
    {   return ModSounds.HEARTH_DEPLETE;
    }

    public boolean checkForSmokestack()
    {
        if (level == null) return false;

        boolean hadSmokestack = this.hasSmokestack;
        this.hasSmokestack = level.getBlockState(this.getBlockPos().above()).getBlock() == ModBlocks.SMOKESTACK;
        // A smokestack has been added
        if (this.hasSmokestack && !hadSmokestack)
        {   this.registerLocation();
            if (this.level.isClientSide)
            {   ClientOnlyHelper.addHearthPosition(this.getBlockPos());
            }
        }
        // A smokestack has been removed
        else if (!this.hasSmokestack && hadSmokestack)
        {   this.resetPaths();
            this.unregisterLocation();
            if (this.level.isClientSide)
            {   ClientOnlyHelper.removeHearthPosition(this.getBlockPos());
            }
        }
        return this.hasSmokestack;
    }

    protected void tickParticles()
    {
        // Calculate the height of the smokestack (can be extended with walls)
        if (this.ticksExisted % 20 == 0)
        {   this.smokestackHeight = 2;
            BlockState state = level.getBlockState(this.getBlockPos().above(this.smokestackHeight));
            while (state.getBlock() instanceof WallBlock || state.getBlock() instanceof SmokestackBlock)
            {   this.smokestackHeight++;
                state = level.getBlockState(this.getBlockPos().above(this.smokestackHeight));
            }
        }

        Random rand = this.level.random;
        if (this.shouldUseColdFuel)
        {
            if (rand.nextDouble() < this.getColdFuel() / 3000d)
            {   double d0 = this.x + 0.5d;
                double d1 = this.y + this.smokestackHeight;
                double d2 = this.z + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 4;
                double d4 = (rand.nextDouble() - 0.5) / 4;
                double d5 = (rand.nextDouble() - 0.5) / 4;
                level.addParticle(ParticleTypesInit.STEAM.get(), d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.04D, 0.0D);
            }
        }
        if (this.shouldUseHotFuel)
        {
            if (rand.nextDouble() < this.getHotFuel() / 3000d)
            {   double d0 = this.x + 0.5d;
                double d1 = this.y + this.smokestackHeight;
                double d2 = this.z + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 2;
                double d4 = (rand.nextDouble() - 0.5) / 2;
                double d5 = (rand.nextDouble() - 0.5) / 2;
                BasicParticleType particle = rand.nextDouble() < 0.5
                                              ? ParticleTypes.LARGE_SMOKE
                                              : ParticleTypes.SMOKE;
                level.addParticle(particle, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public BasicParticleType getAirParticle()
    {   return ParticleTypesInit.HEARTH_AIR.get();
    }

    public void spawnAirParticle(int x, int y, int z, Random rand)
    {
        if (rand.nextFloat() > (spreading ? 0.016f : 0.032f)) return;

        float xr = rand.nextFloat();
        float yr = rand.nextFloat();
        float zr = rand.nextFloat();
        float xm = rand.nextFloat() / 20 - 0.025f;
        float zm = rand.nextFloat() / 20 - 0.025f;

        level.addParticle(this.getAirParticle(), false, x + xr, y + yr, z + zr, xm, 0, zm);
    }

    @Override
    public int getContainerSize()
    {   return 1;
    }

    @Override
    protected Container createMenu(int id, PlayerInventory playerInv)
    {   return new HearthContainer(id, playerInv, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT tag)
    {   super.load(state, tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, this.items);
        this.loadEffects(tag);
        this.coldFuel = FluidStack.loadFluidStackFromNBT(tag.getCompound("ColdFuel"));
        this.hotFuel = FluidStack.loadFluidStackFromNBT(tag.getCompound("HotFuel"));
        this.insulationLevel = tag.getInt("InsulationLevel");
    }

    @Override
    public CompoundNBT save(CompoundNBT tag)
    {   super.save(tag);
        ItemStackHelper.saveAllItems(tag, this.items);
        saveEffects(tag);
        tag.put("ColdFuel", this.coldFuel.writeToNBT(new CompoundNBT()));
        tag.put("HotFuel", this.hotFuel.writeToNBT(new CompoundNBT()));
        tag.putInt("InsulationLevel", this.insulationLevel);

        return tag;
    }

    void saveEffects(CompoundNBT tag)
    {
        if (!this.effects.isEmpty())
        {   ListNBT list = new ListNBT();
            for (EffectInstance effect : this.effects)
            {   list.add(effect.save(new CompoundNBT()));
            }
            tag.put("Effects", list);
        }
    }

    void loadEffects(CompoundNBT tag)
    {   this.effects.clear();
        if (tag.contains("Effects"))
        {   ListNBT list = tag.getList("Effects", 10);
            for (int i = 0; i < list.size(); i++)
            {   this.effects.add(EffectInstance.load(list.getCompound(i)));
            }
        }
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT tag = super.getUpdateTag();
        tag.putInt("HotFuel",  this.getHotFuel());
        tag.putInt("ColdFuel", this.getColdFuel());
        tag.putBoolean("ShouldUseColdFuel", this.shouldUseColdFuel);
        tag.putBoolean("ShouldUseHotFuel", this.shouldUseHotFuel);
        tag.putInt("InsulationLevel", insulationLevel);
        this.saveEffects(tag);

        return tag;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag)
    {   this.setHotFuel(tag.getInt("HotFuel"), false);
        this.setColdFuel(tag.getInt("ColdFuel"), false);
        this.shouldUseColdFuel = tag.getBoolean("ShouldUseColdFuel");
        this.shouldUseHotFuel = tag.getBoolean("ShouldUseHotFuel");
        this.insulationLevel = tag.getInt("InsulationLevel");
        this.loadEffects(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
    {   handleUpdateTag(null, pkt.getTag());
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket()
    {   return new SUpdateTileEntityPacket(this.getBlockPos(), 0, this.getUpdateTag());
    }

    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing)
    {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && facing != null
             ? facing == Direction.DOWN
                       ? bottomFuelHolder.cast()
             : facing != Direction.UP && facing != this.getBlockState().getValue(HearthBottomBlock.FACING)
                       ? sidesFuelHolder.cast()
             : super.getCapability(capability, facing) : super.getCapability(capability, facing);
    }

    public void replacePaths(ArrayList<SpreadPath> newPaths)
    {   this.frozenPaths = 0;
        this.paths = newPaths;
        this.pathLookup = newPaths.stream().map(path -> path.pos).collect(HashSet::new, HashSet::add, HashSet::addAll);
        this.spreading = true;
        if (this.level.isClientSide)
        {   ClientOnlyHelper.addHearthPosition(this.getBlockPos());
        }
        this.sendResetPacket();
    }

    public void addPath(SpreadPath path)
    {   paths.add(path);
    }

    public void addPaths(Collection<SpreadPath> newPaths)
    {   paths.addAll(newPaths);
    }

    public void sendResetPacket()
    {   if (level instanceof ServerWorld)
        {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() ->
                                 (Chunk) WorldHelper.getChunk(level, this.getBlockPos())), new HearthResetMessage(this.getBlockPos()));
        }
    }

    public void sendBlockUpdate()
    {   isRebuildQueued = true;
    }

    public void forceUpdate()
    {   this.forceRebuild = true;
        this.sendBlockUpdate();
    }

    @Override
    public void setRemoved()
    {   super.setRemoved();
        HearthSaveDataHandler.HEARTH_POSITIONS.remove(Pair.of(this.getBlockPos(), this.getLevel().dimension().location()));
        if (this.level.isClientSide)
        {   ClientOnlyHelper.removeHearthPosition(this.getBlockPos());
        }
    }

    public Set<BlockPos> getPathLookup()
    {   return this.pathLookup;
    }

    public boolean isSidePowered()
    {   return this.isSidePowered;
    }

    public boolean isBackPowered()
    {   return this.isBackPowered;
    }

    public void setSidePowered(boolean isPowered)
    {   this.isSidePowered = isPowered;
    }

    public void setBackPowered(boolean isPowered)
    {   this.isBackPowered = isPowered;
    }

    public abstract class FluidHandler implements IFluidHandler
    {
        @Override
        public int getTanks()
        {   return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank)
        {   return tank == 0 ? coldFuel : hotFuel;
        }

        @Override
        public int getTankCapacity(int tank)
        {   return HearthBlockEntity.this.getMaxFuel();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluidStack)
        {
            return tank == 0 ? fluidStack.getFluid() == Fluids.WATER
                             : fluidStack.getFluid() == Fluids.LAVA;
        }

        @Override
        public int fill(FluidStack fluidStack, FluidAction fluidAction)
        {
            if (fluidStack.getFluid() == Fluids.WATER)
            {   int amount = Math.min(fluidStack.getAmount(), this.getTankCapacity(0) - coldFuel.getAmount());
                if (fluidAction.execute())
                {
                    if (coldFuel.isEmpty())
                    {   coldFuel = fluidStack.copy();
                    }
                    else coldFuel.grow(amount);
                }
                return amount;
            }
            else if (fluidStack.getFluid() == Fluids.LAVA)
            {   int amount = Math.min(fluidStack.getAmount(), this.getTankCapacity(1) - hotFuel.getAmount());
                if (fluidAction.execute())
                {
                    if (hotFuel.isEmpty())
                    {   hotFuel = fluidStack.copy();
                    }
                    else hotFuel.grow(amount);
                }
                return amount;
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
        {
            return this.isFluidValid(0, fluidStack) || this.isFluidValid(1, fluidStack)
                 ? this.drain(fluidStack.getAmount(), fluidAction)
                 : FluidStack.EMPTY;
        }

        @Override
        public abstract FluidStack drain(int amount, FluidAction fluidAction);
    }

    /**
     * Drains from water storage by default
     */
    private class SidesFluidHandler extends FluidHandler
    {
        @Override
        public FluidStack drain(int amount, FluidAction fluidAction)
        {
            int drained = Math.min(coldFuel.getAmount(), amount);

            FluidStack stack = new FluidStack(coldFuel, drained);
            if (fluidAction.execute() && drained > 0)
            {   coldFuel.shrink(drained);
            }
            HearthBlockEntity.this.setChanged();

            return stack;
        }
    }

    /**
     * Drains from lava storage by default
     */
    private class BottomFluidHandler extends FluidHandler
    {
        @Override
        public FluidStack drain(int amount, FluidAction fluidAction)
        {
            int drained = Math.min(hotFuel.getAmount(), amount);

            FluidStack stack = new FluidStack(hotFuel, drained);
            if (fluidAction.execute() && drained > 0)
            {   hotFuel.shrink(drained);
            }
            HearthBlockEntity.this.setChanged();

            return stack;
        }
    }
}