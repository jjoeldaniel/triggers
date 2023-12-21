import discord
from dotenv import load_dotenv
import os
import calendar
import util
import cooldown

load_dotenv()

TOKEN = os.getenv("TOKEN")
if TOKEN is None:
    print("No token provided")
    exit(1)


intents = discord.Intents.default()
intents.message_content = True
bot = discord.Bot(intents=intents)
reminder = bot.create_group(name="reminder", description="Reminder commands")


@bot.command(description="Sends the bot's latency.")
async def ping(ctx):
    await ctx.respond(f"Pong! Latency is `{bot.latency}` ms", ephemeral=True)


@bot.event
async def on_ready():
    if bot.user is not None:
        print(f"{bot.user.name} has connected to Discord!")


@bot.event
async def on_message(message: discord.Message):
    if not util.should_reply(bot.user, message):
        return

    text = util.clean_text(message.content)

    with util.get_reminders() as db:
        for key in db:
            if key in text:
                for id in db[key]:
                    if cooldown.is_on_cooldown(id):
                        continue
                    else:
                        cooldown.update_cooldown(id)

                    user = await bot.fetch_user(id)

                    messages = [
                        message async for message in message.channel.history(limit=5)
                    ]
                    messages.reverse()

                    content = [
                        f"<t:{calendar.timegm(message.created_at.timetuple())}:t> **{message.author.name}**: {message.content.strip()}"
                        for message in messages
                    ]

                    embed = discord.Embed(
                        title="Message Notification",
                        description="\n".join(content) + "\n\n" + message.jump_url,
                        color=0x00FF00,
                        timestamp=message.created_at,
                    )

                    await user.send(embed=embed)


@reminder.command(description="Cleares a users stored reminders")
async def clear(ctx: discord.ApplicationContext):
    with util.get_reminders() as db:
        id = str(ctx.author.id)
        [db[key].remove(id) for key in db if id in db[key]]

    await ctx.respond("**Cleared all reminders**", ephemeral=True)


@reminder.command(description="Removes a reminder")
async def remove(ctx: discord.ApplicationContext, phrase: str):
    phrase = util.clean_text(phrase)

    if phrase is None:
        await ctx.respond("**No reminder provided**")
        return

    with util.get_reminders() as db:
        id = str(ctx.author.id)

        if phrase not in db:
            await ctx.respond("**Reminder does not exist**")
            return
        else:
            db[phrase].remove(id)

    await ctx.respond(f"**Reminder removed:** `{phrase}`", ephemeral=True)


@reminder.command(description="Adds a reminder")
async def add(ctx: discord.ApplicationContext, phrase: str):
    if phrase is None:
        await ctx.respond("**No reminder provided**")
        return

    phrase = util.clean_text(phrase)

    with util.get_reminders() as db:
        id = str(ctx.author.id)

        if phrase not in db:
            db[phrase] = set()

        if id in db[phrase]:
            await ctx.respond("**Reminder already exists**")
            return
        else:
            db[phrase].add(id)

    await ctx.respond(f"**Reminder added:** `{phrase}`", ephemeral=True)


@reminder.command(description="Lists all stored reminders")
async def list(ctx: discord.ApplicationContext):
    with util.get_reminders() as db:
        phrases = [f"`{key}`" for key in db if str(ctx.author.id) in db[key]]

        if len(phrases) == 0:
            await ctx.respond("**No reminders found**", ephemeral=True)
            return
        else:
            await ctx.respond("**Reminders:**\n\n" + "\n".join(phrases), ephemeral=True)


bot.run(str(TOKEN))
