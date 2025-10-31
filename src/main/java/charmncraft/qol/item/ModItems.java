package charmncraft.qol.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import charmncraft.qol.Charmncraftqolchanges;

public class ModItems {
    public static final Item DIAMOND_NUGGET = registerItem("diamond_nugget", new Item(new Item.Settings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Charmncraftqolchanges.MOD_ID, name), item);
    }

    public static void registerModItems() {
        registerItem("diamond_nugget", DIAMOND_NUGGET);
    }
}