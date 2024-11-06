package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class PlayerData {

	public String player;
	private int money = 0;
	public int kills = 0;
	public int deaths = 0;
	public int assists = 0;

	public PlayerData(Player p) {
		player = p.getName();
	}

	// used for reseting money when switching teams
	public void removeMoney() {
		money = 0;
	}

	public void addMoney(int amt, String reason) {
		Player p = Bukkit.getPlayer(player);
		if (p == null) {
			return;
		}
		p.sendMessage(Component.text("ʏᴏᴜ ʀᴇᴄᴇɪᴠᴇᴅ ").color(Litestrike.YELLOW)
				.append(Component.text(amt + "\uE104").color(TextColor.color(0x0ab1c4)))
				.append(Component.text(" " + reason).color(Litestrike.YELLOW)));
		money += amt;

		ScoreboardController.set_player_money(player, money);
	}

	// returns false if not enought money was available
	// returns true if the money was successfully subtracted
	public boolean removeMoney(int amt) {
		if (amt >= money) {
			return false;
		} else {
			money -= amt;
			ScoreboardController.set_player_money(player, money);
			return true;
		}
	}

	public String toString() {
		return "\nname: " + player +
				"\nmoney: " + money +
				"\nkills: " + kills +
				"\ndeaths: " + deaths +
				"\n assists: " + assists;
	}

}
