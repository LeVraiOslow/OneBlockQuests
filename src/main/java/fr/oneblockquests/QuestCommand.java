package fr.oneblockquests;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestCommand implements CommandExecutor, TabCompleter {

    private final OneBlockQuestsPlugin plugin;

    public QuestCommand(OneBlockQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Text.plain(plugin.msg("players-only")));
                return true;
            }
            if (!p.hasPermission("oneblockquests.use")) {
                plugin.send(p, "no-permission", new HashMap<>());
                return true;
            }
            plugin.menus().openMain(p);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "bossbar" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(Text.plain(plugin.msg("players-only")));
                    return true;
                }
                if (!bossbarAllowed()) {
                    tell(p, "bossbar-disabled", new HashMap<>());
                    return true;
                }
                PlayerData d = plugin.data().get(p.getUniqueId());
                d.bossbar = !d.bossbar;
                d.dirty = true;
                plugin.bossBars().update(p);
                plugin.send(p, d.bossbar ? "bossbar-on" : "bossbar-off", new HashMap<>());
            }
            case "reload" -> {
                if (!sender.hasPermission("oneblockquests.admin")) {
                    deny(sender);
                    return true;
                }
                plugin.reloadAll();
                tell(sender, "reloaded", new HashMap<>());
            }
            case "admin" -> admin(sender, args);
            default -> {
                if (sender instanceof Player p && p.hasPermission("oneblockquests.use")) {
                    plugin.menus().openMain(p);
                }
            }
        }
        return true;
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oneblockquests.admin")) {
            deny(sender);
            return;
        }
        if (args.length < 3) {
            usage(sender);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            tell(sender, "player-not-found", new HashMap<>());
            return;
        }
        PlayerData d = plugin.data().get(target.getUniqueId());
        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reset" -> plugin.quests().reset(target);
            case "sync" -> plugin.permissions().sync(target, d);
            case "complete" -> {
                Quest q = plugin.quests().current(d);
                if (q != null) plugin.quests().complete(target, d, q);
            }
            case "set" -> {
                if (args.length < 5) {
                    usage(sender);
                    return;
                }
                Category c = plugin.quests().category(args[3].toLowerCase(Locale.ROOT));
                if (c == null) {
                    tell(sender, "unknown-category", ph);
                    return;
                }
                int n;
                try {
                    n = Integer.parseInt(args[4]);
                } catch (NumberFormatException ex) {
                    n = -1;
                }
                if (n < 1 || n > c.quests().size()) {
                    tell(sender, "invalid-number", ph);
                    return;
                }
                plugin.quests().setPosition(target, c.id(), n);
            }
            default -> {
                usage(sender);
                return;
            }
        }
        tell(sender, "admin-done", ph);
    }

    private void usage(CommandSender sender) {
        for (String l : plugin.msgList("admin-usage")) {
            sender.sendMessage(Text.color(l));
        }
    }

    private boolean bossbarAllowed() {
        return plugin.cfg().getBoolean("bossbar.enabled", true);
    }

    private void deny(CommandSender sender) {
        tell(sender, "no-permission", new HashMap<>());
    }

    private void tell(CommandSender sender, String key, Map<String, String> ph) {
        sender.sendMessage(Text.color(plugin.prefix() + Text.replace(plugin.msg(key), ph)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission("oneblockquests.admin");
        if (args.length == 1) {
            if (bossbarAllowed()) out.add("bossbar");
            if (admin) {
                out.add("reload");
                out.add("admin");
            }
        } else if (admin && args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                out.addAll(List.of("reset", "set", "complete", "sync"));
            } else if (args.length == 3) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
                for (Category c : plugin.quests().categories()) out.add(c.id());
            }
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
