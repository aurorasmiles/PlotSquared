package com.github.intellectualsites.plotsquared.plot.commands;

import com.github.intellectualsites.plotsquared.commands.Command;
import com.github.intellectualsites.plotsquared.commands.CommandDeclaration;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.database.DBFunc;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal2;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal3;
import com.github.intellectualsites.plotsquared.plot.util.EventUtil;
import com.github.intellectualsites.plotsquared.plot.util.MainUtil;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@CommandDeclaration(command = "add",
    description = "Allow a user to build in a plot while you are online",
    usage = "/plot add <player>", category = CommandCategory.SETTINGS, permission = "plots.add",
    requiredType = RequiredType.NONE) public class Add extends Command {

    public Add() {
        super(MainCommand.getInstance(), true);
    }

    @Override public void execute(final PlotPlayer player, String[] args,
        RunnableVal3<Command, Runnable, Runnable> confirm,
        RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        final Plot plot = check(player.getCurrentPlot(), Captions.NOT_IN_PLOT);
        checkTrue(plot.hasOwner(), Captions.PLOT_UNOWNED);
        checkTrue(plot.isOwner(player.getUUID()) || Permissions
                .hasPermission(player, Captions.PERMISSION_ADMIN_COMMAND_TRUST),
            Captions.NO_PLOT_PERMS);
        checkTrue(args.length == 1, Captions.COMMAND_SYNTAX, getUsage());
        final Set<UUID> uuids = MainUtil.getUUIDsFromString(args[0]).getView();
        checkTrue(!uuids.isEmpty(), Captions.INVALID_PLAYER, args[0]);
        Iterator<UUID> iter = uuids.iterator();
        int size = plot.getTrusted().size() + plot.getMembers().size();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            if (uuid == DBFunc.EVERYONE && !(
                Permissions.hasPermission(player, Captions.PERMISSION_TRUST_EVERYONE) || Permissions
                    .hasPermission(player, Captions.PERMISSION_ADMIN_COMMAND_TRUST))) {
                MainUtil.sendMessage(player, Captions.INVALID_PLAYER, MainUtil.getName(uuid));
                iter.remove();
                continue;
            }
            if (plot.isOwner(uuid)) {
                MainUtil.sendMessage(player, Captions.ALREADY_OWNER, MainUtil.getName(uuid));
                iter.remove();
                continue;
            }
            if (plot.getMembers().contains(uuid)) {
                MainUtil.sendMessage(player, Captions.ALREADY_ADDED, MainUtil.getName(uuid));
                iter.remove();
                continue;
            }
            size += plot.getTrusted().contains(uuid) ? 0 : 1;
        }
        checkTrue(!uuids.isEmpty(), null);
        checkTrue(size <= plot.getArea().MAX_PLOT_MEMBERS || Permissions
                .hasPermission(player, Captions.PERMISSION_ADMIN_COMMAND_TRUST),
            Captions.PLOT_MAX_MEMBERS);
        confirm.run(this, new Runnable() {
            @Override // Success
            public void run() {
                for (UUID uuid : uuids) {
                    if (uuid != DBFunc.EVERYONE) {
                        if (!plot.removeTrusted(uuid)) {
                            if (plot.getDenied().contains(uuid)) {
                                plot.removeDenied(uuid);
                            }
                        }
                    }
                    plot.addMember(uuid);
                    EventUtil.manager.callMember(player, plot, uuid, true);
                    MainUtil.sendMessage(player, Captions.MEMBER_ADDED);
                }
            }
        }, null);
    }
}
