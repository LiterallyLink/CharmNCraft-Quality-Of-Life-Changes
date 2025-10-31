package charmncraft.qol.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class ModItemGroups {
    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
            .register(content -> {
                content.add(ModItems.DIAMOND_NUGGET);
                content.add(ModItems.NETHERITE_NUGGET);
                content.add(ModItems.EMERALD_NUGGET);
            });
    }
}
