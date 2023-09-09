package com.acmcsuf.triggers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;

public class Register extends ListenerAdapter {

        final static Dotenv dotenv = Dotenv.configure()
                        .ignoreIfMissing()
                        .load();

        @Override
        public void onGenericEvent(@NotNull GenericEvent event) {

                // Register commands (#updateCommands will CLEAR all commands, don't do this
                // more than once per startup)
                updateCommands(event);
        }

        /**
         * Updates bot commands in guild
         *
         * @param event GuildReadyEvent or GuildJoinEvent
         */
        private void updateCommands(GenericEvent event) {

                Guild guild;

                if (event instanceof GuildReadyEvent guildReadyEvent) {
                        guild = guildReadyEvent.getGuild();
                } else if (event instanceof GuildJoinEvent guildJoinEvent) {
                        guild = guildJoinEvent.getGuild();
                } else {
                        return;
                }

                // Registers guild from env file
                if (guild.getId().equals(dotenv.get("GUILD_ID"))) {
                        guild.updateCommands().addCommands(guildCommands()).queue((null),
                                        ((error) -> LoggerFactory.getLogger(Bot.class)
                                                        .info("Failed to update commands for " + guild.getName() + " ("
                                                                        + guild.getId()
                                                                        + ")")));
                }
                // Clears commands from other guilds
                else {
                        guild.updateCommands().queue();
                }
        }

        /**
         * Guild Commands List
         * <p>
         * All commands intended ONLY for guild usage are returned in a List
         * </p>
         *
         * @return List containing bot commands
         */
        private List<CommandData> guildCommands() {

                // List holding all guild commands
                List<CommandData> guildCommandData = new ArrayList<>();

                // Trigger subcommands
                SubcommandData help = new SubcommandData(com.acmcsuf.triggers.Commands.TRIGGER_HELP,
                                com.acmcsuf.triggers.Commands.TRIGGER_HELP_DESCRIPTION);
                SubcommandData reset = new SubcommandData(com.acmcsuf.triggers.Commands.TRIGGER_RESET,
                                com.acmcsuf.triggers.Commands.TRIGGER_RESET_DESCRIPTION);
                SubcommandData list = new SubcommandData(com.acmcsuf.triggers.Commands.TRIGGER_LIST,
                                com.acmcsuf.triggers.Commands.TRIGGER_LIST_DESCRIPTION);
                SubcommandData toggle = new SubcommandData(com.acmcsuf.triggers.Commands.TRIGGER_TOGGLE,
                                com.acmcsuf.triggers.Commands.TRIGGER_TOGGLE_DESCRIPTION).addOption(OptionType.BOOLEAN,
                                                com.acmcsuf.triggers.Commands.TRIGGER_TOGGLE_OPTION_NAME,
                                                com.acmcsuf.triggers.Commands.TRIGGER_TOGGLE_OPTION_DESCRIPTION, true);
                SubcommandData newTrigger = new SubcommandData(com.acmcsuf.triggers.Commands.TRIGGER_NEW,
                                com.acmcsuf.triggers.Commands.TRIGGER_NEW_DESCRIPTION).addOption(OptionType.STRING,
                                                com.acmcsuf.triggers.Commands.TRIGGER_NEW_OPTION_NAME,
                                                com.acmcsuf.triggers.Commands.TRIGGER_NEW_OPTION_DESCRIPTION, true);
                SubcommandData delete = new SubcommandData(com.acmcsuf.triggers.Commands.TRIGGER_DELETE,
                                com.acmcsuf.triggers.Commands.TRIGGER_DELETE_DESCRIPTION).addOption(OptionType.STRING,
                                                com.acmcsuf.triggers.Commands.TRIGGER_DELETE_OPTION_NAME,
                                                com.acmcsuf.triggers.Commands.TRIGGER_DELETE_OPTION_DESCRIPTION, true,
                                                true);

                // View subcommands
                SubcommandData view = new SubcommandData(com.acmcsuf.triggers.Commands.VIEW_OPTION_NAME,
                                com.acmcsuf.triggers.Commands.VIEW_DESCRIPTION).addOption(OptionType.USER,
                                                com.acmcsuf.triggers.Commands.VIEW_OPTION_NAME,
                                                com.acmcsuf.triggers.Commands.VIEW_OPTION_DESCRIPTION, true);

                guildCommandData.add(Commands.slash(com.acmcsuf.triggers.Commands.TRIGGER,
                                com.acmcsuf.triggers.Commands.TRIGGER_DESCRIPTION)
                                .addSubcommands(help, reset, list, toggle, newTrigger, delete));

                guildCommandData.add(Commands.slash(com.acmcsuf.triggers.Commands.VIEW,
                                com.acmcsuf.triggers.Commands.VIEW_DESCRIPTION).addSubcommands(view)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

                return guildCommandData;
        }
}