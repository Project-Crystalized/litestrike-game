package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class PartyManager implements PluginMessageListener {
	private List<List<String>> partys = new ArrayList<>();

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
		if (!channel.equals("crystalized:main")) {
			return;
		}

		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String message1 = in.readUTF();
		if (!(message1.contains("Party"))) {
			return;
		}

		List<String> new_party = new ArrayList<>();
		while (true) {
			try {
				String player_name = in.readUTF();
				partys.removeIf(party -> party.contains(player_name));
				new_party.add(player_name);
			} catch (Exception e) {
				break;
			}
		}
		partys.add(new_party);
	}

	public List<String> generate_teams() {
		List<String> all_players = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			all_players.add(p.getName());
		}
		Collections.shuffle(all_players);
		if (partys.isEmpty()) {
			return all_players;
		}
		Collections.sort(partys, (party1, party2) -> {
			return party1.size() - party2.size();
		});

		// iam os sorry, this code is prolly so confusing ;-;
		List<String> new_list1 = new ArrayList<>();
		List<String> new_list2 = new ArrayList<>();
		// add partys
		boolean alternating = true;
		for (List<String> party : partys) {
			alternating = !alternating;
			for (String member : party) {
				if (alternating) {
					new_list1.add(member);
				} else {
					new_list2.add(member);
				}
			}
		}

		// add remaining people who arent in party
		for (String player : all_players) {
			if (!(new_list1.contains(player) || new_list2.contains(player))) {
				if (new_list1.size() > new_list2.size()) {
					new_list2.add(player);
				} else {
					new_list1.add(player);
				}
			}
		}
		Collections.reverse(new_list2);

		List<String> combined_list = new ArrayList<>();
		combined_list.addAll(new_list1);
		combined_list.addAll(new_list2);
		return combined_list;
	}

	public void clear_partys() {
		partys.clear();
	}

	public String print_partys() {
		String s = "";

		for (List<String> party : partys) {
			s += "\n next party:";
			for (String player : party) {
				s += "\n";
				s += player;
			}
		}

		return s;
	}
}
