/*
 *  This file (RunWealthTax.java) is a part of project XConomy
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

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.yic.xconomy.XConomy;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

/**
 * 财富税定时检查（Paper 异步调度器实现）。
 * <p>
 * 每 60 秒检查一次是否到达结算时间；是否真正结算由数据库每日占位决定，
 * 因此多台子服同时跑也只会结算一次。
 */
public class RunWealthTax {

    private static ScheduledTask task = null;

    public static void runstart() {
        task = Bukkit.getAsyncScheduler().runAtFixedRate(XConomy.getInstance(),
                ScheduledTask -> new me.yic.xconomy.task.WealthTax().run(),
                20L * 1000, 60L * 1000, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
