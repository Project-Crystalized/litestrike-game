package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerData {

	public String player;
	private int money = 1000;
	public int kills = 0;
	public int deaths = 0;
	public int assists = 0;

	public PlayerData(Player p) {
		player = p.getName();
	}

	public void addMoney(int amt, String reason) {
		Player p = Bukkit.getPlayer(player);
		if (p == null) {
			return;
		}
		p.sendMessage("You received " + amt + "g. " + reason);
		money += amt;
	}

	// returns false if not enought money was available
	// returns true if the money was successfully subtracted
	public boolean removeMoney(int amt) {
		if (amt > money) {
			return false;
		} else {
			money -= amt;
			return true;
		}
	}

	public int getMoney(){
		return this.money;
	}

	public String toString() {
		return "\nname: " + player +
				"\nmoney: " + money +
				"\nkills: " + kills +
				"\ndeaths: " + deaths +
				"\n assists: " + assists;
	}

}
