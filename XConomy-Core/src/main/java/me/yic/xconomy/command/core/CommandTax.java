/*
 *  This file (CommandTax.java) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.yic.xconomy.command.core;

import me.yic.xconomy.adapter.comp.CChat;
import me.yic.xconomy.adapter.comp.CSender;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.sql.SQLWealthTax;
import me.yic.xconomy.lang.MessagesManager;
import me.yic.xconomy.task.WealthTax;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财富税管理指令（仅 OP）。
 * <ul>
 *   <li>{@code run}：立即结算（忽略是否开启、时间与生效日期，但校验执行服）</li>
 *   <li>{@code status}：查看今日结算状态</li>
 * </ul>
 * 可通过 {@code /xconomy tax <run|status>} 或 {@code /wealthtax <run|status>} 调用。
 */
public class CommandTax {

    public static boolean onCommand(CSender sender, String[] args) {
        if (!sender.isOp()) {
            send(sender, m("no_permission", "&c你没有权限使用该指令。"));
            return true;
        }
        if (args.length < 1) {
            send(sender, m("tax_usage", "&7用法：/wealthtax <run|status>"));
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "run":
            case "now": {
                WealthTax.Status status = WealthTax.execute(true);
                send(sender, m(statusKey(status), statusFallback(status)));
                return true;
            }
            case "status":
            case "info": {
                String date = LocalDate.now().toString();
                boolean completed = SQLWealthTax.isCompleted(date);
                int count = SQLWealthTax.affected(date);
                BigDecimal total = SQLWealthTax.total(date);
                send(sender, m("tax_status",
                        "&7日期 &f%date% &7｜ 已结算 &f%completed% &7｜ 人数 &f%count% &7｜ 金额 &f%amount%",
                        "%date%", date,
                        "%completed%", completed ? "是" : "否",
                        "%count%", String.valueOf(count),
                        "%amount%", DataFormat.shown(total)));
                return true;
            }
            default:
                send(sender, m("tax_usage", "&7用法：/wealthtax <run|status>"));
                return true;
        }
    }

    private static String statusKey(WealthTax.Status status) {
        return "tax_" + status.name().toLowerCase();
    }

    private static String statusFallback(WealthTax.Status status) {
        switch (status) {
            case STARTED:
                return "&a财富税结算已开始，稍后可用 /wealthtax status 查看结果。";
            case DISABLED:
                return "&c财富税未开启。";
            case NOT_YET:
                return "&e未到结算时间或未过生效日期。";
            case NOT_EXECUTOR:
                return "&e本服不是指定的财富税结算服。";
            case IN_PROGRESS:
                return "&e财富税正在结算中，请稍后。";
            case ALREADY_DONE:
                return "&e今日财富税已结算。";
            case ERROR:
            default:
                return "&c财富税结算出现异常，请查看控制台。";
        }
    }

    private static String m(String key, String fallback, String... placeholders) {
        String raw = MessagesManager.messageFile == null ? null : MessagesManager.messageFile.getString(key);
        if (raw == null) {
            raw = fallback;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace(placeholders[i], placeholders[i + 1]);
        }
        return CommandCore.PREFIX + CChat.translateAlternateColorCodes('&', raw);
    }

    private static void send(CSender sender, String message) {
        CommandCore.sendMessages(sender, message);
    }
}
