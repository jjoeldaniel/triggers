import shelve
from discord import Message
from discord import ClientUser


def get_db():
    return shelve.open("triggers", writeback=True)


def should_reply(client: ClientUser | None, message: Message):
    return not message.author.bot and message.author != client
