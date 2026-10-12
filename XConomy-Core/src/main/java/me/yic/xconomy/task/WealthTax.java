/*
 *  This file (WealthTax.java) is a part of project XConomy
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
package me.yic.xconomy.task;

import me.yic.xconomy.AdapterManager;
import me.yic.xconomy.XConomy;
import me.yic.xconomy.XConomyLoad;
import me.yic.xconomy.adapter.comp.CChat;
import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.caches.Cache;
import me.yic.xconomy.data.sql.SQLWealthTax;
import me.yic.xconomy.lang.MessagesManager;
import me.yic.xconomy.utils.ResidenceHook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 财富税：每日对「合并后的玩家财富」按比例征税并销毁，用于回收通胀。
 * <p>
 * 财富 = 经济账户余额 + 该玩家名下所有 Residence 领地银行余额。
 * 扣除顺序：先扣经济账户，不足部分再从领地银行扣。
 * <p>
 * 多台子服共用同一 MySQL，通过 {@code <xconomy>_tax} 主键占位保证一天只结算一次；
 * {@code <xconomy>_tax_record} 保证中断续跑时不会对同一人重复扣款。
 * <p>
 * 检查在异步线程执行，真正扣款（涉及 Residence 与事件）切回主线程。
 */
public class WealthTax implements Runnable {

    public enum Status {
        /** 已开始结算（主线程执行中）。 */
        STARTED,
        DISABLED,
        NOT_YET,
        NOT_EXECUTOR,
        IN_PROGRESS,
        ALREADY_DONE,
        ERROR
    }

    /** 结算进行中超过该时长才允许其它子服接管（避免主服卡住时永久不结算）。 */
    private static final long RESUME_STALE_MS = 5 * 60 * 1000L;

    private static volatile String appliedDate;

    @Override
    public void run() {
        execute(false);
    }

    /**
     * @param force true = 管理员手动触发，跳过时间、生效日期、执行服与「今日已完成」判断，
     *              但逐人记录仍然保证不会重复扣款。
     */
    public static Status execute(boolean force) {
        if (!XConomyLoad.Config.WEALTH_TAX_ENABLE) {
            return Status.DISABLED;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            String today = now.toLocalDate().toString();

            if (!force) {
                if (SQLWealthTax.isCompleted(today)) {
                    refreshCacheOnce(today);
                    return Status.ALREADY_DONE;
                }
                if (!executorMatches()) {
                    return Status.NOT_EXECUTOR;
                }
                if (!afterEffectiveDate(now.toLocalDate())) {
                    return Status.NOT_YET;
                }
                if (now.toLocalTime().isBefore(parseTime(XConomyLoad.Config.WEALTH_TAX_TIME))) {
                    return Status.NOT_YET;
                }
                boolean claimed = SQLWealthTax.startDay(today);
                if (!claimed && System.currentTimeMillis() - SQLWealthTax.startedAt(today) < RESUME_STALE_MS) {
                    return Status.IN_PROGRESS;
                }
            } else {
                // 手动触发：确保当日状态行存在，逐人记录兜底防重复
                SQLWealthTax.startDay(today);
            }

            AdapterManager.PLUGIN.runTask(() -> executeOnMainThread(today));
            return Status.STARTED;
        } catch (Throwable t) {
            XConomy.getInstance().logger(null, 1, "[财富税] 结算异常: " + t.getMessage());
            t.printStackTrace();
            return Status.ERROR;
        }
    }

