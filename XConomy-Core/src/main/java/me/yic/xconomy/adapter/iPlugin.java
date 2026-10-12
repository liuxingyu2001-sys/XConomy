package me.yic.xconomy.adapter;


import me.yic.xconomy.adapter.comp.CPlayer;
import me.yic.xconomy.data.syncdata.PlayerData;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public interface iPlugin {
    CPlayer getplayer(PlayerData pd);
    boolean getOnlinePlayersisEmpty();

    List<UUID> getOnlinePlayersUUIDs();

    int getOnlinePlayerSize();

    void broadcastMessage(String message);

    UUID NameToUUID(String name);

    boolean isSync();

    /** 在主线程执行（Residence 等 Bukkit API 只能在主线程调用）。 */
    void runTask(Runnable ra);

    void runTaskAsynchronously(Runnable ra);

    void runTaskLaterAsynchronously(Runnable ra, long time);
}
