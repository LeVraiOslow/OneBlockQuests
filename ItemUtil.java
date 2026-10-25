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

    /** spec : MATERIAL, MATERIAL:customModelData ou nexo:id */
    public static ItemStack build(OneBlockQuestsPlugin plugin, String spec, Component name, List<Component> lore) {
        ItemStack item = null;
        if (spec == null) spec = "";
        spec = spec.trim();
        Integer cmd = null;

        if (spec.toLowerCase().startsWith("nexo:")) {
            item = NexoHook.item(spec.substring(5), plugin);
        } else {
            String matName = spec;
            int idx = spec.lastIndexOf(':');
            if (idx > 0 && !spec.toLowerCase().startsWith("minecraft:")) {
                try {
                    cmd = Integer.parseInt(spec.substring(idx + 1));
                    matName = spec.substring(0, idx);
                } catch (NumberFormatException ignored) {
                }
            }
            Material m = Material.matchMaterial(matName);
            if (m != null && m.isItem() && !m.isAir()) item = new ItemStack(m);
        }
        if (item == null) item = new ItemStack(Material.PAPER);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            if (cmd != null) {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();
                component.setFloats(List.of(cmd.floatValue()));
                meta.setCustomModelDataComponent(component);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
