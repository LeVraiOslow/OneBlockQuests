package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Locale;

public final class RewardManager {

    private final OneBlockQuestsPlugin plugin;

    public RewardManager(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void give(Player p, double amount) {
        if (amount <= 0) return;
        String mode = plugin.cfg().getString("rewards.mode", "VAULT").toUpperCase(Locale.ROOT);
        if (mode.equals("VAULT") && depositVault(p, amount)) return;

        String cmd = plugin.cfg().getString("rewards.command", "");
        if (cmd != null && !cmd.isBlank()) {
            cmd = cmd.replace("{player}", p.getName()).replace("{amount}", Text.num(amount));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            plugin.getLogger().warning("Aucune méthode de récompense disponible (Vault absent et rewards.command vide).");
        }
    }

    /** Dépôt via Vault, par réflexion (pas de dépendance de compilation). */
    private boolean depositVault(Player p, double amount) {
        try {
            Class<?> eco = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(eco);
            if (rsp == null) return false;
            Object provider = rsp.getProvider();
            Method m = eco.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            m.invoke(provider, p, amount);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            plugin.getLogger().warning("Vault : échec du dépôt (" + t + ")");
            return false;
        }
    }
}
