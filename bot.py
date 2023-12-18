import discord
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


intents = discord.Intents.default()
intents.message_content = True
bot = commands.Bot(command_prefix="$", intents=intents)


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


@bot.command(name="reminder clear")
async def clear(ctx):
    with util.get_db() as db:
        id = str(ctx.author.id)
        [db[key].remove(id) for key in db if id in db[key]]

    await ctx.send("**Cleared all reminders**")


@bot.command(name="reminder add")
async def add(ctx, *args):
    if args is None:
        await ctx.send("**No reminder provided**")
        return

    phrase = " ".join(args)

    with util.get_db() as db:
        id = str(ctx.author.id)

        if phrase not in db:
            db[phrase] = set()

        if id in db[phrase]:
            await ctx.send("**Reminder already exists**")
            return
        else:
            db[phrase].add(id)

    await ctx.send(f"**Reminder added:** `{phrase}`")


@bot.command(name="reminder list")
async def list(ctx):
    with util.get_db() as db:
        phrases = [f"`{key}`" for key in db if str(ctx.author.id) in db[key]]

        if len(phrases) == 0:
            await ctx.send("**No reminders found**")
            return
        else:
            await ctx.send("**Reminders:**\n\n" + "\n".join(phrases))


bot.run(str(TOKEN))
