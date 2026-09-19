package gg.litestrike.game;

import java.util.List;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import static com.github.retrooper.packetevents.protocol.player.EquipmentSlot.*;


public class ProtocolLibLib implements PacketListener {
	@Override
	public void onPacketSend(PacketSendEvent event){
		if(event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
			event.markForReEncode(true);
			WrapperPlayServerEntityMetadata metaWrapper = new WrapperPlayServerEntityMetadata(event);
			GameController gc = Litestrike.getInstance().game_controller;
			Player updated_player = get_player_by_entity_id(metaWrapper.getEntityId());
			if (gc == null
					|| updated_player == null) {
				return;
			}
			Team receiving_player_team = gc.teams.get_team(event.getUser().getUUID());
			Team updated_player_team = gc.teams.get_team(updated_player);
			if ((receiving_player_team != null) && (receiving_player_team != updated_player_team)) {
				return;
			}
			List<EntityData<?>> data = metaWrapper.getEntityMetadata();
			data.add(new EntityData<>(0, EntityDataTypes.BYTE, ((Integer) 0x40).byteValue()));
			metaWrapper.setEntityMetadata(data);
			return;
		}

		if(event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT){
			event.markForReEncode(true);
			WrapperPlayServerEntityEquipment equipWrapper = new WrapperPlayServerEntityEquipment(event);
			GameController gc = Litestrike.getInstance().game_controller;
			Player updated_player = get_player_by_entity_id(equipWrapper.getEntityId());
			if (gc == null
					|| updated_player == null
					|| (gc.teams.get_team(event.getUser().getUUID()) != null && gc.teams.get_team(updated_player) != gc.teams.get_team(event.getUser().getUUID()))
					|| !(gc.bomb instanceof InvItemBomb)
					|| !(updated_player.equals(((InvItemBomb) gc.bomb).player))) {
				return;
			}
			Bukkit.getLogger().warning("after return");
			for(Equipment e : equipWrapper.getEquipment()){
				if(e.getSlot() != HELMET && e.getSlot() != CHEST_PLATE && e.getSlot() != LEGGINGS && e.getSlot() != BOOTS) continue;
				ItemStack stack = SpigotConversionUtil.toBukkitItemStack(e.getItem());
				if(stack != null && stack.getType().name().toUpperCase().contains("LEATHER")){
					Bukkit.getLogger().warning("setting meta");
					LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
					meta.setColor(Color.fromRGB(0xff8530));
					stack.setItemMeta(meta);
				}
				e.setItem(SpigotConversionUtil.fromBukkitItemStack(stack));
			}
		}
	}
	/*
	public static PacketAdapter make_allys_glow() {
		return new PacketAdapter(Litestrike.getInstance(), PacketType.Play.Server.ENTITY_METADATA) {
			@Override
			public void onPacketSending(PacketEvent event) {
				GameController gc = Litestrike.getInstance().game_controller;
				PacketContainer packet = event.getPacket();
				Player updated_player = get_player_by_entity_id(packet.getIntegers().read(0));
				if (gc == null
					|| updated_player == null) {
					return;
				}
				Team receiving_player_team = gc.teams.get_team(event.getPlayer());
				Team updated_player_team = gc.teams.get_team(updated_player);
				if ((receiving_player_team != null) && (receiving_player_team != updated_player_team)) {
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
	 */

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
				if (i == null || (!(i.getItemMeta() instanceof LeatherArmorMeta))) {
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

	/*
	public static PacketAdapter change_bomb_carrier_armor_color() {
		return new PacketAdapter(Litestrike.getInstance(), PacketType.Play.Server.ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				GameController gc = Litestrike.getInstance().game_controller;
				PacketContainer packet = event.getPacket();
				Player updated_player = get_player_by_entity_id(packet.getIntegers().read(0));
				if (gc == null
						|| updated_player == null
						|| (gc.teams.get_team(event.getPlayer()) != null && gc.teams.get_team(updated_player) != gc.teams.get_team(event.getPlayer()))
						|| !(gc.bomb instanceof InvItemBomb)
						|| !(updated_player.equals(((InvItemBomb) gc.bomb).player))) {
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
	 */
}
