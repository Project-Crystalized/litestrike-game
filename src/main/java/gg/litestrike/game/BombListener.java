package gg.litestrike.game;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.text.Component;

import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class BombListener implements Listener {

	// 5 seconds to place
	final static int PLACE_TIME = (20 * 5);

	// 7 seconds to break
	final static int BREAK_TIME = (20 * 7);

	int is_placing = 0;

	int placing_counter = 0;
	int breaking_counter = 0;

	Block last_planting_block;

	List<Player> mining_players = new ArrayList<>();

	public BombListener() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (is_placing > 0) {
					// as long as we are placing/breaking, advance timer
					placing_counter += 1;
				} else {
					// else reset the timer
					placing_counter = 0;
				}

				// always decrease timer
				is_placing -= 1;

				if (mining_players.size() > 0) {
					Bukkit.getServer().sendMessage(Component.text("a player is mining"));
					breaking_counter += 1;
				} else {
					breaking_counter = 0;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent e) {
		Bukkit.getServer().sendMessage(Component.text("received event"));

		GameController gc = Litestrike.getInstance().game_controller;
		if (mining_players.contains(e.getPlayer()) ||
				gc == null ||
				gc.teams.get_team(e.getPlayer()) == Team.Placer ||
				!(gc.bomb.bomb_loc instanceof PlacedBomb) ||
				e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		Bukkit.getServer().sendMessage(Component.text("got here"));

		Material held_item = e.getPlayer().getInventory().getItemInMainHand().getType();
		if (!(held_item == Material.STONE_PICKAXE || held_item == Material.IRON_PICKAXE)) {
			return;
		}

		PlacedBomb pb = (PlacedBomb) gc.bomb.bomb_loc;
		if (!pb.block.equals(e.getBlock())) {
			return;
		}

		mining_players.add(e.getPlayer());
	}

	@EventHandler
	public void onDamageAbort(BlockDamageAbortEvent e) {
		mining_players.remove(e.getPlayer());
	}

	@EventHandler
	public void onSwingArm(PlayerArmSwingEvent e) {
		if (!mining_players.contains(e.getPlayer())) {
			return;
		}

		e.getPlayer().sendActionBar(Component.text("Breaking progress: " + breaking_counter / 20));

		if (breaking_counter == BREAK_TIME) {
			Bomb b = Litestrike.getInstance().game_controller.bomb;
			b.is_broken = true;

			Bukkit.getServer().sendMessage(Component.text("The BOMB has been broken"));

			reset();
		}
	}

	@EventHandler
	public void onInteractPlacing(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		e.setCancelled(true);
		if (Litestrike.getInstance().game_controller == null) {
			return;
		}

		// uncancel the event when bomb is mined, so we get the BlockDamageEvent
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		if (b.bomb_loc instanceof PlacedBomb) {
			PlacedBomb pb = (PlacedBomb) b.bomb_loc;
			if (e.getClickedBlock() != null && e.getClickedBlock().equals(pb.block)) {
				e.setCancelled(false);
			}
		}

		if (e.getItem() == null ||
				!e.getItem().equals(Bomb.bomb_item()) ||
				e.getAction() != Action.RIGHT_CLICK_BLOCK ||
				e.getClickedBlock().getType() != Material.TERRACOTTA ||
				!(Litestrike.getInstance().game_controller.bomb.bomb_loc instanceof InvItemBomb)) {
			return;
		}

		// sanity check
		if (Litestrike.getInstance().game_controller.teams.get_team(e.getPlayer()) == Team.Breaker) {
			Bukkit.getLogger().severe("ERROR: A Breaker planted the bomb!");
		}

		is_placing = 4;
		e.getPlayer().sendActionBar(Component.text("Placing progress: " + placing_counter / 20));

		// if player starts looking at a different block, reset planting progress
		if (!e.getClickedBlock().equals(last_planting_block)) {
			placing_counter = 0;
			last_planting_block = e.getClickedBlock();
		}

		if (placing_counter == PLACE_TIME) {
			reset();
			Block bomb_block = e.getClickedBlock().getRelative(e.getBlockFace());
			Litestrike.getInstance().game_controller.bomb.place_bomb(bomb_block);
		}
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
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		b.drop_bomb(e.getItemDrop());
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
			Bomb b = Litestrike.getInstance().game_controller.bomb;
			Player p = (Player) e.getEntity();
			b.give_bomb(p.getInventory());
		}
	}

	private void reset() {
		is_placing = 0;
		placing_counter = 0;
		breaking_counter = 0;
		mining_players.clear();
	}
}
