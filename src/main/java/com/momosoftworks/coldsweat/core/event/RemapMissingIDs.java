package com.momosoftworks.coldsweat.core.event;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class RemapMissingIDs
{
    @SubscribeEvent
    public static void remapMissingItems(RegistryEvent.MissingMappings<Item> event)
    {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings())
        {
            if (mapping.key.getNamespace().equals(ColdSweat.MOD_ID) && mapping.key.toString().contains("fur"))
            {   mapping.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ColdSweat.MOD_ID, mapping.key.getPath().replace("fur", "goat_fur"))));
            }
        }
    }
}
