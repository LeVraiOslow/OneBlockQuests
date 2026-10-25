package fr.oneblockquests;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Placeholders : %oneblockquests_<clé>% (texte sans couleur)
 *                %oneblockquests_<clé>_colored% (texte avec couleurs)
 * Clés : category, number, name, description, progress, required, percent,
 *        reward, completed, total, display, finished, category_completed, category_total
 */
public final class QuestExpansion extends PlaceholderExpansion {

    private final OneBlockQuestsPlugin plugin;

    public QuestExpansion(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "oneblockquests"; }

    @Override
    public @NotNull String getAuthor() { return "OneBlockQuests"; }

    @Override
    public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "";
        boolean colored = params.endsWith("_colored");
        String key = colored ? params.substring(0, params.length() - "_colored".length()) : params;

        PlayerData d = plugin.data().get(player.getUniqueId());
        Map<String, String> info = plugin.quests().info(d);
        String value = info.get(key);
        if (value == null) return null;
        return colored ? Text.section(value) : Text.plain(value);
    }
}
