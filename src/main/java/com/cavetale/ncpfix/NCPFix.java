package com.cavetale.ncpfix;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NCPFix extends JavaPlugin implements Listener {
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
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
        exempt(player, CheckType.MOVING_SURVIVALFLY, false);
        exempt(player, CheckType.MOVING_CREATIVEFLY, false);
        if (player.hasPermission("ncpfix.remove")) {
            getServer().dispatchCommand(getServer().getConsoleSender(), "ncp removeplayer " + player.getName());
        }
    }

    /**
     * Exempt when they start gliding, unexempt when they stop, with a
     * 1 second delay to avoid a common false positive.
     */
    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.isGliding()) {
            exempt(player, CheckType.MOVING_SURVIVALFLY, true);
            exempt(player, CheckType.MOVING_CREATIVEFLY, true);
        } else {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline() && !player.isGliding()) {
                        exempt(player, CheckType.MOVING_SURVIVALFLY, false);
                        exempt(player, CheckType.MOVING_CREATIVEFLY, false);
                    }
                }, 60L);
        }
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
}
