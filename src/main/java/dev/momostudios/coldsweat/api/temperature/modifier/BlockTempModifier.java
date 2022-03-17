package dev.momostudios.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3d;
import dev.momostudios.coldsweat.common.world.BlockEffectEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.block.BlockEffect;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BlockTempModifier extends TempModifier
{
    private static final Map<Block, BlockEffect> MAPPED_BLOCKS = new HashMap<>();

    public BlockTempModifier() {}

    @Override
    public Temperature getResult(Temperature temp, Player player)
    {
        double totalTemp = 0;
        Map<Pair<Integer, Integer>, LevelChunk> chunkMap = new HashMap<>();
        ArrayDeque<Pair<Integer, Integer>> chunkPositions = new ArrayDeque<>(30);
        Level level = player.level;

        for (int x1 = -7; x1 < 14; x1++)
        {
            for (int z1 = -7; z1 < 14; z1++)
            {
                Pair<Integer, Integer> chunkPos = Pair.of((player.blockPosition().getX() + x1) >> 4, (player.blockPosition().getZ() + z1) >> 4);
                LevelChunk chunk;
                if (chunkPositions.contains(chunkPos))
                {
                    chunk = chunkMap.get(chunkPos);
                }
                else
                {
                    chunk = level.getChunkSource().getChunkNow(chunkPos.getFirst(), chunkPos.getSecond());
                    if (chunk == null) continue;

                    chunkMap.put(chunkPos, chunk);
                    chunkPositions.add(chunkPos);
                }

                for (int y1 = -7; y1 < 14; y1++)
                {
                    try
                    {
                        BlockPos blockpos = player.blockPosition().offset(x1, y1, z1);

                        // Get the BlockEffect associated with the block
                        // We map the blocks with their corresponding BlockEffects to reduce the amount of calls to BlockEffectEntries
                        PalettedContainer<BlockState> palette = chunk.getSection((blockpos.getY() >> 4) - chunk.getMinSection()).getStates();
                        BlockState state = palette.get(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);

                        if (state.isAir()) continue;

                        BlockEffect be;
                        if (!MAPPED_BLOCKS.keySet().contains(state.getBlock()))
                        {
                            be = BlockEffectEntries.getEntries().getEntryFor(state);
                            MAPPED_BLOCKS.put(state.getBlock(), be);
                        }
                        else
                        {
                            be = MAPPED_BLOCKS.get(state.getBlock());
                        }

                        if (be == null) continue;

                        // Is totalTemp within the bounds of the BlockEffect's min/max allowed temps?
                        if (CSMath.isBetween(totalTemp, be.minEffect(), be.maxEffect())
                        && CSMath.isBetween(temp.get() + totalTemp, be.minTemperature(), be.maxTemperature()))
                        {
                            // Get Vector positions of the centers of the source block and player
                            Vec3 pos = new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
                            Vec3 playerPos1 = new Vec3(player.getX(), player.getY() + player.getEyeHeight() * 0.25, player.getZ());
                            Vec3 playerPos2 = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(player, new Vector3d(pos.x, pos.y, pos.z));
                            double tempToAdd = be.getTemperature(player, state, blockpos, distance);

                            // Lessen the effect with each block between the player and the block
                            Vec3 prevPos1 = playerPos1;
                            Vec3 prevPos2 = playerPos2;
                            int blocksBetween = 0;
                            for (int i = 0; i < distance * 1.25; i++)
                            {
                                // Get the next block (sub)position
                                Vec3 newPos1 = playerPos1.subtract(playerPos1.subtract(pos).scale(i / (distance * 1.25)));
                                Vec3 newPos2 = playerPos2.subtract(playerPos2.subtract(pos).scale(i / (distance * 1.25)));

                                // Check if the newPos1 is not a duplicate BlockPos or solid block
                                BlockPos bpos1 = new BlockPos(newPos1);
                                Vec3 facing1 = newPos1.subtract(prevPos1);
                                Direction dir1 = CSMath.getDirectionFromVector(facing1.x, facing1.y, facing1.z);

                                if (!bpos1.equals(new BlockPos(prevPos1)) && !bpos1.equals(blockpos)
                                && !WorldHelper.canSpreadThrough(level, palette.get(bpos1.getX() & 15, bpos1.getY() & 15, bpos1.getZ() & 15), bpos1, dir1))
                                {
                                    // Divide the added temperature by 2 for each block between the player and the block
                                    blocksBetween++;
                                }

                                // Check if the newPos2 is not a duplicate BlockPos or solid block
                                BlockPos bpos2 = new BlockPos(newPos2);
                                Vec3 facing2 = newPos2.subtract(prevPos2);
                                Direction dir2 = CSMath.getDirectionFromVector(facing2.x, facing2.y, facing2.z);

                                if (!bpos2.equals(new BlockPos(prevPos2)) && !bpos2.equals(blockpos)
                                && !WorldHelper.canSpreadThrough(level, palette.get(bpos2.getX() & 15, bpos2.getY() & 15, bpos2.getZ() & 15), bpos2, dir2))
                                {
                                    // Divide the added temperature by 2 for each block between the player and the block
                                    blocksBetween++;
                                }

                                prevPos1 = newPos1;
                                prevPos2 = newPos2;
                            }
                            double blockDampening = Math.pow(1.5, blocksBetween);

                            totalTemp += tempToAdd / blockDampening;
                        }
                    }
                    catch (Exception e) {}
                }
            }
        }
        
        return temp.add(totalTemp);
    }

    public String getID()
    {
        return "cold_sweat:nearby_blocks";
    }
}