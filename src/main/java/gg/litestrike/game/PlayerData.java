package gg.litestrike.game;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class PlayerData {

	public String player;
	private int money = 1000;
	public int kills = 0;
	public int deaths = 0;
	public int assists = 0;
	private int total_money = 0;
	private int plants = 0;
	private int breaks = 0;

	public PlayerData(Player p) {
		player = p.getName();
	}

	public void add_break() {
		breaks += 1;
	}

	public void add_plant() {
		plants += 1;
	}

	// used for reseting money when switching teams
	public void removeMoney() {
		money = 0;
		ScoreboardController.set_player_money(player, money);
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
		total_money += amt;

		ScoreboardController.set_player_money(player, money);
	}
	public static void addMoney(int amt, Player p){
		if (p == null) {
			return;
		}
		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
		pd.money += amt;
	}
	// returns false if not enought money was available
	// returns true if the money was successfully subtracted
	public boolean removeMoney(int amt) {
		if (amt > money) {
			return false;
		} else {
			money -= amt;
			ScoreboardController.set_player_money(player, money);
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

	public int calc_player_score() {
		return 2 * kills + 3 * breaks + 3 * plants + assists;
	}
}

class PlayerDataComparator implements Comparator<PlayerData> {
	@Override
	public int compare(PlayerData arg0, PlayerData arg1) {
		return arg0.calc_player_score() - arg1.calc_player_score();
	}
}
