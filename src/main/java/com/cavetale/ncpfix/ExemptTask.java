package com.cavetale.ncpfix;

import fr.neatmonster.nocheatplus.checks.CheckType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A task which starts by exempting the player, runs while player is
 * supposed to be exempt, and unexempts them when the timer runs out.
 */
@RequiredArgsConstructor @Getter
final class ExemptTask extends BukkitRunnable {
    private final NCPFix plugin;
    private final Player player;
    @Setter private long timeout;

    public ExemptTask start() {
        runTaskTimer(plugin, 0L, 1L);
        exempt();
        return this;
    }

    /**
     * Stop the task and unexempt the player. Do NOT remove this task
     * from the plugin's map!
     */
    public void stop() {
        unexempt();
        cancel();
    }

    private void exempt() {
        plugin.exempt(player, CheckType.MOVING_SURVIVALFLY, true);
        plugin.exempt(player, CheckType.MOVING_CREATIVEFLY, true);
    }

    private void unexempt() {
        plugin.exempt(player, CheckType.MOVING_SURVIVALFLY, false);
        plugin.exempt(player, CheckType.MOVING_CREATIVEFLY, false);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            stop();
            plugin.removeTask(player.getUniqueId());
            plugin.getLogger().warning("Player went missing: " + player.getName());
            return;
        }
        if (player.isDead()) {
            stop();
            plugin.removeTask(player.getUniqueId());
            return;
        }
        if (player.isGliding()) return;
        long now = System.currentTimeMillis();
        if (now > timeout) {
            stop();
            plugin.removeTask(player.getUniqueId());
        }
    }
}
