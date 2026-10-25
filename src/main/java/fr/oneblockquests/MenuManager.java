package fr.oneblockquests;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Menus entièrement configurables : menus/main.yml et menus/category.yml.
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

    private FileConfiguration cat() { return plugin.configs().categoryMenu(); }

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

    private void fill(Inventory inv, FileConfiguration f) {
        String filler = f.getString("filler", "");
        if (filler == null || filler.isBlank()) return;
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, ItemUtil.build(plugin, filler, Component.text(" "), new ArrayList<>()));
            }
        }
    }

    /** Slots des quêtes (menus/category.yml), sans doublon et dans les limites du menu. */
    private List<Integer> questSlots(int size) {
        Set<Integer> out = new LinkedHashSet<>();
        for (int s : cat().getIntegerList("quest-slots")) {
            if (s >= 0 && s < size) out.add(s);
        }
        return new ArrayList<>(out);
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

    public void openMain(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        QuestManager qm = plugin.quests();
        FileConfiguration f = main();

        int size = size(f, 36);
        QuestMenuHolder holder = new QuestMenuHolder(QuestMenuHolder.Kind.MAIN, null);
        Component title = Text.color(Text.papi(p, f.getString("title", "&8Quêtes")));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        for (Category c : qm.categories()) {
            if (c.slot() < 0 || c.slot() >= size) continue;
            inv.setItem(c.slot(), categoryItem(p, d, c));
        }

        int infoSlot = f.getInt("info.slot", 31);
        if (f.getBoolean("info.enabled", true) && infoSlot >= 0 && infoSlot < size) {
            Map<String, String> ph = qm.info(d);
            inv.setItem(infoSlot, ItemUtil.build(plugin, f.getString("info.icon", "COMPASS"),
                    line(p, f.getString("info.name", ""), ph),
                    lines(p, f.getStringList("info.lore"), ph)));
        }

        fill(inv, f);
        p.openInventory(inv);
    }

    private ItemStack categoryItem(Player p, PlayerData d, Category c) {
        QuestManager qm = plugin.quests();
        FileConfiguration f = main();
        Map<String, String> ph = new HashMap<>();
        ph.put("category", c.name());
        ph.put("description", c.description());
        ph.put("done", String.valueOf(qm.completedIn(d, c)));
        ph.put("total", String.valueOf(c.quests().size()));

        if (!qm.unlocked(d, c)) {
            return ItemUtil.build(plugin, f.getString("category.locked.icon", "BARRIER"),
                    line(p, f.getString("category.locked.name", ""), ph),
                    lines(p, f.getStringList("category.locked.lore"), ph));
        }
        ph.put("status", qm.categoryDone(d, c)
                ? f.getString("category.status-done", "")
                : f.getString("category.status-open", ""));
        return ItemUtil.build(plugin, c.icon(),
                line(p, f.getString("category.name", "{category}"), ph),
                lines(p, f.getStringList("category.lore"), ph));
    }

    // ------------------------------------------------------------------
    // Menu d'une catégorie
    // ------------------------------------------------------------------

    public void openCategory(Player p, Category c) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        QuestManager qm = plugin.quests();

        // Garde-fou : jamais d'ouverture d'une catégorie pas encore atteinte
        if (!qm.unlocked(d, c)) {
            deny(p);
            return;
        }

        FileConfiguration f = cat();
        int size = size(f, 54);
        QuestMenuHolder holder = new QuestMenuHolder(QuestMenuHolder.Kind.CATEGORY, c.id());
        Map<String, String> tph = new HashMap<>();
        tph.put("category", c.name());
        Component title = line(p, f.getString("title", "&8Quêtes &7» &r{category}"), tph);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        List<Integer> slots = questSlots(size);
        int max = Math.min(c.quests().size(), slots.size());
        for (int i = 0; i < max; i++) {
            inv.setItem(slots.get(i), questItem(p, d, c, i));
        }
        if (c.quests().size() > slots.size()) {
            plugin.getLogger().warning("La catégorie " + c.id() + " a plus de quêtes (" + c.quests().size()
                    + ") que de slots dans menus/category.yml (" + slots.size()
                    + ") : les suivantes ne sont pas affichées.");
        }

        int backSlot = f.getInt("back.slot", 49);
        if (backSlot >= 0 && backSlot < size) {
            inv.setItem(backSlot, ItemUtil.build(plugin, f.getString("back.icon", "ARROW"),
                    line(p, f.getString("back.name", "&cRetour"), new HashMap<>()), new ArrayList<>()));
        }

        fill(inv, f);
        p.openInventory(inv);
    }

    /** Seule la quête en cours montre ses détails. */
    private ItemStack questItem(Player p, PlayerData d, Category c, int i) {
        QuestManager qm = plugin.quests();
        FileConfiguration f = cat();
        Quest q = c.quests().get(i);
        int done = qm.completedIn(d, c);
        boolean isDone = i < done;
        boolean isCurrent = !d.finished && i == done && qm.unlocked(d, c) && !qm.categoryDone(d, c);

        if (isDone) {
            Map<String, String> ph = qm.questPlaceholders(c, q, q.amount());
            return ItemUtil.build(plugin, f.getString("quest.completed.icon", "LIME_DYE"),
                    line(p, f.getString("quest.completed.name", ""), ph),
                    lines(p, f.getStringList("quest.completed.lore"), ph));
        }
        if (isCurrent) {
            Map<String, String> ph = qm.questPlaceholders(c, q, d.progress);
            return ItemUtil.build(plugin, f.getString("quest.current.icon", "WRITABLE_BOOK"),
                    line(p, f.getString("quest.current.name", ""), ph),
                    lines(p, f.getStringList("quest.current.lore"), ph));
        }
        // Verrouillée : aucune information sur la quête (pas de nom, pas de description)
        Map<String, String> ph = new HashMap<>();
        ph.put("number", String.valueOf(q.number()));
        ph.put("category", c.name());
        return ItemUtil.build(plugin, f.getString("quest.locked.icon", "BARRIER"),
                line(p, f.getString("quest.locked.name", ""), ph),
                lines(p, f.getStringList("quest.locked.lore"), ph));
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

        if (holder.kind() == QuestMenuHolder.Kind.MAIN) {
            for (Category c : qm.categories()) {
                if (c.slot() != slot) continue;
                if (qm.unlocked(d, c)) {
                    plugin.sound(p, "click");
                    openCategory(p, c);
                } else {
                    deny(p);
                }
                return;
            }
            return;
        }

        // Menu de catégorie
        FileConfiguration f = cat();
        if (slot == f.getInt("back.slot", 49)) {
            plugin.sound(p, "click");
            openMain(p);
            return;
        }
        Category c = qm.category(holder.categoryId());
        if (c == null) return;
        // Revérification : la catégorie ne doit plus être ouverte si elle n'est pas accessible
        if (!qm.unlocked(d, c)) {
            p.closeInventory();
            deny(p);
            return;
        }
        int done = qm.completedIn(d, c);
        List<Integer> slots = questSlots(holder.getInventory().getSize());
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
    }
}
