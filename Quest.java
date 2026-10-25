package fr.oneblockquests;

import java.util.List;
import java.util.Locale;

/**
 * @param objective texte court de ce qu'il faut faire (bossbar) : champ "objective"
 *                  de quests.yml, ou la description sans le point final.
 */
public record Quest(String categoryId, int position, String name, String description, String objective,
                    QuestType type, List<String> targets, int amount, double reward) {

    public int number() {
        return position + 1;
    }

    /** Vérifie si la cible (nom de Material / EntityType) correspond à la quête. */
    public boolean matches(String actual) {
        if (targets.isEmpty()) return true;
        String a = actual == null ? "" : actual.toUpperCase(Locale.ROOT);
        for (String t : targets) {
            if (t.equals("ANY") || t.equals("*")) return true;
            if (t.endsWith("*") && a.startsWith(t.substring(0, t.length() - 1))) return true;
            if (t.startsWith("*") && a.endsWith(t.substring(1))) return true;
            if (t.equals(a)) return true;
        }
        return false;
    }
}
