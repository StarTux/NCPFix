package com.cavetale.ncpfix;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NCPFix extends JavaPlugin implements Listener {
    private Map<UUID, ExemptTask> tasks = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (ExemptTask task : tasks.values()) {
            task.stop();
        }
        tasks.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage(tasks.size() + " tasks:");
        for (ExemptTask task : tasks.values()) {
            long time = task.getTimeout() - System.currentTimeMillis();
            sender.sendMessage("- " + task.getPlayer().getName() + ": " + time);
        }
        return true;
    }

    /**
     * Clear their stats when they join because NCP likes to deny
     * rejoining after false positives.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("ncpfix.remove")) return;
        getServer().dispatchCommand(getServer().getConsoleSender(), "ncp removeplayer " + player.getName());
    }

    /**
     * Delete exemption when they quit, just in case.
     * Clear the stats of permission holders because NCP likes to deny
     * rejoining after false positives.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ExemptTask task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.stop();
        }
        if (player.hasPermission("ncpfix.remove")) {
            getServer().dispatchCommand(getServer().getConsoleSender(), "ncp removeplayer " + player.getName());
        }
    }

    /**
     * Exempt when they start gliding, unexempt when they stop, with a
     * 1 second delay to avoid a common false positive.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.isGliding()) {
            timedExempt(player, 0L);
        } else {
            timedExempt(player, 3000L);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        timedExempt(player, 3000L);
    }

    void exempt(Player player, CheckType type, boolean exempt) {
        if (!player.isOnline()) return;
        if (NCPExemptionManager.isExempted(player, type) == exempt) return;
        if (exempt) {
            NCPExemptionManager.exemptPermanently(player, type);
        } else {
            NCPExemptionManager.unexempt(player, type);
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
}
