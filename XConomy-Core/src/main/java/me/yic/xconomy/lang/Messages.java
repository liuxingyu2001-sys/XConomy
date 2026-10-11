/*
 *  This file (Messages.java) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.yic.xconomy.lang;

/**
 * Maps the internal Chinese log tags used across the Core to the keys of the
 * system language files. Previously written in Kotlin (Messages.kt); rewritten
 * in Java so the build no longer needs the Kotlin toolchain.
 */
public class Messages {

    public String gettag(String message) {
        if (message == null) {
            return null;
        }
        switch (message) {
            case "数据保存方式":
                return "saving-mode";
            case "自定义文件夹路径不存在":
                return "no-custom-path";
            case "连接正常":
                return "connect-success";
            case "连接异常":
                return "connect-fail";
            case "重新连接成功":
                return "reconnect-success";
            case "连接断开失败":
                return "disconnect-fail";
            case "缓存文件创建异常":
                return "cache-file-creation-exception";
            case "升级数据库表格。。。":
                return "upgrade-database";
            case "Redis监听线程创建中":
                return "redis-create";
            case "订阅Redis频道成功, channel ":
                return "redis-subscribe";
            case "取消订阅Redis频道":
                return "redis-unsubscribe";
            case "XConomy加载成功":
                return "enable-success";
            case "XConomy已成功卸载":
                return "disable-success";
            case "SQLite文件路径设置错误":
                return "custom-path-error";
            case "文件夹创建异常":
                return "create-folder-fail";
            case "无法连接到数据库-----":
                return "unable-connect";
            case "JDBC驱动加载失败":
                return "jdbc-fail";
            case "已创建一个新的语言文件":
                return "create-language-file-success";
            case "语言文件创建异常":
                return "create-language-file-fail";
            case "发现 PlaceholderAPI":
                return "found-placeholderapi";
            case "发现 DatabaseDrivers":
                return "found-databasedrivers";
            case "已是最新版本":
                return "is-new-version";
            case "检查更新失败":
                return "check-version-fail";
            case "发现新版本 ":
                return "found-version";
            case "§amessage.yml重载成功":
                return "messege-reload";
            case "vault-baltop-tips-a":
                return "vault-baltop-tips-a";
            case "vault-baltop-tips-b":
                return "vault-baltop-tips-b";
            case " 名称已更改!":
                return "username-modified";
            case "§c该指令不支持在半正版模式中使用":
                return "semi-mode-ban-commands";
            case "§6控制台无法使用该指令":
                return "console-ban-commands";
            case "连接池未启用":
                return "pool-disable";
            case "未找到 'org.slf4j.Logger'":
                return "slf4j-unfound";
            case "收到不同版本插件的数据，无法同步，当前插件版本 ":
                return "different-version";
            default:
                return message;
        }
    }
}
