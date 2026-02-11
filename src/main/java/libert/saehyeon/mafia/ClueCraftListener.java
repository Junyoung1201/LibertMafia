package libert.saehyeon.mafia;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class ClueCraftListener implements Listener {

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        if (ClueManager.isValidClueRecipe(matrix)) {
            inventory.setResult(ClueManager.createWeaponItem());
        } else {
            inventory.setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory)) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        if (!ClueManager.isWeaponItem(result)) {
            return;
        }
        ItemStack[] matrix = ((CraftingInventory) event.getInventory()).getMatrix();
        if (!ClueManager.isValidClueRecipe(matrix)) {
            event.setCancelled(true);
        }
    }
}

