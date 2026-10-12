/*
 *  This file (SQLWealthTax.java) is a part of project XConomy
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
package me.yic.xconomy.data.sql;

import me.yic.xconomy.XConomyLoad;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 财富税的持久化。
 * <p>
 * 两张表：
 * <ul>
 *   <li>{@code <xconomy>_tax}：每日结算状态，主键为日期，跨服靠它保证一天只结算一次；</li>
 *   <li>{@code <xconomy>_tax_record}：每人每日一条扣税记录，主键为（日期, UUID），
 *       既作为审计留痕，也保证结算中断后续跑时不会对同一人重复扣款。</li>
 * </ul>
 */
public class SQLWealthTax extends SQL {

    public static String taxTable() {
        return tableName + "_tax";
    }

    public static String taxRecordTable() {
        return tableName + "_tax_record";
    }

    private static String insertIgnore() {
        return XConomyLoad.DConfig.isMySQL() ? "insert ignore into " : "insert or ignore into ";
    }

    public static void ensureTables() {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("create table if not exists " + taxTable()
                    + "(tax_date varchar(10) not null, started_at bigint not null,"
                    + " completed tinyint(1) not null default 0, affected int not null default 0,"
                    + " total_amount double(20,2) not null default 0, primary key (tax_date))");
            statement.executeUpdate("create table if not exists " + taxRecordTable()
                    + "(tax_date varchar(10) not null, uid varchar(50) not null, player varchar(50) not null,"
                    + " wealth double(20,2) not null, amount double(20,2) not null, tax_time bigint not null,"
                    + " primary key (tax_date, uid))");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    /** 尝试为本日占位；成功（返回 true）表示本服负责结算。 */
    public static boolean startDay(String date) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(insertIgnore() + taxTable()
                + "(tax_date, started_at, completed, affected, total_amount) values(?,?,0,0,0)")) {
            statement.setString(1, date);
            statement.setLong(2, System.currentTimeMillis());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    public static boolean isCompleted(String date) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select completed from " + taxTable() + " where tax_date = ?")) {
            statement.setString(1, date);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() && rows.getInt(1) != 0;
            }
        } catch (SQLException e) {
            return false;
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    public static long startedAt(String date) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return System.currentTimeMillis();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select started_at from " + taxTable() + " where tax_date = ?")) {
            statement.setString(1, date);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : System.currentTimeMillis();
            }
        } catch (SQLException e) {
            return System.currentTimeMillis();
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    public static int affected(String date) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select affected from " + taxTable() + " where tax_date = ?")) {
            statement.setString(1, date);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    public static BigDecimal total(String date) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return BigDecimal.ZERO;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select total_amount from " + taxTable() + " where tax_date = ?")) {
            statement.setString(1, date);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    BigDecimal value = rows.getBigDecimal(1);
                    return value == null ? BigDecimal.ZERO : value;
                }
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            return BigDecimal.ZERO;
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    /** 记录某玩家当日已被征税；返回 false 表示当日已有记录（或写入失败），调用方应跳过扣款。 */
    public static boolean recordIfAbsent(String date, UUID uid, String player, BigDecimal wealth, BigDecimal amount) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(insertIgnore() + taxRecordTable()
                + "(tax_date, uid, player, wealth, amount, tax_time) values(?,?,?,?,?,?)")) {
            statement.setString(1, date);
            statement.setString(2, uid.toString());
            statement.setString(3, player == null ? "" : player);
            statement.setDouble(4, wealth.doubleValue());
            statement.setDouble(5, amount.doubleValue());
            statement.setLong(6, System.currentTimeMillis());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    /** 扣款失败时删除记录，以便后续重试。 */
    public static void removeRecord(String date, UUID uid) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from " + taxRecordTable() + " where tax_date = ? and uid = ?")) {
            statement.setString(1, date);
            statement.setString(2, uid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    public static void finishDay(String date, int affected, BigDecimal total) {
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "update " + taxTable() + " set completed = 1, affected = affected + ?, total_amount = total_amount + ? where tax_date = ?")) {
            statement.setInt(1, affected);
            statement.setDouble(2, total.doubleValue());
            statement.setString(3, date);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    /** 余额高于起征点的玩家，用于确定计税候选名单。 */
    public static List<WealthRow> listWealthy(BigDecimal threshold) {
        List<WealthRow> result = new ArrayList<>();
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return result;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select UID, player, balance from " + tableName + " where balance > ?")) {
            statement.setDouble(1, threshold.doubleValue());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new WealthRow(fromString(rows.getString(1)), rows.getString(2),
                            rows.getBigDecimal(3)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            database.closeHikariConnection(connection);
        }
        return result;
    }

    /** 读取任意账户余额；账户不存在返回 null。 */
    public static BigDecimal balanceOf(UUID uid) {
        if (uid == null) {
            return null;
        }
        Connection connection = database.getConnectionAndCheck();
        if (connection == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select balance from " + tableName + " where UID = ?")) {
            statement.setString(1, uid.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getBigDecimal(1) : null;
            }
        } catch (SQLException e) {
            return null;
        } finally {
            database.closeHikariConnection(connection);
        }
    }

    private static UUID fromString(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 计税候选项。 */
    public static final class WealthRow {
        public final UUID uuid;
        public final String name;
        public final BigDecimal balance;

        public WealthRow(UUID uuid, String name, BigDecimal balance) {
            this.uuid = uuid;
            this.name = name;
            this.balance = balance == null ? BigDecimal.ZERO : balance;
        }
    }
}
