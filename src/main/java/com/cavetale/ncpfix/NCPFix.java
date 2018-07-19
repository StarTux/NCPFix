package com.winthier.ncpfix;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NCPFix extends JavaPlugin implements Listener {
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("ncpfix.remove")) return;
        getServer().dispatchCommand(getServer().getConsoleSender(), "ncp removeplayer " + player.getName());
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player)event.getEntity();
        if (event.isGliding()) {
            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY);
            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_CREATIVEFLY);
        } else {
            NCPExemptionManager.unexempt(player, CheckType.MOVING_SURVIVALFLY);
            NCPExemptionManager.unexempt(player, CheckType.MOVING_CREATIVEFLY);
        }
    }
}
