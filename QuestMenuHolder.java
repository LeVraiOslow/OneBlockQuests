package fr.oneblockquests;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class QuestMenuHolder implements InventoryHolder {

    public enum Kind { MAIN, CATEGORY }

    private final Kind kind;
    private final String categoryId;
    private Inventory inventory;

    public QuestMenuHolder(Kind kind, String categoryId) {
        this.kind = kind;
        this.categoryId = categoryId;
    }

    public Kind kind() { return kind; }

    public String categoryId() { return categoryId; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
