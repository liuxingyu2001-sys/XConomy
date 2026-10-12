/*
 *  This file (ResidenceHook.java) is a part of project XConomy
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
package me.yic.xconomy.utils;

import me.yic.xconomy.XConomy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Residence（领地）银行桥接（纯反射，未安装 Residence 时静默不可用）。
 * <p>
 * 领地银行里的钱不在经济插件账上，是常见的避税藏钱处，财富税需要把它并入
 * 玩家财富后再计税，必要时直接从领地银行扣除。
 * <p>
 * Residence 的数据结构不是线程安全的，{@link #collect()} 与
 * {@link #deduct} 必须在主线程调用。
 */
public class ResidenceHook {

    /** 单个领地银行快照。 */
    public static final class BankRef {
        public final String residence;
        public final Object bank;
        public double balance;

        BankRef(String residence, Object bank, double balance) {
            this.residence = residence;
            this.bank = bank;
            this.balance = balance;
        }
    }

    /** 某玩家名下的全部领地银行。 */
    public static final class OwnerBanks {
        public final String ownerName;
        public final List<BankRef> banks = new ArrayList<>();

        OwnerBanks(String ownerName) {
            this.ownerName = ownerName;
        }

        public double total() {
            double sum = 0;
            for (BankRef ref : banks) {
                sum += ref.balance;
            }
            return sum;
        }
    }

    private static boolean available;
    private static Method getInstance;
    private static Method getResidenceManager;
    private static Method getResidences;
    private static Method getSubzones;
    private static Method getBank;
    private static Method getStoredMoneyD;
    private static Method setStoredMoney;
    private static Method getOwnerUUID;
    private static Method getOwner;
    private static Method getResidenceName;

    private static synchronized boolean init() {
        // 只在成功时缓存；若 Residence 晚于本插件加载，后续调用会重试
        if (available) {
            return true;
        }
        try {
            Class<?> residenceClass = Class.forName("com.bekvon.bukkit.residence.Residence");
            Class<?> managerClass = Class.forName("com.bekvon.bukkit.residence.protection.ResidenceManager");
            Class<?> claimedClass = Class.forName("com.bekvon.bukkit.residence.protection.ClaimedResidence");
            Class<?> bankClass = Class.forName("com.bekvon.bukkit.residence.economy.ResidenceBank");
            getInstance = residenceClass.getMethod("getInstance");
            getResidenceManager = residenceClass.getMethod("getResidenceManager");
            getResidences = managerClass.getMethod("getResidences");
            getSubzones = claimedClass.getMethod("getSubzones");
            getBank = claimedClass.getMethod("getBank");
            getStoredMoneyD = bankClass.getMethod("getStoredMoneyD");
            setStoredMoney = bankClass.getMethod("setStoredMoney", double.class);
            getOwnerUUID = claimedClass.getMethod("getOwnerUUID");
            getOwner = claimedClass.getMethod("getOwner");
            getResidenceName = claimedClass.getMethod("getResidenceName");
            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        return available;
    }

    public static boolean available() {
        return init();
    }

    /**
     * 收集所有领地银行（含子领地），按主人聚合。主线程调用。
     */
    public static Map<UUID, OwnerBanks> collect() {
        Map<UUID, OwnerBanks> result = new LinkedHashMap<>();
        if (!init()) {
            return result;
        }
        try {
            Object residence = getInstance.invoke(null);
            Object manager = getResidenceManager.invoke(residence);
            Object raw = getResidences.invoke(manager);
            if (!(raw instanceof Map)) {
                return result;
            }
            Map<?, ?> map = (Map<?, ?>) raw;
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object value : new ArrayList<>(map.values())) {
                walk(value, result, visited);
            }
        } catch (Throwable t) {
            XConomy.getInstance().logger(null, 1, "[财富税] 读取 Residence 领地银行失败: " + t.getMessage());
        }
        return result;
    }

    private static void walk(Object claimed, Map<UUID, OwnerBanks> result, Set<Object> visited) {
        if (claimed == null || !visited.add(claimed)) {
            return;
        }
        try {
            Object bank = getBank.invoke(claimed);
            if (bank != null) {
                Double money = (Double) getStoredMoneyD.invoke(bank);
                if (money != null && money > 0) {
                    UUID owner = (UUID) getOwnerUUID.invoke(claimed);
                    if (owner != null) {
                        String ownerName = (String) getOwner.invoke(claimed);
                        String residenceName = String.valueOf(getResidenceName.invoke(claimed));
                        OwnerBanks ownerBanks = result.get(owner);
                        if (ownerBanks == null) {
                            ownerBanks = new OwnerBanks(ownerName);
                            result.put(owner, ownerBanks);
                        }
                        ownerBanks.banks.add(new BankRef(residenceName, bank, money));
                    }
                }
            }
            Object subzones = getSubzones.invoke(claimed);
            if (subzones instanceof List) {
                for (Object sub : new ArrayList<>((List<?>) subzones)) {
                    walk(sub, result, visited);
                }
            }
        } catch (Throwable ignored) {
            // 单个领地读取失败不影响其它领地
        }
    }

    /**
     * 从该主人的领地银行中扣除金额，返回实际扣除的数额。主线程调用。
     */
    public static double deduct(OwnerBanks ownerBanks, double amount) {
        if (ownerBanks == null || amount <= 0) {
            return 0;
        }
        double remaining = amount;
        for (BankRef ref : ownerBanks.banks) {
            if (remaining <= 0) {
                break;
            }
            double take = Math.min(ref.balance, remaining);
            if (take <= 0) {
                continue;
            }
            try {
                setStoredMoney.invoke(ref.bank, ref.balance - take);
                ref.balance -= take;
                remaining -= take;
            } catch (Throwable t) {
                XConomy.getInstance().logger(null, 1, "[财富税] 扣除领地银行失败 "
                        + ref.residence + ": " + t.getMessage());
            }
        }
        return amount - remaining;
    }
}
