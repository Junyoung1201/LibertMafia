package libert.saehyeon.mafia.clue;

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
        if (Clue.isValidClueRecipe(matrix)) {
            inventory.setResult(Clue.createWeaponItem());
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
        if (!Clue.isWeaponItem(result)) {
            return;
        }
        ItemStack[] matrix = ((CraftingInventory) event.getInventory()).getMatrix();
        if (!Clue.isValidClueRecipe(matrix)) {
            event.setCancelled(true);
        }
    }
}

