package gg.litestrike.game.mapfeatures;

import gg.litestrike.game.Litestrike;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Pad detection used by all pad listeners.
 * <p>
 * The old single-point check (block under the feet coordinate) missed players who stood
 * on a pad's edge, because the player hitbox is 0.6 blocks wide while the detection point
 * was one exact coordinate. The hitbox overlaps at most two block columns per axis, so
 * sampling the four hitbox corners covers every block column the player is touching.
 */
final class PadDetection {
	private PadDetection() {}

	static boolean isOnPad(Location loc, Material pad) {
		int y = sampleY(loc.getY());
		for (int[] col : footprintColumns(loc.getX(), loc.getZ())) {
			if (loc.getWorld().getBlockAt(col[0], y, col[1]).getType() == pad) {
				return true;
			}
		}
		return false;
	}

	// block level checked for the pad: the block below the one containing the feet.
	// Standing on top of a block at y -> that block. Kept identical to the old
	// getRelative(DOWN) behaviour so existing maps trigger unchanged.
	static int sampleY(double y) {
		return (int) Math.floor(y) - 1;
	}

	// the (possibly duplicated) block columns the 0.6x0.6 hitbox overlaps
	static int[][] footprintColumns(double x, double z) {
		return new int[][] {
				{(int) Math.floor(x - 0.3), (int) Math.floor(z - 0.3)},
				{(int) Math.floor(x + 0.3), (int) Math.floor(z - 0.3)},
				{(int) Math.floor(x - 0.3), (int) Math.floor(z + 0.3)},
				{(int) Math.floor(x + 0.3), (int) Math.floor(z + 0.3)},
		};
	}
}

class LaunchPadListener implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		if (PadDetection.isOnPad(e.getTo(), MapFeatures.launch_pad_block)) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.setVelocity(p.getLocation().getDirection().multiply(2));
			if (p.getPing() > 180) {
				MapFeatures.fall_protect_player(p, Litestrike.getInstance().gameConfig.launchPadHighPingProtection);
			} else {
				MapFeatures.fall_protect_player(p, Litestrike.getInstance().gameConfig.launchPadProtection);
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
		if (PadDetection.isOnPad(e.getTo(), MapFeatures.levi_pad_block) && !(p.hasPotionEffect(PotionEffectType.LEVITATION))) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (55), 2));
			MapFeatures.fall_protect_player(p, Litestrike.getInstance().gameConfig.leviPadProtection);
		}
	}
}

class AutoJumpPadListener implements Listener {
	// players are boosted at most once per cooldown while on the pad. Replaces the old
	// server-side velocity check (getVelocity() <= 0), which is unreliable for players
	// because their velocity is client-authoritative. This listener instance lives for
	// one map load, so the map is bounded by the players of one game.
	private static final int COOLDOWN_TICKS = 10;
	private final Map<UUID, Long> last_boost = new HashMap<>();

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR)
			return;
		if (!PadDetection.isOnPad(e.getTo(), MapFeatures.auto_jump_pad_block)) {
			return;
		}
		long now = p.getWorld().getGameTime();
		Long last = last_boost.get(p.getUniqueId());
		if (last != null && now - last < COOLDOWN_TICKS) {
			return;
		}
		last_boost.put(p.getUniqueId(), now);
		p.setVelocity(p.getVelocity().add(new Vector(0, 1.32, 0)));
		p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
		MapFeatures.fall_protect_player(p, Litestrike.getInstance().gameConfig.autoJumpPadProtection);
	}
}

class JumpPadListener implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() == GameMode.SPECTATOR)
			return;
		if (PadDetection.isOnPad(e.getTo(), MapFeatures.jump_pad_block) && !(p.hasPotionEffect(PotionEffectType.JUMP_BOOST))) {
			p.playSound(Sound.sound(Key.key("crystalized:effect.hazard_positive"), Sound.Source.AMBIENT, 1f, 1f));
			p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (20), 7));
			MapFeatures.fall_protect_player(p, Litestrike.getInstance().gameConfig.jumpPadProtection);
		}
	}
}
