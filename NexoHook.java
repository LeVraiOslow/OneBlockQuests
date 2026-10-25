package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Récupère un item Nexo par réflexion (aucune dépendance de compilation). */
final class NexoHook {
    private static boolean warned = false;

    private NexoHook() {
    }

    static ItemStack item(String id, OneBlockQuestsPlugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            warn(plugin, "Nexo n'est pas chargé : icône nexo:" + id + " remplacée.");
            return null;
        }
        try {
            Class<?> api = Class.forName("com.nexomc.nexo.api.NexoItems");
            Method from = api.getMethod("itemFromId", String.class);
            Object builder = from.invoke(null, id);
            if (builder == null) {
                warn(plugin, "Item Nexo introuvable : " + id);
                return null;
            }
            Method build = builder.getClass().getMethod("build");
            Object result = build.invoke(builder);
            return result instanceof ItemStack is ? is : null;
        } catch (Throwable t) {
            warn(plugin, "Impossible de lire l'item Nexo '" + id + "' : " + t);
            return null;
        }
    }

    private static void warn(OneBlockQuestsPlugin plugin, String msg) {
        if (!warned) {
            plugin.getLogger().warning(msg + " (ce message n'apparaît qu'une fois)");
            warned = true;
        }
    }
}
