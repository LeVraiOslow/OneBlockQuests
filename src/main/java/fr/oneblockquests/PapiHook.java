package fr.oneblockquests;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Isolé dans sa propre classe pour ne se charger que si PlaceholderAPI est présent. */
final class PapiHook {
    private PapiHook() {
    }

    static String set(Player p, String s) {
        return PlaceholderAPI.setPlaceholders(p, s);
    }
}
