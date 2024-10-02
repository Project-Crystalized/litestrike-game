package gg.litestrike.game;
import org.bukkit.entity.Player;

public class PlayerData {

	public Player player;
	private int money = 0;
	public int kills = 0;
	public int deaths = 0;
	public int assists = 0;

	public PlayerData(Player p){
		player = p;
	}

	public void addMoney(int amt, String reason)  {
		player.sendMessage("You received " + amt + "g");
		money += amt;
	}

	// returns false if not enought money was available
	// returns true if the money was successfully subtracted
	public boolean removeMoney(int amt) {
		if (amt >= money) {
			return false;
		} else {
			money -= amt;
			return true;
		}
	}

	// TODO for debugging maybe: 
	// public String toString() {}

}
