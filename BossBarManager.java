package fr.oneblockquests;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BossBarManager {

    private final OneBlockQuestsPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public BossBarManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(Player p) {
        FileConfiguration cfg = plugin.cfg();
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (!cfg.getBoolean("bossbar.enabled", true) || !d.bossbar) {
            hide(p);
            return;
        }

        Quest q = plugin.quests().current(d);
        Map<String, String> info = plugin.quests().info(d);
        info.put("player", p.getName());

        String format = q == null
                ? cfg.getString("bossbar.finished-format", "&aTerminé")
                : cfg.getString("bossbar.format", "{category} #{number} - {name}");
        Component title = Text.color(Text.papi(p, Text.replace(format, info)));
        float progress = q == null ? 1f
                : Math.max(0f, Math.min(1f, d.progress / (float) q.amount()));

        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, color(cfg.getString("bossbar.color", "YELLOW")),
                    overlay(cfg.getString("bossbar.overlay", "PROGRESS")));
            bars.put(p.getUniqueId(), bar);
            p.showBossBar(bar);
        } else {
            bar.name(title);
            bar.progress(progress);
            bar.color(color(cfg.getString("bossbar.color", "YELLOW")));
            bar.overlay(overlay(cfg.getString("bossbar.overlay", "PROGRESS")));
        }
    }

    public void hide(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) p.hideBossBar(bar);
    }

    public void hideAll() {
        for (Player p : Bukkit.getOnlinePlayers()) hide(p);
        bars.clear();
    }

    private BossBar.Color color(String s) {
        try {
            return BossBar.Color.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return BossBar.Color.YELLOW;
        }
    }

    private BossBar.Overlay overlay(String s) {
        try {
            return BossBar.Overlay.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
