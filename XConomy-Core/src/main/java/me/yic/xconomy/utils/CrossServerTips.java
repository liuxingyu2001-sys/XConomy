/*
 *  This file (CrossServerTips.java) is a part of project XConomy
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
package me.yic.xconomy.utils;

import java.util.UUID;

/**
 * 核心模块向外部聊天插件（Paper/Bukkit 上的 LiuChat）转发跨服提示的平台桥接。
 * <p>
 * 核心模块不会直接引用 LiuChat。平台在启动时注册一个 {@link Sender}；
 * 当 LiuChat 不存在（或未注册发送器）时，所有调用都是静默的空操作，
 * 不会影响插件正常运行。
 */
public class CrossServerTips {

    public interface Sender {
        /**
         * @param carrier    承载数据包的在线玩家
         * @param targetName 提示所指向的玩家（用于跳过离线玩家）
         * @param message    已完成颜色转换的消息
         * @return 是否已成功交给聊天插件
         */
        boolean broadcast(UUID carrier, String targetName, String message);

        /**
         * @return 跨服在线玩家名列表（用于 TAB 补全），无法获取时返回空列表
         */
        java.util.List<String> knownPlayers();
    }

    private static Sender sender;

    public static void register(Sender s) {
        sender = s;
    }

    public static boolean available() {
        return sender != null;
    }

    public static boolean broadcast(UUID carrier, String targetName, String message) {
        Sender s = sender;
        return s != null && s.broadcast(carrier, targetName, message);
    }

    /**
     * 获取全服在线玩家名单，用于跨服指令的 TAB 补全。
     * 未安装聊天插件时返回空列表。
     */
    public static java.util.List<String> knownPlayers() {
        Sender s = sender;
        if (s == null) {
            return java.util.Collections.emptyList();
        }
        try {
            return s.knownPlayers();
        } catch (Throwable ignored) {
            return java.util.Collections.emptyList();
        }
    }
}
