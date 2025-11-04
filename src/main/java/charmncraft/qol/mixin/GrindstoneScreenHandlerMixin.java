package charmncraft.qol.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {

    @Shadow @Final Inventory input;
    @Shadow @Final private Inventory result;

    @Unique
    private boolean charmncraft_qol$isCustomRecipe = false;

    @Unique
    private int charmncraft_qol$customRecipeType = 0; // 0 = none, 1 = disenchant to book, 2 = transfer enchantment

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack topSlot = this.input.getStack(0);
        ItemStack bottomSlot = this.input.getStack(1);

        // Reset custom recipe flags
        charmncraft_qol$isCustomRecipe = false;
        charmncraft_qol$customRecipeType = 0;

        // Case 1: Enchanted item + Book -> Enchanted book + unenchanted item
        ItemStack disenchantResult = tryDisenchantToBook(topSlot, bottomSlot);
        if (!disenchantResult.isEmpty()) {
            this.result.setStack(0, disenchantResult);
            charmncraft_qol$isCustomRecipe = true;
            charmncraft_qol$customRecipeType = 1;
            ci.cancel();
            return;
        }

        // Case 2: Enchanted book + Book -> Transfer one enchantment
        ItemStack transferResult = tryTransferEnchantment(topSlot, bottomSlot);
        if (!transferResult.isEmpty()) {
            this.result.setStack(0, transferResult);
            charmncraft_qol$isCustomRecipe = true;
            charmncraft_qol$customRecipeType = 2;
            ci.cancel();
            return;
        }
    }

    @Inject(method = "onTakeOutput", at = @At("HEAD"), cancellable = true)
    private void onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (charmncraft_qol$isCustomRecipe) {
            if (charmncraft_qol$customRecipeType == 1) {
                // Disenchant to book: consume both items
                this.input.setStack(0, ItemStack.EMPTY);
                this.input.setStack(1, ItemStack.EMPTY);
            } else if (charmncraft_qol$customRecipeType == 2) {
                // Transfer enchantment: update the top book and consume bottom book
                ItemStack topSlot = this.input.getStack(0);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(topSlot);

                if (!enchantments.isEmpty()) {
                    // Remove the first enchantment
                    Map.Entry<Enchantment, Integer> firstEnchantment = enchantments.entrySet().iterator().next();
                    enchantments.remove(firstEnchantment.getKey());

                    if (enchantments.isEmpty()) {
                        // If no enchantments remain, convert to regular book
                        this.input.setStack(0, new ItemStack(Items.BOOK));
                    } else {
                        // Otherwise, update the enchanted book
                        ItemStack newTopBook = new ItemStack(Items.ENCHANTED_BOOK);
                        EnchantmentHelper.set(enchantments, newTopBook);
                        this.input.setStack(0, newTopBook);
                    }
                }

                // Consume the bottom book
                this.input.setStack(1, ItemStack.EMPTY);
            }

            this.input.markDirty();
            ci.cancel();
        }
    }

    @Unique
    private ItemStack tryDisenchantToBook(ItemStack topSlot, ItemStack bottomSlot) {
        ItemStack enchantedItem = ItemStack.EMPTY;
        ItemStack book = ItemStack.EMPTY;

        // Check if we have an enchanted item and a book
        if (topSlot.hasEnchantments() && !topSlot.isOf(Items.ENCHANTED_BOOK) && bottomSlot.isOf(Items.BOOK)) {
            enchantedItem = topSlot;
            book = bottomSlot;
        } else if (bottomSlot.hasEnchantments() && !bottomSlot.isOf(Items.ENCHANTED_BOOK) && topSlot.isOf(Items.BOOK)) {
            enchantedItem = bottomSlot;
            book = topSlot;
        }

        if (!enchantedItem.isEmpty() && !book.isEmpty()) {
            // Create enchanted book with the enchantments
            ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(enchantedItem);
            EnchantmentHelper.set(enchantments, enchantedBook);
            return enchantedBook;
        }

        return ItemStack.EMPTY;
    }

    @Unique
    private ItemStack tryTransferEnchantment(ItemStack topSlot, ItemStack bottomSlot) {
        // Check if we have an enchanted book in top slot and a book (or enchanted book) in bottom slot
        if (topSlot.isOf(Items.ENCHANTED_BOOK) && topSlot.hasEnchantments() &&
            (bottomSlot.isOf(Items.BOOK) || bottomSlot.isOf(Items.ENCHANTED_BOOK))) {

            Map<Enchantment, Integer> topEnchantments = EnchantmentHelper.get(topSlot);

            if (!topEnchantments.isEmpty()) {
                // Get the first enchantment
                Map.Entry<Enchantment, Integer> firstEnchantment = topEnchantments.entrySet().iterator().next();

                // Create the result book with enchantments from bottom slot plus the transferred enchantment
                ItemStack resultBook = new ItemStack(Items.ENCHANTED_BOOK);
                Map<Enchantment, Integer> resultEnchantments = new HashMap<>(EnchantmentHelper.get(bottomSlot));
                resultEnchantments.put(firstEnchantment.getKey(), firstEnchantment.getValue());
                EnchantmentHelper.set(resultEnchantments, resultBook);

                return resultBook;
            }
        }

        return ItemStack.EMPTY;
    }
}
