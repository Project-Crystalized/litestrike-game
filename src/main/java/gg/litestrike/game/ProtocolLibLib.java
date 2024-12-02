package gg.litestrike.game;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;

public class ProtocolLibLib {

	public static PacketAdapter make_allys_glow() {
		return new PacketAdapter(Litestrike.getInstance(), PacketType.Play.Server.ENTITY_METADATA) {
			@Override
			public void onPacketSending(PacketEvent event) {
				GameController gc = Litestrike.getInstance().game_controller;
				PacketContainer packet = event.getPacket();
				Player updated_player = get_player_by_entity_id(packet.getIntegers().read(0));
				if (gc == null
						|| updated_player == null
						|| gc.teams.get_team(updated_player) != gc.teams.get_team(event.getPlayer())) {
					return;
				}
				event.setPacket(packet = packet.deepClone());
				List<WrappedDataValue> wrappedData = packet.getDataValueCollectionModifier().read(0);
				for (WrappedDataValue wdv : wrappedData) {
					if (wdv.getIndex() == 0) {
						byte b = (byte) wdv.getValue();
						b |= 0b01000000;
						wdv.setValue(b);
					}
				}
			}
		};
	}

	private static Player get_player_by_entity_id(int id) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getEntityId() == id) {
				return player;
			}
		}
		return null;
	}

	// refreshes all the armor colors, to correct bomb carrier color
	public static void update_armor() {
		for (Player p : Litestrike.getInstance().game_controller.teams.get_placers()) {
			PlayerInventory inv = p.getInventory();
			ItemStack[] items = { inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots() };
			for (ItemStack i : items) {
				if ((!(i.getItemMeta() instanceof LeatherArmorMeta)) || i == null) {
					continue;
				}
				LeatherArmorMeta im = (LeatherArmorMeta) i.getItemMeta();
				im.setColor(Color.fromRGB(im.getColor().asRGB() + 1));
				i.setItemMeta(im);
			}
			inv.setHelmet(items[0]);
			inv.setChestplate(items[1]);
			inv.setLeggings(items[2]);
			inv.setBoots(items[3]);
		}
	}

	public static PacketAdapter change_bomb_carrier_armor_color() {
		return new PacketAdapter(Litestrike.getInstance(), PacketType.Play.Server.ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				GameController gc = Litestrike.getInstance().game_controller;
				PacketContainer packet = event.getPacket();
				Player updated_player = get_player_by_entity_id(packet.getIntegers().read(0));
				if (gc == null
						|| updated_player == null
						|| gc.teams.get_team(updated_player) != gc.teams.get_team(event.getPlayer())
						|| !(gc.bomb instanceof InvItemBomb)
						|| !(updated_player.getInventory().equals(((InvItemBomb) gc.bomb).p_inv))) {
					return;
				}
				event.setPacket(packet = packet.deepClone());
				for (var slot : packet.getSlotStackPairLists().read(0)) {
					ItemStack stack = slot.getSecond();
					if (stack != null && stack.getType().name().contains("LEATHER")) {
						LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
						meta.setColor(Color.fromRGB(0xff8530));
						stack.setItemMeta(meta);
					}
				}
			}
		};
	};
}
