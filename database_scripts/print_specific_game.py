#!/bin/python

import sqlite3
import uuid
import requests
import sys
from datetime import datetime

if len(sys.argv) < 2:
    print("error! no game reference given")
    print("usage: ./print_specific_game.py <game_ref>")
    print("example: ./print_specific_game.py 280983420")
    sys.exit(1)

game_ref = int(sys.argv[1])
print("printing out information on game with reference: ", game_ref)

con = sqlite3.connect("/home/nico/databases/litestrike_db.sql")
cur = con.cursor()
res = cur.execute("SELECT * FROM LitestrikeGames WHERE game_ref = ?;", (game_ref,));

row = res.fetchone()
    # print(row)
print()
game_id, placer_wins, breaker_wins, timestamp, map, winner, gr = row
print("The game happened at: ", datetime.fromtimestamp(timestamp).strftime('%Y-%m-%d %H:%M:%S'))
print("The map was: ", map)
if winner:
    print("The winning team was: the placers!")
else:
    print("The winning team was: the breakers!")
print("The score was: Placers ", placer_wins, ":", breaker_wins, " Breakers")
print()

total_kills = 0
total_assists = 0
total_damage = 0
total_money_spent = 0
number_of_player = 0

##### player stats
res = cur.execute("SELECT * FROM LsGamesPlayers WHERE game = ?;", (game_id,));
for row in res.fetchall():
    print()
    uuid_bytes, game_id, placed_bombs, broken_bombs, kills, assists, gained_money, spent_money, bought_items, was_winner, damage_dealt = row

    uuid_player = uuid.UUID(bytes=uuid_bytes)
    try:
        print(f"https://sessionserver.mojang.com/session/minecraft/profile/{uuid_player}")
        data = requests.get(f"https://sessionserver.mojang.com/session/minecraft/profile/{uuid_player}").json()
        print("stats of player ", data["name"], f" ({uuid_player}) ")
    except requests.JSONDecodeError as e:
        # print(e)
        print("stats of player failed_to_load_name", f" ({uuid_player}) ")
    print(f"They placed {placed_bombs} bombs")
    print(f"They broke {broken_bombs} bombs")
    print(f"They got {kills} kills")
    print(f"They got {assists} assists")
    print(f"They gained {gained_money} money")
    print(f"They spent {spent_money} money")
    print(f"They dealt {damage_dealt} damage")
    if was_winner:
        print("They where part of the winning team")
    else:
        print("They where part of the loosing team")
    total_damage += damage_dealt if damage_dealt else 0
    total_kills += kills
    total_assists += assists
    total_money_spent += spent_money
    number_of_player += 1

##### aggregate stats
number_of_rounds = placer_wins + breaker_wins
print()
print()

print(f"total money spent: {total_money_spent}$ which is {total_money_spent/number_of_rounds} $/round, or {total_money_spent/number_of_player} $/player on avarage")
print(f"total assists: {total_assists} which is {total_assists/number_of_rounds} assists/round, or {total_assists/number_of_player} assists/player")
print(f"total kills: {total_kills} which is {total_kills/number_of_rounds} kills/round, or {total_kills/number_of_player} kills/player")
print(f"total damage dealt {total_damage} which is {total_damage/number_of_rounds} damage/round, or {total_damage/number_of_player} damage/player")
