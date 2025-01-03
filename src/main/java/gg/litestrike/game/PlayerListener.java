package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import static net.kyori.adventure.text.Component.text;

public class PlayerListener implements Listener {
	private LSChatRenderer chat_renderer = new LSChatRenderer();

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			return;
		}

		// the if condition checks if the player is rejoining
		if (gc.teams.wasInitialPlayer(event.getPlayer().getName()) == null) {
			// player isnt rejoining, so we dont allow join
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, text("A game is already in Progress.\n")
					.append(text("If you see this message, it is likely a bug, pls report it to the admins")));
		}
	}

	@EventHandler
	public void onPLayerQuit(PlayerQuitEvent e) {
		e.quitMessage(text(""));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.joinMessage(text(""));
		Player p = event.getPlayer();
		GameController gc = Litestrike.getInstance().game_controller;

		p.teleport(Litestrike.getInstance().mapdata.get_que_spawn(p.getWorld()));
		p.getInventory().clear();
		p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
		p.setFoodLevel(20);
		p.lookAt(Litestrike.getInstance().mapdata.get_placer_spawn(p.getWorld()), LookAnchor.EYES);
		p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1, false, false, true));

		if (gc == null) {
			p.setGameMode(GameMode.SURVIVAL);
			Litestrike.getInstance().qsb.show_que_scoreboard(p);

		} else {
			// if we are here, it means the player is rejoining
			p.setGameMode(GameMode.SPECTATOR);

			Team should_be_team = gc.teams.wasInitialPlayer(event.getPlayer().getName());

			// give player the scoreboard and bossbar again
			ScoreboardController.give_player_scoreboard(p, should_be_team, gc.teams, gc.game_reference);
			Litestrike.getInstance().bbd.showBossBar();

			if (should_be_team == null) {
				p.kick(text("a fatal logic error occured, pls report this as a bug"));
				Bukkit.getLogger().severe("fatal logic error occured:" +
						"a player was allowed to join during a game, but wasnt in any team previously. That should not be possible");
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		// prevent blocks from getting broken
		event.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getItem() != null && e.getItem().getType() == Material.POTION) {
			PotionMeta pm = (PotionMeta) e.getItem().getItemMeta();
			e.getPlayer().addPotionEffects(pm.getCustomEffects());
			if (e.getHand() == EquipmentSlot.HAND) {
				e.getPlayer().getInventory().setItemInMainHand(null);
			} else {
				e.getPlayer().getInventory().setItemInOffHand(null);
			}
		}
	}

	@EventHandler
	public void onChatEvent(AsyncChatEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.round_state == RoundState.GameFinished) {
			return;
		}

		String msg_text = PlainTextComponentSerializer.plainText().serialize(e.message());
		if (!msg_text.startsWith("@all") && !msg_text.startsWith("@a")) {
			e.viewers().removeAll(gc.teams.get_enemy_team_of(e.getPlayer()));
		}

		e.renderer(ChatRenderer.viewerUnaware(chat_renderer));
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		// if game isnt going, cancel event
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			e.setCancelled(true);
			return;
		}

		// if round isnt running, cancel event
		if (gc.round_state != RoundState.Running) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

}

class LSChatRenderer implements ChatRenderer.ViewerUnaware {
	@Override
	public Component render(Player source, Component sourceDisplayName, Component message) {
		Team t = Litestrike.getInstance().game_controller.teams.get_team(source);
		TextColor color;
		if (t == Team.Breaker) {
			color = TextColor.color(0x22fb30);
		} else {
			color = TextColor.color(0xfb3922);
		}
		return (text("<").append(sourceDisplayName).append(text("> ")).append(message)).color(color);
	}
}
