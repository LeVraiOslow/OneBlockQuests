package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

public final class PlayerListener implements Listener {

    private final OneBlockQuestsPlugin plugin;

    public PlayerListener(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        setup(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.bossBars().hide(p);
        plugin.data().unload(p.getUniqueId());
    }

    public void setup(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d.isNew) {
            d.isNew = false;
            Category first = plugin.quests().first();
            if (first != null) {
                // Le joueur commence obligatoirement par la quête n°1 de la première catégorie
                plugin.permissions().grantCategory(p, first);
                plugin.permissions().grantQuest(p, first, 1);
            }
            plugin.send(p, "welcome", new HashMap<>());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) plugin.bossBars().update(p);
        }, 20L);
    }
}
