package gg.litestrike.game;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
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
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.event.player.PlayerArmSwingEvent;

public class BombListener implements Listener {

	// 5 seconds to place
	final static int PLANT_TIME = (20 * 5);

	// 7 seconds to break
	final static int BREAK_TIME = (20 * 7);

	int is_planting = -1;

	int planting_counter = 0;
	int breaking_counter = 0;

	Block last_planting_block;

	BlockFace planting_face;

	List<MiningPlayer> mining_players = new ArrayList<>();

	public BombListener() {
		new BukkitRunnable() {
			@Override
			public void run() {
				GameController gc = Litestrike.getInstance().game_controller;
				if (gc == null || last_planting_block == null) {
					return;
				}

				if (is_planting > 0) {
					// as long as we are placing/breaking, advance timer
					planting_counter += 1;
					if (planting_counter == PLANT_TIME) {
						reset();
						InvItemBomb pb = (InvItemBomb) Litestrike.getInstance().game_controller.bomb;
						pb.place_bomb(last_planting_block.getRelative(planting_face));
					}
				} else if (is_planting == 0) {
					// else reset the timer
					planting_counter = 0;
					Block bomb_block = last_planting_block.getRelative(planting_face);
					SoundEffects.stop_planting(bomb_block.getX(), bomb_block.getY(), bomb_block.getZ());
				}

				// always decrease timer
				is_planting -= 1;

				////// BReaking from here ///////

				List<MiningPlayer> remove_list = new ArrayList<>();
				for (MiningPlayer mp : mining_players) {
					mp.timer -= 1;
					if (mp.timer == 0) {
						remove_list.add(mp);
					}
				}
				mining_players.removeAll(remove_list);

				if (mining_players.size() > 0) {
					breaking_counter += 1;
					if (breaking_counter == BREAK_TIME) {
						PlacedBomb b = (PlacedBomb) Litestrike.getInstance().game_controller.bomb;
						b.is_broken = true;
						Bukkit.getServer().sendMessage(text("ᴛʜᴇ ʙᴏᴍʙ ʜᴀꜱ ʙᴇᴇɴ ʙʀᴏᴋᴇɴ!").color(Litestrike.YELLOW));
						reset();
					}
				} else {
					breaking_counter = 0;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		Material held_item = e.getPlayer().getInventory().getItemInMainHand().getType();
		if (gc == null ||
				gc.teams.get_team(e.getPlayer()) == Team.Placer ||
				!(gc.bomb instanceof PlacedBomb) ||
				!(held_item == Material.STONE_PICKAXE || held_item == Material.IRON_PICKAXE) ||
				e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		// if that player was already mining return
		for (MiningPlayer mp : mining_players) {
			if (mp.p == e.getPlayer()) {
				return;
			}
		}

		// if not mining the bomb, return
		PlacedBomb pb = (PlacedBomb) gc.bomb;
		if (!pb.block.equals(e.getBlock())) {
			return;
		}

		// TODO play a start break sound effect
		mining_players.add(new MiningPlayer(e.getPlayer()));
	}

	@EventHandler
	public void onDamageAbort(BlockDamageAbortEvent e) {
		MiningPlayer to_remove = null;
		for (MiningPlayer mp : mining_players) {
			if (mp.p == e.getPlayer()) {
				to_remove = mp;
				break;
			}
		}
		if (to_remove != null) {
			mining_players.remove(to_remove);
		}
	}

	@EventHandler
	public void onSwingArm(PlayerArmSwingEvent e) {
		for (MiningPlayer mp : mining_players) {
			if (mp.p == e.getPlayer()) {
				mp.timer = 2;
				break;
			}
		}

		// TODO make pretty
		e.getPlayer().sendActionBar(text("Breaking progress: " + breaking_counter / 20));
	}

	@EventHandler
	public void onInteractPlacing(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			e.setCancelled(true);
		}

		if (Litestrike.getInstance().game_controller == null) {
			return;
		}

		// uncancel the event when bomb is mined, so we get the BlockDamageEvent
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		if (b instanceof PlacedBomb) {
			if (e.getClickedBlock() != null && e.getClickedBlock().equals(((PlacedBomb) b).block)) {
				e.setCancelled(false);
			}
		}

		if (e.getItem() == null ||
				!e.getItem().equals(Bomb.bomb_item()) ||
				e.getAction() != Action.RIGHT_CLICK_BLOCK ||
				e.getClickedBlock().getType() != Material.TERRACOTTA ||
				!(Litestrike.getInstance().game_controller.bomb instanceof InvItemBomb)) {
			return;
		}

		// sanity check
		if (Litestrike.getInstance().game_controller.teams.get_team(e.getPlayer()) == Team.Breaker) {
			Bukkit.getLogger().severe("ERROR: A Breaker planted the bomb!");
		}

		if (is_planting < 0) {
			Block lpb = e.getClickedBlock();
			SoundEffects.start_planting(lpb.getX(), lpb.getY(), lpb.getZ());
		}
		is_planting = 4;
		// Bukkit.getServer().sendMessage(text("planting = 4"));

		// TODO make pretty
		e.getPlayer().sendActionBar(text("Placing progress: " + planting_counter / 20));

		// if player starts looking at a different block, reset planting progress
		if (!e.getClickedBlock().equals(last_planting_block)) {
			planting_counter = 0;
			last_planting_block = e.getClickedBlock();
			SoundEffects.start_planting(last_planting_block.getX(), last_planting_block.getY(), last_planting_block.getZ());
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
		if (!e.getItemDrop().getItemStack().equals(Bomb.bomb_item())) {
			return;
		}
		reset();
		InvItemBomb ib = (InvItemBomb) Litestrike.getInstance().game_controller.bomb;
		ib.drop_bomb(e.getItemDrop());
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		if (b instanceof InvItemBomb && ((InvItemBomb) b).p_inv == e.getPlayer().getInventory()) {
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
				&& Litestrike.getInstance().game_controller.teams.get_team(e.getEntity().getName()) == Team.Placer) {
			// if it got picked up by a player and that player is placer, then proceed
			Player p = (Player) e.getEntity();
			Bomb.give_bomb(p.getInventory());
		}
	}

	private void reset() {
		is_planting = 0;
		planting_counter = 0;
		breaking_counter = 0;
		mining_players.clear();
	}
}

class MiningPlayer {
	Player p;
	int timer = 2;

	public MiningPlayer(Player p) {
		this.p = p;
	}
}
