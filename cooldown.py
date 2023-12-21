import time
import util

COOLDOWN_TIME = 60


def update_cooldown(id: int | str):
    with util.get_cooldowns() as cooldowns:
        cooldowns[str(id)] = time.time()


def is_on_cooldown(id: int | str):
    with util.get_cooldowns() as cooldowns:
        if str(id) in cooldowns:
            return (time.time() - cooldowns[str(id)]) < COOLDOWN_TIME
