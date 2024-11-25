package gg.litestrike.game;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class MapFeatures {

	public static void levi_pads() {
		new BukkitRunnable() {
			@Override
			public void run() {

			}
		};
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
