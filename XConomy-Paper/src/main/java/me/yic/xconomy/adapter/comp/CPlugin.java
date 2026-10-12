package me.yic.xconomy.adapter.comp;


import me.yic.xconomy.XConomy;
import me.yic.xconomy.XConomyLoad;
import me.yic.xconomy.adapter.iPlugin;
import me.yic.xconomy.data.syncdata.PlayerData;
import me.yic.xconomy.utils.UUIDMode;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class CPlugin implements iPlugin {

    @Override
    public CPlayer getplayer(PlayerData pd) {
        Player p = null;
        if (pd != null) {
            if (XConomyLoad.Config.UUIDMODE.equals(UUIDMode.SEMIONLINE)){
                p = Bukkit.getPlayer(pd.getName());
            }else{
                p = Bukkit.getPlayer(pd.getUniqueId());
            }
        }
        return new CPlayer(p);
    }

    @Override
    public boolean getOnlinePlayersisEmpty(){
        return Bukkit.getOnlinePlayers().isEmpty();
    }

    @Override
    public int getOnlinePlayerSize(){
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public List<UUID> getOnlinePlayersUUIDs() {
        List<UUID> ol = new ArrayList<>();
        for (Player pp : Bukkit.getOnlinePlayers()) {
            ol.add(pp.getUniqueId());
        }
        return ol;
    }

    @Override
    public void broadcastMessage(String message) {
        Bukkit.broadcast(Component.text(message));
    }

    @Override
    public UUID NameToUUID(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null){
            return null;
        }
        return p.getUniqueId();
    }

    @Override
    public boolean isSync() {
        return Bukkit.isPrimaryThread();
    }
    @Override
    public void runTask(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(XConomy.getInstance(), scheduledTask -> runnable.run());
    }

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(XConomy.getInstance(), scheduledTask -> runnable.run());
    }

    @Override
    public void runTaskLaterAsynchronously(Runnable runnable, long time) {
        Bukkit.getAsyncScheduler().runDelayed(XConomy.getInstance(), scheduledTask -> runnable.run(), time, TimeUnit.MILLISECONDS);
    }
}
