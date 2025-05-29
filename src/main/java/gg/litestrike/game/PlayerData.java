package gg.litestrike.game;

import static net.kyori.adventure.text.Component.translatable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class PlayerData {

	public String player;
	private int money = 0;
	public int kills = 0;
	public int deaths = 0;
	public int assists = 0;
	private int total_money_gained = 0;
	private int total_money_spent = 0;
	private int plants = 0;
	private int breaks = 0;
	public float total_damage = 0;
	public LastDamagerTracker ldt = new LastDamagerTracker();

	// this keeps track of assits in the current round for this player
	public Map<Player, Double> assist_list = new HashMap<Player, Double>();

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

	public void addMoney(int amt, Component reason) {
		Player p = Bukkit.getPlayer(player);
		if (p == null) {
			return;
		}
		p.sendMessage(translatable("crystalized.game.litestrike.money.receive").color(Litestrike.YELLOW)
				.append(Component.text(amt + "\uE104").color(TextColor.color(0x0ab1c4)))
				.append(reason.color(Litestrike.YELLOW)));
		money += amt;
		total_money_gained += amt;

		ScoreboardController.set_player_money(player, money);
	}

	public void giveMoneyBack(int amt) {
		addMoney(amt, translatable("crystalized.game.litestrike.money.selling"));
		total_money_gained -= amt;
		total_money_spent -= amt;
	}

	// returns false if not enought money was available
	// returns true if the money was successfully subtracted
	public boolean removeMoney(int amt) {
		if (amt > money) {
			return false;
		} else {
			if (!Litestrike.getInstance().getConfig().getBoolean("free-shop")) {
				money -= amt;
				total_money_spent += amt;
				ScoreboardController.set_player_money(player, money);
			}
			return true;
		}
	}

	public int getMoney() {
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
		Team team = Teams.get_team(player);
		int team_breaks = Teams.get_team_breaks(team);
		int team_plants = Teams.get_team_plants(team);
		return (int) Math.ceil((kills * 0.34) + (assists * 0.16) + ((team_breaks + team_plants) * 0.24));
	}

	public int getTotalMoneyGained() {
		return total_money_gained;
	}

	public int getTotalMoneySpent() {
		return total_money_spent;
	}

	public int getPlaced() {
		return plants;
	}

	public int getBroken() {
		return breaks;
	}
}

class LastDamagerTracker {
	private int counter = 0;
	private Player player;

	public LastDamagerTracker() {
		new BukkitRunnable() {
			@Override
			public void run() {
				counter--;
				if (player != null && counter == 1) {
					player = null;
				}
			}
		}.runTaskTimerAsynchronously(Litestrike.getInstance(), 10, 20);
	}

	public Player get_last_damager() {
		return player;
	}

	public void update_damager(Player p) {
		counter = 15;
		player = p;
	}

	public void clear_damager() {
		counter = 0;
		player = null;
	}
}

class PlayerDataComparator implements Comparator<PlayerData> {
	@Override
	public int compare(PlayerData arg0, PlayerData arg1) {
		return arg0.calc_player_score() - arg1.calc_player_score();
	}
}
