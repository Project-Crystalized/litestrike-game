#!/bin/python

import sqlite3
import uuid

con = sqlite3.connect("./litestrike_db.sql")

res = con.execute("SELECT * FROM LsGamesPLayers;");

row1 = res.fetchall()[1]

id1 = uuid.UUID(bytes=row1[0])
print(id1)

items = row1[8]

for byte in items:
    print(byte)
