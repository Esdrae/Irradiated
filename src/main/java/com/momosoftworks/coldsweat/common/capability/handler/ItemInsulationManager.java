package com.momosoftworks.coldsweat.common.capability.handler;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.common.capability.ModCapabilities;
import com.momosoftworks.coldsweat.common.capability.insulation.IInsulatableCap;
import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.TypedField;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.enchantment.IArmorVanishable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class ItemInsulationManager
{
    public static Map<ItemStack, LazyOptional<IInsulatableCap>> ITEM_INSULATION_CAPS = new WeakHashMap<>();

    @SubscribeEvent
    public static void attachCapabilityToItemHandler(AttachCapabilitiesEvent<ItemStack> event)
    {
        ItemStack stack = event.getObject();
        if (isInsulatable(stack))
        {
            // Make a new capability instance to attach to the item
            ItemInsulationCap itemInsulationCap = new ItemInsulationCap();
            // Optional that holds the capability instance
            LazyOptional<IInsulatableCap> capOptional = LazyOptional.of(() -> itemInsulationCap);
            Capability<IInsulatableCap> capability = ModCapabilities.ITEM_INSULATION;

            ICapabilityProvider provider = new ICapabilitySerializable<CompoundNBT>()
            {
                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction)
                {
                    // If the requested cap is the insulation cap, return the insulation cap
                    if (cap == capability)
                    {   return capOptional.cast();
                    }
                    return LazyOptional.empty();
                }

                @Override
                public CompoundNBT serializeNBT()
                {   return itemInsulationCap.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundNBT nbt)
                {   itemInsulationCap.deserializeNBT(nbt);
                }
            };

            // Attach the capability to the item
            event.addCapability(new ResourceLocation(ColdSweat.MOD_ID, "item_insulation"), provider);

            // Legacy code for updating items using the pre-2.2 insulation system
            CompoundNBT stackNBT = NBTHelper.getTagOrEmpty(stack);
            if (stack.getItem() instanceof ArmorItem)
            {
                ArmorItem armor = (ArmorItem) stack.getItem();
                if (stackNBT.getBoolean("insulated"))
                {   stackNBT.remove("insulated");
                    switch (armor.getSlot())
                    {   case HEAD  : itemInsulationCap.addInsulationItem(Items.LEATHER_HELMET.getDefaultInstance()); break;
                        case CHEST : itemInsulationCap.addInsulationItem(Items.LEATHER_CHESTPLATE.getDefaultInstance()); break;
                        case LEGS  : itemInsulationCap.addInsulationItem(Items.LEATHER_LEGGINGS.getDefaultInstance()); break;
                        case FEET  : itemInsulationCap.addInsulationItem(Items.LEATHER_BOOTS.getDefaultInstance()); break;
                        default    : itemInsulationCap.addInsulationItem(ItemStack.EMPTY); break;
                    }
                }
            }
        }
    }

    public static LazyOptional<IInsulatableCap> getInsulationCap(ItemStack stack)
    {
        if (!(stack.getItem() instanceof IArmorVanishable)) return LazyOptional.empty();
        return ITEM_INSULATION_CAPS.computeIfAbsent(stack, s -> stack.getCapability(ModCapabilities.ITEM_INSULATION));
    }

    @SubscribeEvent
    public static void handleInventoryOpen(PlayerContainerEvent event)
    {
        event.getPlayer().getPersistentData().putBoolean("InventoryOpen", event instanceof PlayerContainerEvent.Open);
    }

    static IContainerListener INSULATION_LISTENER = new IContainerListener()
    {
        @Override
        public void slotChanged(Container sendingContainer, int slot, ItemStack stack)
        {
            ItemStack containerStack = sendingContainer.getSlot(slot).getItem();
            getInsulationCap(containerStack).ifPresent(cap ->
            {
                // Serialize insulation for syncing to client
                containerStack.getOrCreateTag().merge(cap.serializeNBT());
            });
        }

        @Override
        public void refreshContainer(Container pContainerToSend, NonNullList<ItemStack> pItemsList)
        {

        }

        @Override
        public void setContainerData(Container pContainer, int pVarToUpdate, int pNewValue)
        {

        }
    };

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event)
    {
        event.getContainer().addSlotListener(INSULATION_LISTENER);
    }

    static final TypedField<List<IContainerListener>> SLOT_LISTENERS = TypedField.of(ObfuscationReflectionHelper.findField(Container.class, "field_75149_d"));
    static
    {   SLOT_LISTENERS.field().setAccessible(true);
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event)
    {
        SLOT_LISTENERS.get(event.getContainer()).remove(INSULATION_LISTENER);
        event.getContainer().broadcastChanges();
    }

    public static int getInsulationSlots(ItemStack item)
    {
        return ConfigSettings.INSULATION_SLOTS.get().getSlots(MobEntity.getEquipmentSlotForItem(item), item);
    }

    public static boolean isInsulatable(ItemStack stack)
    {
        return stack.getItem() instanceof IArmorVanishable
            && !ConfigSettings.INSULATION_ITEMS.get().containsKey(stack.getItem());
    }

    public static List<Insulator> getInsulatorsForStack(ItemStack stack)
    {
        List<Insulator> insulators = new ArrayList<>();
        if (isInsulatable(stack))
        {
            getInsulationCap(stack).ifPresent(cap ->
            {
                for (Pair<ItemStack, List<Insulation>> pair : cap.getInsulation())
                {
                    CSMath.doIfNotNull(ConfigSettings.INSULATION_ITEMS.get().get(pair.getFirst().getItem()), insulators::add);
                }
            });
        }

        CSMath.doIfNotNull(ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()), insulators::add);
        CSMath.doIfNotNull(ConfigSettings.INSULATING_CURIOS.get().get(stack.getItem()), insulators::add);

        return insulators;
    }

    public static List<AttributeModifier> getInsulationAttributeModifiers(ItemStack stack, Attribute attribute, @Nullable AttributeModifier.Operation operation, @Nullable Entity owner)
    {
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (Insulator insulator : getInsulatorsForStack(stack))
        {
            if (insulator.test(owner, stack))
            {
                modifiers.addAll(insulator.attributes.get(attribute)
                                          .stream()
                                          .filter(mod -> operation == null || mod.getOperation() == operation)
                                          .collect(Collectors.toList()));
            }
        }
        return modifiers;
    }

    public static List<AttributeModifier> getAttributeModifiersForSlot(ItemStack stack, Attribute attribute, EquipmentSlotType slot, @Nullable AttributeModifier.Operation operation, @Nullable Entity owner)
    {
        List<AttributeModifier> modifiers = new ArrayList<>(operation != null
                                                  ? stack.getAttributeModifiers(slot).get(attribute)
                                                         .stream()
                                                         .filter(mod -> mod.getOperation() == operation)
                                                         .collect(Collectors.toList())
                                                  : stack.getAttributeModifiers(slot).get(attribute));
        modifiers.addAll(getInsulationAttributeModifiers(stack, attribute, operation, owner));
        return modifiers;
    }

    public static List<AttributeModifier> getAttributeModifiersForSlot(ItemStack stack, Attribute attribute, EquipmentSlotType slot)
    {   return getAttributeModifiersForSlot(stack, attribute, slot, null, null);
    }
}
