package gg.litestrike.game;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;

import static net.kyori.adventure.text.Component.text;
import gg.litestrike.game.GameController.RoundState;
import gg.litestrike.game.mapfeatures.MapFeatures;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.min;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import static org.bukkit.Particle.*;

public class BombListener implements Listener {

	// 5 seconds to place
	final static int PLANT_TIME = (20 * 5);

	// 7 seconds to break
	final static int BREAK_TIME = (20 * 7);

	int is_planting = 0;

	int planting_counter = 0;
	int breaking_counter = 0;

	Block last_planting_block;

	BlockFace planting_face;

	List<MiningPlayer> mining_players = new ArrayList<>();

	BombModel bomb_model = new BombModel();

	Player last_planting_player;

	// how it works:
	// when the plugin is started, BombListener is constructed
	// it spawn a BukkitRunnable that will run for the entire time of the plugin
	// there is a list of players that are currently mining, this list is updated
	// throug event
	// as long as there are people in the list, we advance a breaking_counter, if it
	// reaches a value, planting is finished
	//
	// planting works similar, but without a list because only one player can plant
	public BombListener() {
		new BukkitRunnable() {
			@Override
			public void run() {
				GameController gc = Litestrike.getInstance().game_controller;
				if (gc == null) {
					return;
				}

				// Bukkit.getLogger().severe("is planting: "+is_planting);
				// Bukkit.getLogger().severe("plant counter: "+planting_counter);
				if (is_planting > 0) {
					planting_counter += 1;
					bomb_model.raise_bomb(planting_counter, planting_face);
					if (planting_counter == PLANT_TIME) {
						InvItemBomb iib = (InvItemBomb) Litestrike.getInstance().game_controller.bomb;
						iib.place_bomb(last_planting_block.getRelative(planting_face), bomb_model, planting_face);
						last_planting_block.getWorld()
								.spawnParticle(REVERSE_PORTAL, last_planting_block.getLocation().add(0.5, 0.5, 0.5), 5000);
						reset();
						Litestrike.getInstance().game_controller.getPlayerData(last_planting_player).add_plant();

					}
				} else {
					if (planting_counter > 1) {
						Block bomb_block = last_planting_block.getRelative(planting_face);
						SoundEffects.stop_planting(bomb_block.getX(), bomb_block.getY(), bomb_block.getZ());
						bomb_model.remove();
					}
					planting_counter = 0;
				}

				// always decrease timer
				is_planting -= 1;

				////// BReaking from here ///////

				for (MiningPlayer mp : mining_players) {
					mp.timer -= 1;
				}
				if (mining_players.removeIf(mp -> mp.timer == 0)) {
					bomb_model.stop_bomb_mining();
				}

				if (mining_players.size() > 0) {
					if (is_anyone_mining_with_iron_pick()) {
						breaking_counter += 1;
					}
					breaking_counter += 1;
					PlacedBomb b = (PlacedBomb) Litestrike.getInstance().game_controller.bomb;
					if ((breaking_counter < 60 && breaking_counter % 2 == 0) || breaking_counter > 60) {
						mining_players.get(0).p.getWorld()
								.spawnParticle(CRIMSON_SPORE, b.block.getLocation().add(0.5, 0.5, 0.5), breaking_counter / 20, 0, 0, 0);
					}
					if (breaking_counter >= BREAK_TIME) {
						for (Player p : Bukkit.getOnlinePlayers()) {
							p.playSound(b.block.getLocation(), "crystalized:effect.shard.deactivation", 4, 1);
						}

						b.is_broken = true;
						b.remove();
						// mining_players.get(0).p.getWorld()
						// .spawnParticle(CHERRY_LEAVES, b.block.getLocation().add(0.5, 0.5, 0.5), 5000,
						// 1, 1, 1);
						Component broken_by_text = text("ᴛʜᴇ ʙᴏᴍʙ ʜᴀꜱ ʙᴇᴇɴ ʙʀᴏᴋᴇɴ ʙʏ: ").color(Litestrike.YELLOW);
						int i = 0;
						for (MiningPlayer mp : mining_players) {
							if (i == 0) {
								broken_by_text = broken_by_text.append(text(mp.p.getName()).color(Teams.BREAKER_GREEN));
							} else {
								broken_by_text = broken_by_text.append(text("& " + mp.p.getName()).color(Teams.BREAKER_GREEN));
							}
							i++;
						}
						broken_by_text = broken_by_text.append(text("!").color(Litestrike.YELLOW));
						Audience.audience(Bukkit.getOnlinePlayers())
								.sendMessage(broken_by_text);
						Litestrike.getInstance().game_controller.getPlayerData(mining_players.get(0).p).add_break();
						reset();
					}
				} else {
					breaking_counter = 0;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	@EventHandler
	public void onPLayerQuit(PlayerQuitEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || !(gc.bomb instanceof InvItemBomb)) {
			return;
		}
		InvItemBomb b = (InvItemBomb) gc.bomb;
		if (e.getPlayer() == b.player) {
			Item i = Bukkit.getWorld("world").dropItem(e.getPlayer().getLocation(), Bomb.bomb_item());
			b.drop_bomb(i);
		}
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		Material held_item = e.getPlayer().getInventory().getItemInMainHand().getType();
		if (gc == null ||
				gc.teams.get_team(e.getPlayer()) != Team.Breaker ||
				!(gc.bomb instanceof PlacedBomb) ||
				!(held_item == Material.STONE_PICKAXE || held_item == Material.IRON_PICKAXE) ||
				e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		if (is_player_mining(e.getPlayer())) {
			return;
		}

		// if not mining the bomb, return
		PlacedBomb pb = (PlacedBomb) gc.bomb;
		if (!pb.block.equals(e.getBlock())) {
			return;
		}

		mining_players.add(new MiningPlayer(e.getPlayer()));
		bomb_model.bomb_mining();
		SoundEffects.start_breaking(pb.block.getX(), pb.block.getY(), pb.block.getZ());
	}

	@EventHandler
	public void onDamageAbort(BlockDamageAbortEvent e) {
		remove_mining_player(e.getPlayer());
	}

	@EventHandler
	public void onSwingArm(PlayerArmSwingEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		Material held_item = e.getPlayer().getInventory().getItemInMainHand().getType();
		if (gc == null ||
				gc.teams.get_team(e.getPlayer()) != Team.Breaker ||
				!(gc.bomb instanceof PlacedBomb) ||
				!(held_item == Material.STONE_PICKAXE || held_item == Material.IRON_PICKAXE) ||
				e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			remove_mining_player(e.getPlayer());
			return;
		}
		for (MiningPlayer mp : mining_players) {
			if (mp.p == e.getPlayer()) {
				mp.timer = 5 + ping_compensation_ticks(e.getPlayer());
				e.getPlayer().sendActionBar(text(renderBreakingProgress()));
				return;
			}
		}

		// check if add players to mining_players
		PlacedBomb pb = (PlacedBomb) gc.bomb;
		Block target = e.getPlayer().getTargetBlockExact(5, FluidCollisionMode.NEVER);
		if (target == null) {
			return;
		}
		if (target.equals(pb.block)) {
			mining_players.add(new MiningPlayer(e.getPlayer()));
			bomb_model.bomb_mining();
			SoundEffects.start_breaking(pb.block.getX(), pb.block.getY(), pb.block.getZ());
		} else {
			remove_mining_player(e.getPlayer());
		}
	}

	private void remove_mining_player(Player p) {
		if (mining_players.removeIf(mp -> mp.p == p)) {
			if (mining_players.size() == 0) {
				bomb_model.stop_bomb_mining();
			}
		}
	}

	private boolean is_player_mining(Player p) {
		for (MiningPlayer mp : mining_players) {
			if (mp.p == p) {
				return true;
			}
		}
		return false;
	}

	private boolean is_anyone_mining_with_iron_pick() {
		for (MiningPlayer mp : mining_players) {
			if (mp.p.getInventory().getItemInMainHand().getType() == Material.IRON_PICKAXE) {
				return true;
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractPlacing(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && isInteractable(e.getClickedBlock().getType())) {
			e.setCancelled(true);
		}

		GameController gc = Litestrike.getInstance().game_controller;
		ItemStack held_item_main = e.getPlayer().getInventory().getItemInMainHand();
		ItemStack held_item_off = e.getPlayer().getInventory().getItemInOffHand();

		boolean is_holding_bomb = held_item_main.equals(Bomb.bomb_item()) || held_item_off.equals(Bomb.bomb_item());

		if (gc == null ||
				gc.round_state != RoundState.Running ||
				e.getItem() == null ||
				!is_holding_bomb ||
				e.getAction() != Action.RIGHT_CLICK_BLOCK ||
				!e.getClickedBlock().getRelative(e.getBlockFace()).isReplaceable() ||
				e.getClickedBlock().getType() != Litestrike.getInstance().mapdata.bomb_plant_block ||
				!(gc.bomb instanceof InvItemBomb)) {
			return;
		}

		MapFeatures mf = Litestrike.getInstance().mapdata.map_features;
		if (!mf.can_plant_below && e.getBlockFace() == BlockFace.DOWN) {
			return;
		}
		if (!mf.can_plant_side && !(e.getBlockFace() == BlockFace.DOWN || e.getBlockFace() == BlockFace.UP)) {
			return;
		}

		boolean is_holding_bow = held_item_main.getType() == Material.BOW ||
				held_item_main.getType() == Material.CROSSBOW ||
				held_item_off.getType() == Material.BOW ||
				held_item_off.getType() == Material.CROSSBOW;

		if (is_holding_bow) {
			e.setCancelled(true);
		}

		// sanity check
		if (gc.teams.get_team(e.getPlayer()) != Team.Placer) {
			Bukkit.getLogger().severe("ERROR: A Not Placer planted the bomb!");
		}

		if (is_planting < 0) {
			last_planting_block = e.getClickedBlock();
			SoundEffects.start_planting(last_planting_block.getX(), last_planting_block.getY(), last_planting_block.getZ());
			bomb_model.spawn_model(last_planting_block.getLocation());
		}
		is_planting = 6 + ping_compensation_ticks(e.getPlayer());

		e.getPlayer().sendActionBar(text(renderPlacingProgress()));
		last_planting_player = e.getPlayer();

		// if player starts looking at a different block, reset planting progress
		if (!e.getClickedBlock().equals(last_planting_block)) {
			is_planting = 0;
		}
		planting_face = e.getBlockFace();
	}

	@EventHandler
	public void onInvPickup(InventoryPickupItemEvent e) {
		// prevent bombitem from getting picked up by hopper
		if (e.getItem().getItemStack().equals(Bomb.bomb_item())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (!e.getItemDrop().getItemStack().equals(Bomb.bomb_item())) {
			if (gc.round_state != RoundState.PreRound || e.getItemDrop().getItemStack().getType() == Material.EMERALD) {
				e.setCancelled(true);
			}
			String item_name = e.getItemDrop().getItemStack().getType().toString().toLowerCase();
			if (item_name.contains("arrow")
					|| e.getItemDrop().getItemStack().getType() == Material.POTION
					|| e.getItemDrop().getItemStack().getType() == Material.GOLDEN_APPLE) {
				return;
			} else {
				e.setCancelled(true);
				return;
			}
		}
		reset();
		InvItemBomb ib = (InvItemBomb) gc.bomb;
		ib.drop_bomb(e.getItemDrop());
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		if (b == null) {
			Bukkit.getLogger().severe("a player died while no bomb existed? is that possible?");
			return;
		}
		if (b instanceof InvItemBomb && ((InvItemBomb) b).player == e.getPlayer()) {
			Item i = Bukkit.getWorld("world").dropItem(e.getPlayer().getLocation(), Bomb.bomb_item());
			((InvItemBomb) b).drop_bomb(i);
		}
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent e) {
		if (!e.getItem().getItemStack().equals(Bomb.bomb_item())) {
			return;
		}
		e.setCancelled(true);
		if (e.getEntity() instanceof Player
				&& Teams.get_team(e.getEntity().getName()) == Team.Placer) {
			// if it got picked up by a player and that player is placer, then proceed
			Player p = (Player) e.getEntity();
			Bomb.give_bomb(p);
		}
	}

	// renders the breakingprogres for the action bar
	private String renderBreakingProgress() {
		double percentage = (double) breaking_counter / (double) BREAK_TIME;
		String s = "[";
		boolean b = false;
		for (double i = 0.1; i < 1; i += 0.1) {
			if (b) {
				s += " ";
				continue;
			}
			if (i < percentage) {
				s += "=";
				continue;
			}
			s += ">";
			b = true;
		}
		s += "]";
		return s;
	}

	private String renderPlacingProgress() {
		double percentage = (double) planting_counter / (double) PLANT_TIME;
		String s = "[";
		boolean b = false;
		for (double i = 0.1; i < 1; i += 0.1) {
			if (b) {
				s += " ";
				continue;
			}
			if (i < percentage) {
				s += "=";
				continue;
			}
			s += ">";
			b = true;
		}
		s += "]";
		return s;
	}

	private void reset() {
		is_planting = 0;
		planting_counter = 0;
		breaking_counter = 0;
		mining_players.clear();
		last_planting_block = null;
	}

	private int ping_compensation_ticks(Player p) {
		return Math.min(p.getPing() / 50, 10);
	}

	// i got this from here:
	// https://www.spigotmc.org/threads/check-if-a-block-is-interactable.535861/
	public static boolean isInteractable(Material type) {
		boolean interactable = type.isInteractable();
		if (!interactable)
			return false;

		switch (type) {
			case ACACIA_STAIRS:
			case ANDESITE_STAIRS:
			case BIRCH_STAIRS:
			case BLACKSTONE_STAIRS:
			case BRICK_STAIRS:
			case COBBLESTONE_STAIRS:
			case CRIMSON_STAIRS:
			case DARK_OAK_STAIRS:
			case DARK_PRISMARINE_STAIRS:
			case DIORITE_STAIRS:
			case END_STONE_BRICK_STAIRS:
			case GRANITE_STAIRS:
			case JUNGLE_STAIRS:
			case MOSSY_COBBLESTONE_STAIRS:
			case MOSSY_STONE_BRICK_STAIRS:
			case NETHER_BRICK_STAIRS:
			case OAK_STAIRS:
			case POLISHED_ANDESITE_STAIRS:
			case POLISHED_BLACKSTONE_BRICK_STAIRS:
			case POLISHED_BLACKSTONE_STAIRS:
			case POLISHED_DIORITE_STAIRS:
			case POLISHED_GRANITE_STAIRS:
			case PRISMARINE_BRICK_STAIRS:
			case PRISMARINE_STAIRS:
			case PURPUR_STAIRS:
			case QUARTZ_STAIRS:
			case RED_NETHER_BRICK_STAIRS:
			case RED_SANDSTONE_STAIRS:
			case SANDSTONE_STAIRS:
			case SMOOTH_QUARTZ_STAIRS:
			case SMOOTH_RED_SANDSTONE_STAIRS:
			case SMOOTH_SANDSTONE_STAIRS:
			case SPRUCE_STAIRS:
			case STONE_BRICK_STAIRS:
			case STONE_STAIRS:
			case WARPED_STAIRS:

			case ACACIA_FENCE:
			case BIRCH_FENCE:
			case CRIMSON_FENCE:
			case DARK_OAK_FENCE:
			case JUNGLE_FENCE:
			case MOVING_PISTON:
			case NETHER_BRICK_FENCE:
			case OAK_FENCE:
			case PUMPKIN:
			case REDSTONE_ORE:
			case REDSTONE_WIRE:
			case SPRUCE_FENCE:
			case WARPED_FENCE:
			case IRON_TRAPDOOR:
				return false;
			default:
				return true;
		}

	}
}

class MiningPlayer {
	Player p;
	int timer = 12;

	public MiningPlayer(Player p) {
		this.p = p;
	}
}
