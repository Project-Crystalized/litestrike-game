package gg.litestrike.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

enum RoundState {
	PreRound,
	Running,
	PostRound,
	GameFinished,
}


// This will be created by something else, whenever there are 6+ people online and no game is currently going
public class GameController {
	public Teams teams = new Teams();
	private List<PlayerData> playerDatas;

	private int current_round_number = 0;
	private RoundState round_state = RoundState.PreRound;

	// the phase_timer starts counting up from the beginning of the round
	// after it reaches (15 * 20), the game is started. when the round winner is
	// determined its reset to 0 and counts until (5 * 20) for the postround time.
	// then the next round starts and it counts from 0 again
	private int phase_timer = 0;

	// after this round, the sides get switched
	private int switch_round = 4;

	// TODO private Bomb bomb = new Bomb();

	public GameController() {
		next_round();

		playerDatas = new ArrayList<PlayerData>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			PlayerData p = new PlayerData(player);
			playerDatas.add(p);
		}

		// This just calls update_game_state() once every second
		new BukkitRunnable() {
			@Override
			public void run() {
				Boolean game_over = update_game_state();
				if (game_over) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 20, 1);

	}
	
	// This is run every tick
	private Boolean update_game_state() {
		phase_timer += 1;

		// this is like a state-machine, it will check the current state, check a condition, and
		// if the condition is met, call a method to advance to the next state
		switch (round_state) {
			case RoundState.PreRound: {
				if (phase_timer == (15 * 20)) {
					start_round();
				}
			}
			break;
			case RoundState.Running: {
				if (determine_winner() != null) {
					finish_round();
				}
			}
			break;
			case RoundState.PostRound: {
				if (phase_timer == (5 * 20)) {
					if (current_round_number == switch_round * 2) {
						start_podium();
					} else {
						next_round();
					}
				}
			}
			break;
			case RoundState.GameFinished: {
				if (phase_timer == (20 * 20)) {
					finish_game();
					return true; // remove the update_game_state task
				}
			}
			break;
		}
		return false;
	}

	// this is called when we switch from PreRound to Running
	private void start_round() {
		round_state = RoundState.Running;
		phase_timer = 0;

		// play a sound and send messages to the teams
		Bukkit.getServer().playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 1));
		if (current_round_number == 1) {
			Audience.audience(teams.placers).sendMessage(Component.text("You are a ")
				.append(Litestrike.PLACER_TEXT)
				.append(Component.text("\nGo with your team and place the bomb at one of the designated bomb sites!!\n Or kill the enemy Team!")));
			Audience.audience(teams.breakers).sendMessage(Component.text("You are a ")
				.append(Litestrike.BREAKER_TEXT)
				.append(Component.text("\nKill the Enemy team and prevent them from placing the bomb!\n If they place the bomb, break it.")));
		}

		// remove the border
		Litestrike.getInstance().mapdata.lowerBorder(Bukkit.getWorld("world"));

		// TODO remove shop item
	}

	// this is called when we switch from Running to PostRound
	private void finish_round() {
		round_state = RoundState.PostRound;
		phase_timer = 0;
		Team winner = determine_winner();

		// play sound
		Bukkit.getServer().playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 1));

		// announce winner
		Component winner_component;
		if (winner == Team.Placer) {
			winner_component = Litestrike.PLACER_TEXT;
		} else {
			winner_component = Litestrike.BREAKER_TEXT;
		}
		Bukkit.getServer().sendMessage(Component.text("The winner was the ").append(winner_component).append(Component.text(" team!")));

		// give money
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (teams.get_team(p) == winner) {
				getPlayerData(p).addMoney(2000, "For winning the round!");
			} else {
				getPlayerData(p).addMoney(1200, "For loosing the round.");
			}
		}
	}

	// this is called when the last round is over and the podium should begin
	private void start_podium() {
		round_state = RoundState.GameFinished;
		phase_timer = 0;
		// TODO
	};

	// this is called when we go from PostRound to PreRound and when the first round starts
	private void next_round() {
		round_state = RoundState.PreRound;
		phase_timer = 0;
		current_round_number += 1;

		World w = Bukkit.getWorld("world");
		Location placer_spawn = Litestrike.getInstance().mapdata.get_placer_spawn(w);
		Location breaker_spawn = Litestrike.getInstance().mapdata.get_breaker_spawn(w);

		// raise border
		Litestrike.getInstance().mapdata.raiseBorder(w);

		// teleport everyone to spawn and set to survival
		for (Player p : teams.breakers) {
			p.teleport(breaker_spawn);
			p.lookAt(placer_spawn.x(), placer_spawn.y(), placer_spawn.z(), LookAnchor.EYES);
			p.setGameMode(GameMode.SURVIVAL);
		}
		for (Player p : teams.placers) {
			p.teleport(placer_spawn);
			p.lookAt(breaker_spawn.x(), breaker_spawn.y(), breaker_spawn.z(), LookAnchor.EYES);
			p.setGameMode(GameMode.SURVIVAL);
		}

		// TODO give armor and weapons
		tmp_give_default_armor();

		// TODO give shop item

	}

	// is called when the game will be finished after the podium
	private void finish_game() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.kick();
		}
		Litestrike.getInstance().game_controller = null;
	}

	// this will determine the winner of the round and return it.
	// if the round isnt over, it will return null
	private Team determine_winner() {
		if (phase_timer == (120 * 20)) {
			return Team.Placer;
		}

		// check if all placers are alive
		Boolean all_placers_dead = true;
		for (Player p : teams.placers) {
			// if a placer isnt in spectator mode they are alive
			if (p.getGameMode() != GameMode.SPECTATOR) {
				all_placers_dead = false;
				break;
			}
		}
		if (all_placers_dead) {
			return Team.Breaker;
		}

		Boolean all_breakers_dead = true;
		for (Player p : teams.breakers) {
			// if a breaker isnt in spectator mode they are alive
			if (p.getGameMode() != GameMode.SPECTATOR) {
				all_breakers_dead = false;
				break;
			}
		}
		if (all_breakers_dead) {
			return Team.Placer;
		}

		// TODO check_bomb_exploded ||
		// TODO check bomb_mined ||

		return null;
	}

	// this gives everyone default armor for now
	// TODO remove this once shop system is implemented
	private void tmp_give_default_armor() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			PlayerInventory inv = p.getInventory();
			inv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
			inv.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
			inv.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
			inv.setBoots(new ItemStack(Material.LEATHER_BOOTS));
			inv.setItem(0, new ItemStack(Material.STONE_SWORD));
		}
	}

	public PlayerData getPlayerData(Player p) {
		for (PlayerData pd : playerDatas) {
			if (pd.player == p) {
				return pd;
			}
		}
		return null;
	}
}
