package com.acmcsuf.triggers;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Trigger extends ListenerAdapter {

    public Trigger(List<String> authorizedRoleIDs) {
        this.authorizedRoleIDs = authorizedRoleIDs;
    }

    final static Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    List<String> authorizedRoleIDs;

    // Discord member ID : Set of trigger phrases
    HashMap<String, LinkedHashSet<String>> triggerMap = new HashMap<>();

    // Discord member ID : Trigger Activation (true = activated, false =
    // deactivated)
    HashMap<String, Boolean> triggerToggle = new HashMap<>();

    int min = 0;
    int max = 5;

    final int MAX_TRIGGERS = 50;
    final int MINIMUM_SECONDS_BETWEEN_MESSAGES = 30;

    // SLF4J Logger
    final Logger log = LoggerFactory.getLogger(Trigger.class);

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (event.getGuild().getId().equals(dotenv.get("GUILD_ID"))) {
            // Loads triggers from database
            try {
                Database.syncData(triggerMap, triggerToggle);
            } catch (SQLException e) {
                log.error("Failed to sync database", e);
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (!isValidInteraction(event)) {
            return;
        }

        if (!authenticateMemberByRole(event.getMember())) {
            EmbedBuilder builder = new EmbedBuilder()
                    .setColor(Color.red)
                    .setDescription("You are not authorized to use this feature!")
                    .setDescription("If you believe this is a mistake, contact your server admin.");

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        switch (event.getSubcommandName()) {

            case (Commands.TRIGGER_HELP) -> {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Trigger Commands")
                        .setColor(Color.green)
                        .addField("/trigger new", "Add trigger", false)
                        .addField("/trigger reset", "Removes trigger", false)
                        .addField("/trigger list", "Lists triggers", false)
                        .addField("/trigger delete", "Delete trigger", false)
                        .addField("/trigger toggle", "Toggles trigger feature", false);

                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
            case (Commands.TRIGGER_NEW) -> {
                // Takes string result of option ID matching "word"
                String triggerPhrase = event.getOption("word").getAsString().toLowerCase();

                if (triggerMap.containsKey(event.getMember().getId())) {
                    if (inSet(triggerPhrase, triggerMap.get(event.getMember().getId()))) {
                        EmbedBuilder builder = new EmbedBuilder()
                                .setColor(Color.red)
                                .setTitle("Error")
                                .setDescription("Duplicate trigger!");

                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                        return;
                    }
                    if (triggerMap.get(event.getMember().getId()).size() >= MAX_TRIGGERS) {
                        EmbedBuilder builder = new EmbedBuilder()
                                .setColor(Color.red)
                                .setTitle("Error")
                                .setDescription("Max triggers reached!");

                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                        return;
                    }
                }

                try {
                    Database.initializeIfNotExistsAndAppend(event.getMember(), triggerPhrase, triggerMap,
                            triggerToggle);
                } catch (SQLException e) {
                    log.error("Failed to append trigger", e);
                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("Error")
                            .setDescription("An error occurred while adding your trigger. Please try again later.");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                    return;
                }

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.green)
                        .setDescription("Trigger added: \"" + triggerPhrase + "\"");

                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
            case (Commands.TRIGGER_RESET) -> {
                if (triggerMap.get(event.getMember().getId()).isEmpty()) {
                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("Error")
                            .setDescription("No triggers to remove!");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                    return;
                }

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.red)
                        .setTitle("Are you sure you want to reset all triggers?");

                event.replyEmbeds(builder.build()).setActionRow(
                        Button.danger("reset", "Yes")).setEphemeral(true).queue();
            }
            case (Commands.TRIGGER_LIST) -> {
                // If no triggers are found
                if (!triggerMap.containsKey(event.getMember().getId()) || triggerMap.get(event.getMember().getId())
                        .isEmpty() || triggerMap.get(event.getMember().getId()) == null) {
                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("No triggers found");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                } else {
                    List<String> list = new ArrayList<>(triggerMap.get(event.getMember().getId()));
                    EmbedBuilder builder = triggerList(0, 5, list);

                    // If next page does not exist
                    if (list.size() <= 5) {
                        event.replyEmbeds(builder.build()).setActionRow(
                                Button.secondary("previous", "Previous").asDisabled(),
                                Button.secondary("next", "Next").asDisabled()).setEphemeral(true).queue();
                    } else {
                        event.replyEmbeds(builder.build()).setActionRow(
                                Button.secondary("previous", "Previous").asDisabled(),
                                Button.secondary("next", "Next").asEnabled()).setEphemeral(true).queue();
                    }
                }
            }
            case (Commands.TRIGGER_DELETE) -> {
                // Takes string result of option ID matching "word"
                String query = event.getOption("word").getAsString().toLowerCase();

                // Check if query is stored
                if (triggerMap.get(event.getMember().getId()) != null && inSet(query,
                        triggerMap.get(event.getMember().getId()))) {

                    triggerMap.get(event.getMember().getId()).remove(query);
                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.green)
                            .setDescription("Trigger deleted: \"" + query + "\"");

                    try {
                        Database.deletePhrase(event.getMember(), query, triggerMap, triggerToggle);
                    } catch (SQLException e) {
                        log.error("Failed to delete trigger or sync database", e);
                        EmbedBuilder error = new EmbedBuilder()
                                .setColor(Color.red)
                                .setTitle("Error")
                                .setDescription(
                                        "An error occurred while deleting your trigger. Please try again later.");

                        event.replyEmbeds(error.build()).setEphemeral(true).queue();
                        return;
                    }

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                } else {

                    String similarPhrase = null;

                    for (String str : triggerMap.get(event.getMember().getId())) {
                        if (FuzzySearch.ratio(str, query) > 80) {
                            similarPhrase = str;
                            break;
                        }
                    }

                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("Error")
                            .setDescription("Trigger not found!");

                    if (similarPhrase != null) {
                        builder.setDescription("Trigger not found! Did you mean \"" + similarPhrase + "\"?");
                    }

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            }
            case (Commands.TRIGGER_TOGGLE) -> {
                try {
                    Database.initializeIfNotExists(event.getMember());

                    boolean toggle = event.getOption("switch").getAsBoolean();
                    Database.toggleTrigger(event.getMember(), toggle);
                    triggerToggle.put(event.getMember().getId(), toggle);

                    EmbedBuilder builder = new EmbedBuilder();

                    if (toggle) {
                        builder.setTitle("Trigger features are now enabled");
                        builder.setColor(Color.green);
                    } else {
                        builder.setTitle("Trigger features are now disabled");
                        builder.setColor(Color.red);
                    }

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                } catch (SQLException e) {
                    log.error("Failed to check for stored user", e);
                    EmbedBuilder error = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("Error")
                            .setDescription("A database error has occurred. Please try again later.");

                    event.replyEmbeds(error.build()).setEphemeral(true).queue();
                    return;
                }
            }
            case (Commands.VIEW) -> {
                Member member = event.getOption("user").getAsMember();

                // If no triggers are found
                if (!triggerMap.containsKey(member.getId()) || triggerMap.get(member.getId())
                        .isEmpty() || triggerMap.get(member.getId()) == null) {
                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("No triggers found");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }

                List<String> list = new ArrayList<>(triggerMap.get(member.getId()));
                EmbedBuilder builder = new EmbedBuilder(triggerList(0, list.size(), list));

                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals(Commands.TRIGGER) && event.getFocusedOption().getName().equals("word")) {

            if (triggerMap.get(event.getMember().getId()) == null) {
                return;
            }

            String[] words = new String[triggerMap.get(event.getMember().getId()).size()];
            words = triggerMap.get(event.getMember().getId()).toArray(words);

            List<Command.Choice> options = Stream.of(words)
                    .filter(word -> word.startsWith(event.getFocusedOption().getValue()))
                    .map(word -> new Command.Choice(word, word))
                    .collect(Collectors.toList());

            event.replyChoices(options).queue();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        // Only listen to guild messages from live users
        if (!isValidInteraction(event)) {
            return;
        }

        String messageContent = event.getMessage().getContentRaw().toLowerCase();

        // Loop through HashMap keySet
        for (String id : triggerMap.keySet()) {

            // If members value contains messageContent
            if (inSet(messageContent, triggerMap.get(id))) {

                // Retrieve triggered member
                RestAction<Member> action = event.getGuild().retrieveMemberById(id);
                action.queue(

                        // Handle success
                        (member) -> {
                            sendTriggerMessage(event, member);
                        },

                        // Handle failure if the member does not exist (or another issue appeared)
                        (error) -> LoggerFactory.getLogger(Trigger.class).error(error.toString()));
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        if (!isValidInteraction(event)) {
            return;
        }

        List<String> list = new ArrayList<>(triggerMap.get(event.getMember().getId()));

        switch (event.getComponentId()) {
            // List Command
            case "previous" -> {

                // If not on first page
                if (min != 0) {

                    min -= 5;
                    max -= 5;

                    EmbedBuilder builder = triggerList(min, max, list);

                    // If new previous page is first page
                    if (min == 0) {
                        event.editMessageEmbeds(builder.build()).setActionRow(
                                Button.secondary("previous", "Previous").asDisabled(),
                                Button.secondary("next", "Next").asEnabled()).queue();
                    } else {
                        event.editMessageEmbeds(builder.build()).setActionRow(
                                Button.secondary("previous", "Previous").asEnabled(),
                                Button.secondary("next", "Next").asEnabled()).queue();
                    }
                }
                // If on first page
                else {

                    EmbedBuilder builder = triggerList(min, max, list);

                    // If there is a next page
                    if (list.size() <= 5) {

                        event.editMessageEmbeds(builder.build()).setActionRow(
                                Button.secondary("previous", "Previous").asDisabled(),
                                Button.secondary("next", "Next").asEnabled()).queue();
                    } else {

                        event.editMessageEmbeds(builder.build()).setActionRow(
                                Button.secondary("previous", "Previous").asDisabled(),
                                Button.secondary("next", "Next").asDisabled()).queue();
                    }
                }
            }
            case "next" -> {

                min += 5;
                max += 5;

                EmbedBuilder builder = triggerList(min, max, list);

                if (max >= list.size()) {
                    event.editMessageEmbeds(builder.build()).setActionRow(
                            Button.secondary("previous", "Previous").asEnabled(),
                            Button.secondary("next", "Next").asDisabled()).queue();
                } else {
                    event.editMessageEmbeds(builder.build()).setActionRow(
                            Button.secondary("previous", "Previous").asEnabled(),
                            Button.secondary("next", "Next").asEnabled()).queue();
                }

            }

            // Confirmation
            case "reset" -> {

                event.deferEdit().queue();
                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.green)
                        .setTitle("Triggers reset");

                triggerMap.get(event.getMember().getId()).clear();

                try {
                    Database.resetTriggers(event.getMember(), triggerMap, triggerToggle);
                } catch (SQLException e) {
                    log.error("Failed to reset user's triggers", e);
                    EmbedBuilder error = new EmbedBuilder()
                            .setColor(Color.red)
                            .setTitle("Error")
                            .setDescription("An error occurred while resetting your triggers. Please try again later.");

                    event.getHook().editOriginalEmbeds(error.build()).queue();
                    return;
                }

                event.getHook().editOriginalEmbeds(builder.build()).setActionRow(
                        Button.danger("reset", "Yes").asDisabled()).queue();
            }
        }
    }

    /**
     * Sends trigger message to member
     * 
     * @param event  MessageReceivedEvent
     * @param member Triggered member
     */
    void sendTriggerMessage(MessageReceivedEvent event, Member member) {

        // Skip if message is self-triggered or member is missing view permissions
        if (event.getMember() == member || !member.hasPermission(event.getGuildChannel(),
                Permission.VIEW_CHANNEL) || member == null) {
            return;
        }

        // If no toggle setting exists
        if (!triggerToggle.containsKey(event.getMember().getId())) {
            triggerToggle.put(event.getMember().getId(), true);
        }
        // If toggle == false, skip to next ID
        else if (!triggerToggle.get(event.getMember().getId())) {
            return;
        }

        /*
         * Spam Check
         * 
         * Spam check should be done by checking the last 50 messages in the channel and
         * comparing the
         * timestamps of the current triggering message and the last triggering message.
         * If the difference
         * is less than MINIMUM_SECONDS_BETWEEN_MESSAGES, the message should be ignored.
         */
        MessageHistory previousHistory = event.getChannel().getHistoryBefore(event.getMessageId(), 50)
                .complete();

        for (Message message : previousHistory.getRetrievedHistory()) {

            // Check if message contains trigger phrase
            if (inSet(message.getContentRaw().toLowerCase(), triggerMap.get(member.getId()))) {

                // Check if message was sent by the same user
                if (!message.getAuthor().getId().equals(member.getId())) {

                    // Check if the difference between the current message and the previous message
                    // is less than MINIMUM_SECONDS_BETWEEN_MESSAGES
                    long difference = event.getMessage().getTimeCreated().toEpochSecond()
                            - message.getTimeCreated()
                                    .toEpochSecond();

                    if (difference < MINIMUM_SECONDS_BETWEEN_MESSAGES) {
                        return;
                    }
                }
            }
        }

        // Embed
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Message Trigger")
                .setColor(Color.green)
                .setFooter("All timestamps are formatted in PST / UTC+7 !");

        // Retrieve last 4 messages in channel message history
        MessageHistory history = event.getChannel().getHistoryBefore(event.getMessageId(), 4).complete();
        List<String> messages = new ArrayList<>();

        // Add messages to list and reverse messages in order of least -> most recent
        for (Message message : history.getRetrievedHistory()) {
            String memberName = message.getAuthor().getName();
            messages.add(
                    "**[" + TimeFormat.TIME_LONG.atTimestamp(message.getTimeCreated().toEpochSecond() * 1000)
                            + "] " + memberName + ":** " + message.getContentRaw() + "\n");
        }
        Collections.reverse(messages);

        // Add trigger message
        String triggerMember = event.getMessage().getAuthor().getName();
        builder.addField("",
                "**[" + TimeFormat.TIME_LONG.now() + "] " + triggerMember + ":** " + event.getMessage()
                        .getContentRaw(),
                false);

        // Finish embed
        builder.setDescription(String.join("", messages));
        builder.addField("**Source Message**", "[Jump to](" + event.getJumpUrl() + ")", false);

        // DM triggered member
        member.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).addActionRow(
                        Button.secondary("server-id", "Server: " + event.getGuild().getName()).asDisabled()))
                .queue();
    }

    /**
     * Checks if Set contains String
     *
     * @param str String
     * @param set Containing Set
     * @return True if set contains String
     */
    boolean inSet(String str, LinkedHashSet<String> set) {
        for (String string : set) {
            if (str.equals(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if interaction is valid
     *
     * @param event Interaction/MessageReceived event
     * @return True if interaction is valid
     */
    boolean isValidInteraction(GenericEvent event) {

        if (event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
            return slashCommandInteractionEvent.getMember() != null && slashCommandInteractionEvent.isGuildCommand();
        } else if (event instanceof MessageReceivedEvent messageReceivedEvent) {
            String guild_id = dotenv.get("GUILD_ID");

            return messageReceivedEvent.getGuild().getId().equals(guild_id)
                    && messageReceivedEvent.getMember() != null
                    && messageReceivedEvent.isFromGuild()
                    && !messageReceivedEvent.getMember().getUser().isBot();
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            return buttonInteractionEvent.getMember() != null && buttonInteractionEvent.isFromGuild();
        } else {
            return false;
        }
    }

    /**
     * Checks if member contains an authorized role
     *
     * @param member Event member
     * @return True if member is authorized
     */
    boolean authenticateMemberByRole(Member member) {

        for (Role role : member.getRoles()) {
            if (authorizedRoleIDs.contains(role.getId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Template Embed for /trigger list
     *
     * <p>
     * Formats EmbedBuilder containing triggers from range1 to range2
     * </p>
     *
     * @param range1 Beginning list index
     * @param range2 Ending list index
     * @param list   List of member triggers
     * @return Embed Message
     */
    EmbedBuilder triggerList(int range1, int range2, List<String> list) {

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.green)
                .setTitle("Trigger List")
                .setFooter("Size: #" + list.size());

        for (int i = range1; i < list.size() && i < range2; ++i) {
            builder.addField("Trigger #" + (i + 1), list.get(i), false);
        }

        return builder;
    }

}
