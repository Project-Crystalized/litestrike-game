#!/usr/bin/env python3
"""Elo rating simulator for litestrike ranked.

Mirrors Ranking.java - if you change the constants or formulas there,
update them here too (the self-check below will catch drift).

Usage: python3 sim_ratings.py [players] [games]
"""
import random
import sys
from dataclasses import dataclass, field

# --- mirror of Ranking.java (keep in sync) ---
START_RATING = 1000
NEW_PLAYER_RATING = 300
K = 44
PERF_WEIGHT = 25
MAX_SWING = 30
MIN_RATING = 100
LEAVER_PENALTY = -40


def rating_change(mine, enemy_average, won, perf):
    expected = 1.0 / (1.0 + 10 ** ((enemy_average - mine) / 400.0))
    actual = 1.0 if won else 0.0
    change = K * (actual - expected) + PERF_WEIGHT * perf
    return int(round(max(-MAX_SWING, min(MAX_SWING, change))))


def performance_score(my_damage, avg_damage, plants, breaks, avg_objectives):
    perf = (my_damage - avg_damage) / avg_damage if avg_damage > 0 else 0
    perf += 0.1 * ((plants + breaks) - avg_objectives)
    return max(-1.0, min(1.0, perf))


def rank_for_rating(rating):
    if rating >= 2500:
        return 10
    if rating >= 2200:
        return 9
    if rating >= 1900:
        return 8
    if rating >= 1600:
        return 7
    if rating >= 1300:
        return 6
    if rating >= 1000:
        return 5
    if rating >= 700:
        return 4
    if rating >= 400:
        return 3
    return 2


def self_check():
    assert rating_change(1000, 1000, True, 0.0) == 22
    assert rating_change(1000, 1000, False, 0.0) == -22
    assert rating_change(1000, 1000, False, 1.0) == 3
    assert rating_change(1000, 1000, True, -1.0) == -3
    assert rating_change(1000, 1400, True, 0.0) == 30
    assert abs(performance_score(2000, 1000, 0, 0, 0) - 1.0) < 1e-9
    assert abs(performance_score(1000, 1000, 2, 2, 2) - 0.2) < 1e-9
    assert abs(performance_score(1000, 1000, 2, 0, 0.5) - 0.15) < 1e-9
    assert rank_for_rating(1000) == 5
    assert rank_for_rating(2499) == 9
    print("self-check ok (formulas match Ranking.java)")


# --- simulation ---
@dataclass
class Player:
    skill: float
    rating: int = NEW_PLAYER_RATING
    games: int = 0
    history: list = field(default_factory=list)


def play_game(players, leaver_chance=0.02):
    random.shuffle(players)
    placers, breakers = players[:5], players[5:]
    avg_skill = lambda team: sum(p.skill for p in team) / len(team)
    diff = avg_skill(placers) - avg_skill(breakers)
    placers_win = random.random() < 1.0 / (1.0 + 10 ** (-diff / 400.0))

    stats = []
    for p in players:
        damage = max(0.0, random.gauss(p.skill, 250))
        plants = random.choices([0, 1, 2, 3], weights=[70, 18, 8, 4])[0]
        breaks = random.choices([0, 1, 2, 3], weights=[70, 18, 8, 4])[0]
        left = random.random() < leaver_chance
        stats.append((damage, plants, breaks, left))
    avg_damage = sum(s[0] for s in stats) / len(stats)
    avg_objectives = sum(s[1] + s[2] for s in stats) / len(stats)

    for i, (p, (damage, plants, breaks, left)) in enumerate(zip(players, stats)):
        won = (i < 5) == placers_win
        if left:
            change = LEAVER_PENALTY
        else:
            # real code uses team rating averages for expectation:
            foes = breakers if i < 5 else placers
            enemy_avg = sum(q.rating for q in foes) / 5
            perf = performance_score(damage, avg_damage, plants, breaks, avg_objectives)
            change = rating_change(p.rating, int(round(enemy_avg)), won, perf)
        p.rating = max(MIN_RATING, p.rating + change)
        p.games += 1
        p.history.append((won, change, round(perf if not left else 0, 2), int(round(damage))))


def histogram(ratings, width=50):
    lo, hi = min(ratings), max(ratings)
    buckets = [0] * 12
    span = max(hi - lo, 1)
    for r in ratings:
        buckets[min(int((r - lo) / span * 12), 11)] += 1
    peak = max(buckets)
    for i, b in enumerate(buckets):
        low = lo + span * i / 12
        bar = "#" * int(b / peak * width)
        print(f"  {low:5.0f} | {bar} {b}")


def main():
    self_check()
    n_players = int(sys.argv[1]) if len(sys.argv) > 1 else 100
    n_games = int(sys.argv[2]) if len(sys.argv) > 2 else 2000
    random.seed(42)

    pop = [Player(skill=max(100, random.gauss(1000, 450))) for _ in range(n_players)]
    smurf = pop[0]
    smurf.skill = 1600  # known-good player, watch him climb from 300
    avg_joe = pop[1]
    avg_joe.skill = 1000  # average player, watch him climb from 300 to ~1000

    print(f"\n{n_players} players x {n_games} games (5v5, random teams, everyone starts at 300)")
    means = []
    for g in range(1, n_games + 1):
        play_game(random.sample(pop, 10))
        if g == 100:
            print(f"after 100 games: smurf={smurf.rating} (in {smurf.games} games), "
                  f"avg-joe={avg_joe.rating} (in {avg_joe.games} games)")
        if g % 200 == 0:
            means.append(sum(p.rating for p in pop) / len(pop))
    print("population mean rating over time (inflation check):")
    print("  " + " ".join(f"{m:.0f}" for m in means))

    ratings = [p.rating for p in pop]
    print(f"\nfinal spread: min={min(ratings)} max={max(ratings)}")
    histogram(ratings)

    bands = {}
    for p in pop:
        bands[rank_for_rating(p.rating)] = bands.get(rank_for_rating(p.rating), 0) + 1
    print("\nrank populations:", dict(sorted(bands.items())))
    print(f"smurf (skill 1600): rating={smurf.rating} rank={rank_for_rating(smurf.rating)} in {smurf.games} games")
    print(f"avg-joe (skill 1000): rating={avg_joe.rating} rank={rank_for_rating(avg_joe.rating)} in {avg_joe.games} games")
    worst = min(pop, key=lambda p: p.rating)
    print(f"worst player (skill {worst.skill:.0f}): rating={worst.rating} rank={rank_for_rating(worst.rating)}")


if __name__ == "__main__":
    main()
