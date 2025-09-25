package gg.litestrike.game.mapfeatures;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

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
			if (p.getPing() > 180) {
				MapFeatures.fall_protect_player(p, (20 * 4));
			} else {
				MapFeatures.fall_protect_player(p, (20 * 2));
			}
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

class AutoJumpPadListener implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR)
			return;
		Block block_under = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (block_under.getType() == MapFeatures.auto_jump_pad_block && p.getVelocity().getY() <= 0) {
			p.setVelocity(p.getVelocity().add(new Vector(0, 1.32, 0)));
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			MapFeatures.fall_protect_player(p, 8);
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
			MapFeatures.fall_protect_player(p, 8);
		}
	}
}
