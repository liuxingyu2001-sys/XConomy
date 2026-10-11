/*
 *  This file (DefaultConfig.java) is a part of project XConomy
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
import me.yic.xconomy.utils.UUIDMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DefaultConfig {
    public static CConfig config;

    public DefaultConfig() {
        setUUIDMode();
        setnonplayeraccount();
        setformatbalance();
        setpaytips();
        setliuchat();
    }
    public boolean ISOLDCONFIG = false;

    public UUIDMode UUIDMODE = UUIDMode.DEFAULT;
    public boolean IMPORTMODE = config.getBoolean("Importdata-mode");

    public String LANGUAGE = config.getString("Settings.language");
    public boolean CHECK_UPDATE = config.getBoolean("Settings.check-update");
    public int REFRESH_TIME = Math.max(config.getInt("Settings.refresh-time"), 30);
    public boolean ECO_COMMAND = config.getBoolean("Settings.eco-command");
    public boolean DISABLE_ESSENTIAL = config.getBoolean("Settings.disable-essentials");
    public double INITIAL_BAL = config.getDouble("Settings.initial-bal");
    public BigDecimal PAYMENT_TAX = BigDecimal.ZERO;
    public int RANKING_SIZE = getrankingsize();
    public int LINES_PER_PAGE = config.getInt("Settings.lines-per-page");
    public boolean DISABLE_CACHE = config.getBoolean("Settings.disable-cache");

    public boolean TRANSACTION_RECORD = config.getBoolean("Settings.transaction-record");
    public boolean PAY_TIPS = false;
    public boolean USERNAME_IGNORE_CASE = config.getBoolean("Settings.username-ignore-case");
    public boolean HIDDEN_IN_BALTOP = config.getBoolean("Settings.hidden-in-baltop");

    // LiuChat 跨服转账提示
    public boolean LIUCHAT_CROSS_SERVER_PAY_TIPS = false;
    public String LIUCHAT_PAY_TIP_FORMAT = "&8[&b跨服转账&8] &f%player% &7向你转账了 &a%amount%";
    public boolean LIUCHAT_TIP_ONLY_ONLINE = true;

    public boolean NON_PLAYER_ACCOUNT = config.getBoolean("non-player-account.enable");
    public List<String> NON_PLAYER_ACCOUNT_SUBSTRING = null;

    public String SINGULAR_NAME = config.getString("Currency.singular-name");
    public String PLURAL_NAME = config.getString("Currency.plural-name");
    public boolean INTEGER_BAL = config.getBoolean("Currency.integer-bal");
    public int ROUNDING_MODE = config.getInt("Currency.rounding-mode");
    public String THOUSANDS_SEPARATOR = config.getString("Currency.thousands-separator");
    public String DISPLAY_FORMAT = config.getString("Currency.display-format");
    public String MAX_NUMBER = config.getString("Currency.max-number");
    public List<BigDecimal> FORMAT_BALANCE = null;
    public LinkedHashMap<BigDecimal, String> FORMAT_BALANCE_C = null;

    public SyncChannalType SYNCDATA_TYPE = SyncChannalType.OFF;

    public String SYNCDATA_SIGN = config.getString("SyncData.sign");

    //==================================================
    public final int FUTURE_TIMEOUT = Math.max(config.getInt("Thread.future-timeout"), 1);

    //==================================================
    public String RE_WORLD = config.getString("Region-Thread.world");
    public int RE_X = config.getInt("Region-Thread.range-x");
    public int RE_Y = config.getInt("Region-Thread.range-y");

    private int getrankingsize() {
        return Math.min(config.getInt("Settings.ranking-size"), 100);
    }


    private void setUUIDMode() {
        if (config.getString("UUID-mode").equalsIgnoreCase("Online")) {
            UUIDMODE = UUIDMode.ONLINE;
        } else if (config.getString("UUID-mode").equalsIgnoreCase("Offline")) {
            UUIDMODE = UUIDMode.OFFLINE;
            USERNAME_IGNORE_CASE = false;
        } else if (config.getString("UUID-mode").equalsIgnoreCase("SemiOnline")) {
            UUIDMODE = UUIDMode.SEMIONLINE;
        }
        XConomy.getInstance().logger(null, 0, UUIDMODE.toString());
    }

    private void setformatbalance() {
        FORMAT_BALANCE = new ArrayList<>();
        try {
            FORMAT_BALANCE_C = config.getConfigurationSectionSort("Currency.format-balance");
            FORMAT_BALANCE.addAll(FORMAT_BALANCE_C.keySet());
        } catch (Exception e) {
            e.printStackTrace();
            FORMAT_BALANCE = null;
            XConomy.getInstance().logger(null, 1, "Error getting balance custom format");
            return;
        }
        if (FORMAT_BALANCE.isEmpty()){
            FORMAT_BALANCE = null;
            XConomy.getInstance().logger(null, 1, "Error getting balance custom format");
        }
    }

    private void setnonplayeraccount() {
        if (NON_PLAYER_ACCOUNT) {
            if (config.getBoolean("non-player-account.whitelist.enable")) {
                NON_PLAYER_ACCOUNT_SUBSTRING = config.getStringList("non-player-account.whitelist.fields-list");
            }
        }
    }

    public void setSyncData() {
        if (!config.contains("SyncData.enable")) {
            ISOLDCONFIG = true;
            return;
        }


        if (config.getBoolean("SyncData.enable")) {
            // 跨服同步仅支持 Redis 通道（Velocity / BungeeCord 代理均无需额外安装插件）
            String channeltype = config.getString("SyncData.channel-type");
            if (channeltype != null && channeltype.equalsIgnoreCase("Redis")) {
                SYNCDATA_TYPE = SyncChannalType.REDIS;
            }
        }
    }

    private void setpaytips() {
        if (TRANSACTION_RECORD) {
            PAY_TIPS = config.getBoolean("Settings.offline-pay-transfer-tips");
        }
    }

    private void setliuchat() {
        LIUCHAT_CROSS_SERVER_PAY_TIPS = config.getBoolean("LiuChat.cross-server-pay-tips");
        LIUCHAT_TIP_ONLY_ONLINE = !config.contains("LiuChat.tip-only-online") || config.getBoolean("LiuChat.tip-only-online");
        String format = config.getString("LiuChat.pay-tip-format");
        if (format != null && !format.isEmpty()) {
            LIUCHAT_PAY_TIP_FORMAT = format;
        }
    }
}
