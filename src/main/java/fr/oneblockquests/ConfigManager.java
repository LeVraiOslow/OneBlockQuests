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

/**
 * Charge tous les fichiers de configuration :
 *   config/config.yml, config/quests.yml, messages/messages.yml,
 *   menus/main.yml, menus/category.yml
 * Les clés manquantes sont complétées par les valeurs par défaut du jar.
 */
public final class ConfigManager {

    public static final String CONFIG = "config/config.yml";
    public static final String QUESTS = "config/quests.yml";
    public static final String MESSAGES = "messages/messages.yml";
    public static final String MENU_MAIN = "menus/main.yml";
    public static final String MENU_CATEGORY = "menus/category.yml";

    private final OneBlockQuestsPlugin plugin;
    private YamlConfiguration config = new YamlConfiguration();
    private YamlConfiguration quests = new YamlConfiguration();
    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration mainMenu = new YamlConfiguration();
    private YamlConfiguration categoryMenu = new YamlConfiguration();

    public ConfigManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        migrateLegacy();
        config = read(CONFIG, true);
        quests = read(QUESTS, false);
        messages = read(MESSAGES, true);
        mainMenu = read(MENU_MAIN, true);
        categoryMenu = read(MENU_CATEGORY, true);
    }

    public YamlConfiguration config() { return config; }

    public YamlConfiguration quests() { return quests; }

    public YamlConfiguration messages() { return messages; }

    public YamlConfiguration mainMenu() { return mainMenu; }

    public YamlConfiguration categoryMenu() { return categoryMenu; }

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
            try (InputStream in = plugin.getResource(path)) {
                if (in != null) {
                    try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        y.setDefaults(YamlConfiguration.loadConfiguration(r));
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Valeurs par défaut illisibles pour " + path + " : " + e.getMessage());
            }
        }
        return y;
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
