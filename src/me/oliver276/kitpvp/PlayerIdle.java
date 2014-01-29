package me.oliver276.kitpvp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerIdle extends BukkitRunnable{
    private final JavaPlugin plugin;
    private static ArrayList<Player> array = new ArrayList<Player>();
    private static HashMap<Player, Integer> map = new HashMap<Player, Integer>();
    public static void addPlayer(Player player){
        map.put(player,0);
    }
    public static void removePlayer(Player player){
        map.remove(player);
    }
    public static void HeMoved(Player player){
        map.remove(player);
        map.put(player,0);
    }

    public PlayerIdle(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
       for (Player player : map.keySet()){
           if (player == null) continue;
           int i = map.remove(player);
           map.put(player,i + 1);
           if (map.get(player) == 60){
               player.sendMessage(ChatColor.RED + "You'll be killed for idling in 10 seconds");
           }
           if (map.get(player) == 70){
               player.setHealth(0);
               map.remove(player);
               map.put(player,0);
           }
       }
    }
}
