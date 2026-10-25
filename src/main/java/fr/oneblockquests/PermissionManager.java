package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PermissionManager {

    private final OneBlockQuestsPlugin plugin;

    public PermissionManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void grantQuest(Player p, Category c, int number) {
        String node = plugin.cfg().getString("permissions.quest-node", "oneblockquests.quest.{category}.{number}")
                .replace("{category}", c.id())
                .replace("{number}", String.valueOf(number));
        grant(p, node);
    }

    public void grantCategory(Player p, Category c) {
        String node = plugin.cfg().getString("permissions.category-node", "oneblockquests.category.{category}")
                .replace("{category}", c.id());
        grant(p, node);
    }

    private void grant(Player p, String node) {
        if (!plugin.cfg().getBoolean("permissions.enabled", true)) return;
        String cmd = plugin.cfg().getString("permissions.grant-command", "");
        if (cmd == null || cmd.isBlank()) return;
        cmd = cmd.replace("{player}", p.getName()).replace("{permission}", node);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    /** Redonne toutes les permissions correspondant à l'avancement actuel. */
    public void sync(Player p, PlayerData d) {
        QuestManager qm = plugin.quests();
        for (Category c : qm.categories()) {
            if (!qm.unlocked(d, c)) continue;
            grantCategory(p, c);
            int done = qm.completedIn(d, c);
            int upTo = qm.categoryDone(d, c) ? c.quests().size() : done + 1;
            for (int n = 1; n <= upTo && n <= c.quests().size(); n++) {
                grantQuest(p, c, n);
            }
        }
    }
}
