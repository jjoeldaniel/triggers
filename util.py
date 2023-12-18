import shelve
from discord.ext import commands
from discord import Message


def get_db():
    return shelve.open("triggers", writeback=True)


def should_reply(client: commands.bot.Bot, message: Message):
    return not message.author.bot and message.author != client.user
