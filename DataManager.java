package fr.oneblockquests;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DataManager {

    private final OneBlockQuestsPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Impossible de créer le dossier data/");
        }
    }

    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, this::load);
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    private PlayerData load(UUID id) {
        PlayerData d = new PlayerData();
        File f = file(id);
        Category first = plugin.quests().first();
        String firstId = first == null ? "" : first.id();
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            d.category = y.getString("category", firstId);
            d.position = y.getInt("position", 0);
            d.progress = y.getInt("progress", 0);
            d.finished = y.getBoolean("finished", false);
            d.bossbar = y.getBoolean("bossbar", true);
        } else {
            d.category = firstId;
            d.isNew = true;
            d.dirty = true;
        }
        plugin.quests().normalize(d);
        return d;
    }

    public void save(UUID id) {
        PlayerData d = cache.get(id);
        if (d == null || !d.dirty) return;
        YamlConfiguration y = new YamlConfiguration();
        y.set("category", d.category);
        y.set("position", d.position);
        y.set("progress", d.progress);
        y.set("finished", d.finished);
        y.set("bossbar", d.bossbar);
        try {
            y.save(file(id));
            d.dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Sauvegarde impossible pour " + id + " : " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID id : cache.keySet()) save(id);
    }

    public void unload(UUID id) {
        save(id);
        cache.remove(id);
    }
}
