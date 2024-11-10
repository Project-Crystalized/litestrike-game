package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static net.kyori.adventure.text.Component.text;

enum RoundState {
	PreRound,
	Running,
	PostRound,
	GameFinished,
}

// This will be created by something else, whenever there are 6+ people online
// and no game is currently going
public class GameController {
	public Teams teams = new Teams();
	public List<PlayerData> playerDatas;
	public Bomb bomb;

	private int current_round_number = 0;
	public RoundState round_state = RoundState.PreRound;

	public List<Team> round_results = new ArrayList<>();

	// the phase_timer starts counting up from the beginning of the round
	// after it reaches (15 * 20), the game is started. when the round winner is
	// determined its reset to 0 and counts until (5 * 20) for the postround time.
	// then the next round starts and it counts from 0 again
	public int phase_timer = 0;

	// after this round, the sides get switched
	public final static int switch_round = 4;

	public final static int PRE_ROUND_TIME = (15 * 20);
	public final static int RUNNING_TIME = (180 * 20);
	public final static int POST_ROUND_TIME = (5 * 20);
	public final static int FINISH_TIME = (20 * 20);

	public GameController() {
		playerDatas = new ArrayList<PlayerData>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			PlayerData p = new PlayerData(player);
			playerDatas.add(p);
		}

		// setup scoreboard and bossbar
		ScoreboardController.setup_scoreboard(teams);
		Litestrike.getInstance().bbd.showBossBar();

		new BukkitRunnable() {
			@Override
			public void run() {
				next_round();
			}
		}.runTaskLater(Litestrike.getInstance(), 1);