    private static void executeOnMainThread(String today) {
        try {
            // 清空本服缓存，强制从数据库读取最新余额，避免用旧缓存计算税基
            Cache.clearCache();

            BigDecimal threshold = XConomyLoad.Config.WEALTH_TAX_THRESHOLD;
            double rate = XConomyLoad.Config.WEALTH_TAX_RATE;
            boolean excess = XConomyLoad.Config.WEALTH_TAX_EXCESS;

            Map<UUID, SQLWealthTax.WealthRow> candidates = new LinkedHashMap<>();
            for (SQLWealthTax.WealthRow row : SQLWealthTax.listWealthy(threshold)) {
                if (row.uuid != null) {
                    candidates.put(row.uuid, row);
                }
            }

            Map<UUID, ResidenceHook.OwnerBanks> banks = Collections.emptyMap();
            if (XConomyLoad.Config.WEALTH_TAX_RESIDENCE && ResidenceHook.available()) {
                banks = ResidenceHook.collect();
                for (Map.Entry<UUID, ResidenceHook.OwnerBanks> entry : banks.entrySet()) {
                    if (!candidates.containsKey(entry.getKey())) {
                        BigDecimal personal = SQLWealthTax.balanceOf(entry.getKey());
                        candidates.put(entry.getKey(), new SQLWealthTax.WealthRow(
                                entry.getKey(), entry.getValue().ownerName,
                                personal == null ? BigDecimal.ZERO : personal));
                    }
                }
            }

            int count = 0;
            BigDecimal collected = BigDecimal.ZERO;
            for (SQLWealthTax.WealthRow row : candidates.values()) {
                ResidenceHook.OwnerBanks ownerBanks = banks.get(row.uuid);
                BigDecimal bankTotal = ownerBanks == null
                        ? BigDecimal.ZERO : BigDecimal.valueOf(ownerBanks.total());
                BigDecimal combined = row.balance.add(bankTotal);
                if (combined.compareTo(threshold) <= 0) {
                    continue;
                }
                BigDecimal taxable = excess ? combined.subtract(threshold) : combined;
                BigDecimal tax = DataFormat.formatBigDecimal(taxable.multiply(BigDecimal.valueOf(rate)));
                if (tax.signum() <= 0) {
                    continue;
                }
                // 每人每日只扣一次
                if (!SQLWealthTax.recordIfAbsent(today, row.uuid, row.name, combined, tax)) {
                    continue;
                }
                try {
                    BigDecimal actual = BigDecimal.ZERO;
                    BigDecimal fromPersonal = row.balance.min(tax);
                    if (fromPersonal.signum() > 0) {
                        DataCon.changeplayerdata("WEALTH_TAX", row.uuid, fromPersonal, false,
                                "wealth-tax " + today, null);
                        actual = actual.add(fromPersonal);
                    }
                    BigDecimal remaining = tax.subtract(fromPersonal);
                    if (remaining.signum() > 0 && ownerBanks != null) {
                        double taken = ResidenceHook.deduct(ownerBanks, remaining.doubleValue());
                        actual = actual.add(BigDecimal.valueOf(taken));
                    }
                    if (actual.signum() > 0) {
                        count++;
                        collected = collected.add(actual);
                    }
                } catch (Throwable t) {
                    // 扣款失败：撤销记录以便后续重试
                    SQLWealthTax.removeRecord(today, row.uuid);
                    XConomy.getInstance().logger(null, 1, "[财富税] 扣除失败 " + row.name + ": " + t.getMessage());
                }
            }

            SQLWealthTax.finishDay(today, count, collected);
            appliedDate = today;

            if (!XConomyLoad.Config.WEALTH_TAX_DESTROY) {
                String account = XConomyLoad.Config.WEALTH_TAX_ACCOUNT;
                if (account != null && !account.trim().isEmpty() && collected.signum() > 0) {
                    DataCon.changeaccountdata("WEALTH_TAX", account.trim(), collected, true,
                            "wealth-tax " + today);
                }
            }

            if (count > 0 && XConomyLoad.Config.WEALTH_TAX_BROADCAST) {
                String key = XConomyLoad.Config.WEALTH_TAX_DESTROY ? "tax_broadcast" : "tax_broadcast_account";
                String def = XConomyLoad.Config.WEALTH_TAX_DESTROY
                        ? "&6[财富税] &e今日对 &f%count% &e名玩家征收 &f%amount% &e金币，税款已销毁。"
                        : "&6[财富税] &e今日对 &f%count% &e名玩家征收 &f%amount% &e金币。";
                AdapterManager.PLUGIN.broadcastMessage(taxMessage(key, def,
                        "%count%", String.valueOf(count),
                        "%amount%", DataFormat.shown(collected)));
            }
            XConomy.getInstance().logger(null, 0, "[财富税] 结算完成：玩家 "
                    + count + " 名，共 " + DataFormat.shown(collected));
        } catch (Throwable t) {
            XConomy.getInstance().logger(null, 1, "[财富税] 结算异常: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /** 非执行服在观察到当日已由其它服结算后，刷新本服缓存。 */
    private static void refreshCacheOnce(String date) {
        if (!date.equals(appliedDate)) {
            Cache.clearCache();
            appliedDate = date;
        }
    }

    private static boolean executorMatches() {
        String executor = XConomyLoad.Config.WEALTH_TAX_EXECUTOR;
        if (executor == null || executor.trim().isEmpty()) {
            return true;
        }
        String serverId = XConomyLoad.Config.WEALTH_TAX_SERVER_ID;
        return serverId != null && serverId.trim().equalsIgnoreCase(executor.trim());
    }

    private static boolean afterEffectiveDate(LocalDate today) {
        String effective = XConomyLoad.Config.WEALTH_TAX_EFFECTIVE;
        if (effective == null || effective.trim().isEmpty()) {
            return true;
        }
        try {
            return !today.isBefore(LocalDate.parse(effective.trim()));
        } catch (Exception ignored) {
            return true;
        }
    }

    private static LocalTime parseTime(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return LocalTime.MIDNIGHT;
            }
            String[] parts = value.trim().split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return LocalTime.MIDNIGHT;
            }
            return LocalTime.of(hour, minute);
        } catch (Exception ignored) {
            return LocalTime.MIDNIGHT;
        }
    }

    /** 读取语言项并替换占位符、翻译颜色；缺失时使用兜底中文。 */
    private static String taxMessage(String key, String fallback, String... placeholders) {
        String raw = MessagesManager.messageFile == null ? null : MessagesManager.messageFile.getString(key);
        if (raw == null) {
            raw = fallback;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace(placeholders[i], placeholders[i + 1]);
        }
        return CChat.translateAlternateColorCodes('&', raw);
    }
}
