package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestManager {

    private final OneBlockQuestsPlugin plugin;
    private final Map<String, Category> byId = new LinkedHashMap<>();
    private final List<Category> ordered = new ArrayList<>();

    public QuestManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Chargement
    // ------------------------------------------------------------------

    public void load() {
        byId.clear();
        ordered.clear();
        YamlConfiguration y = plugin.configs().quests();
        ConfigurationSection root = y.getConfigurationSection("categories");
        if (root == null) {
            plugin.getLogger().severe("config/quests.yml : section 'categories' manquante.");
            return;
        }
        int idx = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection cs = root.getConfigurationSection(id);
            if (cs == null) continue;

            List<Quest> quests = new ArrayList<>();
            ConfigurationSection qs = cs.getConfigurationSection("quests");
            if (qs != null) {
                List<String> keys = new ArrayList<>(qs.getKeys(false));
                keys.sort(Comparator.comparingInt(QuestManager::parseKey));
                for (String k : keys) {
                    ConfigurationSection s = qs.getConfigurationSection(k);
                    if (s == null) continue;
                    QuestType type;
                    try {
                        type = QuestType.valueOf(s.getString("type", "BREAK").toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("config/quests.yml : type invalide pour " + id + "/" + k + " (ignorée).");
                        continue;
                    }
                    List<String> targets = new ArrayList<>();
                    if (s.isList("target")) {
                        for (String t : s.getStringList("target")) targets.add(t.toUpperCase(Locale.ROOT));
                    } else if (s.isString("target")) {
                        targets.add(s.getString("target", "ANY").toUpperCase(Locale.ROOT));
                    }
                    String description = s.getString("description", "");
                    String objective = s.getString("objective", "");
                    if (objective == null || objective.isBlank()) {
                        objective = description.replaceAll("[\\s.]+$", "");
                    }
                    quests.add(new Quest(id, quests.size(),
                            s.getString("name", "Quête"),
                            description, objective,
                            type, targets,
                            Math.max(1, s.getInt("amount", 1)),
                            s.getDouble("reward", 0)));
                }
            }
            int index = idx++;
            Category c = new Category(id, index,
                    cs.getString("name", id),
                    cs.getString("description", ""),
                    cs.getInt("slot", 10 + index),
                    quests);
            byId.put(id, c);
            ordered.add(c);
        }
        plugin.configs().loadCategoryMenus(byId.keySet());
    }

    private static int parseKey(String k) {
        try {
            return Integer.parseInt(k);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    // ------------------------------------------------------------------
    // Accès
    // ------------------------------------------------------------------

    public List<Category> categories() { return ordered; }

    public Category category(String id) { return id == null ? null : byId.get(id); }

    public Category first() { return ordered.isEmpty() ? null : ordered.get(0); }

    public Category next(Category c) {
        int i = c.index() + 1;
        return i < ordered.size() ? ordered.get(i) : null;
    }

    public int totalQuests() {
        int n = 0;
        for (Category c : ordered) n += c.quests().size();
        return n;
    }

    /** Quête en cours, ou null si tout est terminé. */
    public Quest current(PlayerData d) {
        if (d.finished) return null;
        Category c = category(d.category);
        if (c == null || d.position < 0 || d.position >= c.quests().size()) return null;
        return c.quests().get(d.position);
    }

    public int completedIn(PlayerData d, Category c) {
        if (d.finished) return c.quests().size();
        Category cur = category(d.category);
        if (cur == null) return 0;
        if (c.index() < cur.index()) return c.quests().size();
        if (c.index() == cur.index()) return d.position;
        return 0;
    }

    public boolean unlocked(PlayerData d, Category c) {
        if (d.finished) return true;
        Category cur = category(d.category);
        return cur != null && c.index() <= cur.index();
    }

    public boolean categoryDone(PlayerData d, Category c) {
        return completedIn(d, c) >= c.quests().size();
    }

    public int totalCompleted(PlayerData d) {
        int n = 0;
        for (Category c : ordered) n += completedIn(d, c);
        return n;
    }

    // ------------------------------------------------------------------
    // Cohérence des données
    // ------------------------------------------------------------------

    public void normalize(PlayerData d) {
        if (ordered.isEmpty()) return;
        Category c = category(d.category);
        if (c == null) {
            d.category = ordered.get(0).id();
            d.position = 0;
            d.progress = 0;
            d.finished = false;
            d.dirty = true;
            return;
        }
        while (!d.finished && d.position >= c.quests().size()) {
            Category n = next(c);
            if (n == null) {
                d.finished = true;
            } else {
                d.category = n.id();
                d.position = 0;
                d.progress = 0;
                c = n;
            }
            d.dirty = true;
        }
        if (!d.finished) {
            Quest q = c.quests().get(d.position);
            if (d.progress >= q.amount()) {
                d.progress = Math.max(0, q.amount() - 1);
                d.dirty = true;
            }
        }
    }

    // ------------------------------------------------------------------
    // Progression
    // ------------------------------------------------------------------

    public void progress(Player p, QuestType type, String target, int amount) {
        if (amount <= 0) return;
        PlayerData d = plugin.data().get(p.getUniqueId());
        Quest q = current(d);
        if (q == null || q.type() != type || !q.matches(target)) return;
        d.progress = Math.min(q.amount(), d.progress + amount);
        d.dirty = true;
        if (d.progress >= q.amount()) {
            complete(p, d, q);
        } else {
            plugin.bossBars().update(p);
        }
    }

    /** Termine la quête en cours : récompense, permissions, passage à la suivante. */
    public void complete(Player p, PlayerData d, Quest q) {
        Category c = category(q.categoryId());
        if (c == null) return;

        plugin.rewards().give(p, q.reward());

        Map<String, String> ph = new HashMap<>();
        ph.put("player", p.getName());
        ph.put("quest", Text.plain(q.name()));
        ph.put("reward", Text.num(q.reward()));
        ph.put("category", c.name());
        ph.put("number", String.valueOf(q.number()));
        plugin.send(p, "quest-completed", ph);
        plugin.sound(p, "complete");

        d.progress = 0;
        d.position++;
        d.dirty = true;

        if (d.position >= c.quests().size()) {
            Category n = next(c);
            plugin.send(p, "category-completed", ph);
            if (n == null) {
                d.finished = true;
                plugin.send(p, "all-finished", ph);
            } else {
                d.category = n.id();
                d.position = 0;
                plugin.permissions().grantCategory(p, n);
                plugin.permissions().grantQuest(p, n, 1);
                Map<String, String> ph2 = new HashMap<>(ph);
                ph2.put("category", n.name());
                plugin.send(p, "category-unlocked", ph2);
            }
            plugin.sound(p, "category");
        } else {
            plugin.permissions().grantQuest(p, c, d.position + 1);
        }
        plugin.bossBars().update(p);
    }

    // ------------------------------------------------------------------
    // Administration
    // ------------------------------------------------------------------

    public void setPosition(Player p, String categoryId, int number) {
        Category c = category(categoryId);
        if (c == null) return;
        PlayerData d = plugin.data().get(p.getUniqueId());
        d.category = c.id();
        d.position = Math.max(0, Math.min(number - 1, c.quests().size() - 1));
        d.progress = 0;
        d.finished = false;
        d.dirty = true;
        plugin.permissions().sync(p, d);
        plugin.bossBars().update(p);
        // Un menu de catégorie ouvert pourrait n'être plus accessible : on le ferme
        plugin.menus().closeMenus(p);
    }

    public void reset(Player p) {
        Category f = first();
        if (f == null) return;
        setPosition(p, f.id(), 1);
    }

    // ------------------------------------------------------------------
    // Placeholders (menus, bossbar, PlaceholderAPI)
    // ------------------------------------------------------------------

    public Map<String, String> info(PlayerData d) {
        FileConfiguration cfg = plugin.cfg();
        Map<String, String> m = new HashMap<>();
        m.put("completed", String.valueOf(totalCompleted(d)));
        m.put("total", String.valueOf(totalQuests()));
        Quest q = current(d);
        Category c = category(d.category);
        if (q == null || c == null) {
            String fin = cfg.getString("placeholders.finished-name", "&aTerminé");
            m.put("finished", "true");
            m.put("category", fin);
            m.put("name", fin);
            m.put("number", "-");
            m.put("description", "");
            m.put("objective", "");
            m.put("progress", "0");
            m.put("required", "0");
            m.put("percent", "100");
            m.put("reward", "0");
            m.put("display", fin);
            m.put("category_completed", c == null ? "0" : String.valueOf(c.quests().size()));
            m.put("category_total", c == null ? "0" : String.valueOf(c.quests().size()));
            return m;
        }
        m.put("finished", "false");
        m.put("category", c.name());
        m.put("number", String.valueOf(q.number()));
        m.put("name", q.name());
        m.put("description", q.description());
        m.put("objective", q.objective());
        m.put("progress", String.valueOf(d.progress));
        m.put("required", String.valueOf(q.amount()));
        m.put("percent", String.valueOf(d.progress * 100 / Math.max(1, q.amount())));
        m.put("reward", Text.num(q.reward()));
        m.put("category_completed", String.valueOf(d.position));
        m.put("category_total", String.valueOf(c.quests().size()));
        m.put("display", Text.replace(
                cfg.getString("placeholders.display-format", "{category} #{number} - {name}"), m));
        return m;
    }

    public Map<String, String> questPlaceholders(Category c, Quest q, int progress) {
        Map<String, String> m = new HashMap<>();
        m.put("category", c.name());
        m.put("number", String.valueOf(q.number()));
        m.put("name", q.name());
        m.put("description", q.description());
        m.put("objective", q.objective());
        m.put("progress", String.valueOf(progress));
        m.put("required", String.valueOf(q.amount()));
        m.put("percent", String.valueOf(progress * 100 / Math.max(1, q.amount())));
        m.put("reward", Text.num(q.reward()));
        return m;
    }

    public void logWarn(String s) {
        Bukkit.getLogger().warning("[OneBlockQuests] " + s);
    }
}
