package fr.oneblockquests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Text {

    public static boolean papiAvailable = false;

    private static final Pattern BARE_HEX = Pattern.compile("(?<![&\\w#])#([A-Fa-f0-9]{6})");
    private static final Pattern STRIP = Pattern.compile(
            "(?i)(&#[0-9a-f]{6}|[&§]x([&§][0-9a-f]){6}|[&§][0-9a-fk-orx])");
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    /** Convertit &a, &#RRGGBB et #RRGGBB en Component (italique désactivé par défaut). */
    public static Component color(String s) {
        if (s == null) s = "";
        s = s.replace('§', '&');
        s = BARE_HEX.matcher(s).replaceAll("&#$1");
        return AMP.deserialize(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Texte sans aucun code couleur. */
    public static String plain(String s) {
        if (s == null) return "";
        s = BARE_HEX.matcher(s).replaceAll("");
        return STRIP.matcher(s).replaceAll("");
    }

    /** Texte avec codes couleur en §. */
    public static String section(String s) {
        return SECTION.serialize(color(s));
    }

    public static String replace(String s, Map<String, String> ph) {
        if (s == null) return "";
        if (ph == null) return s;
        for (Map.Entry<String, String> e : ph.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return s;
    }

    public static String papi(Player p, String s) {
        if (!papiAvailable || p == null || s == null) return s;
        try {
            return PapiHook.set(p, s);
        } catch (Throwable t) {
            return s;
        }
    }

    public static String num(double v) {
        if (v == Math.rint(v)) return String.format(Locale.US, "%d", (long) v);
        return String.format(Locale.US, "%.2f", v);
    }
}