		// This just calls update_game_state() once every second
		new BukkitRunnable() {
			@Override
			public void run() {
				boolean game_over = update_game_state();
				if (game_over) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.kick();
					}
					Litestrike.getInstance().game_controller = null;
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 20, 1);

	}

	// This is run every tick
	private boolean update_game_state() {
		phase_timer += 1;

		// if round_state is GameFinished then the podium is already running
		if (check_if_podium_start() != null) {
			start_podium(check_if_podium_start());
		}

		// this is like a state-machine, it will check the current state, check a
		// condition, and
		// if the condition is met, call a method to advance to the next state
		switch (round_state) {
			case RoundState.PreRound: {
				if (phase_timer == PRE_ROUND_TIME) {
					start_round();
				}
			}
				break;
			case RoundState.Running: {
				if (determine_winner() != null) {
					finish_round(determine_winner());
				}
			}
				break;
			case RoundState.PostRound: {
				if (phase_timer == POST_ROUND_TIME) {
					next_round();
				}
			}
				break;
			case RoundState.GameFinished: {
				if (phase_timer == FINISH_TIME) {
					return true; // remove the update_game_state task
				}
			}
				break;
		}
		return false;
	}

	// this checks if the podium should start
	private Team check_if_podium_start() {

		// if round_state is GameFinished then podium is already started
		if  (round_state == RoundState.GameFinished) {
			return null;
		}

		// end if a team has won
		int placer_wins_amt = 0;
		int breaker_wins_amt = 0;
		for (gg.litestrike.game.Team w : round_results) {
			if (w == gg.litestrike.game.Team.Placer) {
				placer_wins_amt += 1;
			} else {
				breaker_wins_amt += 1;
			}
		}
		
		// if the enemy team is empty, or if the team has reached the required rounds, win
		if (teams.get_placers().size() == 0 || breaker_wins_amt == switch_round + 1) {
			return Team.Breaker;
		}
		if (teams.get_breakers().size() == 0 || placer_wins_amt == switch_round + 1) {
			return Team.Placer;
		}

		// end if a team has reached the required wins
		return null;
	}

	// this is called when we switch from PreRound to Running
	private void start_round() {
		round_state = RoundState.Running;
		phase_timer = 0;

		// send messages to the teams
		if (current_round_number == 1) {
			for (Player p : teams.get_placers()) {
				p.sendMessage(text("\n ʏᴏᴜ ᴀʀᴇ ᴀ ").color(Litestrike.YELLOW)
						.append(Litestrike.PLACER_TEXT)
						.append(text(
								"\n ɢᴏ ᴡɪᴛʜ ʏᴏᴜʀ ᴛᴇᴀᴍ ᴀɴᴅ ᴘʟᴀᴄᴇ ᴛʜᴇ ʙᴏᴍʙ ᴀᴛ ᴏɴᴇ ᴏғ ᴛʜᴇ ᴅᴇsɪɢɴᴀᴛᴇᴅ ʙᴏᴍʙ sɪᴛᴇs!!\n ᴏʀ ᴋɪʟʟ ᴛʜᴇ ᴇɴᴇᴍʏ Tᴇᴀᴍ!\n")
								.color(Litestrike.YELLOW)));
			}
			Audience.audience(teams.get_breakers()).sendMessage(text("\n ʏᴏᴜ ᴀʀᴇ ᴀ ").color(Litestrike.YELLOW)
					.append(Litestrike.BREAKER_TEXT)
					.append(text(
							"\n ᴋɪʟʟ ᴛʜᴇ Eɴᴇᴍʏ ᴛᴇᴀᴍ ᴀɴᴅ ᴘʀᴇᴠᴇɴᴛ ᴛʜᴇᴍ ғʀᴏᴍ ᴘʟᴀᴄɪɴɢ ᴛʜᴇ ʙᴏᴍʙ!\n ɪғ ᴛʜᴇʏ ᴘʟᴀᴄᴇ ᴛʜᴇ ʙᴏᴍʙ, ʙʀᴇᴀᴋ ɪᴛ.\n")
							.color(Litestrike.YELLOW)));
		}

		// remove the border
		Litestrike.getInstance().mapdata.lowerBorder(Bukkit.getWorld("world"));

		// TODO remove shop item
	}

	// this is called when we switch from Running to PostRound
	private void finish_round(Team winner) {
		if (winner == null) {
			Bukkit.getLogger().severe("critical error: finish_round() was called with null as input");
		}
		round_state = RoundState.PostRound;
		phase_timer = 0;
		bomb.remove();
		bomb = null;

		round_results.add(winner);

		ScoreboardController.set_win_display(round_results);

		// announce winner
		Component winner_component;
		if (winner == Team.Placer) {
			winner_component = Litestrike.PLACER_TEXT;
		} else {
			winner_component = Litestrike.BREAKER_TEXT;
		}
		Bukkit.getServer()
				.sendMessage(text("\nᴛʜᴇ ").color(Litestrike.YELLOW).append(winner_component)
						.append(text(" ᴛᴇᴀᴍ ᴡᴏɴ ʀᴏᴜɴᴅ ").color(Litestrike.YELLOW)).append(text(current_round_number))
						.append(text("!\n").color(Litestrike.YELLOW)));

		// give money and play sound
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (teams.get_team(p) == winner) {
				getPlayerData(p).addMoney(1000, "ғᴏʀ ᴡɪɴɴɪɴɢ ᴛʜᴇ ʀᴏᴜɴᴅ.");
				SoundEffects.round_won(p);
			} else {
				getPlayerData(p).addMoney(200, "ғᴏʀ ʟᴏᴏsɪɴɢ ᴛʜᴇ ʀᴏᴜɴᴅ.");
				SoundEffects.round_lost(p);
			}
		}
	}

	// this is called when the last round is over and the podium should begin
	private void start_podium(Team team) {
		round_state = RoundState.GameFinished;
		phase_timer = 0;
		if (bomb != null) {
			bomb.remove();
			bomb = null;
		}

		World w = Bukkit.getWorld("world");

		print_result_table(team);
		teleport_players_podium(w);
		// TODO
		// SoundEffects.round_end_sound();

		// summon fireworks after 5 secs
		new BukkitRunnable() {
			@Override
			public void run() {
				// summon firework
			}
		}.runTaskLater(Litestrike.getInstance(), (20 * 5));
	};

	// this is called when we go from PostRound to PreRound and when the first round
	// starts
	private void next_round() {
		round_state = RoundState.PreRound;
		phase_timer = 0;
		current_round_number += 1;

		World w = Bukkit.getWorld("world");
		Location placer_spawn = Litestrike.getInstance().mapdata.get_placer_spawn(w);
		Location breaker_spawn = Litestrike.getInstance().mapdata.get_breaker_spawn(w);

		if (current_round_number == switch_round + 1) {
			Bukkit.getServer().sendMessage(text("ꜱᴡɪᴛᴄʜɪɴɢ ꜱɪᴅᴇꜱ!!").color(Litestrike.YELLOW));
			teams.switch_teams();
			for (PlayerData pd : playerDatas) {
				pd.removeMoney();
			}
			for (int i = 0; i < round_results.size(); i++) {
				if (round_results.get(i) == Team.Placer) {
					round_results.set(i, Team.Breaker);
				} else {
					round_results.set(i, Team.Placer);
				}
			}
			ScoreboardController.setup_scoreboard(teams);
			ScoreboardController.set_win_display(round_results);
			// TODO shop clear inventory
		}

		// raise border
		Litestrike.getInstance().mapdata.raiseBorder(w);

		// teleport everyone to spawn and make them look at enemy spawn
		for (Player p : teams.get_breakers()) {
			p.teleport(breaker_spawn);
			p.lookAt(placer_spawn.x(), placer_spawn.y(), placer_spawn.z(), LookAnchor.EYES);
		}
		for (Player p : teams.get_placers()) {
			p.teleport(placer_spawn);
			p.lookAt(breaker_spawn.x(), breaker_spawn.y(), breaker_spawn.z(), LookAnchor.EYES);
		}

		// heal and set everyone to survival
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.setGameMode(GameMode.SURVIVAL);
			p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		}

		// sound effect has a cooldown, so we call it here instead of in round_start
		SoundEffects.round_start();

		// TODO give armor and weapons
		tmp_give_default_armor();

		// give bomb to a random player
		// generate int between 0 and placer teams size
		int random = ThreadLocalRandom.current().nextInt(0, teams.get_placers().size());
		Bomb.give_bomb(teams.get_placers().get(random).getInventory());

		for (Player p : Bukkit.getOnlinePlayers()) {
			getPlayerData(p).addMoney(1000, "");
		}

		// TODO give shop item

	}

	// this will determine the winner of the round and return it.
	// if the round isnt over, it will return null
	private Team determine_winner() {
		if (bomb instanceof PlacedBomb) {
			PlacedBomb pb = (PlacedBomb) bomb;
			if (pb.is_detonated) {
				return Team.Placer;
			}
			if (pb.is_broken) {
				return Team.Breaker;
			}
		}

		boolean all_breakers_dead = true;
		for (Player p : teams.get_breakers()) {
			// if a breaker isnt in spectator mode they are alive
			if (p.getGameMode() != GameMode.SPECTATOR) {
				all_breakers_dead = false;
				break;
			}
		}
		if (all_breakers_dead) {
			return Team.Placer;
		}

		// if the bomb is Placed we skip the rest of the checks
		if (bomb instanceof PlacedBomb) {
			return null;
		}

		if (phase_timer == RUNNING_TIME) {
			return Team.Breaker;
		}

		// check if all placers are alive
		boolean all_placers_dead = true;
		for (Player p : teams.get_placers()) {
			// if a placer isnt in spectator mode they are alive
			if (p.getGameMode() != GameMode.SPECTATOR) {
				all_placers_dead = false;
				break;
			}
		}
		if (all_placers_dead) {
			return Team.Breaker;
		}

		return null;
	}

	// this gives everyone default armor for now
	// TODO remove this once shop system is implemented
	private void tmp_give_default_armor() {
		for (Player p : teams.get_placers()) {
			PlayerInventory inv = p.getInventory();
			inv.setHelmet(tmp_color_armor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_HELMET)));
			inv.setChestplate(tmp_color_armor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_CHESTPLATE)));
			inv.setLeggings(tmp_color_armor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_LEGGINGS)));
			inv.setBoots(tmp_color_armor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_BOOTS)));
			inv.setItem(0, new ItemStack(Material.STONE_SWORD));
		}

		for (Player p : teams.get_breakers()) {
			PlayerInventory inv = p.getInventory();
			inv.setHelmet(tmp_color_armor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_HELMET)));
			inv.setChestplate(tmp_color_armor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_CHESTPLATE)));
			inv.setLeggings(tmp_color_armor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_LEGGINGS)));
			inv.setBoots(tmp_color_armor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_BOOTS)));
			inv.addItem(new ItemStack(Material.STONE_SWORD));
			inv.addItem(new ItemStack(Material.STONE_PICKAXE));
		}
	}

	// TODO remove this once shop is implemented
	private ItemStack tmp_color_armor(Color c, ItemStack i) {
		LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
		lam.setColor(c);
		i.setItemMeta(lam);
		return i;
	}

	public PlayerData getPlayerData(Player p) {
		for (PlayerData pd : playerDatas) {
			if (pd.player.equals(p.getName())) {
				return pd;
			}
		}
		Bukkit.getServer().sendMessage(text("error occured, a player didnt have associated data"));
		Bukkit.getLogger().warning("player name: " + p.getName());
		Bukkit.getLogger().warning("known names: ");

		for (PlayerData pd : playerDatas) {
			Bukkit.getLogger().warning(pd.player);
		}

		return null;
	}

	private void print_result_table(Team winner) {
		Server s = Bukkit.getServer();
		s.sendMessage(text("-----------------------------\n").color(NamedTextColor.GOLD));
		s.sendMessage(text(" ʟɪᴛᴇsᴛʀɪᴋᴇ \uE100").color(NamedTextColor.GREEN));
		Component winner_text = text("Winner: ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
		if (winner == Team.Placer) {
			s.sendMessage(winner_text.append(Litestrike.PLACER_TEXT));
		} else {
			s.sendMessage(winner_text.append(Litestrike.BREAKER_TEXT));
		}
		s.sendMessage(text("ɢᴀᴍᴇ ʀᴇsᴜʟᴛs:").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
		
		Collections.sort(playerDatas, new PlayerDataComparator());
		s.sendMessage(text(" \uE108").append(text(" 1st. ").color(NamedTextColor.GREEN)
			.append(text(playerDatas.get(0).player)).append(text())));
		// TODO
	}

	// teleports players to the podium
	private void teleport_players_podium(World w) {
		MapData md = Litestrike.getInstance().mapdata;

		if (md.podium == null) {
			// dont teleport if there are no podium coordinates
			return;
		}
		Collections.sort(playerDatas, new PlayerDataComparator());
		for (int i = 0; i < playerDatas.size(); i++) {
			Player p = Bukkit.getPlayer(playerDatas.get(i).player);
			switch (i) {
				case 0:
					p.teleport(md.podium.get_first(w));
					break;
				case 1:
					p.teleport(md.podium.get_second(w));
					break;
				case 2:
					p.teleport(md.podium.get_third(w));
					break;
				default:
					p.teleport(md.podium.get_spawn(w));
			}
		}
		
	}
}
