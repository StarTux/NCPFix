package com.cavetale.ncpfix;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.hooks.AbstractNCPHook;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class FixHook extends AbstractNCPHook {
    private final NCPFix plugin;

    @Override
    public String getHookName() {
        return "NCPFix";
    }

    @Override
    public String getHookVersion() {
        return "0.1";
    }

    @Override
    public boolean onCheckFailure(CheckType checkType, Player player, IViolationInfo info) {
        boolean exempted = plugin.isExempt(player);
        if (plugin.isDebug(player)) {
            player.sendMessage("[NCPFix] " + checkType
                               + " add=" + info.getAddedVl()
                               + " vl=" + info.getTotalVl()
                               + " cancel=" + info.willCancel()
                               + " exempted=" + exempted);
        }
        return exempted;
    }
}
