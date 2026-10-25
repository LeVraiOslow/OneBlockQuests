package fr.oneblockquests;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Menus entièrement configurables (menus/main.yml, menus/categories/&lt;id&gt;.yml) :
 * taille, titre, décor, slots, icônes, noms, lores, objets libres avec actions.
 *
 * Règle de progression : un joueur ne peut ouvrir que la catégorie où il se trouve
 * et celles qu'il a déjà terminées. Les catégories suivantes sont refusées à
 * l'ouverture ET revérifiées à chaque clic.
 */
public final class MenuManager {

    private final OneBlockQuestsPlugin plugin;

    public MenuManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Helpers de configuration
    // ------------------------------------------------------------------

    private FileConfiguration main() { return plugin.configs().mainMenu(); }

    private FileConfiguration catMenu(String categoryId) { return plugin.configs().categoryMenu(categoryId); }

    private static int size(FileConfiguration f, int def) {
        return Math.max(9, Math.min(54, (f.getInt("size", def) / 9) * 9));
    }

    private Component line(Player p, String raw, Map<String, String> ph) {
        return Text.color(Text.papi(p, Text.replace(raw, ph)));
    }

    private List<Component> lines(Player p, List<String> raw, Map<String, String> ph) {
        List<Component> out = new ArrayList<>();
        for (String s : raw) out.add(line(p, s, ph));
        return out;
    }

    /** Objet défini par base+"icon", "name", "lore", "amount", "glow". */
    private ItemStack buildItem(Player p, FileConfiguration f, String base, String defIcon, Map<String, String> ph) {
        return buildItem(p, f.getString(base + "icon", defIcon), f.getString(base + "name", ""),
                f.getStringList(base + "lore"), f.getInt(base + "amount", 1),
                f.getBoolean(base + "glow", false), ph);
    }

    private ItemStack buildItem(Player p, String icon, String name, List<String> lore, int amount,
                                boolean glow, Map<String, String> ph) {
        return ItemUtil.build(plugin, icon, line(p, name, ph), lines(p, lore, ph), amount, glow);
    }

