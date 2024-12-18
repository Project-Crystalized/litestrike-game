package gg.litestrike.game;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class MapFeatures {

}

class LaunchPadListener implements Listener {
	// people will keep if fall protection until they actually fall, can be abused
	private Set<Player> protected_players = new HashSet<>();

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		Block block_under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (block_under.getType() == Material.PURPLE_CONCRETE_POWDER && !(p.hasPotionEffect(PotionEffectType.LEVITATION))) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard-positive"), Sound.Source.AMBIENT, 1f, 1f));
      p.setVelocity(p.getLocation().getDirection().multiply(2.5));
			protected_players.add(p);
			protected_players.removeIf(pl -> !pl.isConnected());
		}
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent e) {
		if (protected_players.contains(e.getEntity()) && e.getCause() == DamageCause.FALL) {
			e.setCancelled(true);
			boolean removed = protected_players.remove(e.getEntity());
		}
	}
}

class LeviPadListener implements Listener {

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		Block block_under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (block_under.getType() == Material.PURPUR_BLOCK && !(p.hasPotionEffect(PotionEffectType.LEVITATION))) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard-positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (20 * 3), 2));
		}
	}
}
