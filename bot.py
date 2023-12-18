import discord
from discord import SlashCommandGroup
from discord.ext import commands
from dotenv import load_dotenv
import os
import calendar
import util

load_dotenv()

TOKEN = os.getenv("TOKEN")
if TOKEN is None:
    print("No token provided")
    exit(1)


debug_guild = [971225319153479790, 1131033337188855869]


intents = discord.Intents.default()
intents.message_content = True
bot = commands.Bot(intents=intents, debug_guild=debug_guild)
reminder = SlashCommandGroup(name="reminder", description="Reminder commands")


@bot.event
async def on_ready():
    if bot.user is not None:
        print(f"{bot.user.name} has connected to Discord!")


@bot.event
async def on_message(message: discord.Message):
    if not util.should_reply(bot, message):
        return

    with util.get_db() as db:
        for key in db:
            if key in message.content:
                for id in db[key]:
                    user = await bot.fetch_user(id)

                    messages = [
                        message async for message in message.channel.history(limit=5)
                    ]
                    messages.reverse()

                    content = [
                        f"<t:{calendar.timegm(message.created_at.timetuple())}:t> **{message.author.name}**: {message.content}"
                        for message in messages
                    ]

                    embed = discord.Embed(
                        title="Message Notification",
                        description="\n".join(content) + "\n\n" + message.jump_url,
                        color=0x00FF00,
                        timestamp=message.created_at,
                    )

                    await user.send(embed=embed)

    await bot.process_commands(message)


@reminder.command()
async def clear(ctx: discord.ApplicationContext):
    with util.get_db() as db:
        id = str(ctx.author.id)
        [db[key].remove(id) for key in db if id in db[key]]

    await ctx.respond("**Cleared all reminders**")


@reminder.command()
async def add(ctx: discord.ApplicationContext, phrase: str):
    if phrase is None:
        await ctx.respond("**No reminder provided**")
        return

    with util.get_db() as db:
        id = str(ctx.author.id)

        if phrase not in db:
            db[phrase] = set()

        if id in db[phrase]:
            await ctx.respond("**Reminder already exists**")
            return
        else:
            db[phrase].add(id)

    await ctx.respond(f"**Reminder added:** `{phrase}`")


@reminder.command()
async def list(ctx: discord.ApplicationContext):
    with util.get_db() as db:
        phrases = [f"`{key}`" for key in db if str(ctx.author.id) in db[key]]

        if len(phrases) == 0:
            await ctx.respond("**No reminders found**")
            return
        else:
            await ctx.respond("**Reminders:**\n\n" + "\n".join(phrases))


bot.run(str(TOKEN))
