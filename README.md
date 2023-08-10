# Triggers

[![docker](https://img.shields.io/badge/docker-%232496ED.svg?&style=for-the-badge&logo=docker&logoColor=white)](https://hub.docker.com/repository/docker/jjoeldaniel/acm_trigger/general)

> A JDA bot to reply to user-defined phrases (e.g. "im crying")

## Table of Contents

- [Commands](#commands)
- [Bot Setup](#bot-setup)
  - [Setup](#setup)
  - [Running](#running)
- [Contributing](#contributing)
- [FAQ](#faq)
- [References](#references)

## Commands

| Command           | Description                |
|-------------------|----------------------------|
| `/trigger new`    | Adds new trigger           |
| `/trigger reset`  | Resets all stored triggers |
| `/trigger list`   | Lists all stored triggers  |
| `/trigger delete` | Deletes specified trigger  |
| `/trigger toggle` | Toggles trigger feature    |

## Bot Setup

1. Go to the [Discord Developer Dashboard](https://discord.com/developers/applications)
2. Register your bot
3. Enable Server Members Intent and Message Content Intent

### Setup

1. Clone the repository
2. Create a `.env` file in the main directory (copy the template provided in [.env.example](.env.example)):
3. Ensure you are using JDK 19

### Running

1. Build the project

   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. Run the project

   ```bash
   java -jar triggers.jar
   ```

3. To invite your bot,
   use the following link:

   ```terminal
   https://discord.com/api/oauth2/authorize?client_id=$DISCORD_CLIENT_ID&permissions=66560&scope=bot%20applications.commands
   ```

   and replace `$DISCORD_CLIENT_ID` with the bot ID

## Contributing

1. Create a new branch
2. Make your changes
3. Create a pull request

## FAQ

1. **"I can't invite my bot"**

   - Make sure you have the correct permissions (66560)
   - Make sure you have the correct scope (bot%20applications.commands)
   - Make sure you have the correct client ID

2. **"I can't run the bot"**

   - Make sure you have the correct token
   - Make sure you have the correct role IDs
   - Make sure you have the correct JDK version

3. **"How do I get my bot ID?"**

   - Go to Discord Developer Dashboard → Your bot → General Information → Application ID

4. **"How do I get my bot token?"**

   - Go to Discord Developer Dashboard → Your bot → Bot → Token → Copy

5. **"Why does the bot not respond to my messages?"**

   - Make sure you have the correct role IDs

6. **"How can I get my role ID?"**

   - Go to your server → Server Settings → Roles
   - Right-click on the role you want to get the ID of and select "Copy ID"

7. **"It says 'Class has been compiled by a more recent version of the Java Environment' when I try to run the bot"**

   - Make sure you have the correct JDK version. Download [JDK 19](https://www.oracle.com/java/technologies/downloads/#jdk19-windows) here.

## References

- [JDA](https://github.com/DV8FromTheWorld/JDA)
- [SLF4J](https://github.com/qos-ch/slf4j)
- [dotenv Java](https://github.com/cdimascio/dotenv-java)
- [fuzzywuzzy](https://github.com/xdrop/fuzzywuzzy)

---

Created with 💖 by **[acmcsuf.com](https://acmcsuf.com) com.acmcsuf.bot_committee**
