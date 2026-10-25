package fr.oneblockquests;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quêtes WALK (marcher) et SWIM (nager) : la progression est comptée en blocs.
 * Les fractions de bloc sont cumulées pour ne rien perdre entre deux mouvements.
 */
public final class MovementListener implements Listener {

    private final OneBlockQuestsPlugin plugin;
    private final Map<UUID, Double> buffer = new HashMap<>();

    public MovementListener(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        // Les téléportations (perle, commande...) ne comptent pas
        if (e instanceof PlayerTeleportEvent) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        if (dx == 0 && dy == 0 && dz == 0) return; // simple rotation de la tête

        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p.getUniqueId());
        Quest q = plugin.quests().current(d);
        if (q == null) return;
        QuestType type = q.type();
        if (type != QuestType.WALK && type != QuestType.SWIM) return;

        if (p.getGameMode() == GameMode.SPECTATOR || p.isFlying() || p.isGliding() || p.isInsideVehicle()) {
            return;
        }

        double distance;
        if (type == QuestType.SWIM) {
            if (!p.isInWater()) return;
            distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            if (p.isInWater() || p.isSwimming() || !p.isOnGround()) return;
            distance = Math.sqrt(dx * dx + dz * dz);
        }

        double maxStep = plugin.cfg().getDouble("movement.max-step", 3.0);
        if (distance <= 0 || distance > maxStep) return;

        double total = buffer.merge(p.getUniqueId(), distance, Double::sum);
        int whole = (int) total;
        if (whole > 0) {
            buffer.put(p.getUniqueId(), total - whole);
            plugin.quests().progress(p, type, "ANY", whole);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        buffer.remove(e.getPlayer().getUniqueId());
    }
}
