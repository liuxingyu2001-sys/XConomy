package me.yic.xconomy.adapter.comp;


import me.yic.xconomy.adapter.iPlugin;
import me.yic.xconomy.data.syncdata.PlayerData;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class CPlugin implements iPlugin {
    @Override
    public CPlayer getplayer(PlayerData pd) {
        return null;
    }

    @Override
    public boolean getOnlinePlayersisEmpty() {
        return false;
    }

    @Override
    public int getOnlinePlayerSize(){
        return 0;
    }

    @Override
    public List<UUID> getOnlinePlayersUUIDs() {
        return null;
    }

    @Override
    public void broadcastMessage(String message) {

    }
    @Override
    public UUID NameToUUID(String name) {
        return null;
    }
    @Override
    public boolean isSync() {
        return false;
    }
    @Override
    public void runTask(Runnable ra) {

    }
    @Override
    public void runTaskAsynchronously(Runnable ra) {

    }

    @Override
    public void runTaskLaterAsynchronously(Runnable ra, long time) {

    }
}
