package com.cavetale.ncpfix;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;
import fr.neatmonster.nocheatplus.players.DataManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NCPFix extends JavaPlugin implements Listener {
    private Map<UUID, ExemptTask> tasks = new HashMap<>();
    private Set<UUID> debugs = new HashSet<>();
    private Set<UUID> exempts = new HashSet<>();
    private FixHook fixHook = new FixHook(this);
    public static final CheckType[] CHECKS = {
        CheckType.MOVING_SURVIVALFLY,
        CheckType.MOVING_CREATIVEFLY
    };

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        NCPHookManager.addHook(CHECKS, fixHook);
    }

    @Override
    public void onDisable() {
        NCPHookManager.removeHook(fixHook);
        for (ExemptTask task : tasks.values()) {
            task.stop();
        }
        tasks.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        switch (args[0]) {
        case "info": {
            sender.sendMessage(tasks.size() + " tasks:");
            for (ExemptTask task : tasks.values()) {
                long time = task.getTimeout() - System.currentTimeMillis();
                sender.sendMessage("- " + task.getPlayer().getName() + ": " + time
                                   + (task.getPlayer().isGliding() ? " (glide)" : "")
                                   + " exempt=" + isExempt(player)
                                   + " ncpExempt=" + NCPExemptionManager.isExempted(player, CheckType.MOVING_SURVIVALFLY));
            }
            return true;
        }
        case "debug": {
            if (player == null) {
                sender.sendMessage("[NCPFix] Player expected!");
                return true;
            }
            if (debugs.contains(player.getUniqueId())) {
                debugs.remove(player.getUniqueId());
                player.sendMessage("Debug mode disabled");
            } else {
                debugs.add(player.getUniqueId());
                player.sendMessage("Debug mode enabled");
            }
            return true;
        }
        case "me": {
            ExemptTask task = tasks.get(player.getUniqueId());
            sender.sendMessage("task: " + (task != null ? "" + task.getTimeLeft() : "none"));
            sender.sendMessage("exempt: " + isExempt(player));
            sender.sendMessage("ncp-exempt: " + NCPExemptionManager.isExempted(player, CheckType.MOVING_SURVIVALFLY));
            return true;
        }
        default: return false;
        }
    }

    /**
     * Clear their stats when they join because NCP likes to deny
     * rejoining after false positives.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ncpfix.remove")) {
            getServer().dispatchCommand(getServer().getConsoleSender(), "ncp removeplayer " + player.getName());
        }
        timedExempt(player, 5000L);
    }

    /**
     * Delete exemption when they quit, just in case.
     * Clear the stats of permission holders because NCP likes to deny
     * rejoining after false positives.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        debugs.remove(player.getUniqueId());
        exempts.remove(player.getUniqueId());
        ExemptTask task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.stop();
        }
        if (player.hasPermission("ncpfix.remove")) {
            getServer().dispatchCommand(getServer().getConsoleSender(), "ncp removeplayer " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (debugs.contains(player.getUniqueId())) {
            player.sendMessage("[NCPFix] " + event.getEventName());
        }
        ExemptTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.stop();
    }

    /**
     * Exempt when they start gliding, unexempt when they stop, with a
     * 1 second delay to avoid a common false positive.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (debugs.contains(player.getUniqueId())) {
            player.sendMessage("[NCPFix] " + event.getEventName() + " " + event.isGliding());
        }
        if (event.isGliding()) {
            timedExempt(player, 0L);
        } else {
            timedExempt(player, 5000L);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        if (debugs.contains(player.getUniqueId())) {
            player.sendMessage("[NCPFix] " + event.getEventName());
        }
        timedExempt(player, 3000L);
        if (player.isGliding()) {
            player.setGliding(false);
        }
    }

    boolean isExempt(Player player) {
        return exempts.contains(player.getUniqueId());
    }

    void exempt(Player player, boolean exempt) {
        if (!player.isOnline()) return;
        if (isExempt(player) == exempt) return;
        if (debugs.contains(player.getUniqueId())) {
            player.sendMessage("[NCPFix] exempt " + (exempt ? "ON" : "OFF"));
        }
        if (exempt) {
            exempts.add(player.getUniqueId());
            for (CheckType checkType : CHECKS) {
                NCPExemptionManager.exemptPermanently(player, checkType);
            }
        } else {
            exempts.remove(player.getUniqueId());
            for (CheckType checkType : CHECKS) {
                NCPExemptionManager.unexempt(player, checkType);
                DataManager.removeData(player.getName(), checkType);
            }
        }
    }

    /**
     * Exempt the player for some milliseconds in real time.
     */
    public void timedExempt(Player player, long time) {
        ExemptTask task = tasks.computeIfAbsent(player.getUniqueId(), u -> new ExemptTask(this, player).start());
        task.setTimeout(System.currentTimeMillis() + time);
    }

    /**
     * Remove an exemption task. Take no further action. Called by
     * ExemptTask.
     */
    void removeTask(UUID uuid) {
        tasks.remove(uuid);
    }

    public boolean isDebug(Player player) {
        return debugs.contains(player.getUniqueId());
    }
}