    private void fill(Inventory inv, FileConfiguration f) {
        String filler = f.getString("filler", "");
        if (filler == null || filler.isBlank()) return;
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, ItemUtil.build(plugin, filler, Component.text(" "), new ArrayList<>()));
            }
        }
    }

    // ----- Objets libres (section "items" des fichiers de menu) -----

    private Set<String> itemKeys(FileConfiguration f) {
        ConfigurationSection s = f.getConfigurationSection("items");
        return s == null ? Set.of() : s.getKeys(false);
    }

    /** "slot: 5" et/ou "slots: [0, 1, 2]" */
    private List<Integer> slotsOf(FileConfiguration f, String base, int size) {
        Set<Integer> out = new LinkedHashSet<>();
        if (f.contains(base + "slot")) out.add(f.getInt(base + "slot"));
        out.addAll(f.getIntegerList(base + "slots"));
        out.removeIf(s -> s < 0 || s >= size);
        return new ArrayList<>(out);
    }

    private void placeItems(Player p, Inventory inv, FileConfiguration f, Map<String, String> ph) {
        for (String key : itemKeys(f)) {
            String base = "items." + key + ".";
            if (!f.getBoolean(base + "enabled", true)) continue;
            ItemStack item = buildItem(p, f, base, "PAPER", ph);
            for (int slot : slotsOf(f, base, inv.getSize())) {
                inv.setItem(slot, item.clone());
            }
        }
    }

    /** Exécute l'action de l'objet cliqué (NONE, MAIN, CLOSE, COMMAND). Renvoie true si un objet a été trouvé. */
    private boolean clickItems(Player p, FileConfiguration f, int slot, int size) {
        for (String key : itemKeys(f)) {
            String base = "items." + key + ".";
            if (!f.getBoolean(base + "enabled", true)) continue;
            if (!slotsOf(f, base, size).contains(slot)) continue;
            String action = f.getString(base + "action", "NONE").toUpperCase(Locale.ROOT);
            switch (action) {
                case "MAIN" -> {
                    plugin.sound(p, "click");
                    openMain(p);
                }
                case "CLOSE" -> {
                    plugin.sound(p, "click");
                    p.closeInventory();
                }
                case "COMMAND" -> {
                    plugin.sound(p, "click");
                    String cmd = f.getString(base + "command", "");
                    p.closeInventory();
                    if (cmd != null && !cmd.isBlank()) {
                        Bukkit.dispatchCommand(p, cmd.replace("{player}", p.getName()));
                    }
                }
                default -> {
                }
            }
            return true;
        }
        return false;
    }

    /** Ferme le menu des quêtes d'un joueur s'il en a un d'ouvert. */
    public void closeMenus(Player p) {
        if (p.getOpenInventory().getTopInventory().getHolder() instanceof QuestMenuHolder) {
            p.closeInventory();
        }
    }

    // ------------------------------------------------------------------
    // Menu principal
    // ------------------------------------------------------------------

    private int categorySlot(Category c) {
        return main().getInt("categories." + c.id() + ".slot", c.slot());
    }

    /** Texte d'une catégorie : sa propre valeur dans "categories.<id>", sinon la valeur commune "category". */
    private String catStr(Category c, String key, String def) {
        String own = "categories." + c.id() + "." + key;
        if (main().contains(own, true)) return main().getString(own, def);
        return main().getString("category." + key, def);
    }

    private List<String> catList(Category c, String key) {
        String own = "categories." + c.id() + "." + key;
        if (main().contains(own, true)) return main().getStringList(own);
        return main().getStringList("category." + key);
    }

    public void openMain(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        QuestManager qm = plugin.quests();
        FileConfiguration f = main();

        int size = size(f, 36);
        QuestMenuHolder holder = new QuestMenuHolder(QuestMenuHolder.Kind.MAIN, null);
        Component title = Text.color(Text.papi(p, f.getString("title", "&8Quêtes")));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        placeItems(p, inv, f, qm.info(d));

        for (Category c : qm.categories()) {
            int slot = categorySlot(c);
            if (slot < 0 || slot >= size) continue;
            inv.setItem(slot, categoryItem(p, d, c));
        }

        fill(inv, f);
        p.openInventory(inv);
    }

    private ItemStack categoryItem(Player p, PlayerData d, Category c) {
        QuestManager qm = plugin.quests();
        Map<String, String> ph = new HashMap<>();
        ph.put("category", c.name());
        ph.put("description", c.description());
        ph.put("done", String.valueOf(qm.completedIn(d, c)));
        ph.put("total", String.valueOf(c.quests().size()));

        if (!qm.unlocked(d, c)) {
            return buildItem(p, catStr(c, "locked.icon", "BARRIER"), catStr(c, "locked.name", ""),
                    catList(c, "locked.lore"), 1, false, ph);
        }
        ph.put("status", qm.categoryDone(d, c) ? catStr(c, "status-done", "") : catStr(c, "status-open", ""));
        return buildItem(p, catStr(c, "icon", "PAPER"), catStr(c, "name", "{category}"),
                catList(c, "lore"), 1, false, ph);
    }

    // ------------------------------------------------------------------
    // Menu d'une catégorie
    // ------------------------------------------------------------------

    /** Slots des quêtes (quest-slots), sans doublon et dans les limites du menu. */
    private List<Integer> questSlots(FileConfiguration f, int size) {
        Set<Integer> out = new LinkedHashSet<>();
        for (int s : f.getIntegerList("quest-slots")) {
            if (s >= 0 && s < size) out.add(s);
        }
        return new ArrayList<>(out);
    }

    private Map<String, String> categoryPlaceholders(PlayerData d, Category c) {
        QuestManager qm = plugin.quests();
        Map<String, String> ph = new HashMap<>(qm.info(d));
        ph.put("category", c.name());
        ph.put("description", c.description());
        ph.put("done", String.valueOf(qm.completedIn(d, c)));
        ph.put("total", String.valueOf(c.quests().size()));
        return ph;
    }

    public void openCategory(Player p, Category c) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        QuestManager qm = plugin.quests();

        // Garde-fou : jamais d'ouverture d'une catégorie pas encore atteinte
        if (!qm.unlocked(d, c)) {
            deny(p);
            return;
        }

        FileConfiguration f = catMenu(c.id());
        int size = size(f, 54);
        QuestMenuHolder holder = new QuestMenuHolder(QuestMenuHolder.Kind.CATEGORY, c.id());
        Map<String, String> tph = new HashMap<>();
        tph.put("category", c.name());
        Component title = line(p, f.getString("title", "&8Quêtes &7» &r{category}"), tph);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        placeItems(p, inv, f, categoryPlaceholders(d, c));

        List<Integer> slots = questSlots(f, size);
        int max = Math.min(c.quests().size(), slots.size());
        for (int i = 0; i < max; i++) {
            inv.setItem(slots.get(i), questItem(p, f, d, c, i));
        }
        if (c.quests().size() > slots.size()) {
            plugin.getLogger().warning("La catégorie " + c.id() + " a plus de quêtes (" + c.quests().size()
                    + ") que de slots dans menus/categories/" + c.id() + ".yml (" + slots.size()
                    + ") : les suivantes ne sont pas affichées.");
        }

        fill(inv, f);
        p.openInventory(inv);
    }

    /** Seule la quête en cours montre ses détails. */
    private ItemStack questItem(Player p, FileConfiguration f, PlayerData d, Category c, int i) {
        QuestManager qm = plugin.quests();
        Quest q = c.quests().get(i);
        int done = qm.completedIn(d, c);
        boolean isDone = i < done;
        boolean isCurrent = !d.finished && i == done && qm.unlocked(d, c) && !qm.categoryDone(d, c);

        if (isDone) {
            return buildItem(p, f, "quest.completed.", "LIME_DYE", qm.questPlaceholders(c, q, q.amount()));
        }
        if (isCurrent) {
            return buildItem(p, f, "quest.current.", "WRITABLE_BOOK", qm.questPlaceholders(c, q, d.progress));
        }
        // Verrouillée : aucune information sur la quête (pas de nom, pas de description)
        Map<String, String> ph = new HashMap<>();
        ph.put("number", String.valueOf(q.number()));
        ph.put("category", c.name());
        return buildItem(p, f, "quest.locked.", "BARRIER", ph);
    }

    private void deny(Player p) {
        plugin.sound(p, "locked");
        plugin.send(p, "locked-category", new HashMap<>());
    }

    // ------------------------------------------------------------------
    // Clics
    // ------------------------------------------------------------------

    public void handleClick(Player p, QuestMenuHolder holder, int slot) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        QuestManager qm = plugin.quests();
        int menuSize = holder.getInventory().getSize();

        if (holder.kind() == QuestMenuHolder.Kind.MAIN) {
            for (Category c : qm.categories()) {
                if (categorySlot(c) != slot) continue;
                if (qm.unlocked(d, c)) {
                    plugin.sound(p, "click");
                    openCategory(p, c);
                } else {
                    deny(p);
                }
                return;
            }
            clickItems(p, main(), slot, menuSize);
            return;
        }

        // Menu de catégorie
        Category c = qm.category(holder.categoryId());
        if (c == null) return;
        // Revérification : la catégorie ne doit plus être ouverte si elle n'est pas accessible
        if (!qm.unlocked(d, c)) {
            p.closeInventory();
            deny(p);
            return;
        }
        FileConfiguration f = catMenu(c.id());
        int done = qm.completedIn(d, c);
        List<Integer> slots = questSlots(f, menuSize);
        for (int i = 0; i < slots.size() && i < c.quests().size(); i++) {
            if (slots.get(i) != slot) continue;
            if (i == done && !d.finished && !qm.categoryDone(d, c)) {
                plugin.sound(p, "click");
                Map<String, String> ph = qm.questPlaceholders(c, c.quests().get(i), d.progress);
                for (String l : plugin.msgList("quest-details")) {
                    plugin.sendRaw(p, l, ph);
                }
            } else if (i > done) {
                plugin.sound(p, "locked");
                plugin.send(p, "locked-quest", new HashMap<>());
            }
            return;
        }
        clickItems(p, f, slot, menuSize);
    }
}
