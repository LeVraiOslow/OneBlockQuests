package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class OneBlockQuestsPlugin extends JavaPlugin {

    private ConfigManager configs;
    private QuestManager quests;
    private DataManager data;
    private BossBarManager bossBars;
    private RewardManager rewards;
    private PermissionManager permissions;
    private MenuManager menus;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        configs = new ConfigManager(this);
        configs.load();
        Text.papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        quests = new QuestManager(this);
        quests.load();
        data = new DataManager(this);
        bossBars = new BossBarManager(this);
        rewards = new RewardManager(this);
        permissions = new PermissionManager(this);
        menus = new MenuManager(this);
        playerListener = new PlayerListener(this);

        Bukkit.getPluginManager().registerEvents(new QuestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MovementListener(this), this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        QuestCommand command = new QuestCommand(this);
        PluginCommand pc = getCommand("quetes");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        if (Text.papiAvailable) {
            new QuestExpansion(this).register();
            getLogger().info("PlaceholderAPI détecté : %oneblockquests_...% disponible.");
        }

        // Sauvegarde automatique toutes les 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> data.saveAll(), 6000L, 6000L);

        // Joueurs déjà connectés (cas du /reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            playerListener.setup(p);
        }
        getLogger().info(quests.categories().size() + " catégories chargées.");
    }

    @Override
    public void onDisable() {
        if (data != null) data.saveAll();
        if (bossBars != null) bossBars.hideAll();
    }

    public void reloadAll() {
        configs.load();
        quests.load();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = data.get(p.getUniqueId());
            quests.normalize(d);
            bossBars.update(p);
        }
    }

    public ConfigManager configs() { return configs; }
    public QuestManager quests() { return quests; }
    public DataManager data() { return data; }
    public BossBarManager bossBars() { return bossBars; }
    public RewardManager rewards() { return rewards; }
    public PermissionManager permissions() { return permissions; }
    public MenuManager menus() { return menus; }

    /** Réglages généraux (config/config.yml). */
    public FileConfiguration cfg() { return configs.config(); }

    // ---------- Messages & sons ----------

    public String prefix() {
        return configs.messages().getString("prefix", "");
    }

    public String msg(String key) {
        return configs.messages().getString(key, "&cMessage manquant : " + key);
    }

    public List<String> msgList(String key) {
        return configs.messages().getStringList(key);
    }

    public void send(Player p, String key, Map<String, String> ph) {
        String line = Text.replace(msg(key), ph);
        p.sendMessage(Text.color(Text.papi(p, prefix() + line)));
    }

    public void sendRaw(Player p, String line, Map<String, String> ph) {
        p.sendMessage(Text.color(Text.papi(p, Text.replace(line, ph))));
    }

    public void sound(Player p, String key) {
        String s = cfg().getString("sounds." + key, "");
        if (s != null && !s.isBlank()) {
            p.playSound(p.getLocation(), s, 1f, 1f);
        }
    }
}
