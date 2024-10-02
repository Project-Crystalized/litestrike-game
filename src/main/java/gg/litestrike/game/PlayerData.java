package gg.litestrike.game;
import org.bukkit.entity.Player;
public class PlayerData {
    //here all player data: uuid, money count, kills, deaths, assists
    // cool facts
     public Player player;
     public int money = 0;
     public int kills = 0;
     public int deaths = 0;
     public int assists = 0;
    public PlayerData(Player p){
       player = p;
    }
}
