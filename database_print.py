#!/bin/python

import sqlite3
import uuid
import requests

con = sqlite3.connect("/home/nico/databases/litestrike_db.sql")

cur = con.cursor()

res = cur.execute("SELECT player_uuid, SUM(was_winner) FROM LsGamesPlayers GROUP BY player_uuid ORDER BY SUM(was_winner) DESC;");

for row in res.fetchall():
    id1 = uuid.UUID(bytes=row[0])
    try:
        data = requests.get(f"https://sessionserver.mojang.com/session/minecraft/profile/{id1}").json()

        print(data["name"], "wins: ", row[1])
    except requests.JSONDecodeError:
        print(id1, "wins: ", row[1])
