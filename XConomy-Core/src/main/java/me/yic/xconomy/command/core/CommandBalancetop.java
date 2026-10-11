/*
 *  This file (CommandBalancetop.java) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.yic.xconomy.command.core;

import me.yic.xconomy.adapter.comp.CSender;
import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataLink;
import me.yic.xconomy.data.caches.Cache;
import me.yic.xconomy.data.syncdata.PlayerData;
import me.yic.xconomy.info.MessageConfig;
import me.yic.xconomy.lang.MessagesManager;

import java.util.UUID;

public class CommandBalancetop extends CommandCore{
    public static boolean onCommand(CSender sender, String[] args) {
        if (args.length == 1 && (args[0].equalsIgnoreCase("hide") || args[0].equalsIgnoreCase("display"))) {
            return selfToggle(sender, args[0]);
        }

        if (args.length == 0 || args.length == 1) {

            if (!(sender.isOp() || sender.hasPermission("xconomy.user.balancetop"))) {
                sendMessages(sender, PREFIX + translateColorCodes("no_permission"));
                return true;
            }

            if (Cache.baltop.isEmpty()) {
                sendMessages(sender, PREFIX + translateColorCodes("top_nodata"));
                return true;
            }

            if (args.length == 0) {
                sendRankingMessage(sender, 1);
            } else {
                if (isDouble(args[0])) {
                    if (Integer.parseInt(args[0]) > 0) {
                        sendRankingMessage(sender, Integer.valueOf(args[0]));
                    } else {
                        sendRankingMessage(sender, 1);
                    }
                } else {
                    sendRankingMessage(sender, 1);
                }
            }

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("hide") || args[0].equalsIgnoreCase("display")) {

                if (!(sender.isOp() || sender.hasPermission("xconomy.admin.balancetop"))) {
                    sendHelpMessage(sender, 1);
                    return true;
                }

                PlayerData pd = DataCon.getPlayerData(args[1]);

                if (pd == null) {
                    sendMessages(sender, PREFIX + translateColorCodes(MessageConfig.NO_ACCOUNT));
                    return true;
                }

                UUID targetUUID = pd.getUniqueId();
                if (args[0].equalsIgnoreCase("hide")) {
                    DataLink.setTopBalHide(targetUUID, 1);
                    sendMessages(sender, PREFIX + translateColorCodes("top_hidden").replace("%player%", args[1]));
                } else if (args[0].equalsIgnoreCase("display")) {
                    DataLink.setTopBalHide(targetUUID, 0);
                    sendMessages(sender, PREFIX + translateColorCodes("top_displayed").replace("%player%", args[1]));
                }
                DataCon.baltop();

            }
        }
        return true;
    }

    /**
     * 玩家自助隐藏 / 显示自己在排行榜中的数据。
     * 管理员指定其他玩家的用法在双参数分支中处理。
     */
    private static boolean selfToggle(CSender sender, String action) {
        if (!sender.isPlayer()) {
            sendMessages(sender, PREFIX + MessagesManager.systemMessage("§6控制台无法使用该指令"));
            return true;
        }
        if (!(sender.isOp() || sender.hasPermission("xconomy.user.balancetop.hide"))) {
            sendMessages(sender, PREFIX + translateColorCodes("no_permission"));
            return true;
        }
        boolean hide = action.equalsIgnoreCase("hide");
        UUID targetUUID = sender.toPlayer().getUniqueId();
        DataLink.setTopBalHide(targetUUID, hide ? 1 : 0);
        DataCon.baltop();
        String key = hide ? "top_hidden" : "top_displayed";
        sendMessages(sender, PREFIX + translateColorCodes(key).replace("%player%", sender.getName()));
        return true;
    }

}
