#!/bin/python

import sys
import sqlite3
from pathlib import Path
from uuid import UUID
import requests

if len(sys.argv) < 2:
    print("error! no player name given")
    print("usage: ./print_specific_player.py <username>")
    print("example: ./print_specific_game.py cooltexture")
    sys.exit(1)

username = sys.argv[1]
print("printing out information on player with name: ", username)

try:
    data = requests.get(f"https://api.mojang.com/users/profiles/minecraft/{username}").json()
    uuid = UUID(data["id"])
except requests.JSONDecodeError as e:
    print(e)
    sys.exit(1)

con = sqlite3.connect(Path.home() / "databases/litestrike_db.sql")
cur = con.cursor()
res = cur.execute("SELECT placed_bombs, broken_bombs, kills, assists, gained_money, spent_money, bought_items, was_winner, damage_dealt FROM LsGamesPlayers WHERE player_uuid = ?;", (uuid.bytes,));

total_games = 0
total_placed_bombs = 0
total_broken_bombs = 0
total_kills = 0
total_assists = 0
total_gained_money = 0
total_spent_money = 0
total_bought_items = 0
total_wins = 0
total_damage = 0

for row in res.fetchall():
    placed_bombs, broken_bombs, kills, assists, gained_money, spent_money, bought_items, was_winner, damage_dealt = row

    total_games += 1
    total_placed_bombs += placed_bombs
    total_broken_bombs += broken_bombs
    total_kills += kills
    total_assists += assists
    total_gained_money += gained_money
    total_spent_money += spent_money
    total_wins += was_winner
    total_damage += damage_dealt if damage_dealt else 0

    total_bought_items += len(bought_items)

print("total_games: ", total_games)
print("wins: ", total_wins, f" {(total_wins/total_games)*100}% winrate")
print("placed_bombs: ", total_placed_bombs)
print("broken_bombs: ", total_broken_bombs)
print("kills: ", total_kills)
print("assists: ", total_assists)
print("spent_money: ", total_spent_money)
print("items bought: ", total_bought_items)
print()
print("damage_dealt: ", total_damage)
print("NOTE: the total damage statistic wasnt present in old versions of litestrike, so it might not include older games")
