/*
 *  This file (XConomyLoad.java) is a part of project XConomy
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
package me.yic.xconomy;

import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.sql.SQL;
import me.yic.xconomy.adapter.comp.CConfig;
import me.yic.xconomy.info.DataBaseConfig;
import me.yic.xconomy.info.DefaultConfig;
import me.yic.xconomy.info.SyncChannalType;
import me.yic.xconomy.lang.MessagesManager;
import me.yic.xconomy.utils.RedisConnection;

import java.io.File;
import java.math.BigDecimal;

public class XConomyLoad{
    public static boolean DDrivers = false;

    public static DataBaseConfig DConfig;
    public static DefaultConfig Config;

    public static void LoadConfig(){

        Config = new DefaultConfig();
        DConfig = new DataBaseConfig();

        MessagesManager.loadsysmess();
        MessagesManager.loadlangmess();

        DConfig.Initialization();
        Config.setSyncData();
    }

    public static void Initial(){
        DataCon.baltop();
        DataFormat.load();
    }

    /**
     * 重新从磁盘读取 config.yml 中的财富税配置，覆盖内存中的值。
     * 这样管理员改完 config.yml 后，无需重启服务器，直接执行 /wealthtax run 即可生效。
     */
    public static void reloadWealthTax() {
        try {
            File file = new File(XConomy.getInstance().getDataFolder(), "config.yml");
            if (!file.isFile()) {
                return;
            }
            CConfig fresh = new CConfig(file);
            DefaultConfig c = Config;
            c.WEALTH_TAX_ENABLE = fresh.getBoolean("WealthTax.enable");
            c.WEALTH_TAX_SERVER_ID = fresh.getString("WealthTax.server-id");
            c.WEALTH_TAX_EXECUTOR = fresh.getString("WealthTax.executor");
            c.WEALTH_TAX_EFFECTIVE = fresh.getString("WealthTax.effective-date");
            c.WEALTH_TAX_TIME = fresh.getString("WealthTax.time");
            c.WEALTH_TAX_THRESHOLD = BigDecimal.valueOf(fresh.getDouble("WealthTax.threshold"));
            c.WEALTH_TAX_RATE = fresh.getDouble("WealthTax.rate");
            c.WEALTH_TAX_EXCESS = "excess".equalsIgnoreCase(String.valueOf(fresh.getString("WealthTax.basis")));
            c.WEALTH_TAX_RESIDENCE = !fresh.contains("WealthTax.include-residence-bank")
                    || fresh.getBoolean("WealthTax.include-residence-bank");
            c.WEALTH_TAX_BROADCAST = !fresh.contains("WealthTax.broadcast")
                    || fresh.getBoolean("WealthTax.broadcast");
            c.WEALTH_TAX_DESTROY = !fresh.contains("WealthTax.destroy")
                    || fresh.getBoolean("WealthTax.destroy");
            c.WEALTH_TAX_ACCOUNT = fresh.getString("WealthTax.account");
        } catch (Throwable t) {
            XConomy.getInstance().logger(null, 1, "[财富税] 重新读取配置失败: " + t.getMessage());
        }
    }

    public static void Unload() {
        if (Config.SYNCDATA_TYPE.equals(SyncChannalType.REDIS)) {
            RedisConnection.close();
        }
        SQL.close();
    }

    public static boolean getSyncData_Enable(){
        return !Config.SYNCDATA_TYPE.equals(SyncChannalType.OFF);
    }

}
