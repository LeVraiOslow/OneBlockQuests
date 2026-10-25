package fr.oneblockquests;

import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class QuestListener implements Listener {

    private final OneBlockQuestsPlugin plugin;

    /** Blocs posés par des joueurs : ils ne comptent pas pour les quêtes BREAK (anti-farm). */
    private final Set<String> placed = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 50000;
        }
    });

    public QuestListener(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    private static String key(Block b) {
        return b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (placed.remove(key(b))) return;
        plugin.quests().progress(e.getPlayer(), QuestType.BREAK, b.getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        placed.add(key(b));
        plugin.quests().progress(e.getPlayer(), QuestType.PLACE, b.getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || e.getEntity() instanceof Player) return;
        plugin.quests().progress(killer, QuestType.KILL, e.getEntityType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack result = e.getRecipe().getResult();
        int amount = result.getAmount();
        if (e.isShiftClick()) {
            int min = Integer.MAX_VALUE;
            for (ItemStack it : e.getInventory().getMatrix()) {
                if (it != null && !it.getType().isAir()) min = Math.min(min, it.getAmount());
            }
            if (min == Integer.MAX_VALUE) return;
            amount = result.getAmount() * min;
        }
        plugin.quests().progress(p, QuestType.CRAFT, result.getType().name(), amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        String target = "ANY";
        if (e.getCaught() instanceof Item item) {
            target = item.getItemStack().getType().name();
        }
        plugin.quests().progress(e.getPlayer(), QuestType.FISH, target, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player p) {
            plugin.quests().progress(p, QuestType.BREED, e.getEntityType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        plugin.quests().progress(e.getEnchanter(), QuestType.ENCHANT, e.getItem().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent e) {
        plugin.quests().progress(e.getPlayer(), QuestType.SMELT, e.getItemType().name(), e.getItemAmount());
    }
}
