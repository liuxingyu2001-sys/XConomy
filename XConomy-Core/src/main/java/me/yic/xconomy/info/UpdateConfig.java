/*
 *  This file (UpdateConfig.java) is a part of project XConomy
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
package me.yic.xconomy.info;

import me.yic.xconomy.XConomy;
import me.yic.xconomy.adapter.comp.CConfig;

import java.io.IOException;

public class UpdateConfig {

    /** 仅当键不存在时写入默认值。 */
    private static boolean ensure(CConfig config, String path, Object def) {
        if (!config.contains(path)) {
            config.set(path, def);
            return true;
        }
        return false;
    }

    public static void update(CConfig config) {
        boolean update = false;
        if (!config.contains("SyncData")) {
            XConomy.getInstance().logger(null, 1, "==================================================");
            XConomy.getInstance().logger(null, 1, "The configuration file is an older version");
            XConomy.getInstance().logger(null, 1, "The plugin may occur configuration problems");
            XConomy.getInstance().logger(null, 1, "It is recommended to regenerate configuration file");
            XConomy.getInstance().logger(null, 1, "==================================================");
        }
        if (!config.contains("Currency.rounding-mode")) {
            config.createSection("Currency.rounding-mode");
            config.set("Currency.rounding-mode", 0);
            update = true;
        }
        if (!config.contains("Thread.future-timeout")) {
            config.createSection("Thread.future-timeout");
            config.set("Thread.future-timeout", 3);
            update = true;
        }
        if (!config.contains("Settings.hidden-in-baltop")) {
            config.set("Settings.hidden-in-baltop", false);
            update = true;
        }
        if (!config.contains("LiuChat")) {
            config.createSection("LiuChat");
            config.set("LiuChat.cross-server-pay-tips", true);
            config.set("LiuChat.tip-only-online", true);
            config.set("LiuChat.pay-tip-format", "&8[&b跨服转账&8] &f%player% &7给 &f%target% &7转账了 &a%amount%");
            update = true;
        }
        if (!config.contains("WealthTax")) {
            config.createSection("WealthTax");
        }
        update = ensure(config, "WealthTax.enable", false) || update;
        update = ensure(config, "WealthTax.server-id", "") || update;
        update = ensure(config, "WealthTax.executor", "") || update;
        update = ensure(config, "WealthTax.effective-date", "") || update;
        update = ensure(config, "WealthTax.time", "00:00") || update;
        update = ensure(config, "WealthTax.threshold", 50000000) || update;
        update = ensure(config, "WealthTax.rate", 0.005) || update;
        update = ensure(config, "WealthTax.basis", "full") || update;
        update = ensure(config, "WealthTax.include-residence-bank", true) || update;
        update = ensure(config, "WealthTax.broadcast", true) || update;
        update = ensure(config, "WealthTax.destroy", true) || update;
        update = ensure(config, "WealthTax.account", "") || update;
        if (update){
            try {
                config.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
