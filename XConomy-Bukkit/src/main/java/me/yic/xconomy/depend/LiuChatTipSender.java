/*
 *  This file (LiuChatTipSender.java) is a part of project XConomy
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
package me.yic.xconomy.depend;

import me.yic.xconomy.XConomy;
import me.yic.xconomy.XConomyLoad;
import me.yic.xconomy.utils.CrossServerTips;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 通过 LiuChat 的跨服公告流程转发 XConomy 的提示消息
 * （LiuChat 内部可使用 Redis 或代理插件通道）。
 * <p>
 * LiuChat 只是软依赖：全部通过反射调用，即使 LiuChat 不存在或其 API 发生变化，
 * 插件仍能正常加载，只是跳过转发。LiuChat 要求在主线程发送公告，
 * 因此必要时会切换到主线程执行。
 */
public class LiuChatTipSender implements CrossServerTips.Sender {

    private Plugin liuchat;
    /** LiuChat 的广播公告 API（旧版本回退用）。 */
    private Method broadcastAnnouncement;
    /** LiuChat 的私密投递 API（优先使用，只发给目标玩家）。 */
    private Method deliverPrivate;
    private Object crossServer;
    private Method knownPlayerNames;

    private void init() {
        if (liuchat != null) {
            return;
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("LiuChat");
            if (plugin == null || !plugin.isEnabled()) {
                return;
            }
            liuchat = plugin;
            // 优先使用私密投递；旧版 LiuChat 没有该方法时退回广播公告
            deliverPrivate = findMethod(plugin, "deliverPrivate", Player.class, String.class, BaseComponent[].class);
            broadcastAnnouncement = findMethod(plugin, "broadcastAnnouncement", Player.class, BaseComponent[].class);
            // 可选：LiuChat 的跨服在线列表，用于跳过离线玩家
            try {
                Field field = plugin.getClass().getDeclaredField("crossServer");
                field.setAccessible(true);
                crossServer = field.get(plugin);
                if (crossServer != null) {
                    knownPlayerNames = crossServer.getClass().getMethod("knownPlayerNames");
                }
            } catch (Throwable ignored) {
                crossServer = null;
                knownPlayerNames = null;
            }
        } catch (Throwable ignored) {
            liuchat = null;
            broadcastAnnouncement = null;
            deliverPrivate = null;
        }
    }

    /** 反射查找公开方法，不存在时返回 null（旧版兼容）。 */
    private static Method findMethod(Plugin plugin, String name, Class<?>... types) {
        try {
            return plugin.getClass().getMethod(name, types);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean broadcast(UUID carrier, String targetName, String message) {
        init();
        if (liuchat == null || (deliverPrivate == null && broadcastAnnouncement == null)) {
            return false;
        }
        Player player = carrier == null ? null : Bukkit.getPlayer(carrier);
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (XConomyLoad.Config.LIUCHAT_TIP_ONLY_ONLINE && !isOnlineSomewhere(targetName)) {
            return false;
        }
        if (Bukkit.isPrimaryThread()) {
            send(player, targetName, message);
        } else {
            Bukkit.getScheduler().runTask(XConomy.getInstance(), () -> send(player, targetName, message));
        }
        return true;
    }

    private void send(Player carrier, String targetName, String message) {
        try {
            if (!carrier.isOnline()) {
                return;
            }
            BaseComponent[] components = TextComponent.fromLegacyText(message);
            if (deliverPrivate != null) {
                deliverPrivate.invoke(liuchat, carrier, targetName, (Object) components);
            } else {
                broadcastAnnouncement.invoke(liuchat, carrier, (Object) components);
            }
        } catch (Throwable ignored) {
            // 聊天插件异常不能影响已完成的交易
        }
    }

    /** 尽力判断：LiuChat 是否认为该玩家在任意服务器在线。 */
    private boolean isOnlineSomewhere(String name) {
        if (knownPlayerNames == null || crossServer == null || name == null) {
            return true;
        }
        List<String> names = knownPlayers();
        if (names.isEmpty()) {
            // 无法读取在线列表时默认视为在线，避免提示被静默丢弃
            return true;
        }
        for (String online : names) {
            if (name.equalsIgnoreCase(online)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 跨服在线玩家名单（含本服与远程服务器），用于指令 TAB 补全。
     * 未安装 LiuChat 或无法读取时返回空列表。
     */
    @Override
    public List<String> knownPlayers() {
        init();
        if (knownPlayerNames == null || crossServer == null) {
            return Collections.emptyList();
        }
        try {
            Object result = knownPlayerNames.invoke(crossServer);
            if (result instanceof Collection) {
                List<String> names = new ArrayList<>();
                for (Object online : (Collection<?>) result) {
                    if (online != null) {
                        names.add(online.toString());
                    }
                }
                return names;
            }
        } catch (Throwable ignored) {
            // 忽略异常，返回空列表
        }
        return Collections.emptyList();
    }
}
