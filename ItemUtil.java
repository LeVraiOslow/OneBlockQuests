package fr.oneblockquests;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static ItemStack build(OneBlockQuestsPlugin plugin, String spec, Component name, List<Component> lore) {
        return build(plugin, spec, name, lore, 1, false);
    }

    /**
     * spec : MATERIAL, MATERIAL:customModelData ou nexo:id.
     * Plusieurs choix possibles séparés par | : le premier qui existe est utilisé
     * (ex : "nexo:quest|BOOK" = l'item Nexo, ou un livre si Nexo est absent).
     */
    public static ItemStack build(OneBlockQuestsPlugin plugin, String spec, Component name,
                                  List<Component> lore, int amount, boolean glow) {
        ItemStack item = null;
        if (spec == null) spec = "";
        for (String part : spec.split("\\|")) {
            item = resolve(plugin, part.trim());
            if (item != null) break;
        }
        if (item == null) item = new ItemStack(Material.PAPER);
        item.setAmount(Math.max(1, Math.min(64, amount)));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            if (glow) meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack resolve(OneBlockQuestsPlugin plugin, String spec) {
        if (spec.isEmpty()) return null;
        if (spec.toLowerCase().startsWith("nexo:")) {
            return NexoHook.item(spec.substring(5), plugin);
        }
        String matName = spec;
        Integer cmd = null;
        int idx = spec.lastIndexOf(':');
        if (idx > 0 && !spec.toLowerCase().startsWith("minecraft:")) {
            try {
                cmd = Integer.parseInt(spec.substring(idx + 1));
                matName = spec.substring(0, idx);
            } catch (NumberFormatException ignored) {
            }
        }
        Material m = Material.matchMaterial(matName);
        if (m == null || !m.isItem() || m.isAir()) return null;

        ItemStack item = new ItemStack(m);
        if (cmd != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();
                component.setFloats(List.of(cmd.floatValue()));
                meta.setCustomModelDataComponent(component);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
