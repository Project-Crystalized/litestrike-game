package gg.litestrike.game;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class MapFeatures implements Listener {
	private static Set<Player> fall_protected_players = new HashSet<>();

	protected static Material launch_pad_block;
	protected static Material levi_pad_block;
	protected static Material jump_pad_block;

	public MapFeatures(JsonObject json) {
		JsonElement lp_block = json.get("launch_pad_block");
		if (lp_block != null) {
			launch_pad_block = Material.matchMaterial(lp_block.getAsString());
		}

		JsonElement levi_block = json.get("levi_pad_block");
		if (levi_block == null) {
			levi_block = json.get("levitation_pad_block");
		}
		if (levi_block != null) {
			levi_pad_block = Material.matchMaterial(levi_block.getAsString());
		}

		JsonElement jump_block = json.get("jump_pad_block");
		if (jump_block != null) {
			jump_pad_block = Material.matchMaterial(jump_block.getAsString());
		}
	}

	public String toString() {
		return "\nlaunch_pad_block: " + launch_pad_block +
				"\nlevi_pad_block: " + levi_pad_block +
				"\njump_pad_block: " + jump_pad_block;
	}

	// this constructor is for map data version 2, when pad blocks where hardcoded
	public MapFeatures(boolean launch_pad, boolean levi_pad) {
		if (launch_pad) {
			launch_pad_block = Material.PURPLE_CONCRETE_POWDER;
		}
		if (levi_pad) {
			levi_pad_block = Material.PURPUR_BLOCK;
		}
	}

	public void register_listeners(Litestrike plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		if (launch_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new LaunchPadListener(), plugin);
		}
		if (levi_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new LeviPadListener(), plugin);
		}
		if (jump_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new JumpPadListener(), plugin);
		}
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FALL && fall_protected_players.contains(e.getEntity())) {
			e.setCancelled(true);
			fall_protected_players.remove(e.getEntity());
			fall_protected_players.removeIf(pl -> !pl.isConnected());
		}
	}

	public static void fall_protect_player(Player p, int time) {
		fall_protected_players.add(p);
		new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				i++;
				if (i > time) {
					fall_protected_players.remove(p);
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 5, 1);
	}
}

class LaunchPadListener implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		Block block_under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (block_under.getType() == MapFeatures.launch_pad_block) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.setVelocity(p.getLocation().getDirection().multiply(2));
			MapFeatures.fall_protect_player(p, (20 * 5));
		}
	}
}

class LeviPadListener implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR)
			return;
		Block block_under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (block_under.getType() == MapFeatures.levi_pad_block && !(p.hasPotionEffect(PotionEffectType.LEVITATION))) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (55), 2));
			MapFeatures.fall_protect_player(p, (20 * 6));
		}
	}
}

class JumpPadListener implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR)
			return;
		Block block_under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (block_under.getType() == MapFeatures.jump_pad_block && !(p.hasPotionEffect(PotionEffectType.JUMP_BOOST))) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (20), 7));
			MapFeatures.fall_protect_player(p, (20 * 5));
		}
	}
}
