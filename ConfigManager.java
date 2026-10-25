package fr.oneblockquests;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge tous les fichiers de configuration :
 *   config/config.yml, config/quests.yml, messages/messages.yml,
 *   menus/main.yml, menus/category.yml (modèle) et menus/categories/&lt;id&gt;.yml (un par catégorie).
 * Les clés manquantes sont complétées par les valeurs par défaut du jar.
 */
public final class ConfigManager {

    public static final String CONFIG = "config/config.yml";
    public static final String QUESTS = "config/quests.yml";
    public static final String MESSAGES = "messages/messages.yml";
    public static final String MENU_MAIN = "menus/main.yml";
    public static final String MENU_CATEGORY = "menus/category.yml";
    public static final String MENU_CATEGORIES_DIR = "menus/categories";

    /** Ancien format de la bossbar (1.1.0) : remplacé par {objective} s'il n'a pas été personnalisé. */
    private static final String OLD_BOSSBAR_FORMAT =
            "format: \"{category} &8#&f{number} &7- &f{name} &8[&a{progress}&7/&a{required}&8]\"";
    private static final String NEW_BOSSBAR_FORMAT =
            "format: \"{category} &8#&f{number} &7- &f{objective} &8[&a{progress}&7/&a{required}&8]\"";

    private final OneBlockQuestsPlugin plugin;
    private YamlConfiguration config = new YamlConfiguration();
    private YamlConfiguration quests = new YamlConfiguration();
    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration mainMenu = new YamlConfiguration();
    private YamlConfiguration categoryMenu = new YamlConfiguration();
    private final Map<String, YamlConfiguration> categoryMenus = new HashMap<>();

    public ConfigManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        migrateLegacy();
        migrateBossbarFormat();
        config = read(CONFIG, true);
        quests = read(QUESTS, false);
        messages = read(MESSAGES, true);
        mainMenu = read(upgraded(MENU_MAIN), true);
        categoryMenu = read(upgraded(MENU_CATEGORY), true);
    }

    public YamlConfiguration config() { return config; }

    public YamlConfiguration quests() { return quests; }

    public YamlConfiguration messages() { return messages; }

    public YamlConfiguration mainMenu() { return mainMenu; }

    /** Modèle des menus de catégorie. */
    public YamlConfiguration categoryMenu() { return categoryMenu; }

    /** Menu d'une catégorie précise (menus/categories/&lt;id&gt;.yml), sinon le modèle. */
    public YamlConfiguration categoryMenu(String id) {
        return categoryMenus.getOrDefault(id, categoryMenu);
    }

    /**
     * Crée (une seule fois) menus/categories/&lt;id&gt;.yml pour chaque catégorie, en copiant le modèle,
     * puis les charge. Chaque fichier peut ensuite être modifié librement.
     */
    public void loadCategoryMenus(Collection<String> ids) {
        categoryMenus.clear();
        File template = new File(plugin.getDataFolder(), MENU_CATEGORY);
        File dir = new File(plugin.getDataFolder(), MENU_CATEGORIES_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Impossible de créer le dossier " + MENU_CATEGORIES_DIR + "/");
            return;
        }
        for (String id : ids) {
            if (!id.matches("[A-Za-z0-9_\\-]+")) {
                plugin.getLogger().warning("Identifiant de catégorie '" + id
                        + "' : pas de fichier de menu dédié (caractères autorisés : lettres, chiffres, _ et -).");
                continue;
            }
            File file = new File(dir, id + ".yml");
            if (!file.exists() && template.exists()) {
                try {
                    Files.copy(template.toPath(), file.toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("Création de " + file.getName() + " impossible : " + e.getMessage());
                    continue;
                }
            }
            if (!file.exists()) continue;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            y.setDefaults(categoryMenu);
            categoryMenus.put(id, y);
        }
    }

    // ------------------------------------------------------------------

    private YamlConfiguration jarDefaults(String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in != null) {
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return YamlConfiguration.loadConfiguration(r);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Valeurs par défaut illisibles pour " + path + " : " + e.getMessage());
        }
        return null;
    }

    private YamlConfiguration read(String path, boolean withDefaults) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Impossible de créer le dossier " + parent.getName() + "/");
            }
            plugin.saveResource(path, false);
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        if (withDefaults) {
            YamlConfiguration defaults = jarDefaults(path);
            if (defaults != null) y.setDefaults(defaults);
        }
        return y;
    }

    /**
     * Si le fichier de menu présent sur le serveur est plus ancien que celui du plugin
     * (clé "version"), il est sauvegardé en .old et remplacé par le nouveau modèle.
     */
    private String upgraded(String path) {
        File file = new File(plugin.getDataFolder(), path);
        YamlConfiguration defaults = jarDefaults(path);
        if (file.exists() && defaults != null) {
            int have = YamlConfiguration.loadConfiguration(file).getInt("version", 1);
            int want = defaults.getInt("version", 1);
            if (have < want) {
                File backup = new File(plugin.getDataFolder(), path + ".old");
                try {
                    Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info(path + " mis à jour (ancienne version sauvegardée en "
                            + backup.getName() + ").");
                } catch (IOException e) {
                    plugin.getLogger().warning("Mise à jour de " + path + " impossible : " + e.getMessage());
                }
            }
        }
        return path;
    }

    /** Remplace l'ancien format de bossbar par défaut (texte simple, le reste du fichier est conservé). */
    private void migrateBossbarFormat() {
        File file = new File(plugin.getDataFolder(), CONFIG);
        if (!file.exists()) return;
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (text.contains(OLD_BOSSBAR_FORMAT)) {
                Files.writeString(file.toPath(), text.replace(OLD_BOSSBAR_FORMAT, NEW_BOSSBAR_FORMAT),
                        StandardCharsets.UTF_8);
                plugin.getLogger().info("bossbar.format : {name} remplacé par {objective}.");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Migration du format de bossbar impossible : " + e.getMessage());
        }
    }

    /**
     * Ancienne structure (1.0.x) : config.yml et quests.yml à la racine du dossier.
     * quests.yml est déplacé tel quel ; l'ancien config.yml est sauvegardé en config.yml.old
     * (les réglages sont maintenant répartis entre config/, messages/ et menus/).
     */
    private void migrateLegacy() {
        File dir = plugin.getDataFolder();
        File oldQuests = new File(dir, "quests.yml");
        File newQuests = new File(dir, QUESTS);
        if (oldQuests.exists() && !newQuests.exists()) {
            try {
                Files.createDirectories(newQuests.getParentFile().toPath());
                Files.move(oldQuests.toPath(), newQuests.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("quests.yml déplacé vers " + QUESTS);
            } catch (IOException e) {
                plugin.getLogger().warning("Migration de quests.yml impossible : " + e.getMessage());
            }
        }
        File oldConfig = new File(dir, "config.yml");
        File backup = new File(dir, "config.yml.old");
        if (oldConfig.exists() && !backup.exists()) {
            try {
                Files.move(oldConfig.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Ancien config.yml sauvegardé en config.yml.old : "
                        + "reporte tes réglages dans config/, messages/ et menus/.");
            } catch (IOException e) {
                plugin.getLogger().warning("Sauvegarde de l'ancien config.yml impossible : " + e.getMessage());
            }
        }
    }
}
