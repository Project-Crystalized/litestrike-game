package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

// This will be created by something else, whenever there are 6+ people online
// and no game is currently going
public class GameController {
	public Teams teams = new Teams();
	public List<PlayerData> playerDatas;
	public Bomb bomb;

	public int round_number = 0;
	public RoundState round_state = RoundState.PreRound;

	public List<Team> round_results = new ArrayList<Team>();
	public int placer_wins_amt = 0;
	public int breaker_wins_amt = 0;

	public HashMap<String, Shop> shopList = new HashMap<>();

	// the phase_timer starts counting up from the beginning of the round
	// after it reaches (15 * 20), the game is started. when the round winner is
	// determined its reset to 0 and counts until (5 * 20) for the postround time.
	// then the next round starts and it counts from 0 again
	public int phase_timer = 0;

	// the game_reference is printed in chat so that we can later search for the
	// number in the chat logs
	public final int game_reference = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE - 1);

	// after this round, the sides get switched
	public final static int SWITCH_ROUND = 4;

	public final static int PRE_ROUND_TIME = (20 * 23);
	// public final static int PRE_ROUND_TIME = (20 * 5);
	public final static int RUNNING_TIME = (180 * 20);
	public final static int POST_ROUND_TIME = (5 * 20);
	// public final static int POST_ROUND_TIME = (1 * 20);
	public final static int FINISH_TIME = (20 * 12);

	public enum RoundState {
		PreRound,
		Running,
		PostRound,
		GameFinished,
	}

	public GameController() {
		Bukkit.getLogger().info("Starting game with game_id: " + game_reference);

		new BukkitRunnable() {
			@Override
			public void run() {
				playerDatas = new ArrayList<PlayerData>();
				for (Player player : Bukkit.getOnlinePlayers()) {
					PlayerData pd = new PlayerData(player);
					playerDatas.add(pd);
					Shop s = new Shop(player);
					s.resetEquip();
					s.resetEquipCounters();
					new TabListController();
					for (Player p : Bukkit.getOnlinePlayers()) {
						player.unlistPlayer(p);
					}
				}
				next_round();
			}
		}.runTaskLater(Litestrike.getInstance(), 1);

		// setup scoreboard and bossbar
		ScoreboardController.setup_scoreboard(teams, game_reference);
		Litestrike.getInstance().bbd.showBossBar();

		// This just calls update_game_state() once every second
		new BukkitRunnable() {
			@Override
			public void run() {
				boolean game_over = update_game_state();
				if (game_over) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.kick();
					}
					Litestrike.getInstance().party_manager.clear_partys();
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
		if (round_state == RoundState.GameFinished) {
			return null;
		}

		// if the enemy team is empty, or if the team has reached the required rounds,
		// win
		if (teams.get_placers().size() == 0 || breaker_wins_amt == SWITCH_ROUND + 1) {
			return Team.Breaker;
		}
		if (teams.get_breakers().size() == 0 || placer_wins_amt == SWITCH_ROUND + 1) {
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
		if (round_number == 1) {
			Audience.audience(teams.get_placers()).sendMessage(text("\n")
					.append(translatable("crystalized.game.litestrike.tutorial.generic1")).color(Litestrike.YELLOW)
					.append(Litestrike.PLACER_TEXT)
					.append(text("\n"))
					.append(translatable("crystalized.game.litestrike.tutorial.placer1").color(Litestrike.YELLOW))
					.append(text("\n"))
					.append(translatable("crystalized.game.litestrike.tutorial.placer2").color(Litestrike.YELLOW))
					.append(text("\n")));
			Audience.audience(teams.get_breakers()).sendMessage(text("\n")
					.append(translatable("crystalized.game.litestrike.tutorial.generic1")).color(Litestrike.YELLOW)
					.append(Litestrike.BREAKER_TEXT)
					.append(text("\n"))
					.append(translatable("crystalized.game.litestrike.tutorial.breaker1").color(Litestrike.YELLOW))
					.append(text("\n"))
					.append(translatable("crystalized.game.litestrike.tutorial.breaker2").color(Litestrike.YELLOW))
					.append(text("\n")));
		}

		Litestrike ls = Litestrike.getInstance();
		if (ls.mapdata.map_features != null && ls.mapdata.map_features.bigDoor != null) {
			ls.mapdata.map_features.bigDoor.regenerate_door();
		}
		// remove the border
		ls.mapdata.lowerBorder(Bukkit.getWorld("world"));
		Litestrike.getInstance().sendPluginMessage("crystalized:essentials", "BreezeDagger_DisableRecharging:true");

		for (Player p : Bukkit.getOnlinePlayers()) {
			Shop.removeShop(p);
			Inventory inv = p.getInventory();
			for (int i = 0; i < inv.getSize(); i++) {
				if (LSItem.isBreezeDagger(inv.getItem(i))) {
					ItemMeta meta = inv.getItem(i).getItemMeta();
					NamespacedKey key = new NamespacedKey("namespace", "key");
					PersistentDataContainer cont = meta.getPersistentDataContainer();
					cont.set(key, PersistentDataType.INTEGER, 2);
					inv.getItem(i).setItemMeta(meta);
				}
			}
		}
	}

	// this is called when we switch from Running to PostRound
	private void finish_round(Team winner) {
		if (winner == null) {
			Bukkit.getLogger().severe("critical error: finish_round() was called with null as input");
		}
		round_state = RoundState.PostRound;
		phase_timer = 0;
		round_results.add(winner);
		if (winner == Team.Placer) {
			placer_wins_amt += 1;
		} else {
			breaker_wins_amt += 1;
		}

		ScoreboardController.set_win_display(round_results);

		// remove arrows and items
		for (Entity e : Bukkit.getWorld("world").getEntities()) {
			if (e instanceof Arrow || e instanceof Item || e instanceof SpectralArrow) {
				e.remove();
			}
		}

		// announce winner
		Component winner_component;
		if (winner == Team.Placer) {
			winner_component = Litestrike.PLACER_TEXT;
			Bukkit.getLogger().info("The Placer Team won round " + round_number);
		} else {
			winner_component = Litestrike.BREAKER_TEXT;
			Bukkit.getLogger().info("The Breaker Team won round " + round_number);
		}
		Audience.audience(Bukkit.getOnlinePlayers())
				.sendMessage(text("\nᴛʜᴇ ").color(Litestrike.YELLOW).append(winner_component)
						.append(text(" ᴛᴇᴀᴍ ᴡᴏɴ ʀᴏᴜɴᴅ ").color(Litestrike.YELLOW)).append(text(round_number))
						.append(text("!\n").color(Litestrike.YELLOW)));

		Litestrike.getInstance().sendPluginMessage("crystalized:essentials", "BreezeDagger_DisableRecharging:false");

		for (Player p : teams.get_all_players()) {
			PlayerData pd = getPlayerData(p);
			pd.assist_list.clear();
			pd.ldt.clear_damager();
			Inventory inv = p.getInventory();
			for (int i = 0; i < inv.getSize(); i++) {
				if (LSItem.is_underdog_sword(inv.getItem(i))) {
					inv.setItem(i, LSItem.do_underdog_sword(teams.get_team(p)));
				}
			}
			if (teams.get_team(p) == winner) {
				pd.addMoney(700, translatable("crystalized.game.litestrike.money.win_round"));
				SoundEffects.round_won(p);
			} else {
				pd.addMoney(400, translatable("crystalized.game.litestrike.money.loose_round"));
				SoundEffects.round_lost(p);
			}
		}
	}

	// this is called when the last round is over and the podium should begin
	private void start_podium(Team winner) {
		round_state = RoundState.GameFinished;
		phase_timer = 0;
		if (bomb != null) {
			bomb.remove();
			bomb = null;
		}

		World w = Bukkit.getWorld("world");

		print_result_table(winner);
		teleport_players_podium(w);
		SoundEffects.round_end_sound(winner);
		LsDatabase.save_game(winner);
		for(Player p : Bukkit.getOnlinePlayers()){
			LsDatabase.writeTemporaryData(p, 5, 20);
		}
		// summon fireworks
		new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				i++;
				if (i == 4) {
					cancel();
				}
				for (Player p : teams.get_team_of(winner)) {
					Firework fw = w.spawn(p.getLocation(), Firework.class);
					FireworkMeta fwm = fw.getFireworkMeta();
					fwm.setPower(2);
					FireworkEffect effect1 = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withTrail()
							.withColor(Color.RED).withColor(Color.BLUE).build();
					FireworkEffect effect2 = FireworkEffect.builder().with(FireworkEffect.Type.CREEPER).withFlicker().withTrail()
							.withColor(Color.GREEN).withColor(Color.AQUA).build();
					fwm.addEffects(effect1, effect2);
					fw.setFireworkMeta(fwm);
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 0, (20 * 2));

		// this sends players back to lobby
		new BukkitRunnable() {
			@Override
			public void run() {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("Connect");
				out.writeUTF("lobby");
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendPluginMessage(Litestrike.getInstance(), "crystalized:main", out.toByteArray());
				}
			}
		}.runTaskLater(Litestrike.getInstance(), FINISH_TIME - (20 * 2));
	}

	// called when we go from PostRound to PreRound and when the first round starts
	private void next_round() {
		round_state = RoundState.PreRound;
		phase_timer = 0;
		round_number += 1;

		if (bomb != null) {
			bomb.remove();
			bomb = null;
		}

		Litestrike ls = Litestrike.getInstance();
		if (ls.mapdata.map_features != null && ls.mapdata.map_features.bigDoor != null) {
			ls.mapdata.map_features.bigDoor.regenerate_door();
		}

		if (round_number == SWITCH_ROUND + 1) {
			Audience.audience(Bukkit.getOnlinePlayers())
					.sendMessage(translatable("crystalized.game.litestrike.switching").color(Litestrike.YELLOW));
			Bukkit.getLogger().info("Switching the Sides");
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
			int tmp = breaker_wins_amt;
			breaker_wins_amt = placer_wins_amt;
			placer_wins_amt = tmp;
			ScoreboardController.setup_scoreboard(teams, game_reference);
			ScoreboardController.set_win_display(round_results);
			for (Shop s : Litestrike.getInstance().game_controller.shopList.values()) {
				s.resetEquip();
				s.resetEquipCounters();
			}
		}
		if (round_number == (SWITCH_ROUND * 2) + 1) {
			Audience.audience(Bukkit.getOnlinePlayers())
					.sendMessage(translatable("crystalized.game.litestrike.tie_breaker").color(Litestrike.YELLOW));
			for (PlayerData pd : playerDatas) {
				pd.removeMoney();
				pd.addMoney(5000, translatable("crystalized.game.litestrike.money.last_round"));
			}
			for (Shop s : Litestrike.getInstance().game_controller.shopList.values()) {
				s.resetEquip();
				s.resetEquipCounters();
			}
		}

		World w = Bukkit.getWorld("world");
		Litestrike.getInstance().mapdata.raiseBorder(w);

		// teleport everyone to spawn and make them look at enemy spawn
		Location placer_spawn = Litestrike.getInstance().mapdata.get_placer_spawn(w);
		Location breaker_spawn = Litestrike.getInstance().mapdata.get_breaker_spawn(w);
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
			Shop.giveDefaultArmor(p);
			p.setGameMode(GameMode.SURVIVAL);
			p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
			p.clearActivePotionEffects();
			getPlayerData(p).addMoney(1000, translatable("crystalized.game.litestrike.money.next_round"));
			Shop s = Litestrike.getInstance().game_controller.getShop(p);
			s.resetEquipCounters();
			s.previousEquip.clear();
			// this is needed because of some weird packet nonsense, to make everyone glow
			p.setSneaking(true);
			p.setSneaking(false);
		}

		// sound effect has a cooldown, so we call it here instead of in round_start
		SoundEffects.round_start();

		// give bomb to a random player
		// generate int between 0 and placer teams size
		int random = ThreadLocalRandom.current().nextInt(0, teams.get_placers().size());
		Bomb.give_bomb(teams.get_placers().get(random));

		Shop.giveShop_and_update();
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

	public PlayerData getPlayerData(String p) {
		for (PlayerData pd : playerDatas) {
			if (pd.player.equals(p)) {
				return pd;
			}
		}
		Bukkit.getServer().sendMessage(text("error occured, a player didnt have associated data"));
		Bukkit.getLogger().warning("player name: " + p);
		Bukkit.getLogger().warning("known names: ");

		for (PlayerData pd : playerDatas) {
			Bukkit.getLogger().warning(pd.player);
		}

		return null;
	}

	private void print_result_table(Team winner) {
		Server s = Bukkit.getServer();
		s.sendMessage(text("-----------------------------\n").color(NamedTextColor.GOLD));
		s.sendMessage(text(" ʟɪᴛᴇsᴛʀɪᴋᴇ").color(NamedTextColor.GREEN).append(text(" \uE100").color(NamedTextColor.WHITE)));
		Component winner_text = text("Winner: ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
		if (winner == Team.Placer) {
			s.sendMessage(winner_text.append(Litestrike.PLACER_TEXT));
		} else {
			s.sendMessage(winner_text.append(Litestrike.BREAKER_TEXT));
		}
		s.sendMessage(translatable("crystalized.game.generic.gameresults").color(NamedTextColor.BLUE)
				.decorate(TextDecoration.BOLD)
				.append(text(":")).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));

		Collections.sort(playerDatas, new PlayerDataComparator());
		Collections.reverse(playerDatas);
		if (playerDatas.size() > 0) {
			PlayerData first = playerDatas.get(0);
			s.sendMessage(text(" \uE108").color(NamedTextColor.WHITE).append(text(" 1st. ").color(NamedTextColor.GREEN)
					.append(text(first.player)).append(text(" ".repeat(20 - first.player.length())))
					.append(text(first.kills + " / " + first.deaths + " / " + first.assists))));
		}
		if (playerDatas.size() > 1) {
			PlayerData second = playerDatas.get(1);
			s.sendMessage(text("   2nd. ").color(NamedTextColor.YELLOW)
					.append(text(second.player)).append(text(" ".repeat(20 - second.player.length())))
					.append(text(second.kills + " / " + second.deaths + " / " + second.assists)));
		}
		if (playerDatas.size() > 2) {
			PlayerData third = playerDatas.get(2);
			s.sendMessage(text("   3rd. ").color(NamedTextColor.YELLOW)
					.append(text(third.player)).append(text(" ".repeat(20 - third.player.length())))
					.append(text(third.kills + " / " + third.deaths + " / " + third.assists)));
			s.sendMessage(text("-----------------------------\n").color(NamedTextColor.GOLD));
		}
	}

	// teleports players to the podium
	private void teleport_players_podium(World w) {
		MapData md = Litestrike.getInstance().mapdata;

		if (md.podium == null) {
			// dont teleport if there are no podium coordinates
			return;
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.setGameMode(GameMode.ADVENTURE);
		}
		Collections.sort(playerDatas, new PlayerDataComparator());
		Collections.reverse(playerDatas);
		for (int i = 0; i < playerDatas.size(); i++) {
			Player p = Bukkit.getPlayer(playerDatas.get(i).player);
			if (p == null) {
				continue;
			}
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

	public Shop getShop(Player p) {
		return shopList.get(p.getName());
	}

}
