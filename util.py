import shelve
from discord import Message
from discord import ClientUser


def get_reminders():
    return shelve.open("reminders", writeback=True)


def get_cooldowns():
    return shelve.open("cooldowns", writeback=True)


def should_reply(client: ClientUser | None, message: Message):
    return not message.author.bot and message.author != client


def clean_text(text: str):
    return text.lower().strip()
